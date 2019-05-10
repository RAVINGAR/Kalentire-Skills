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

import java.util.Arrays;

public class SkillTimeShift extends TargettedSkill {
    public static String skillName = "TimeShift";

    private String upShiftApplyText;
    private String upShiftExpireText;
    private String downShiftApplyText;
    private String downShiftExpireText;

    private String accelEffectName = "AcceleratedShiftedTime";
    private String decelEffectName = "DeceleratedShiftedTime";
    private String maxStacksErrorMessage = ChatComponents.GENERIC_SKILL + "Your target is already shifted as far as they can go!";

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
        config.set(SkillSetting.DAMAGE.node(), 25.0);
        config.set(SkillSetting.HEALING.node(), 30.0);
        config.set("ally-percent-speed-increase", 0.05);
        config.set("enemy-percent-speed-decrease", 0.05);
        config.set("max-stacks", 5);
        config.set(SkillSetting.DURATION.node(), 8000);
        return config;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        // This is necessary for compatibility with AoE versions of this skill.
        boolean shouldBroadcast = args == null || args.length == 0 || Arrays.stream(args).noneMatch(x -> x.equalsIgnoreCase("NoBroadcast"));

        CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
        if (targetCT.hasEffect("TemporallyWarded")) {
            player.sendMessage(ChatColor.WHITE + "Unable to shift " + target.getName() + "'s time. They are currently warded against time altering effects!");
            return SkillResult.INVALID_TARGET;
        }

        SkillResult result = timeLinkTarget(hero, target, player, shouldBroadcast, targetCT);
        if (result == SkillResult.NORMAL) {
            if (hero.hasEffect(SkillTimeLink.timeLinkEffectName)) {
                SkillTimeLink.TimeLinkEffect effect = (SkillTimeLink.TimeLinkEffect) hero.getEffect(SkillTimeLink.timeLinkEffectName);
                CharacterTemplate linkedTargetCT = effect.getTargetCT();
                if (linkedTargetCT != null && !targetCT.equals(linkedTargetCT)) {
                    timeLinkTarget(hero, linkedTargetCT.getEntity(), player, shouldBroadcast, linkedTargetCT);
                }
            }
        }

        return result;
    }

    public SkillResult timeLinkTarget(Hero hero, LivingEntity target, Player player, boolean shouldBroadcast, CharacterTemplate ctTarget) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 8000, false);
        int maxStacks = SkillConfigManager.getUseSetting(hero, this, "max-stacks", 5, false);

        if (hero.isAlliedTo(target))
            return acceleratedShift(player, hero, target, ctTarget, duration, maxStacks, shouldBroadcast);
        return deceleratedShift(player, hero, target, ctTarget, duration, maxStacks, shouldBroadcast);
    }

    public SkillResult deceleratedShift(Player player, Hero hero, LivingEntity target, CharacterTemplate targetCT, int duration, int maxStacks, boolean shouldBroadcast) {
        double speedDecrease = SkillConfigManager.getUseSetting(hero, this, "enemy-percent-speed-decrease", 0.1, false);

        DeceleratedShiftedTime effect = null;
        if (!targetCT.hasEffect(decelEffectName)) {
            effect = new DeceleratedShiftedTime(this, player, duration, speedDecrease, maxStacks);
            targetCT.addEffect(effect);
        } else {
            effect = (DeceleratedShiftedTime) targetCT.getEffect(decelEffectName);
        }

        boolean addedNewStack = effect.addStack(targetCT);
        if (!addedNewStack) {
            player.sendMessage(maxStacksErrorMessage);
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        if (shouldBroadcast)
            broadcastExecuteText(hero, target);

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 20.0, false);
        addSpellTarget(target, hero);
        damageEntity(target, player, damage, EntityDamageEvent.DamageCause.MAGIC, false);

        World world = target.getWorld();
        Location location = target.getLocation();
        world.spigot().playEffect(location, Effect.COLOURED_DUST, 0, 0, 1.0F, 1.0F, 0.0F, 1.0F, 5, 2);
//        world.playSound(location, Sound.BLOCK_BEACON_DEACTIVATE, 0.5F, 2.0F);
        return SkillResult.NORMAL;
    }

    public SkillResult acceleratedShift(Player player, Hero hero, LivingEntity target, CharacterTemplate targetCT, int duration, int maxStacks, boolean shouldBroadcast) {
        double speedIncrease = SkillConfigManager.getUseSetting(hero, this, "ally-percent-speed-increase", 0.1, false);

        AcceleratedShiftedTime effect = null;
        if (!targetCT.hasEffect(accelEffectName)) {
            effect = new AcceleratedShiftedTime(this, player, duration, speedIncrease, maxStacks);
            targetCT.addEffect(effect);
        } else {
            effect = (AcceleratedShiftedTime) targetCT.getEffect(accelEffectName);
        }

        boolean addedNewStack = effect.addStack(targetCT);
        if (!addedNewStack) {
            player.sendMessage(maxStacksErrorMessage);
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        if (shouldBroadcast)
            broadcastExecuteText(hero, target);

        double healing = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING, 20.0, false);
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

        public boolean addStack(CharacterTemplate character) {
            if (currentStackCount >= maxStacks)
                return false;

            currentStackCount++;
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

        public boolean addStack(CharacterTemplate character) {
            if (currentStackCount >= maxStacks)
                return false;

            currentStackCount++;
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
