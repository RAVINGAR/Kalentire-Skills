package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.heroes.nms.physics.NMSPhysics;
import com.herocraftonline.heroes.nms.physics.RayCastFlag;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.CylinderEffect;
import de.slikey.effectlib.util.ParticleEffect;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;

import java.util.EnumSet;
import java.util.function.Predicate;

import static com.herocraftonline.heroes.characters.skill.SkillType.*;
import static com.herocraftonline.heroes.characters.skill.SkillType.SILENCEABLE;
import static com.herocraftonline.heroes.characters.skill.SkillType.UNINTERRUPTIBLE;

public class SkillDamageHomingMissile extends SkillBaseHomingMissile {

    private static NMSPhysics physics = NMSHandler.getInterface().getNMSPhysics();

    public SkillDamageHomingMissile(Heroes plugin) {
        super(plugin, "DamageHomingMissile");
        setDescription("Damage stuff with homing missile");
        setUsage("/skill DamageHomingMissile");
        setIdentifiers("skill " + getName());
        setTypes(DAMAGING, NO_SELF_TARGETTING, UNINTERRUPTIBLE, SILENCEABLE);
        setArgumentRange(0, 0);
    }

    @Override
    public String getDescription(Hero hero) {
        return super.getDescription();
    }

    @Override
    public SkillResult use(Hero hero, String[] strings) {

        broadcastExecuteText(hero);

        Player player = hero.getPlayer();
        World world = player.getWorld();

        Vector start = player.getEyeLocation().toVector();
        Vector end = player.getEyeLocation().getDirection().multiply(50);

        Vector target = physics.rayCast(world, player, start, end,
                RayCastFlag.BLOCK_HIGH_DETAIL, RayCastFlag.BLOCK_IGNORE_NON_SOLID).getPoint();

        super.fireHomingMissile(hero, true, 5,
                () -> target, start, player.getEyeLocation().getDirection().multiply(0.2), 0.2, 4, 1, 600, entity -> false, block -> false,
                EnumSet.of(RayCastFlag.BLOCK_HIGH_DETAIL, RayCastFlag.BLOCK_HIT_FLUID_SOURCE, RayCastFlag.ENTITY_HIT_SPECTATORS));

        return SkillResult.NORMAL;
    }

    @Override
    protected void onEntityHit(Hero hero, Entity entity, Vector hitOrigin, Vector hitForce) {

    }

    @Override
    protected void onEntityPassed(Hero hero, Entity entity, Vector hitOrigin, Vector hitForce) {
        if (entity instanceof  LivingEntity) {
            LivingEntity livingEntity = (LivingEntity) entity;

            if (damageCheck(livingEntity, hero.getPlayer())) {
                damageEntity(livingEntity, hero.getPlayer(), 10.0, EntityDamageEvent.DamageCause.MAGIC, false);
                livingEntity.setVelocity(livingEntity.getVelocity().add(hitForce));
            }
        }
    }

    @Override
    protected void onBlockHit(Hero hero, Block block, Vector hitPosition, Vector hitForce, BlockFace hitFace) {

    }

    @Override
    protected void onBlockPassed(Hero hero, Block block, Vector hitPosition, Vector hitForce, BlockFace hitFace) {

    }

    @Override
    protected void renderMissilePath(World world, Vector start, Vector end, double radius) {

        EffectManager em = new EffectManager(plugin);

        Location loc = start.clone().add(end.clone().subtract(start).multiply(0.5)).toLocation(world);
        loc.setDirection(end.clone().subtract(start));

        CylinderEffect cyl = new CylinderEffect(em);
        cyl.setLocation(loc);
        cyl.asynchronous = true;

        cyl.radius = (float) (radius * 1);
        cyl.height = (float) (start.distance(end));
        cyl.particle = ParticleEffect.CLOUD;
        cyl.particles = 40;
        cyl.solid = true;
        cyl.rotationX = Math.toRadians(loc.getPitch() + 90);
        cyl.rotationY = -Math.toRadians(loc.getYaw());
        cyl.angularVelocityX = 0;
        cyl.angularVelocityY = 0;
        cyl.angularVelocityZ = 0;
        cyl.iterations = 10;
        cyl.visibleRange = 100;

        cyl.start();
        em.disposeOnTermination();
    }
}
