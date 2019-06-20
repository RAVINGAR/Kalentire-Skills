package com.herocraftonline.heroes.characters.skill.reborn.disciple;

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
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class SkillTornadoKick extends ActiveSkill {

    public SkillTornadoKick(Heroes plugin) {
        super(plugin, "TornadoKick");
        setDescription("Strike the ground with an iron fist, striking all targets within $1 blocks, dealing $2 damage and knocking them away from you. " +
                "Targets hit will also be slowed for $3 second(s).");
        setUsage("/skill tornadokick");
        setIdentifiers("skill tornadokick");
        setArgumentRange(0, 0);
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.FORCE, SkillType.DAMAGING, SkillType.AGGRESSIVE, SkillType.AREA_OF_EFFECT);
    }

    @Override
    public String getDescription(Hero hero) {
        double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5.0, false);
        double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE, false);
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 4000, false);

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

        double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5.0, false);
        double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE, false);
        double hPower = SkillConfigManager.getScaledUseSettingDouble(hero, this, "horizontal-power", false);
        double vPower = SkillConfigManager.getScaledUseSettingDouble(hero, this, "vertical-power", false);
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 4000, false);

        int slowAmplifier = SkillConfigManager.getUseSetting(hero, this, "slow-amplifier", 1, false);

        broadcastExecuteText(hero);

        applyJumpVelocity(player, new Vector(0, 1.25, 0));

        List<Entity> entities = hero.getPlayer().getNearbyEntities(radius, radius, radius);
        for (Entity entity : entities) {
            if (!(entity instanceof LivingEntity)) {
                continue;
            }

            // Check if the target is damagable
            final LivingEntity target = (LivingEntity) entity;
            if (!damageCheck(player, target)) {
                continue;
            }

            applyJumpVelocity(target, new Vector(0, 1.25, 0));

            knockback(hero, player, damage, hPower, vPower, target);

            SlowEffect sEffect = new SlowEffect(this, player, duration, slowAmplifier, null, null);
            sEffect.types.add(EffectType.DISPELLABLE);
            CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
            targetCT.addEffect(sEffect);
        }

        // TORNADOOOO
        for (int h = 0; h < 2; h++) {
            List<Location> locations = GeometryUtil.circle(player.getLocation(), 36, (double) h + 1.2);
            for (int i = 0; i < locations.size(); i++) {
                //player.getWorld().spigot().playEffect(locations.get(i).add(0, (double) h + 0.2, 0), org.bukkit.Effect.CLOUD, 0, 0, 0, 0, 0, 0, 8, 16);
                player.getWorld().spawnParticle(Particle.CLOUD, locations.get(i).add(0, (double) h + 0.2, 0), 8, 0, 0, 0, 0);
            }
        }

        player.getWorld().playEffect(player.getLocation(), Effect.MOBSPAWNER_FLAMES, 3);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.5F, 1.0F);

        return SkillResult.NORMAL;
    }

    public void knockback(Hero hero, Player player, double damage, double hPower, double vPower, LivingEntity target) {
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
        NCPUtils.applyExemptions(target, new NCPFunction() {

            @Override
            public void execute() {
                target.setVelocity(new Vector(x, y, z));
            }
        }, Lists.newArrayList("MOVING"), SkillConfigManager.getUseSetting(hero, this, "ncp-exemption-duration", 500, false));
    }

    public void launchUpwards(Hero hero, LivingEntity target, Vector velocity) {
        long exemptionDuration = SkillConfigManager.getUseSetting(hero, this, "ncp-exemption-duration", 0, false);
        if (exemptionDuration > 0) {
            NCPUtils.applyExemptions(target, new NCPFunction() {
                @Override
                public void execute() {
                    applyJumpVelocity(target, velocity);
                }
            }, Lists.newArrayList("MOVING"), exemptionDuration);
        } else {
            applyJumpVelocity(target, velocity);
        }
    }

    private void applyJumpVelocity(LivingEntity target, Vector velocity) {
        target.setVelocity(velocity);
        target.setFallDistance(-3F);
    }
}
