package com.herocraftonline.heroes.characters.skill.reborn.chronomancer;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectStack;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.Stacking;
import com.herocraftonline.heroes.characters.effects.common.SoundEffect.Song;
import com.herocraftonline.heroes.characters.effects.common.WalkSpeedDecreaseEffect;
import com.herocraftonline.heroes.characters.effects.common.WalkSpeedIncreaseEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SkillTimeShift extends TargettedSkill {

    private static final float DEFAULT_MINECRAFT_MOVEMENT_SPEED = 0.2f;

//    private String applyText;
//    private String expireText;

    private String accelEffectName = "AcceleratedShiftedTime";
    private String decelEffectName = "DeceleratedShiftedTime";

    public SkillTimeShift(Heroes plugin) {
        super(plugin, "TimeShift");
        setDescription("You tap into the web of time around you in a $1 radius, accelerating anyone and anything possible for $2 second(s).");
        setUsage("/skill timeshift");
        setArgumentRange(0, 0);
        setIdentifiers("skill timeshift");
        setTypes(SkillType.MULTI_GRESSIVE, SkillType.NO_SELF_TARGETTING, SkillType.MOVEMENT_INCREASING, SkillType.MOVEMENT_SLOWING);
    }

    @Override
    public String getDescription(Hero hero) {
        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 16, false);
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
        String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription()
                .replace("$1", radius + "")
                .replace("$2", formattedDuration);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set("ally-percent-speed-increase", 0.05);
        config.set("enemy-percent-speed-decrease", 0.05);
        config.set("max-stacks", 10);
        config.set(SkillSetting.DURATION.node(), 10000);
        return config;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        CharacterTemplate ctTarget = plugin.getCharacterManager().getCharacter(target);
        if (ctTarget == null)
            return SkillResult.INVALID_TARGET;

        if (ctTarget.hasEffect("TemporallyWarded")) {
            player.sendMessage(ChatColor.WHITE + "Unable to shift " + target.getName() + "'s time. They are currently warded against time altering effects!");
            return SkillResult.INVALID_TARGET;
        }

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
        int maxStacks = SkillConfigManager.getUseSetting(hero, this, "max-stacks", 10, false);

        if (ctTarget instanceof Hero) {
            Hero targetHero = (Hero) ctTarget;
            if (hero.getParty() != null && hero.getParty().isPartyMember(targetHero)) {
                return acceleratedShift(player, hero, target, targetHero, duration, maxStacks);
            }
        }

        return deceleratedShift(player, hero, target, ctTarget, duration, maxStacks);
    }

    private SkillResult deceleratedShift(Player player, Hero hero, LivingEntity target, CharacterTemplate ctTarget, int duration, int maxStacks) {
        if (!damageCheck(player, target))
            return SkillResult.INVALID_TARGET;

        double speedDecrease = SkillConfigManager.getUseSetting(hero, this, "enemy-percent-speed-decrease", 0.1, false);
        boolean addedNewStack = ctTarget.addEffectStack(
                decelEffectName, this, player, duration,
                () -> new DeceleratedShiftedTime(this, player, duration, toFlatSpeedModifier(speedDecrease), maxStacks)
        );

        if (!addedNewStack) {
            player.sendMessage(ChatColor.WHITE + "Your target is already shifted as far as they can go!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        World world = target.getWorld();
        Location location = target.getLocation();
        world.spawnParticle(Particle.REDSTONE, location, 45, 0.6, 1, 0.6, 0, new Particle.DustOptions(Color.YELLOW, 1));
        world.playSound(location, Sound.BLOCK_BEACON_DEACTIVATE, 0.5F, 2.0F);
        broadcastExecuteText(hero, target);
        return SkillResult.NORMAL;
    }

    //TODO: Make this method work like the decelerateShift method does, but test decel first.
    private SkillResult acceleratedShift(Player player, Hero hero, LivingEntity target, Hero targetHero, int duration, int maxStacks) {
        AcceleratedShiftedTime effect;
        if (!targetHero.hasEffect(accelEffectName)) {
            double percentIncrease = SkillConfigManager.getUseSetting(hero, this, "ally-percent-speed-increase", 0.1, false);
            effect = new AcceleratedShiftedTime(this, player, duration, toFlatSpeedModifier(percentIncrease), maxStacks);
            targetHero.addEffect(effect);
        } else {
            effect = (AcceleratedShiftedTime) targetHero.getEffect(accelEffectName);
            if (effect.effectStack.hasMax()) {
                player.sendMessage(ChatColor.WHITE + "Your target is already shifted as far as they can go!");
                return SkillResult.INVALID_TARGET_NO_MSG;
            }
        }
        effect.addStack(this, player, duration);

        World world = target.getWorld();
        Location location = target.getLocation();
        world.spawnParticle(Particle.REDSTONE, location, 45, 0.6, 1, 0.6, 0, new Particle.DustOptions(Color.TEAL, 1));
        world.playSound(location, Sound.BLOCK_BEACON_ACTIVATE, 1.0F, 1.7F);

        broadcastExecuteText(hero, target);
        return SkillResult.NORMAL;
    }

    private double toFlatSpeedModifier(double percent) {
        return DEFAULT_MINECRAFT_MOVEMENT_SPEED * percent;
    }

    private class DeceleratedShiftedTime extends WalkSpeedDecreaseEffect implements Stacking {

        private final EffectStack effectStack;
        private final double decreasePerStack;
        private boolean stackCountChanged = false;

        DeceleratedShiftedTime(Skill skill, Player applier, int duration, double decreasePerStack, int maxStacks) {
            super(skill, decelEffectName, applier, duration, decreasePerStack, "DEBUG: Shifting down", "DEBUG: Back to normal");
            this.decreasePerStack = decreasePerStack;

            types.add(EffectType.HARMFUL);
            types.add(EffectType.MAGIC);

            effectStack = new EffectStack(maxStacks);
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            removeAllStacks();
        }

        @Override
        public int getStackCount() {
            return effectStack.count();
        }

        @Override
        public int getMaxStacks() {
            return effectStack.getMax();
        }

        public EffectStack.Entry getStackEntry(int index) {
            return effectStack.get(index);
        }

        @Override
        public int addStacks(Skill skill, Player applier, long duration, int amount) {
            if (effectStack == null) {
                return 0;
            }

            int added = effectStack.add(skill, applier, duration, amount);
            if (added > 0) {
                effectStack.resetAllStackDurationsToFull();
                stackCountChanged = true;
                setDuration(duration);
            }
            return added;
        }

        @Override
        public int removeStacks(int amount) {
            if (effectStack == null) {
                return 0;
            }

            int removed = effectStack.remove(amount);
            if (removed > 0) {
                stackCountChanged = true;
            }
            return removed;
        }

        @Override
        public int removeAllStacks() {
            if (effectStack == null)
                return 0;

            int removed = effectStack.removeAll();
            if (removed > 0) {
                stackCountChanged = true;
            }
            return removed;
        }

        @Override
        public int refresh(CharacterTemplate character) {
            removeExpiredStacks();
            if (stackCountChanged) {
                stackCountChangedOnCharacter(character);
            }
            return getStackCount();
        }

        void stackCountChangedOnCharacter(CharacterTemplate character) {
            if (character instanceof Hero) {
                setDelta(decreasePerStack * getStackCount());
                syncTask((Hero) character);
            }
//            else if (character instanceof Monster) {
//                // Do nothing for now...
//            }
        }

        private void removeExpiredStacks() {
            if (effectStack != null && effectStack.removeExpired() > 0) {
                stackCountChanged = true;
            }
        }
    }

    private class AcceleratedShiftedTime extends WalkSpeedIncreaseEffect implements Stacking {

        private final EffectStack effectStack;
        private final double increasePerStack;
        private boolean stackCountChanged = false;

        AcceleratedShiftedTime(Skill skill, Player applier, int duration, double increasePerStack, int maxStacks) {
            super(skill, accelEffectName, applier, duration, increasePerStack, "DEBUG: Shifting Up", "DEBUG: Back to normal");
            this.increasePerStack = increasePerStack;

            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.MAGIC);

            effectStack = new EffectStack(maxStacks);
        }

        @Override
        public int getStackCount() {
            return effectStack.count();
        }

        @Override
        public int getMaxStacks() {
            return effectStack.getMax();
        }

        public EffectStack.Entry getStackEntry(int index) {
            return effectStack.get(index);
        }

        @Override
        public int addStacks(Skill skill, Player applier, long duration, int amount) {
            if (effectStack == null) {
                return 0;
            }

            int added = effectStack.add(skill, applier, duration, amount);
            if (added > 0) {
                effectStack.resetAllStackDurationsToFull();
                stackCountChanged = true;
                setDuration(duration);
            }
            return added;
        }

        @Override
        public int removeStacks(int amount) {
            if (effectStack == null)
                return 0;

            int removed = effectStack.remove(amount);
            if (removed > 0) {
                stackCountChanged = true;
            }
            return removed;
        }

        @Override
        public int removeAllStacks() {
            if (effectStack == null)
                return 0;

            int removed = effectStack.removeAll();
            if (removed > 0) {
                stackCountChanged = true;
            }
            return removed;
        }

        @Override
        public int refresh(CharacterTemplate character) {
            removeExpiredStacks();
            if (stackCountChanged) {
                stackCountChangedOnCharacter(character);
            }
            return getStackCount();
        }

        void stackCountChangedOnCharacter(CharacterTemplate character) {
            if (character instanceof Hero) {
                setDelta(increasePerStack * getStackCount());
                syncTask((Hero) character);
            }
//            else if (character instanceof Monster) {
//                // Do nothing for now...
//            }
        }

        private void removeExpiredStacks() {
            if (effectStack != null && effectStack.removeExpired() > 0) {
                stackCountChanged = true;
            }
        }
    }
}
