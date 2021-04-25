package com.herocraftonline.heroes.characters.skill.remastered.druid;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.characters.skill.tools.BasicDamageMissile;
import com.herocraftonline.heroes.characters.skill.tools.BasicMissile;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.Vector;

public class SkillNaturesWrath extends ActiveSkill {

    public SkillNaturesWrath(Heroes plugin) {
        super(plugin, "NaturesWrath");
        setDescription("Unleash nature’s wrath in front of you, passing through enemies, dealing $1 damage to all enemies hit.");
        setUsage("/skill natureswrath");
        setArgumentRange(0, 0);
        setIdentifiers("skill natureswrath", "skill nwrath");
        setTypes(SkillType.DAMAGING, SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.SILENCEABLE, SkillType.AGGRESSIVE,
                SkillType.AREA_OF_EFFECT);
    }

    public String getDescription(Hero hero) {
        double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE, false);
        return getDescription().replace("$1", Util.decFormat.format(damage));
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.DAMAGE.node(), 80.0);
        config.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 1.125);
        config.set(SkillSetting.RADIUS.node(), 2);

        // may need tweaking copied from Bonespear
        config.set(BasicMissile.PROJECTILE_SIZE_NODE, 2.0);
        config.set(BasicMissile.PROJECTILE_VELOCITY_NODE, 20.0);
        config.set(BasicMissile.PROJECTILE_DURATION_TICKS_NODE, 20);
        config.set(BasicMissile.PROJECTILE_GRAVITY_NODE, 0.0);
        config.set(BasicDamageMissile.PROJECTILE_PIERCES_ON_HIT_NODE, true);
        config.set(BasicDamageMissile.PROJECTILE_KNOCKS_BACK_ON_HIT_NODE, true);
        config.set(BasicDamageMissile.PROJECTILE_CUSTOM_KNOCKBACK_FORCE_NODE, 1.0);
        config.set(BasicDamageMissile.PROJECTILE_CUSTOM_KNOCKBACK_Y_MULTIPLIER_NODE, 0.5);
        return config;
    }

    public SkillResult use(final Hero hero, String[] args) {
        final Player player = hero.getPlayer();

        broadcastExecuteText(hero);

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CHORUS_FLOWER_GROW, 0.7F, 1);
        NaturesWrathProjectile missile = new NaturesWrathProjectile(plugin, this, hero);
        missile.fireMissile();

        return SkillResult.NORMAL;
    }

    // Note Basic damage Missile internals take care of damage and entity detect radius
    class NaturesWrathProjectile extends BasicDamageMissile {
        NaturesWrathProjectile(Heroes plugin, Skill skill, Hero hero) {
            super(plugin, skill, hero, Particle.SPELL_WITCH, Color.GREEN, DamageCause.MAGIC);
        }

        @Override
        protected void onValidTargetFound(LivingEntity target, Vector origin, Vector force) {
            super.onValidTargetFound(target, origin, force); // use default
        }
    }

/* Old ChaoticVisions use code, using BlockIterator though only makes sound and no damage.
    public SkillResult use(final Hero hero, String[] args) {
        final Player player = hero.getPlayer();

        int distance = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.MAX_DISTANCE, false);

        Block tempBlock;
        BlockIterator iter = null;
        try {
            iter = new BlockIterator(player, distance);
        }
        catch (IllegalStateException e) {
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        broadcastExecuteText(hero);

        final double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE, false);

        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5, false);
        final int radiusSquared = radius * radius;

        int delay = SkillConfigManager.getUseSetting(hero, this, "spear-move-delay", 1, false);

        final List<Entity> nearbyEntities = player.getNearbyEntities(distance * 2, distance, distance * 2);
        final List<Entity> hitEnemies = new ArrayList<>();

        int numBlocks = 0;
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_PORTAL_TRAVEL, 0.7F, 1);
        while (iter.hasNext()) {
            tempBlock = iter.next();
            Material tempBlockType = tempBlock.getType();
            if (Util.transparentBlocks.contains(tempBlockType)) {
                final Location targetLocation = tempBlock.getLocation().clone().add(new Vector(.5, 0, .5));

                // Schedule the action in advance
                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                    public void run() {
                        // Play effect

                    	//ParticleEffect.SPELL_WITCH.display(1, 1, 1, 0.1, 5, targetLocation, 20);
                    	//public void playEffect(Location location, Effect effect,  id,  data,  offsetX,  offsetY,  offsetZ,  speed,  particleCount,  radius)
                    	//targetLocation.getWorld().spigot().playEffect(targetLocation, Effect.WITCH_MAGIC, 1, 1, 0F, 0F, 0F, 0.2F, 10, 20);
                        targetLocation.getWorld().spawnParticle(Particle.SPELL_WITCH, targetLocation, 10, 0, 0, 0, 0.2, true);

                        // Check our entity list to see if they are on this specific block at the moment the firework plays
                        for (Entity entity : nearbyEntities) {
                            // Ensure that we have a valid entity
                            if (!(entity instanceof LivingEntity) || hitEnemies.contains(entity) || entity.getLocation().distanceSquared(targetLocation) > radiusSquared)
                                continue;

                            // Check to see if the entity can be damaged
                            if (!damageCheck(player, (LivingEntity) entity))
                                continue;

                            // Damage target
                            LivingEntity target = (LivingEntity) entity;
                            addSpellTarget(target, hero);
                            damageEntity(target, player, damage, DamageCause.MAGIC);

                            // Add the target to the hitEntity map to ensure we don't ever hit them again with this specific ChaoticVisions
                            hitEnemies.add(entity);
                        }
                    }
                }, numBlocks * delay);

                numBlocks++;
            }
            else
                break;
        }

        return SkillResult.NORMAL;
    }
*/
}
