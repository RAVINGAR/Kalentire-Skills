package com.herocraftonline.heroes.characters.skill.unfinishedskills;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;

import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.hooks.NCPExemptionManager;

public class SkillJump extends ActiveSkill {

    private boolean ncpEnabled = false;

    public SkillJump(Heroes plugin) {
        super(plugin, "Jump");
        setDescription("You jump into the air");
        setUsage("/skill jump");
        setArgumentRange(0, 0);
        setIdentifiers("skill jump");
        setTypes(SkillType.MOVEMENT, SkillType.PHYSICAL);

        if (Bukkit.getServer().getPluginManager().getPlugin("NoCheatPlus") != null) {
            ncpEnabled = true;
        }
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set("no-air-jump", false);
        node.set("horizontal-power", Double.valueOf(1.0));
        node.set("vertical-power", Double.valueOf(1.0));
        node.set("ncp-exemption-duration", Integer.valueOf(2000));

        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        Material mat = player.getLocation().getBlock().getRelative(BlockFace.DOWN).getType();
        if ((SkillConfigManager.getUseSetting(hero, this, "no-air-jump", true) && noJumpMaterials.contains(mat)) || player.isInsideVehicle()) {
            Messaging.send(player, "You can't jump while mid-air or from inside a vehicle!");
            return SkillResult.FAIL;
        }

        // Let's bypass the nocheat issues...
        if (ncpEnabled) {
            if (!player.isOp()) {
                long duration = SkillConfigManager.getUseSetting(hero, this, "ncp-exemption-duration", 2000, false);
                if (duration > 0) {
                    NCPExemptionEffect ncpExemptEffect = new NCPExemptionEffect(this, duration);
                    hero.addEffect(ncpExemptEffect);
                }
            }
        }

        // Calculate jump values
        float pitch = player.getEyeLocation().getPitch();
        if (pitch > 0) {
            pitch = -pitch;
        }
        float multiplier = (90f + pitch) / 50f;

        double vPower = SkillConfigManager.getUseSetting(hero, this, "vertical-power", 1.0, false);
        Vector velocity = player.getVelocity().setY(vPower);

        Vector directionVector = player.getLocation().getDirection();
        directionVector.setY(0);
        directionVector.normalize();
        directionVector.multiply(multiplier);

        velocity.add(directionVector);
        double hPower = SkillConfigManager.getUseSetting(hero, this, "horizontal-power", 1.0, false);
        velocity.multiply(new Vector(hPower, 1, hPower));

        // Jump!
        player.setVelocity(velocity);
        player.setFallDistance(-8f);

        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.SKELETON_IDLE, 10.0F, 1.0F);
        broadcastExecuteText(hero);

        return SkillResult.NORMAL;
    }

    private class NCPExemptionEffect extends ExpirableEffect {

        public NCPExemptionEffect(Skill skill, long duration) {
            super(skill, "NCPExemptionEffect_MOVING", duration);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            final Player player = hero.getPlayer();

            NCPExemptionManager.exemptPermanently(player, CheckType.MOVING);
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            final Player player = hero.getPlayer();

            NCPExemptionManager.unexempt(player, CheckType.MOVING);
        }
    }

    private static final Set<Material> noJumpMaterials;
    static {
        noJumpMaterials = new HashSet<Material>();
        noJumpMaterials.add(Material.WATER);
        noJumpMaterials.add(Material.AIR);
        noJumpMaterials.add(Material.LAVA);
        noJumpMaterials.add(Material.LEAVES);
        noJumpMaterials.add(Material.SOUL_SAND);
    }
}
