package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.hooks.NCPExemptionManager;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

public class SkillDisengage extends ActiveSkill 
{

    private boolean ncpEnabled = false;

    public SkillDisengage(Heroes plugin) 
    {
        super(plugin, "Disengage");
        setDescription("You leap backwards through the air. The power of the leap increases per level.");
        setUsage("/skill disengage");
        setArgumentRange(0, 0);
        setIdentifiers("skill disengage");
        setTypes(SkillType.MOVEMENT_INCREASING);

        if (Bukkit.getServer().getPluginManager().getPlugin("NoCheatPlus") != null) ncpEnabled = true;
    }

    @Override
    public String getDescription(Hero hero) 
    {
        return getDescription();
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set("no-air-jump", false);
        node.set("horizontal-power", Double.valueOf(0.5));
        node.set("horizontal-power-increase", Double.valueOf(0.0125));
        node.set("vertical-power", Double.valueOf(0.5));
        node.set("vertical-power-increase", Double.valueOf(0.00625));
        node.set("ncp-exemption-duration", Integer.valueOf(2000));

        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        Location playerLoc = player.getLocation();
        Material belowMat = playerLoc.getBlock().getRelative(BlockFace.DOWN).getType();

        if ((SkillConfigManager.getUseSetting(hero, this, "no-air-jump", true) && nodisengageMaterials.contains(belowMat)) || player.isInsideVehicle()) 
        {
            Messaging.send(player, "You can't Disengage while mid-air or from inside a vehicle!");
            return SkillResult.FAIL;
        }

        broadcastExecuteText(hero);

        if (ncpEnabled) 
        {
            if (!player.isOp()) 
            {
                long duration = SkillConfigManager.getUseSetting(hero, this, "ncp-exemption-duration", 2000, false);
                if (duration > 0) 
                {
                    NCPExemptionEffect ncpExemptEffect = new NCPExemptionEffect(this, player, duration);
                    hero.addEffect(ncpExemptEffect);
                }
            }
        }
        
        float pitch = player.getEyeLocation().getPitch();
        if (pitch > 0)
            pitch = -pitch;
        float multiplier = (90f + pitch) / 50f;

        boolean weakenVelocity = false;
        switch (belowMat) {
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

        double vPower = SkillConfigManager.getUseSetting(hero, this, "vertical-power", Double.valueOf(0.5), false);
        double vPowerIncrease = SkillConfigManager.getUseSetting(hero, this, "vertical-power-increase", Double.valueOf(0.0125), false)
                * hero.getAttributeValue(AttributeType.DEXTERITY);
        vPower += vPowerIncrease;

        if (vPower > 2.0)
            vPower = 2.0;

        if (weakenVelocity)
            vPower *= 0.75;

        Vector velocity = player.getVelocity().setY(vPower);

        Vector directionVector = player.getLocation().getDirection();
        directionVector.setY(0);
        directionVector.normalize();
        directionVector.multiply(multiplier);

        velocity.add(directionVector);
        double hPower = SkillConfigManager.getUseSetting(hero, this, "horizontal-power", Double.valueOf(0.5), false);
        double hPowerIncrease = SkillConfigManager.getUseSetting(hero, this, "horizontal-power-increase", Double.valueOf(0.0125), false)
                * hero.getAttributeValue(AttributeType.DEXTERITY);
        hPower += hPowerIncrease;

        if (weakenVelocity)
            hPower *= 0.75;

        velocity.multiply(new Vector(-hPower, 1, -hPower));

        player.setVelocity(velocity);
        player.setFallDistance(-8f);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERDRAGON_FLAP, 7.0F, 1.0F);
		player.getWorld().spigot().playEffect(player.getLocation(), Effect.CLOUD, 0, 0, 0, 0.1F, 0, 0.5F, 25, 12);

        return SkillResult.NORMAL;
    }

    private class NCPExemptionEffect extends ExpirableEffect 
    {

        public NCPExemptionEffect(Skill skill, Player applier, long duration) 
        {
            super(skill, "NCPExemptionEffect_MOVING", applier, duration);
        }

        @Override
        public void applyToHero(Hero hero) 
        {
            super.applyToHero(hero);
            Player player = hero.getPlayer();

            NCPExemptionManager.exemptPermanently(player, CheckType.MOVING);
        }

        @Override
        public void removeFromHero(Hero hero) 
        {
            super.removeFromHero(hero);
            Player player = hero.getPlayer();

            NCPExemptionManager.unexempt(player, CheckType.MOVING);

        }
    }

    private static final Set<Material> nodisengageMaterials;
    static {
        nodisengageMaterials = new HashSet<Material>();
        nodisengageMaterials.add(Material.STATIONARY_WATER);
        nodisengageMaterials.add(Material.STATIONARY_LAVA);
        nodisengageMaterials.add(Material.WATER);
        nodisengageMaterials.add(Material.LAVA);
        nodisengageMaterials.add(Material.AIR);
        nodisengageMaterials.add(Material.LEAVES);
        nodisengageMaterials.add(Material.SOUL_SAND);
    }
}