package com.herocraftonline.heroes.characters.skill.pack8;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.Vector;

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

public class SkillForcePull extends TargettedSkill {

    public SkillForcePull(Heroes plugin) {
        super(plugin, "Forcepull");
        setDescription("Deal $1 physical damage and force your target towards you. The Targeting distance of this ability is affected by your Intellect.");
        setUsage("/skill forcepull");
        setArgumentRange(0, 0);
        setIdentifiers("skill forcepull");
        setTypes(SkillType.FORCE, SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.INTERRUPTING, SkillType.SILENCEABLE, SkillType.DAMAGING, SkillType.AGGRESSIVE);
    }

    @Override
    public String getDescription(Hero hero) {
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 50, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.6, false);
        damage += (int) (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        return getDescription().replace("$1", damage + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 8);
        node.set(SkillSetting.DAMAGE.node(), 50);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 1.6);
        node.set("horizontal-power", 0.3);
        node.set("horizontal-power-increase-per-intellect", 0.0125);
        node.set("vertical-power", 0.4);
        node.set("ncp-exemption-duration", 1000);
        node.set("pull-delay", 0.2);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, final LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero, target);

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 50, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.6, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT);

        if (damage > 0) {
            addSpellTarget(target, hero);
            damageEntity(target, player, damage, DamageCause.MAGIC, false);
        }

        Location playerLoc = player.getLocation();
        Location targetLoc = target.getLocation();

        Material mat = targetLoc.getBlock().getRelative(BlockFace.DOWN).getType();

        boolean weakenVelocity = false;
        switch (mat) {
            case STATIONARY_WATER:
            case STATIONARY_LAVA:
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
            public void execute()
            {
                target.setVelocity(pushUpVector);
            }
        }, Lists.newArrayList("MOVING"), SkillConfigManager.getUseSetting(hero, this, "ncp-exemption-duration", 1000, false));

        final double xDir = (playerLoc.getX() - targetLoc.getX()) / 3;
        final double zDir = (playerLoc.getZ() - targetLoc.getZ()) / 3;

        double tempHPower = SkillConfigManager.getUseSetting(hero, this, "horizontal-power", 0.5, false);
        double hPowerIncrease = SkillConfigManager.getUseSetting(hero, this, "horizontal-power-increase-per-intellect", 0.0125, false);
        tempHPower += (hPowerIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

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

        player.getWorld().spigot().playEffect(target.getLocation().add(0, 0.5, 0), org.bukkit.Effect.FLYING_GLYPH, 0, 0, 0, 0, 0, 1, 150, 16);

        return SkillResult.NORMAL;
    }
}
