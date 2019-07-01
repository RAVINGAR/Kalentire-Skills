package com.herocraftonline.heroes.characters.skill.reborn.chronomancer;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.CustomNameManager;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.common.WalkSpeedPercentDecreaseEffect;
import com.herocraftonline.heroes.characters.effects.common.WalkSpeedPercentIncreaseEffect;
import com.herocraftonline.heroes.characters.effects.common.interfaces.Stacked;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SkillTimeShift extends TargettedSkill {
    public static String skillName = "TimeShift";

    private String upShiftApplyText;
    private String upShiftExpireText;
    private String downShiftApplyText;
    private String downShiftExpireText;

    private String accelEffectName = "AcceleratedShiftedTime";
    private String decelEffectName = "DeceleratedShiftedTime";
    private String maxStacksErrorMessage = ChatComponents.GENERIC_SKILL + "$1 is already shifted as far as they can go!";

    public SkillTimeShift(Heroes plugin) {
        super(plugin, "TimeShift");
        setDescription("You focus your temporal powers on a target, shifting their time. " +
                "If used on ally, it will instantly heal them for $1 health and increase their movement speed by $2%. " +
                "If used on an enemy, it will instantly deal $3 damage and decrease their movement speed by $4%. " +
                "The movement speed effects can stack up to $5 times and lasts up to a maximum of $6 seconds.");
        setUsage("/skill timeshift <ally>");
        setIdentifiers("skill timeshift");
        setArgumentRange(0, 1);
        setTypes(SkillType.ABILITY_PROPERTY_TEMPORAL, SkillType.MULTI_GRESSIVE, SkillType.NO_SELF_TARGETTING, SkillType.DEFENSIVE_NAME_TARGETTING_ENABLED,
                SkillType.MOVEMENT_INCREASING, SkillType.MOVEMENT_SLOWING, SkillType.SILENCEABLE);
    }

    @Override
    public String getDescription(Hero hero) {
        double healing = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.HEALING, false);
        double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE, false);

        double speedIncrease = SkillConfigManager.getUseSetting(hero, this, "ally-percent-speed-increase", 0.1, false);
        double speedDecrease = SkillConfigManager.getUseSetting(hero, this, "enemy-percent-speed-decrease", 0.1, false);

        long duration = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.DURATION, false);
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
        config.set(SkillSetting.DAMAGE.node(), 25.0);
        config.set(SkillSetting.HEALING.node(), 30.0);
        config.set("allow-timelink-timeshift-on-same-target", false);
        config.set("ally-percent-speed-increase", 0.05);
        config.set("enemy-percent-speed-decrease", 0.05);
        config.set("max-stacks", 5);
        config.set(SkillSetting.DURATION.node(), 8000);
        return config;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        return timeshiftTarget(hero, target, 1, false);
    }

    public SkillResult timeshiftTarget(Hero hero, LivingEntity target, int numStacks, boolean shouldBroadcast) {
        Player player = hero.getPlayer();

        CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
        if (targetCT.hasEffect("TemporallyWarded")) {
            player.sendMessage(ChatColor.WHITE + "Unable to shift " + target.getName() + "'s time. They are currently warded against time altering effects!");
            return SkillResult.INVALID_TARGET;
        }

        SkillResult result = determineWhichTimeShift(player, hero, target, targetCT, numStacks, shouldBroadcast);
        if (result == SkillResult.NORMAL) {
            if (hero.hasEffect(SkillTimeLink.timeLinkEffectName)) {
                SkillTimeLink.TimeLinkEffect effect = (SkillTimeLink.TimeLinkEffect) hero.getEffect(SkillTimeLink.timeLinkEffectName);
                CharacterTemplate linkedTargetCT = effect.getTargetCT();

                boolean allowDoubleTimeShift = SkillConfigManager.getUseSetting(hero, this, "allow-timelink-timeshift-on-same-target", false);
                if (linkedTargetCT != null && (allowDoubleTimeShift || !targetCT.equals(linkedTargetCT))) {
                    determineWhichTimeShift(player, hero, linkedTargetCT.getEntity(), linkedTargetCT, numStacks, shouldBroadcast);
                }
            }
        }

        return result;
    }

    private SkillResult determineWhichTimeShift(Player player, Hero hero, LivingEntity target, CharacterTemplate ctTarget, int numStacks, boolean shouldBroadcast) {
        int duration = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.DURATION, false);
        int maxStacks = SkillConfigManager.getUseSetting(hero, this, "max-stacks", 5, false);

        if (hero.isAlliedTo(target))
            return acceleratedShift(player, hero, target, ctTarget, duration, maxStacks, numStacks, shouldBroadcast);
        return deceleratedShift(player, hero, target, ctTarget, duration, maxStacks, numStacks, shouldBroadcast);
    }

    private SkillResult deceleratedShift(Player player, Hero hero, LivingEntity target, CharacterTemplate targetCT,
                                         int duration, int maxStacks, int numStacks, boolean shouldBroadcast) {

        double speedDecrease = SkillConfigManager.getUseSetting(hero, this, "enemy-percent-speed-decrease", 0.1, false);

        DeceleratedShiftedTime effect = null;
        if (!targetCT.hasEffect(decelEffectName)) {
            effect = new DeceleratedShiftedTime(this, player, duration, speedDecrease, maxStacks);
            targetCT.addEffect(effect);
        } else {
            effect = (DeceleratedShiftedTime) targetCT.getEffect(decelEffectName);
        }

        boolean addedNewStack = effect.addStack(targetCT, numStacks);
        if (!addedNewStack) {
            player.sendMessage(maxStacksErrorMessage.replace("$1", target.getName()));
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        if (shouldBroadcast) {
            broadcastExecuteText(hero, target);
        }

        double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE, false);
        addSpellTarget(target, hero);
        damageEntity(target, player, damage, EntityDamageEvent.DamageCause.MAGIC, false);

        World world = target.getWorld();
        Location location = target.getLocation();
        world.spigot().playEffect(location, Effect.COLOURED_DUST, 0, 0, 1.0F, 1.0F, 0.0F, 1.0F, 5, 2);
//        world.playSound(location, Sound.BLOCK_BEACON_DEACTIVATE, 0.5F, 2.0F);
        return SkillResult.NORMAL;
    }

    private SkillResult acceleratedShift(Player player, Hero hero, LivingEntity target, CharacterTemplate targetCT, int duration, int maxStacks, int numStacks, boolean shouldBroadcast) {
        double speedIncrease = SkillConfigManager.getUseSetting(hero, this, "ally-percent-speed-increase", 0.1, false);

        AcceleratedShiftedTime effect = null;
        if (!targetCT.hasEffect(accelEffectName)) {
            effect = new AcceleratedShiftedTime(this, player, duration, speedIncrease, maxStacks);
            targetCT.addEffect(effect);
        } else {
            effect = (AcceleratedShiftedTime) targetCT.getEffect(accelEffectName);
        }

        boolean addedNewStack = effect.addStack(targetCT, numStacks);
        if (!addedNewStack) {
            player.sendMessage(maxStacksErrorMessage.replace("$1", target.getName()));
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        if (shouldBroadcast) {
            broadcastExecuteText(hero, target);
        }

        double healing = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.HEALING, false);
        targetCT.tryHeal(hero, this, healing);  // Ignore failures

        World world = target.getWorld();
        Location location = target.getLocation();
        world.spigot().playEffect(location, Effect.COLOURED_DUST, 0, 0, 0.0F, 0.5450F, 0.5450F, 1.0F, 5, 2);
//        world.playSound(location, Sound.BLOCK_BEACON_ACTIVATE, 1.0F, 1.7F);
        return SkillResult.NORMAL;
    }

    private class DeceleratedShiftedTime extends WalkSpeedPercentDecreaseEffect implements Stacked {
        private final int maxStacks;
        private final double decreasePerStack;
        private int currentStackCount;

        DeceleratedShiftedTime(Skill skill, Player applier, int duration, double decreasePerStack, int maxStacks) {
            super(skill, decelEffectName, applier, duration, decreasePerStack, downShiftApplyText, downShiftExpireText);
            this.decreasePerStack = decreasePerStack;
            this.maxStacks = maxStacks;

            types.add(EffectType.HARMFUL);
            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.TEMPORAL);
        }

        @Override
        public void applyToMonster(Monster monster) {
            // We can't modify a monsters walk speed so this will have to do.
            addPotionEffect(new PotionEffect(PotionEffectType.SLOW, (int) (getDuration() / 50), 1));

            super.applyToMonster(monster);
        }

        public int getMaxStacks() {
            return maxStacks;
        }

        public int getStackCount() {
            return currentStackCount;
        }

        public boolean addStack(CharacterTemplate character, int numStacks) {
            if (currentStackCount >= maxStacks)
                return false;

            currentStackCount = Math.min(maxStacks, currentStackCount + numStacks);
            if (character instanceof Hero) {
                // WalkSpeed only works on Players
                setDelta(decreasePerStack * currentStackCount);
                syncTask((Hero) character);
            }

            this.setDuration(getDuration());
            applier.sendMessage("    " + ChatComponents.GENERIC_SKILL + ChatColor.WHITE + CustomNameManager.getName(character) + "'s "
                    + ChatColor.GOLD + "time has been shifted " + currentStackCount + " times!");

            return true;
        }
    }

    private class AcceleratedShiftedTime extends WalkSpeedPercentIncreaseEffect implements Stacked {
        private final int maxStacks;
        private final double increasePerStack;
        private int currentStackCount;

        AcceleratedShiftedTime(Skill skill, Player applier, int duration, double increasePerStack, int maxStacks) {
            super(skill, accelEffectName, applier, duration, increasePerStack, upShiftApplyText, upShiftExpireText);
            this.increasePerStack = increasePerStack;
            this.maxStacks = maxStacks;

            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.TEMPORAL);
        }

        @Override
        public void applyToMonster(Monster monster) {
            // We can't modify a monsters walk speed so this will have to do.
            addPotionEffect(new PotionEffect(PotionEffectType.SPEED, (int) (getDuration() / 50), 1));

            super.applyToMonster(monster);
        }

        public int getMaxStacks() {
            return maxStacks;
        }

        public int getStackCount() {
            return currentStackCount;
        }

        public boolean addStack(CharacterTemplate character, int numStacks) {
            if (currentStackCount >= maxStacks)
                return false;

            currentStackCount = Math.min(maxStacks, currentStackCount + numStacks);
            if (character instanceof Hero) {
                // WalkSpeed only works on Players
                setDelta(increasePerStack * currentStackCount);
                syncTask((Hero) character);
            }

            this.setDuration(getDuration());
            applier.sendMessage("    " + ChatComponents.GENERIC_SKILL + ChatColor.WHITE + CustomNameManager.getName(character) + "'s "
                    + ChatColor.GOLD + "time has been shifted " + currentStackCount + " times!");

            return true;
        }
    }
}
