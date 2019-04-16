package com.herocraftonline.heroes.characters.skill.reborn.chronomancer;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainHealthEvent;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectStack;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.Stacking;
import com.herocraftonline.heroes.characters.effects.common.WalkSpeedPercentDecreaseEffect;
import com.herocraftonline.heroes.characters.effects.common.WalkSpeedPercentIncreaseEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SkillTimeShift extends TargettedSkill {

    private static final float DEFAULT_MINECRAFT_MOVEMENT_SPEED = 0.2f;

    private String upShiftApplyText;
    private String upShiftExpireText;
    private String downShiftApplyText;
    private String downShiftExpireText;

    private String accelEffectName = "AcceleratedShiftedTime";
    private String decelEffectName = "DeceleratedShiftedTime";

    public SkillTimeShift(Heroes plugin) {
        super(plugin, "TimeShift");
        setDescription("You focus your temporal powers on a target, shifting their time. " +
                "If used on ally, it will instantly heal them for $1 health and increase their movement speed by $2%. " +
                "If used on an enemy, it will instantly deal $3 damage and decrease their movement speed by $4%. " +
                "The movement speed effects can stack up to $5 times and lasts up to a maximum of $6 seconds.");
        setUsage("/skill timeshift");
        setArgumentRange(0, 0);
        setIdentifiers("skill timeshift");
        setTypes(SkillType.MULTI_GRESSIVE, SkillType.NO_SELF_TARGETTING, SkillType.MOVEMENT_INCREASING, SkillType.MOVEMENT_SLOWING);
    }

    @Override
    public String getDescription(Hero hero) {
        double healing = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING, 25.0, false);
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 25.0, false);

        double speedIncrease = SkillConfigManager.getUseSetting(hero, this, "ally-percent-speed-increase", 0.1, false);
        double speedDecrease = SkillConfigManager.getUseSetting(hero, this, "enemy-percent-speed-decrease", 0.1, false);

        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
        int maxStacks = SkillConfigManager.getUseSetting(hero, this, "max-stacks", 10, false);

        return getDescription()
                .replace("$1", Util.decFormat.format(healing))
                .replace("$2", Util.decFormat.format(speedIncrease))
                .replace("$3", Util.decFormat.format(damage))
                .replace("$4", Util.decFormat.format(speedDecrease))
                .replace("$5", Util.decFormat.format(maxStacks))
                .replace("$6", Util.decFormat.format((double) duration / 1000.0));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.DAMAGE.node(), 20);
        config.set(SkillSetting.HEALING.node(), 20);
        config.set("ally-percent-speed-increase", 0.05);
        config.set("enemy-percent-speed-decrease", 0.05);
        config.set("max-stacks", 5);
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

        if (hero.isAlliedTo(target)) {
            return acceleratedShift(player, hero, target, ctTarget, duration, maxStacks);
        }
        return deceleratedShift(player, hero, target, ctTarget, duration, maxStacks);
    }

    private SkillResult deceleratedShift(Player player, Hero hero, LivingEntity target, CharacterTemplate targetCT, int duration, int maxStacks) {
        broadcastExecuteText(hero, target);

        double speedDecrease = SkillConfigManager.getUseSetting(hero, this, "enemy-percent-speed-decrease", 0.1, false);
        boolean addedNewStack = targetCT.addEffectStack(
                decelEffectName, this, player, duration,
                () -> new DeceleratedShiftedTime(this, player, duration, speedDecrease, maxStacks)
        );

        if (!addedNewStack) {
            player.sendMessage(ChatColor.WHITE + "Your target is already shifted as far as they can go!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        } else {
            DeceleratedShiftedTime effect = (DeceleratedShiftedTime) targetCT.getEffect(decelEffectName);
            int stackCount = effect.getStackCount();
            player.sendMessage(ChatComponents.GENERIC_SKILL + ChatColor.GOLD + targetCT.getName() + "now has shifted " + stackCount + " times!");
        }

        if (!damageCheck(player, target))
            return SkillResult.INVALID_TARGET;

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 20.0, false);
        addSpellTarget(target, hero);
        damageEntity(target, player, damage, EntityDamageEvent.DamageCause.MAGIC, false);

        World world = target.getWorld();
        Location location = target.getLocation();
        world.spawnParticle(Particle.REDSTONE, location, 45, 0.6, 1, 0.6, 0, new Particle.DustOptions(Color.YELLOW, 1));
        world.playSound(location, Sound.BLOCK_BEACON_DEACTIVATE, 0.5F, 2.0F);
        return SkillResult.NORMAL;
    }

    private SkillResult acceleratedShift(Player player, Hero hero, LivingEntity target, CharacterTemplate targetCT, int duration, int maxStacks) {
        broadcastExecuteText(hero, target);

        double healing = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING, 20.0, false);

        targetCT.tryHeal(hero, this, healing);  // Ignore failures

        double speedIncrease = SkillConfigManager.getUseSetting(hero, this, "ally-percent-speed-increase", 0.1, false);
        boolean addedNewStack = targetCT.addEffectStack(
                accelEffectName, this, player, duration,
                () -> new AcceleratedShiftedTime(this, player, duration, speedIncrease, maxStacks)
        );

        if (!addedNewStack) {
            player.sendMessage(ChatColor.WHITE + "Your target is already shifted as far as they can go!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        } else {
            AcceleratedShiftedTime effect = (AcceleratedShiftedTime) targetCT.getEffect(accelEffectName);
            int stackCount = effect.getStackCount();
            player.sendMessage(ChatComponents.GENERIC_SKILL + ChatColor.GOLD + targetCT.getName() + " now has shifted " + stackCount + " times!");
        }

        World world = target.getWorld();
        Location location = target.getLocation();
        world.spawnParticle(Particle.REDSTONE, location, 45, 0.6, 1, 0.6, 0, new Particle.DustOptions(Color.TEAL, 1));
        world.playSound(location, Sound.BLOCK_BEACON_ACTIVATE, 1.0F, 1.7F);
        return SkillResult.NORMAL;
    }

    private class DeceleratedShiftedTime extends WalkSpeedPercentDecreaseEffect implements Stacking {
        private final EffectStack effectStack;
        private final double decreasePerStack;
        private boolean stackCountChanged = false;

        DeceleratedShiftedTime(Skill skill, Player applier, int duration, double decreasePerStack, int maxStacks) {
            super(skill, decelEffectName, applier, duration, decreasePerStack, downShiftApplyText, downShiftExpireText);
            this.decreasePerStack = decreasePerStack;

            types.add(EffectType.HARMFUL);
            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.TEMPORAL);

            effectStack = new EffectStack(maxStacks);
        }

        @Override
        public void applyToMonster(Monster monster) {
            addPotionEffect(new PotionEffect(PotionEffectType.SLOW, (int) (getDuration() / 50), getStackCount()));
            super.applyToMonster(monster);
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

    private class AcceleratedShiftedTime extends WalkSpeedPercentIncreaseEffect implements Stacking {

        private final EffectStack effectStack;
        private final double increasePerStack;
        private boolean stackCountChanged = false;

        AcceleratedShiftedTime(Skill skill, Player applier, int duration, double increasePerStack, int maxStacks) {
            super(skill, accelEffectName, applier, duration, increasePerStack, upShiftApplyText, upShiftExpireText);
            this.increasePerStack = increasePerStack;

            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.TEMPORAL);

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
