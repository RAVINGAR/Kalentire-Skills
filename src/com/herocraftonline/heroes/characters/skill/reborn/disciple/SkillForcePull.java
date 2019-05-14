package com.herocraftonline.heroes.characters.skill.reborn.disciple;

import com.google.common.collect.Lists;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.characters.skill.ncp.NCPFunction;
import com.herocraftonline.heroes.characters.skill.ncp.NCPUtils;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.Vector;

public class SkillForcePull extends TargettedSkill {

    public SkillForcePull(Heroes plugin) {
        super(plugin, "Forcepull");
        setDescription("Deal $1 magic damage and force your target away from you. " +
                "If you target an ally, they will be healed for $2 instead.");
        setUsage("/skill forcepull");
        setIdentifiers("skill forcepull");
        setArgumentRange(0, 0);
        setTypes(SkillType.FORCE, SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.INTERRUPTING, SkillType.SILENCEABLE,
                SkillType.MULTI_GRESSIVE, SkillType.NO_SELF_TARGETTING);
    }

    @Override
    public String getDescription(Hero hero) {
        double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE, false);
        double healing = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.HEALING, false);

        return getDescription()
                .replace("$1", Util.decFormat.format(damage))
                .replace("$2", Util.decFormat.format(healing));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.MAX_DISTANCE.node(), 10.0);
        config.set(SkillSetting.DAMAGE.node(), 50.0);
        config.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 0.0);
        config.set(SkillSetting.HEALING.node(), 50.0);
        config.set(SkillSetting.HEALING_INCREASE_PER_INTELLECT.node(), 0.0);
        config.set("horizontal-power", 0.3);
        config.set("horizontal-power-increase-per-intellect", 0.0125);
        config.set("vertical-power", 0.4);
        config.set("ncp-exemption-duration", 1000);
        config.set("pull-delay", 0.2);
        return config;
    }

    @Override
    public SkillResult use(Hero hero, final LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero, target);

        Location playerLoc = player.getLocation();
        Location targetLoc = target.getLocation();

        Material mat = targetLoc.getBlock().getRelative(BlockFace.DOWN).getType();

        boolean weakenVelocity = false;
        switch (mat) {
            case WATER:
            case LAVA:
            case SOUL_SAND:
                weakenVelocity = true;
                break;
            default:
                break;
        }

        double tempVPower = SkillConfigManager.getUseSetting(hero, this, "vertical-power", 0.4, false);

        if (weakenVelocity)
            tempVPower *= 0.75;

        final double vPower = tempVPower;

        final Vector pushUpVector = new Vector(0, vPower, 0);
        // Let's bypass the nocheat issues...
        NCPUtils.applyExemptions(target, new NCPFunction() {

            @Override
            public void execute() {
                target.setVelocity(pushUpVector);
            }
        }, Lists.newArrayList("MOVING"), SkillConfigManager.getUseSetting(hero, this, "ncp-exemption-duration", 1000, false));

        final double xDir = (playerLoc.getX() - targetLoc.getX()) / 3;
        final double zDir = (playerLoc.getZ() - targetLoc.getZ()) / 3;

        double tempHPower = SkillConfigManager.getScaledUseSettingDouble(hero, this, "horizontal-power", false);

        if (weakenVelocity)
            tempHPower *= 0.75;

        final double hPower = tempHPower;

        // push them "up" first. THEN we can pull them to us.
        double delay = SkillConfigManager.getUseSetting(hero, this, "pull-delay", 0.2, false);
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            public void run() {
                // Push them away
                //double yDir = player.getVelocity().getY();
                Vector pushVector = new Vector(xDir, 0, zDir).normalize().multiply(hPower).setY(vPower);
                target.setVelocity(pushVector);
            }
        }, (long) (delay * 20));

        if (hero.isAlliedTo(target)) {
            double healing = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.HEALING, false);
            target.setFallDistance(-1);

            CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
            targetCT.tryHeal(hero, this, healing);  // Ignore failures
        } else {
            double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE, false);

            if (damage > 0) {
                addSpellTarget(target, hero);
                damageEntity(target, player, damage, DamageCause.MAGIC, false);
            }
        }

        //player.getWorld().spigot().playEffect(target.getLocation().add(0, 0.5, 0), org.bukkit.Effect.FLYING_GLYPH, 0, 0, 0, 0, 0, 1, 150, 16);
        player.getWorld().spawnParticle(Particle.ENCHANTMENT_TABLE, target.getLocation().add(0, 0.5, 0), 150, 0, 0, 0, 1);

        return SkillResult.NORMAL;
    }
}
