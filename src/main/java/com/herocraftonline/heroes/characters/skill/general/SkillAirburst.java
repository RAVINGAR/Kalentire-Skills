package com.herocraftonline.heroes.characters.skill.general;

import com.google.common.collect.Lists;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.characters.skill.ncp.NCPFunction;
import com.herocraftonline.heroes.characters.skill.ncp.NCPUtils;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.Vector;

public class SkillAirburst extends TargettedSkill {

    public SkillAirburst(Heroes plugin) {
        super(plugin, "Airburst");
        setDescription("You strike the target with nature's gust for $1 earth damage.");
        setUsage("/skill airburst");
        setArgumentRange(0, 0);
        setIdentifiers("skill airburst");
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.DAMAGING, SkillType.ABILITY_PROPERTY_EARTH, SkillType.SILENCEABLE, SkillType.AGGRESSIVE, SkillType.FORCE);
    }

    public String getDescription(Hero hero) {
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 80, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 0.75, false);
        damage += (int) (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        return getDescription().replace("$1", damage + "");
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 12);
        node.set(SkillSetting.DAMAGE.node(), 80);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 0.75);
        node.set("horizontal-power", 0.30);
        node.set("horizontal-power-increase-per-intellect", 0.0375);
        node.set("vertical-power", 0.15);
        node.set("vertical-power-increase-per-intellect", 0.0075);
        node.set("ncp-exemption-duration", 1500);
        node.set("push-delay", 0.2);
        return node;
    }

    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 80, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 0.75, false);
        damage += (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        addSpellTarget(target, hero);
        damageEntity(target, player, damage, DamageCause.MAGIC, false);

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

        double tempVPower = SkillConfigManager.getUseSetting(hero, this, "vertical-power", 0.25, false);
        double vPowerIncrease = SkillConfigManager.getUseSetting(hero, this, "vertical-power-increase-per-intellect", 0.0075, false);
        tempVPower += (vPowerIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        if (weakenVelocity)
            tempVPower *= 0.75;

        final double vPower = tempVPower;

        final Vector pushUpVector = new Vector(0, vPower, 0);
        // Let's bypass the nocheat issues...
        NCPUtils.applyExemptions(target, () -> target.setVelocity(pushUpVector), Lists.newArrayList("MOVING"), SkillConfigManager.getUseSetting(hero, this, "ncp-exemption-duration", 1500, false));

        final double xDir = targetLoc.getX() - playerLoc.getX();
        final double zDir = targetLoc.getZ() - playerLoc.getZ();

        double tempHPower = SkillConfigManager.getUseSetting(hero, this, "horizontal-power", 1.5, false);
        double hPowerIncrease = SkillConfigManager.getUseSetting(hero, this, "horizontal-power-increase-per-intellect", 0.0375, false);
        tempHPower += hPowerIncrease * hero.getAttributeValue(AttributeType.INTELLECT);

        if (weakenVelocity)
            tempHPower *= 0.75;

        final double hPower = tempHPower;

        // Push them "up" first. THEN we can push them away.
        double delay = SkillConfigManager.getUseSetting(hero, this, "push-delay", 0.2, false);
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            // Push them away
            //double yDir = player.getVelocity().getY();
            Vector pushVector = new Vector(xDir, 0, zDir).normalize().multiply(hPower).setY(vPower);
            target.setVelocity(pushVector);
        }, (long) (delay * 20));

        broadcastExecuteText(hero, target);

        target.getWorld().spawnParticle(Particle.CLOUD, target.getLocation().add(0, 0.5, 0), 100, 0, 0, 0, 1);
        //target.getWorld().spawnParticle(Particle.BLOCK_CRACK, target.getLocation(), 75, 0.2, 0.2, 0.2, 0.1, Bukkit.createBlockData(Material.DIRT));
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_GRAVEL_HIT, 7.0F, 0.6F);

        return SkillResult.NORMAL;
    }
}
