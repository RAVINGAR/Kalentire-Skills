package com.herocraftonline.heroes.characters.skill.reborn;

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

public class SkillForcePush extends TargettedSkill {

    public SkillForcePush(Heroes plugin) {
        super(plugin, "Forcepush");
        setDescription("Deal $1 physical damage and force your target away from you. The push power is affected by your Intellect.");
        setUsage("/skill forcepush");
        setArgumentRange(0, 0);
        setIdentifiers("skill forcepush");
        setTypes(SkillType.FORCE, SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.SILENCEABLE, SkillType.DAMAGING, SkillType.AGGRESSIVE);
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

        node.set(SkillSetting.MAX_DISTANCE.node(), 5);
        node.set(SkillSetting.DAMAGE.node(), 50);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 1.6);
        node.set("horizontal-power", 1.5);
        node.set("horizontal-power-increase-per-intellect", 0.0375);
        node.set("vertical-power", 0.25);
        node.set("vertical-power-increase-per-intellect", 0.0075);
        node.set("ncp-exemption-duration", 1500);
        node.set("push-delay", 0.2);

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
        NCPUtils.applyExemptions(target, new NCPFunction() {
            
            @Override
            public void execute()
            {
                target.setVelocity(pushUpVector);                
            }
        }, Lists.newArrayList("MOVING"), SkillConfigManager.getUseSetting(hero, this, "ncp-exemption-duration", 1500, false));

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
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            public void run() {
                // Push them away
                //double yDir = player.getVelocity().getY();
                Vector pushVector = new Vector(xDir, 0, zDir).normalize().multiply(hPower).setY(vPower);
                target.setVelocity(pushVector);
            }
        }, (long) (delay * 20));

        player.getWorld().playSound(target.getLocation(), Sound.ENTITY_GENERIC_BURN, 0.5f, 2.0f);

//        player.getWorld().spigot().playEffect(target.getLocation().add(0, 0.5, 0),
//                org.bukkit.Effect.WITCH_MAGIC,
//                0, 0,
//                0, 0, 0,
//                1,
//                150,
//                SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 5, false) +
        player.getWorld().spawnParticle(Particle.SPELL_WITCH, target.getLocation().add(0, 0.5, 0), 150, 0, 0, 0, 1, null, true);

        return SkillResult.NORMAL;
    }
}
