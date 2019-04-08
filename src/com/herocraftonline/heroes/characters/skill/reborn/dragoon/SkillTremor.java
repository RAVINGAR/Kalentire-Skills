package com.herocraftonline.heroes.characters.skill.reborn.dragoon;

import com.google.common.collect.Lists;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.ncp.NCPFunction;
import com.herocraftonline.heroes.characters.skill.ncp.NCPUtils;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class SkillTremor extends ActiveSkill {

    public SkillTremor(Heroes plugin) {
        super(plugin, "Tremor");
        setDescription("Strike the ground with a powerful tremor, affecting all targets within $1 blocks. " +
                "All targets hit with the tremor are dealt $2 physical damage and knocked back a great distance.");
        setUsage("/skill tremor");
        setArgumentRange(0, 0);
        setIdentifiers("skill tremor");
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.FORCE, SkillType.DAMAGING, SkillType.AGGRESSIVE, SkillType.AREA_OF_EFFECT);
    }

    @Override
    public String getDescription(Hero hero) {
        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5, false);
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 50, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 1.6, false);
        damage += (int) (damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH));

        return getDescription()
                .replace("$1", radius + "")
                .replace("$2", damage + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.DAMAGE.node(), 50);
        config.set(SkillSetting.DAMAGE_INCREASE_PER_STRENGTH.node(), 1.125);
        config.set(SkillSetting.RADIUS.node(), 5);
        config.set("horizontal-power", 0.0);
        config.set("vertical-power", 0.4);
        config.set("ncp-exemption-duration", 1500);
        config.set(SkillSetting.DELAY.node(), 500);
        return config;
    }

    public ArrayList<Location> circle(Location centerPoint, int particleAmount, double circleRadius) {
        World world = centerPoint.getWorld();

        double increment = (2 * Math.PI) / particleAmount;

        ArrayList<Location> locations = new ArrayList<Location>();

        for (int i = 0; i < particleAmount; i++) {
            double angle = i * increment;
            double x = centerPoint.getX() + (circleRadius * Math.cos(angle));
            double z = centerPoint.getZ() + (circleRadius * Math.sin(angle));
            locations.add(new Location(world, x, centerPoint.getY(), z));
        }
        return locations;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5, false);

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 50, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 1.125, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH);

        double hPower = SkillConfigManager.getUseSetting(hero, this, "horizontal-power", 2.8, false);
        double vPower = SkillConfigManager.getUseSetting(hero, this, "vertical-power", 0.5, false);

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

            final LivingEntity target = (LivingEntity) entity;

            double individualHPower = hPower;
            double individualVPower = vPower;

            Material mat = target.getLocation().getBlock().getRelative(BlockFace.DOWN).getType();

            switch (mat) {
                case WATER:
                case LAVA:
                case SOUL_SAND:
                    individualHPower /= 2;
                    individualVPower /= 2;
                    break;
                default:
                    break;
            }

            // Damage the target
            addSpellTarget(target, hero);
            damageEntity(target, player, damage, DamageCause.MAGIC, false);

            // Do our knockback
            Location playerLoc = player.getLocation();
            Location targetLoc = target.getLocation();

            double xDir = targetLoc.getX() - playerLoc.getX();
            double zDir = targetLoc.getZ() - playerLoc.getZ();
            double magnitude = Math.sqrt(xDir * xDir + zDir * zDir);

            xDir = xDir / magnitude * individualHPower;
            zDir = zDir / magnitude * individualHPower;

            // Let's bypass the nocheat issues...
            final Vector velocity = new Vector(xDir, individualVPower, zDir);
            NCPUtils.applyExemptions(target, new NCPFunction() {

                @Override
                public void execute() {
                    target.setVelocity(velocity);
                }
            }, Lists.newArrayList("MOVING"), SkillConfigManager.getUseSetting(hero, this, "ncp-exemption-duration", 500, false));
        }

        //player.getWorld().playSound(player.getLocation(), Sound.HURT, 1.3F, 0.5F);
        //player.getWorld().playEffect(player.getLocation(), Effect.EXPLOSION, 3);
        player.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, player.getLocation(), 3, 0, 0, 0, 1);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.5F, 1.0F);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8F, 1.0F);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8F, 1.0F);

        for (double r = 1; r < 5 * 2; r++) {
            ArrayList<Location> particleLocations = circle(player.getLocation(), 72, r / 2);
            for (int i = 0; i < particleLocations.size(); i++) {
                //player.getWorld().spigot().playEffect(particleLocations.get(i).add(0, 0.1, 0), Effect.TILE_BREAK, player.getLocation().getBlock().getRelative(BlockFace.DOWN).getType().getId(), 0, 0, 0.3F, 0, 0.1F, 2, 16);
                player.getWorld().spawnParticle(Particle.BLOCK_CRACK, particleLocations.get(i).add(0, 0.1, 0), 2, 0, 0.3, 0, 0.1, player.getLocation().getBlock().getRelative(BlockFace.DOWN).getBlockData());
            }
        }

        return SkillResult.NORMAL;
    }
}
