package com.herocraftonline.heroes.characters.skill.reborn.hookwarrior;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class SkillHemorrhage extends TargettedSkill {

    private static final DustOptions enterParticleOptions = new Particle.DustOptions(Color.RED, 1);
    private static final DustOptions exitParticleOptions = new Particle.DustOptions(Color.RED, 2);
    private final float enterParticleDisplaySpeed = 1.5F;   // 1.0F is default
    private final float exitParticleDisplaySpeed = 1.5F;   // 1.0F is default

    public SkillHemorrhage(Heroes plugin) {
        super(plugin, "Hemorrhage");
        this.setDescription("Deliver a strong bash to your target, dealing $1 physical damage and interrupting their casting.");
        this.setUsage("/skill hemorrhage");
        this.setArgumentRange(0, 0);
        this.setIdentifiers("skill hemorrhage");
        this.setTypes(SkillType.SILENCEABLE, SkillType.DAMAGING, SkillType.AGGRESSIVE);
    }

    @Override
    public String getDescription(Hero hero) {
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 60, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 0.0, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH);

        String formattedDamage = Util.decFormat.format(damage);

        return getDescription().replace("$1", formattedDamage);
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.MAX_DISTANCE.node(), 3);
        config.set(SkillSetting.DAMAGE.node(), 30);
        config.set(SkillSetting.DAMAGE_INCREASE_PER_STRENGTH.node(), 0.0);
        config.set("ticks-before-damage", 3);
        return config;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero, target);

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 30, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 0.0, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH);

        // display entrance of the hook
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SQUID_HURT, 0.2F, 0.5F);
        player.getWorld().spawnParticle(Particle.REDSTONE, target.getEyeLocation(), 5, 0.5F, 0.25F, 0.3F, enterParticleDisplaySpeed, enterParticleOptions);

        int ticksBeforeDamage = SkillConfigManager.getUseSetting(hero, this, "ticks-before-damage", 3, false);

        final double finalDamage = damage;  // final so that it can be used in the runnable
        new BukkitRunnable() {
            @Override
            public void run() {
                // Make sure that both player and target are still alive after our delay.
                if (player.isDead() || player.getHealth() < 0)
                    return;
                if (target.isDead() || target.getHealth() < 0)
                    return;

                // do damage
                addSpellTarget(target, hero);
                damageEntity(target, player, finalDamage, EntityDamageEvent.DamageCause.ENTITY_ATTACK);

                // display removal of the hook
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SQUID_HURT, 0.4F, 1.0F);
                player.getWorld().spawnParticle(Particle.REDSTONE, target.getEyeLocation(), 15, 0.25F, 0.15F, 0.4F, exitParticleDisplaySpeed, exitParticleOptions);
            }
        }.runTaskLaterAsynchronously(plugin, ticksBeforeDamage);

        return SkillResult.NORMAL;
    }
}
