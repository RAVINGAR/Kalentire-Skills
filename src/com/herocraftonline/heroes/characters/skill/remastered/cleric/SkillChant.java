package com.herocraftonline.heroes.characters.skill.remastered.cleric;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.characters.skill.tools.BasicHealMissile;
import com.herocraftonline.heroes.characters.skill.tools.BasicMissile;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SkillChant extends ActiveSkill {

    public SkillChant(Heroes plugin) {
        super(plugin, "Chant");
        setDescription("Chant towards your target, restoring $1 health to party member(s) hit.");
        setUsage("/skill chant");
        setArgumentRange(0, 0);
        setIdentifiers("skill chant");
        setTypes(SkillType.ABILITY_PROPERTY_LIGHT, SkillType.HEALING, SkillType.SILENCEABLE, SkillType.AREA_OF_EFFECT);
    }

    @Override
    public String getDescription(Hero hero) {
        double healing = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.HEALING, false);
        return this.getDescription().replace("$1", Util.decFormat.format(healing));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.HEALING.node(), 65);
        config.set(SkillSetting.HEALING_INCREASE_PER_WISDOM.node(), 1.5);
//        config.set(SkillSetting.RADIUS.node(), 1.5);

        config.set(BasicMissile.PROJECTILE_SIZE_NODE, 1.5);
        config.set(BasicMissile.PROJECTILE_VELOCITY_NODE, 20.0);
        config.set(BasicMissile.PROJECTILE_DURATION_TICKS_NODE, 10.0);
        config.set(BasicMissile.PROJECTILE_GRAVITY_NODE, 0.0);
        return config;
    }

    public SkillResult use(final Hero hero, String[] args) {
        final Player player = hero.getPlayer();
        broadcastExecuteText(hero);

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BELL_RESONATE, 0.7F, 1);
        ChantHealingProjectile missile = new ChantHealingProjectile(plugin, this, hero);
        missile.fireMissile();

        return SkillResult.NORMAL;
    }

    // Note Basic damage Missile internals take care of damage and entity detect radius
    class ChantHealingProjectile extends BasicHealMissile {
        ChantHealingProjectile(Heroes plugin, Skill skill, Hero hero) {
            super(plugin, skill, hero);
        }

        @Override
        protected void onValidTargetFound(LivingEntity target) {
            super.onValidTargetFound(target);
            target.getWorld().spawnParticle(Particle.HEART, target.getLocation().add(0, 0.5, 0), 1);
            target.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, target.getLocation().add(0, 0.5, 0), 3);
            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_CHICKEN_EGG, 0.5F, 0.01F);
        }
    }
}
