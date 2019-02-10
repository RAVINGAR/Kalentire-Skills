package com.herocraftonline.heroes.characters.skill.reborn;

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
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SkillTimeShift extends TargettedSkill {

    private static final float DEFAULT_MINECRAFT_MOVEMENT_SPEED = 0.2f;

//    private String applyText;
//    private String expireText;

    private Song skillSong;

    public SkillTimeShift(Heroes plugin) {
        super(plugin, "TimeShift");
        setDescription("You tap into the web of time around you in a $1 radius, accelerating anyone and anything possible for $2 seconds.");
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
        return getDescription().replace("$1", radius + "").replace("$2", formattedDuration);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set("ally-percent-speed-increase", 0.05);
        config.set("enemy-percent-speed-decrease", 0.05);
        config.set("max-stacks", 5);
        config.set(SkillSetting.DURATION.node(), 10000);
        return config;
    }

//    @Override
//    public void init() {
//        super.init();
//
////        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%hero% is accelerating time!").replace("%hero%", "$1");
////        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%hero% is no longer accelerating time.").replace("%hero%", "$1");
////        setUseText(null);
//    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        CharacterTemplate ctTarget = plugin.getCharacterManager().getCharacter(target);
        if (ctTarget == null)
            return SkillResult.INVALID_TARGET;

        if (ctTarget.hasEffect("TimeWarded")) {
            player.sendMessage(ChatColor.WHITE + "Unable to shift " + target.getName() + "'s time. They are currently warded against time altering effects!");
        }

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
        int maxStacks = SkillConfigManager.getUseSetting(hero, this, "max-stacks", 10, false);

        if (ctTarget instanceof Hero) {
            Hero targetHero = (Hero) ctTarget;
            if (hero.getParty() != null && hero.getParty().isPartyMember(targetHero)) {

                AcceleratedShiftedTime effect;
                if (!targetHero.hasEffect("AcceleratedShiftedTime")) {
                    double percentIncrease = SkillConfigManager.getUseSetting(hero, this, "ally-percent-speed-increase", 0.1, false);
                    effect = new AcceleratedShiftedTime(this, player, duration, toFlatSpeedModifier(percentIncrease), maxStacks);
                    targetHero.addEffect(effect);
                } else {
                    effect = (AcceleratedShiftedTime) targetHero.getEffect("AcceleratedShiftedTime");
                }
                effect.addStack(this, player, duration);

                broadcastExecuteText(hero, target);
                return SkillResult.NORMAL;
            }
        }

        if (!damageCheck(player, target))
            return SkillResult.INVALID_TARGET;

        DeceleratedShiftedTime effect;
        if (!ctTarget.hasEffect("DeceleratedShiftedTime")) {
            double speedDecrease = SkillConfigManager.getUseSetting(hero, this, "enemy-percent-speed-decrease", 0.1, false);
            effect = new DeceleratedShiftedTime(this, player, duration, toFlatSpeedModifier(speedDecrease), maxStacks);
            ctTarget.addEffect(effect);
        } else {
            effect = (DeceleratedShiftedTime) ctTarget.getEffect("DeceleratedShiftedTime");
        }
        effect.addStack(this, player, duration);

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
            super(skill, "DeceleratedShiftedTime", applier, duration, decreasePerStack, "DEBUG: Shifting down", "DEBUG: Back to normal");
            this.decreasePerStack = decreasePerStack;

            types.add(EffectType.HARMFUL);
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
            if (effectStack != null) {
                int added = effectStack.add(skill, applier, duration, amount);
                if (added > 0) {
                    stackCountChanged = true;
                }
                return added;
            }
            return 0;
        }

        @Override
        public int removeStacks(int amount) {
            if (effectStack != null) {
                int removed = effectStack.remove(amount);
                if (removed > 0) {
                    stackCountChanged = true;
                }
                return removed;
            }
            return 0;
        }

        @Override
        public int removeAllStacks() {
            if (effectStack != null) {
                int removed = effectStack.removeAll();
                if (removed > 0) {
                    stackCountChanged = true;
                }
                return removed;
            }
            return 0;
        }

        @Override
        public int refresh(CharacterTemplate character) {
            character.getEntity().sendMessage("DEBUG: Refresh called...");
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
            super(skill, "AcceleratedShiftedTime", applier, duration, increasePerStack, "DEBUG: Shifting Up", "DEBUG: Back to normal");
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
            if (effectStack != null) {
                int added = effectStack.add(skill, applier, duration, amount);
                if (added > 0) {
                    stackCountChanged = true;
                }
                return added;
            }
            return 0;
        }

        @Override
        public int removeStacks(int amount) {
            if (effectStack != null) {
                int removed = effectStack.remove(amount);
                if (removed > 0) {
                    stackCountChanged = true;
                }
                return removed;
            }
            return 0;
        }

        @Override
        public int removeAllStacks() {
            if (effectStack != null) {
                int removed = effectStack.removeAll();
                if (removed > 0) {
                    stackCountChanged = true;
                }
                return removed;
            }
            return 0;
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
