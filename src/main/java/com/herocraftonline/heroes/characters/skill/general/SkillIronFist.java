package com.herocraftonline.heroes.characters.skill.general;

import com.google.common.collect.Lists;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.common.SlowEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.ncp.NCPFunction;
import com.herocraftonline.heroes.characters.skill.ncp.NCPUtils;
import com.herocraftonline.heroes.util.GeometryUtil;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.Vector;

import java.util.List;

public class SkillIronFist extends ActiveSkill {

    public SkillIronFist(Heroes plugin) {
        super(plugin, "IronFist");
        setDescription("Strike the ground with an iron fist, striking all targets within $1 blocks, dealing $2 damage and knocking them away from you. " +
                "Targets hit will also be slowed for $3 second(s).");
        setUsage("/skill ironfist");
        setArgumentRange(0, 0);
        setIdentifiers("skill ironfist");
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.FORCE, SkillType.DAMAGING, SkillType.AGGRESSIVE, SkillType.AREA_OF_EFFECT);
    }

    @Override
    public String getDescription(Hero hero) {
        double radius = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.RADIUS, false);
        double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE, false);
        int duration = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.DURATION, false);

        String formattedDamage = Util.decFormat.format(damage);
        String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription()
                .replace("$1", Util.decFormat.format(radius))
                .replace("$2", formattedDamage)
                .replace("$3", formattedDuration);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.DAMAGE.node(), 50.0);
        config.set(SkillSetting.DAMAGE_INCREASE_PER_STRENGTH.node(), 0.0);
        config.set(SkillSetting.RADIUS.node(), 5.0);
        config.set(SkillSetting.DURATION.node(), 5000);
        config.set("slow-amplifier", 1);
        config.set("horizontal-power", 0.0);
        config.set("horizontal-power-increase-per-intellect", 0.0);
        config.set("vertical-power", 0.4);
        config.set("vertical-power-increase-per-intellect", 0.0);
        config.set("ncp-exemption-duration", 1000);
        return config;
    }

    @Override
    public void onWarmup(Hero hero) {
        Player player = hero.getPlayer();
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.7F, 0.4F);
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        double radius = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.RADIUS, false);
        double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE, false);
        double hPower = SkillConfigManager.getScaledUseSettingDouble(hero, this, "horizontal-power", false);
        double vPower = SkillConfigManager.getScaledUseSettingDouble(hero, this, "vertical-power", false);
        int duration = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.DURATION, false);

        int slowAmplifier = SkillConfigManager.getUseSetting(hero, this, "slow-amplifier", 1, false);

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
            damageEntity(target, player, damage, DamageCause.ENTITY_ATTACK, false);

            // Do our knockback
            Location playerLoc = player.getLocation();
            Location targetLoc = target.getLocation();

            double xDir = targetLoc.getX() - playerLoc.getX();
            double zDir = targetLoc.getZ() - playerLoc.getZ();
            double magnitude = Math.sqrt(xDir * xDir + zDir * zDir);

            final double x = xDir / magnitude * individualHPower;
            final double z = zDir / magnitude * individualHPower;
            final double y = individualVPower;

            // Let's bypass the nocheat issues...
            NCPUtils.applyExemptions(target, () -> target.setVelocity(new Vector(x, y, z)), Lists.newArrayList("MOVING"), SkillConfigManager.getUseSetting(hero, this, "ncp-exemption-duration", 500, false));

            SlowEffect sEffect = new SlowEffect(this, player, duration, slowAmplifier, null, null);
            sEffect.types.add(EffectType.DISPELLABLE);
            CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
            targetCT.addEffect(sEffect);
        }

        for (double r = 1; r < 5 * 2; r++) {
            List<Location> particleLocations = GeometryUtil.circle(player.getLocation(), 72, r / 2);
            for (Location particleLocation : particleLocations) {
                // player.getWorld().spigot().playEffect(particleLocations.get(i).add(0, 0.1, 0), Effect.TILE_BREAK, player.getLocation().getBlock().getRelative(BlockFace.DOWN).getType().getId(), 0, 0, 0.3F, 0, 0.1F, 2, 16);
                player.getWorld().spawnParticle(Particle.BLOCK_CRACK, particleLocation.add(0, 0.1, 0), 2, 0, 0.3, 0, 0.1, player.getLocation().getBlock().getRelative(BlockFace.DOWN).getBlockData());
            }
        }

        player.getWorld().playEffect(player.getLocation(), Effect.MOBSPAWNER_FLAMES, 3);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.5F, 1.0F);

        return SkillResult.NORMAL;
    }
}
