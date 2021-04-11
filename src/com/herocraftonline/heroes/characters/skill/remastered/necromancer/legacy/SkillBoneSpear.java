package com.herocraftonline.heroes.characters.skill.remastered.necromancer.legacy;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.characters.skill.tools.BasicDamageMissile;
import com.herocraftonline.heroes.characters.skill.tools.BasicMissile;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class SkillBoneSpear extends ActiveSkill {

    public SkillBoneSpear(Heroes plugin) {
        super(plugin, "BoneSpear");
        setDescription("Launch a magical spear of bone in front of you. " +
                "The spear will $1deal $2 damage to any targets it hits$3");
        setUsage("/skill bonespear");
        setIdentifiers("skill bonespear");
        setArgumentRange(0, 0);
        setTypes(SkillType.DAMAGING, SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.SILENCEABLE, SkillType.AGGRESSIVE);
    }

    public String getDescription(Hero hero) {
        double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE, false);
        boolean pierces = SkillConfigManager.getUseSetting(hero, this, BasicDamageMissile.PROJECTILE_PIERCES_ON_HIT_NODE, false);
        String pierceText = pierces ? "pierce enemies and " : "";

        double knockbackPower = SkillConfigManager.getUseSetting(hero, this, "projectile-knockback-force", 2.0, false);
        String knockbackText = "";
        if (knockbackPower <= 0) {
            knockbackText = ".";
        } else {
            knockbackText = ", and knock them back ";
            if (knockbackPower >= 2.0) {
                knockbackText += "with exessive force.";
            } else if (knockbackPower >= 1.5) {
                knockbackText += "by a significant amount.";
            } else if (knockbackPower >= 1.0) {
                knockbackText += "by a decent amount.";
            } else {
                knockbackText += "slightly.";
            }
        }

        return getDescription()
                .replace("$1", pierceText)
                .replace("$2", Util.decFormat.format(damage))
                .replace("$3", knockbackText);
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.DAMAGE.node(), 75.0);
        config.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 0.0);
        config.set(BasicMissile.PROJECTILE_SIZE_NODE, 2.0);
        config.set(BasicMissile.PROJECTILE_VELOCITY_NODE, 20.0);
        config.set(BasicMissile.PROJECTILE_DURATION_TICKS_NODE, 20);
        config.set("projectile-block-collision-size", 0.35);
        config.set(BasicMissile.PROJECTILE_GRAVITY_NODE, 0.0);
        config.set(BasicDamageMissile.PROJECTILE_PIERCES_ON_HIT_NODE, true);
        config.set(BasicDamageMissile.PROJECTILE_KNOCKS_BACK_ON_HIT_NODE, true);
        config.set(BasicDamageMissile.PROJECTILE_CUSTOM_KNOCKBACK_FORCE_NODE, 1.5);
        config.set(BasicDamageMissile.PROJECTILE_CUSTOM_KNOCKBACK_Y_MULTIPLIER_NODE, 0.5);
        config.set("projectile-effect-display-servertick-rate", 10);
        return config;
    }

    public SkillResult use(final Hero hero, String[] args) {
        final Player player = hero.getPlayer();

        broadcastExecuteText(hero);

        BoneSpearProjectile missile = new BoneSpearProjectile(plugin, this, hero);
        missile.fireMissile();

        return SkillResult.NORMAL;
    }

    class BoneSpearProjectile extends BasicDamageMissile {
        private final double blockCollisionSizeSquared;
        private final int visualTickRate;

        BoneSpearProjectile(Heroes plugin, Skill skill, Hero hero) {
            super(plugin, skill, hero);
            this.replaceEffects(null, null);

            double size = SkillConfigManager.getUseSetting(hero, skill, "projectile-block-collision-size", 0.35, false);
            this.blockCollisionSizeSquared = size * size;
            this.visualTickRate = SkillConfigManager.getUseSetting(hero, skill, "projectile-effect-display-servertick-rate", 10, false);
        }

        @Override
        protected Location buildMissileStartLocation() {
            // Center on the player
            return player.getEyeLocation().clone().subtract(0, player.getEyeHeight() / 2, 0).setDirection(player.getEyeLocation().getDirection());
        }

        private void updateVisualLocation() {
            FireworkEffect firework = FireworkEffect.builder()
                    .flicker(false)
                    .trail(true)
                    .withColor(Color.AQUA)
                    .withColor(Color.AQUA)
                    .withColor(Color.GRAY)
                    .with(FireworkEffect.Type.BURST)
                    .build();
            VisualEffect.playInstantFirework(firework, getLocation());
        }

        @Override
        protected void onStart() {
            super.onStart();
            updateVisualLocation();
        }

        @Override
        protected void onTick() {
            if (getTicksLived() % this.visualTickRate == 0)
                updateVisualLocation();
        }

        @Override
        protected void onFinalTick() {
            super.onStart();
            updateVisualLocation();
        }

        @Override
        protected boolean onCollideWithBlock(Block block, Vector point, BlockFace face) {
            // Custom "block radius"
            return getLocation().distanceSquared(block.getLocation()) >= this.blockCollisionSizeSquared;
        }
    }
}
