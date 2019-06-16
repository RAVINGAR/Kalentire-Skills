package com.herocraftonline.heroes.characters.skill.reborn.bard;

import com.google.common.collect.Lists;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.ncp.NCPFunction;
import com.herocraftonline.heroes.characters.skill.ncp.NCPUtils;
import com.herocraftonline.heroes.util.GeometryUtil;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.Vector;

import java.util.List;

public class SkillDoomstarRequiem extends ActiveSkill {

    public SkillDoomstarRequiem(Heroes plugin) {
        super(plugin, "DoomstarRequiem");
        setDescription("You start chanting the Doomstar Requiem, affecting all targets within $1 blocks. All targets " +
                "hit with the Doomstar are dealt $2 magical damage and knocked back a great distance.");
        setUsage("/skill doomstarrequiem");
        setIdentifiers("skill doomstarrequiem");
        setArgumentRange(0, 0);
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.ABILITY_PROPERTY_SONG, SkillType.FORCE, SkillType.DAMAGING, SkillType.AGGRESSIVE, SkillType.AREA_OF_EFFECT);
    }

    @Override
    public String getDescription(Hero hero) {
        double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5.0, false);
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 50.0, false);

        return getDescription()
                .replace("$1", Util.decFormat.format(radius))
                .replace("$2", Util.decFormat.format(damage));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.DAMAGE.node(), 50.0);
        config.set(SkillSetting.DAMAGE_INCREASE_PER_CHARISMA.node(), 0.0);
        config.set(SkillSetting.RADIUS.node(), 5.0);
        config.set("horizontal-power", 0.0);
        config.set("vertical-power", 0.4);
        config.set("ncp-exemption-duration", 0);
        config.set(SkillSetting.DELAY.node(), 1000);
        return config;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5.0, false);
        double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE, false);

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
        player.getWorld().playEffect(player.getLocation(), Effect.EXPLOSION, 3);
        //player.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, player.getLocation(), 1, 0, 0, 0, 1); 1.13
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.5F, 1.0F);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8F, 1.0F);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8F, 1.0F);

        for (double r = 1; r < 5 * 2; r++) {
            List<Location> particleLocations = GeometryUtil.circle(player.getLocation(), 72, r / 2);
            for (int i = 0; i < particleLocations.size(); i++) {
                player.getWorld().spigot().playEffect(particleLocations.get(i).add(0, 0.1, 0), Effect.TILE_BREAK, player.getLocation().getBlock().getRelative(BlockFace.DOWN).getType().getId(), 0, 0, 0.3F, 0, 0.1F, 2, 16);
                //player.getWorld().spawnParticle(Particle.BLOCK_CRACK, particleLocations.get(i), 2, 0, 0.3, 0, 0.1, player.getLocation().getBlock().getRelative(BlockFace.DOWN).getBlockData()); 1.13
            }
        }

        return SkillResult.NORMAL;
    }
}
