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
import com.herocraftonline.heroes.nms.physics.NMSPhysics;
import com.herocraftonline.heroes.nms.physics.RayCastHit;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;

public class SkillRisingLance extends ActiveSkill {
    protected final NMSPhysics physics = NMSPhysics.instance();

    public SkillRisingLance(Heroes plugin) {
        super(plugin, "RisingLance");
        setDescription("You point your lance straight up and launch into the air, grabbing any target foolish enough to be in your path. " +
                "Grabbed targets are dealt $1 damage.");
        setUsage("/skill risinglance");
        setIdentifiers("skill risinglance");
        setArgumentRange(0, 0);
        setTypes(SkillType.DAMAGING, SkillType.MOVEMENT_INCREASING, SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.AGGRESSIVE);
    }

    @Override
    public String getDescription(Hero hero) {
        long damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE.node(), 50, false);
        return getDescription()
                .replace("$1", Util.decFormat.format(damage));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.MAX_DISTANCE.node(), 3.5);
        config.set(SkillSetting.DAMAGE.node(), 35);
        config.set("vertical-power", 0.85);
        config.set("vertical-power-increase-per-dexterity", 0.0);
        config.set("ncp-exemption-duration", 0);
        return config;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        final Player player = hero.getPlayer();

        broadcastExecuteText(hero);

        int dexterity = hero.getAttributeValue(AttributeType.DEXTERITY);
        double vPower = SkillConfigManager.getUseSetting(hero, this, "vertical-power", 0.5, false);
        double dexIncrease = SkillConfigManager.getUseSetting(hero, this, "vertical-power-increase-per-dexterity", 0.0, false);
        vPower += dexterity * dexIncrease;

        if (vPower > 4.0)
            vPower = 4.0;

        final Vector velocity = player.getVelocity().setY(vPower);

        LivingEntity target = checkForTarget(hero, player);
        if (target != null) {
            double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 35, false);
            addSpellTarget(target, hero);
            damageEntity(target, player, damage, EntityDamageEvent.DamageCause.ENTITY_ATTACK, false);
            launchTarget(hero, target, velocity);
        }

        launchTarget(hero, player, velocity);

        return SkillResult.NORMAL;
    }

    public LivingEntity checkForTarget(Hero hero, Player player) {
        double maxDistance = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 25.0, false);
        Location eyeLocation = player.getEyeLocation();
        Vector normal = eyeLocation.getDirection();
        Vector start = eyeLocation.toVector();
        Vector end = normal.clone().multiply(maxDistance).add(start);

        RayCastHit hit = this.physics.rayCast(player.getWorld(), player, start, end,
                x -> true,
                x -> x instanceof LivingEntity && damageCheck(player, (LivingEntity) x));

        if (hit == null || !(hit.getEntity() instanceof LivingEntity))
            return null;
        return (LivingEntity) hit.getEntity();
    }

    public void launchTarget(Hero hero, LivingEntity target, Vector velocity) {
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
        target.setFallDistance(-1F);
    }

    private boolean shouldWeaken(Location targetLoc) {
        Material mat = targetLoc.getBlock().getRelative(BlockFace.DOWN).getType();
        switch (mat) {
            case WATER:
            case LAVA:
            case SOUL_SAND:
                return true;
            default:
                return false;
        }
    }
}