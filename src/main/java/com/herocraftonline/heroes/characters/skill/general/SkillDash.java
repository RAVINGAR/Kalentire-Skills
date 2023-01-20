package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;

/**
 * Port of Ardorlon's Dash Skill
 */
public class SkillDash extends ActiveSkill {
    public SkillDash(final Heroes plugin) {
        super(plugin, "Dash");
        setUsage("/skill dash");
        setIdentifiers("skill dash");
        setArgumentRange(0, 0);
        setDescription("You dash forward, dealing $1 damage to all enemies in your path and knocking them back. Deals an extra $2 damage to airborne enemies and knocks them back further. Dash speed increases with Dexterity.");
        setTypes(SkillType.DAMAGING, SkillType.MOVEMENT_INCREASING, SkillType.ABILITY_PROPERTY_PHYSICAL);
    }

    @Override
    public String getDescription(final Hero hero) {
        final double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE, 35, true);
        final double bonusDmg = SkillConfigManager.getScaledUseSettingDouble(hero, this, "bonus-damage", 15, true);

        return getDescription().replace("$1", damage + "")
                .replace("$2", bonusDmg + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection cs = super.getDefaultConfig();

        cs.set(SkillSetting.DAMAGE.node(), 35);
        cs.set(SkillSetting.DAMAGE_INCREASE_PER_STRENGTH.node(), 0.4);
        cs.set("bonus-damage", 15);
        cs.set("bonus-damage-per-dexterity", 0.5);
        cs.set("speed-boost-per-dexterity", 0.0125);
        cs.set("speed-mult", 1.0001);

        return cs;
    }

    @Override
    public SkillResult use(final Hero hero, final String[] args) {
        final Player player = hero.getPlayer();

        final double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE, 35, true);
        final double bonusDmg = SkillConfigManager.getScaledUseSettingDouble(hero, this, "bonus-damage", 15, true);

        final double speedMult = SkillConfigManager.getUseSetting(hero, this, "speed-mult", 1.0001, true);
        final double boost = SkillConfigManager.getUseSetting(hero, this, "speed-boost-per-dexterity", 0.0125, true) * hero.getAttributeValue(AttributeType.DEXTERITY);

        final Vector velocity = player.getLocation().getDirection().clone().setY(0.0D).multiply(speedMult + boost);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0F, 1.0F);

        new DashRunnable(hero, velocity, damage, bonusDmg).runTaskTimer(plugin, 0, 1);

        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

    public class DashRunnable extends BukkitRunnable {
        private final ArrayList<LivingEntity> hit = new ArrayList<>();
        private final long maxTicks = 5;
        private final Hero hero;
        private final Player player;
        private final Vector velocity;
        private final double damage;
        private final double bonusDamage;
        private long ticks = 0;

        public DashRunnable(final Hero hero, final Vector vector, final double damage, final double bonusDamage) {
            this.hero = hero;
            this.player = hero.getPlayer();
            this.velocity = vector;
            this.damage = damage;
            this.bonusDamage = bonusDamage;
        }

        @Override
        public void run() {
            if (ticks == maxTicks) {
                cancel();
            }
            player.setVelocity(velocity);
            player.getLocation().getWorld().spawnParticle(Particle.CLOUD, player.getLocation().add(0, 0.2, 0), 20, 0.02, 0.02, 0.02, 0.01);
            for (final Entity e : player.getNearbyEntities(3.0D, 3.0D, 3.0D)) {
                if (!(e instanceof LivingEntity)) {
                    continue;
                }
                final LivingEntity le = (LivingEntity) e;
                if (!damageCheck(player, le)) {
                    continue;
                }
                if (hit.contains(le)) {
                    continue;
                }

                final boolean inAir = le.isOnGround();
                addSpellTarget(le, hero);
                damageEntity(le, player, inAir ? damage + bonusDamage : damage, EntityDamageEvent.DamageCause.ENTITY_ATTACK, inAir ? 0.7f : 0.4f);
                le.setVelocity(velocity.clone().divide(new Vector(3, 3, 3)).setY(0.4));
                le.getWorld().playSound(le.getLocation(), Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 2.0F, 1.0F);
                hit.add(le);
            }
            ticks++;
        }
    }
}
