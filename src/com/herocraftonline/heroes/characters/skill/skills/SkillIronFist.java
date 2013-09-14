package com.herocraftonline.heroes.characters.skill.skills;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.Vector;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.effects.common.SlowEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Util;

import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.hooks.NCPExemptionManager;

public class SkillIronFist extends ActiveSkill {

    private boolean ncpEnabled = false;

    public SkillIronFist(Heroes plugin) {
        super(plugin, "IronFist");
        setDescription("Strike the ground with an iron fist, striking all targets within $1 blocks, dealing $2 damage and knocking them away from you. Targets hit will also be slowed for $3 seconds.");
        setUsage("/skill ironfist");
        setArgumentRange(0, 0);
        setIdentifiers("skill ironfist");
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.FORCE, SkillType.DAMAGING, SkillType.AGGRESSIVE);

        if (Bukkit.getServer().getPluginManager().getPlugin("NoCheatPlus") != null) {
            ncpEnabled = true;
        }
    }

    @Override
    public String getDescription(Hero hero) {
        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, Integer.valueOf(5), false);

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, Integer.valueOf(50), false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, Double.valueOf(1.6), false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH);

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 3000, false);

        String formattedDamage = Util.decFormat.format(damage);
        String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription().replace("$1", radius + "").replace("$2", formattedDamage).replace("$3", formattedDuration);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DAMAGE.node(), Integer.valueOf(50));
        node.set(SkillSetting.DAMAGE_INCREASE_PER_STRENGTH.node(), Double.valueOf(1.125));
        node.set(SkillSetting.RADIUS.node(), Integer.valueOf(5));
        node.set(SkillSetting.DURATION.node(), Integer.valueOf(5000));
        node.set("slow-amplifier", Integer.valueOf(1));
        node.set("horizontal-power", Double.valueOf(0.0));
        node.set("horizontal-power-increase-per-intellect", Double.valueOf(0.0));
        node.set("vertical-power", Double.valueOf(0.4));
        node.set("vertical-power-increase-per-intellect", Double.valueOf(0.015));
        node.set("ncp-exemption-duration", Integer.valueOf(1000));
        node.set(SkillSetting.DELAY.node(), Integer.valueOf(500));

        return node;
    }

    @Override
    public void onWarmup(Hero hero) {
        Player player = hero.getPlayer();
        player.getWorld().playSound(player.getLocation(), Sound.CHEST_OPEN, 0.7F, 0.4F);
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, Integer.valueOf(5), false);

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 50, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, Double.valueOf(1.125), false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH);

        double hPower = SkillConfigManager.getUseSetting(hero, this, "horizontal-power", Double.valueOf(0.4), false);
        double hPowerIncrease = SkillConfigManager.getUseSetting(hero, this, "horizontal-power-increase-per-intellect", Double.valueOf(0.015), false);
        hPower += hPowerIncrease * hero.getAttributeValue(AttributeType.INTELLECT);

        double vPower = SkillConfigManager.getUseSetting(hero, this, "vertical-power", Double.valueOf(0.0), false);
        double vPowerIncrease = SkillConfigManager.getUseSetting(hero, this, "vertical-power-increase-per-intellect", Double.valueOf(0.0), false);
        vPower += vPowerIncrease * hero.getAttributeValue(AttributeType.INTELLECT);

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 3000, false);

        int slowAmplifier = SkillConfigManager.getUseSetting(hero, this, "slow-amplifier", Integer.valueOf(1), false);

        broadcastExecuteText(hero);

        List<Entity> entities = hero.getPlayer().getNearbyEntities(radius, radius, radius);
        for (Entity entity : entities) {
            if (!(entity instanceof LivingEntity)) {
                continue;
            }

            // Check if the target is damagable
            if (!damageCheck(player, (LivingEntity) entity)) {
                continue;
            }

            LivingEntity target = (LivingEntity) entity;

            // Damage the target
            addSpellTarget(target, hero);
            damageEntity(target, player, damage, DamageCause.ENTITY_ATTACK, false);

            // Do our knockback
            Location playerLoc = player.getLocation();
            Location targetLoc = target.getLocation();

            double xDir = targetLoc.getX() - playerLoc.getX();
            double zDir = targetLoc.getZ() - playerLoc.getZ();
            double magnitude = Math.sqrt(xDir * xDir + zDir * zDir);

            xDir = xDir / magnitude * hPower;
            zDir = zDir / magnitude * hPower;

            // Let's bypass the nocheat issues...
            if (ncpEnabled) {
                if (target instanceof Player) {
                    Player targetPlayer = (Player) target;
                    Hero targetHero = plugin.getCharacterManager().getHero(targetPlayer);
                    if (!targetPlayer.isOp()) {
                        long ncpDuration = SkillConfigManager.getUseSetting(hero, this, "ncp-exemption-duration", 500, false);
                        NCPExemptionEffect ncpExemptEffect = new NCPExemptionEffect(this, targetPlayer, ncpDuration);
                        targetHero.addEffect(ncpExemptEffect);
                    }
                }
            }

            target.setVelocity(new Vector(xDir, vPower, zDir));

            SlowEffect sEffect = new SlowEffect(this, player, duration, slowAmplifier, null, null);
            CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
            targetCT.addEffect(sEffect);
        }

        player.getWorld().playEffect(player.getLocation(), Effect.MOBSPAWNER_FLAMES, 3);
        player.getWorld().playSound(player.getLocation(), Sound.EXPLODE, 0.5F, 1.0F);

        return SkillResult.NORMAL;
    }

    private class NCPExemptionEffect extends ExpirableEffect {

        public NCPExemptionEffect(Skill skill, Player applier, long duration) {
            super(skill, "NCPExemptionEffect_MOVING", applier, duration);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            final Player player = hero.getPlayer();

            NCPExemptionManager.exemptPermanently(player, CheckType.MOVING);
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            final Player player = hero.getPlayer();

            NCPExemptionManager.unexempt(player, CheckType.MOVING);
        }
    }
}
