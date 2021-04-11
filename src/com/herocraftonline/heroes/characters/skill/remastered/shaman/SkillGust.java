package com.herocraftonline.heroes.characters.skill.remastered.shaman;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.characters.skill.tools.BasicDamageMissile;
import com.herocraftonline.heroes.characters.skill.tools.BasicMissile;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class SkillGust extends ActiveSkill {

    public SkillGust(Heroes plugin) {
        super(plugin, "Gust");
        setDescription("Summon a gust of wind in front of you. " +
                "The gust will $1deal $2 damage to any targets it hits$3");
        setUsage("/skill gust");
        setIdentifiers("skill gust");
        setArgumentRange(0, 0);
        setTypes(SkillType.ABILITY_PROPERTY_AIR, SkillType.DAMAGING, SkillType.SILENCEABLE, SkillType.AGGRESSIVE);
    }

    public String getDescription(Hero hero) {
        boolean pierces = SkillConfigManager.getUseSetting(hero, this, BasicDamageMissile.PROJECTILE_PIERCES_ON_HIT_NODE, false);
        String pierceText = pierces ? "pass through enemies and " : "";

        double knockbackPower = SkillConfigManager.getUseSetting(hero, this, "projectile-knockback-force", 2.0, false);
        String knockbackText = "";
        if (knockbackPower <= 0) {
            knockbackText = ".";
        } else {
            knockbackText = ", while knocking them back ";
            if (knockbackPower >= 2.0) {
                knockbackText+= "with exessive force.";
            } else if (knockbackPower >= 1.5) {
                knockbackText+= "by a significant amount.";
            } else if (knockbackPower >= 1.0) {
                knockbackText+= "by a decent amount.";
            } else {
                knockbackText += "slightly.";
            }
        }

        double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE, false);

        return getDescription()
                .replace("$1", pierceText)
                .replace("$2", Util.decFormat.format(damage))
                .replace("$3", knockbackText);
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.DAMAGE.node(), 40.0);
        config.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 0.0);
        config.set(BasicMissile.PROJECTILE_SIZE_NODE, 2.5);
        config.set(BasicMissile.PROJECTILE_VELOCITY_NODE, 35.0);
        config.set(BasicMissile.PROJECTILE_DURATION_TICKS_NODE, 20);
        config.set("projectile-block-collision-size", 0.35);
        config.set(BasicMissile.PROJECTILE_GRAVITY_NODE, 0.0);
        config.set(BasicDamageMissile.PROJECTILE_PIERCES_ON_HIT_NODE, true);
        config.set(BasicDamageMissile.PROJECTILE_CUSTOM_KNOCKBACK_FORCE_NODE, 2.0);
        config.set(BasicDamageMissile.PROJECTILE_CUSTOM_KNOCKBACK_Y_MULTIPLIER_NODE, 0.65);
        return config;
    }

    public SkillResult use(final Hero hero, String[] args) {
        final Player player = hero.getPlayer();

        broadcastExecuteText(hero);

        GustProjectile missile = new GustProjectile(plugin, this, hero);
        missile.fireMissile();

        return SkillResult.NORMAL;
    }

    class GustProjectile extends BasicDamageMissile {
        private final double blockCollisionSizeSquared;

        GustProjectile(Heroes plugin, Skill skill, Hero hero) {
            super(plugin, skill, hero, Particle.SWEEP_ATTACK);

            double size = SkillConfigManager.getUseSetting(hero, skill, "projectile-block-collision-size", 0.35, false);
            this.blockCollisionSizeSquared = size * size;
        }

        @Override
        protected Location buildMissileStartLocation() {
            // Center on the player
            return player.getEyeLocation().clone().subtract(0, player.getEyeHeight() / 2, 0).setDirection(player.getEyeLocation().getDirection());
        }

        @Override
        protected boolean onCollideWithBlock(Block block, Vector point, BlockFace face) {
            return getLocation().distanceSquared(block.getLocation()) >= this.blockCollisionSizeSquared;
        }
    }
}
