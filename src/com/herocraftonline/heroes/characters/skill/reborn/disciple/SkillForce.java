package com.herocraftonline.heroes.characters.skill.reborn.disciple;

import com.google.common.collect.Lists;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.common.SlowEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.characters.skill.ncp.NCPFunction;
import com.herocraftonline.heroes.characters.skill.ncp.NCPUtils;
import com.herocraftonline.heroes.characters.skill.tools.VelocityActions;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.Vector;

public class SkillForce extends TargettedSkill {

    public SkillForce(Heroes plugin) {
        super(plugin, "Force");
        setDescription("Using the energy from your current stance, you apply force to your target. " +
                "Allies are healed for $1. Enemies are damaged for $2$3. \n" +
                ChatColor.GRAY + "Unfocused: " + ChatColor.WHITE + "You push allies and pull enemies.\n" +
                ChatColor.GOLD + "Tiger: " + ChatColor.WHITE + "You pull all targets.\n" +
                ChatColor.YELLOW + "Jin: " + ChatColor.WHITE + "You push all targets.");
        setUsage("/skill force");
        setIdentifiers("skill force");
        setArgumentRange(0, 0);
        setTypes(SkillType.FORCE, SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.INTERRUPTING, SkillType.SILENCEABLE,
                SkillType.MULTI_GRESSIVE, SkillType.NO_SELF_TARGETTING);
    }

    @Override
    public String getDescription(Hero hero) {
        double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE, false);
        double healing = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.HEALING, false);

        int duration = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.DURATION, false);
        int slowAmplifier = SkillConfigManager.getUseSetting(hero, this, "slow-amplifier", 1, false);

        String slowText = "";
        if (duration > 0 && slowAmplifier > -1) {
            slowText = " and slowed for " + Util.decFormat.format(duration / 1000.0) + " second(s)";
        }

        return getDescription()
                .replace("$1", Util.decFormat.format(healing))
                .replace("$2", Util.decFormat.format(damage))
                .replace("$3", slowText);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.MAX_DISTANCE.node(), 10.0);
        config.set(SkillSetting.DAMAGE.node(), 50.0);
        config.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 0.0);
        config.set(SkillSetting.HEALING.node(), 50.0);
        config.set(SkillSetting.HEALING_INCREASE_PER_INTELLECT.node(), 0.0);
        config.set(SkillSetting.DURATION.node(), 0);
        config.set("slow-amplifier", -1);
        config.set(VelocityActions.PUSH_HORIZONTAL_POWER_NODE, 1.5);
        config.set(VelocityActions.PUSH_VERTICAL_POWER_NODE, 0.25);
        config.set(VelocityActions.PUSH_SERVER_TICKS_DELAY, 4);
        config.set(VelocityActions.PULL_HORIZONTAL_DIVISOR_NODE, 1.5);
        config.set(VelocityActions.PULL_SERVER_TICKS_DELAY, 4);
        return config;
    }

    @Override
    public SkillResult use(Hero hero, final LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero, target);

        SkillTigerStance.StanceEffect stanceEffect = null;
        if (hero.hasEffect(SkillTigerStance.stanceEffectName)) {
            stanceEffect = (SkillTigerStance.StanceEffect) hero.getEffect(SkillTigerStance.stanceEffectName);
        }

        boolean isAllied = hero.isAlliedTo(target);
        boolean isPushInsteadOfPull;
        if (stanceEffect == null || stanceEffect.getCurrentStance().equals(SkillTigerStance.StanceType.UNFOCUSED)) {
            isPushInsteadOfPull = isAllied;
        } else if (stanceEffect.getCurrentStance().equals(SkillTigerStance.StanceType.JIN)) {
            isPushInsteadOfPull = false;
        } else {
            isPushInsteadOfPull = true;
        }

        if (isPushInsteadOfPull) {
            VelocityActions.pushWithConsistentForce(this, hero, target);
        } else {
            VelocityActions.pullWithConsistentForce(this, hero, target);

        }
        if (isAllied) {
            double healing = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.HEALING, false);
            target.setFallDistance(-1);

            CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
            targetCT.tryHeal(hero, this, healing);  // Ignore failures
        } else {
            double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE, false);
            if (damage > 0) {
                addSpellTarget(target, hero);
                damageEntity(target, player, damage, DamageCause.MAGIC, false);
            }

            int duration = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.DURATION, false);
            int slowAmplifier = SkillConfigManager.getUseSetting(hero, this, "slow-amplifier", 1, false);

            if (slowAmplifier > -1 && duration > 0) {
                SlowEffect slowEffect = new SlowEffect(this, player, duration, slowAmplifier, null, null);
                slowEffect.types.add(EffectType.DISPELLABLE);

                CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
                targetCT.addEffect(slowEffect);
            }
        }

        player.getWorld().playSound(target.getLocation(), Sound.ENTITY_GENERIC_BURN, 0.5f, 2.0f);
        player.getWorld().spawnParticle(Particle.SPELL_WITCH, target.getLocation().add(0, 0.5, 0), 150, 0, 0, 0, 1);

        return SkillResult.NORMAL;
    }
}
