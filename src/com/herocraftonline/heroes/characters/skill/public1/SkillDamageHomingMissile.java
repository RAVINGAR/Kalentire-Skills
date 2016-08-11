package com.herocraftonline.heroes.characters.skill.public1;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.heroes.nms.physics.NMSPhysics;
import com.herocraftonline.heroes.nms.physics.RayCastFlag;
import com.herocraftonline.heroes.nms.physics.RayCastHit;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.CylinderEffect;
import de.slikey.effectlib.util.ParticleEffect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.NumberConversions;
import org.bukkit.util.Vector;

import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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
        Vector end = player.getEyeLocation().getDirection().multiply(100).add(start);

        final RayCastHit hitBlock = physics.rayCastBlocks(world, start, end, false, true, true);

        if (hitBlock != null) {
            end = hitBlock.getPoint();
        }

        final List<LivingEntity> targets = physics.getEntitiesInVolume(world, player,
                physics.createCapsule(start, end, 2.5), entity -> entity instanceof LivingEntity, false)
                .stream()
                .map(entity -> (LivingEntity) entity)
                .collect(Collectors.toList());

        Random random = new Random();

        for (int i = 0; i < 10; i++) {

            final Supplier<Vector> targetSupplier;

            if (targets.isEmpty()) {
                final Vector target = end.clone();
                targetSupplier = () -> target;
            } else {
                final LivingEntity target = targets.get(random.nextInt(targets.size()));
                targetSupplier = () -> physics.getEntityAABB(target).getCenter();
            }

            Vector launchVelocity = Vector.getRandom().subtract(new Vector(0.5, 0.5, 0.5)).multiply(5);

            super.fireHomingMissile(hero, true, 5,
                    targetSupplier,
                    player.getEyeLocation().toVector(), launchVelocity, 0.5, 2, 0.5, 100,
                    null, null,
                    EnumSet.of(RayCastFlag.BLOCK_HIGH_DETAIL, RayCastFlag.BLOCK_IGNORE_NON_SOLID));
        }


        final RayCastHit targetHit = physics.rayCast(world, player,start, end,
                EnumSet.of(RayCastFlag.BLOCK_HIGH_DETAIL, RayCastFlag.BLOCK_IGNORE_NON_SOLID));

        // ----- Old Method ----- //

//        Supplier<Vector> target;
//
//        if (targetHit != null) {
//            if (targetHit.isEntity()) {
//                target = new Supplier<Vector>() {
//
//                    Entity entity;
//                    Vector point;
//
//                    {
//                        entity = targetHit.getEntity();
//                        point = physics.getEntityAABB(entity).getCenter();
//                    }
//
//                    @Override
//                    public Vector get() {
//                        if (entity.isValid() && !entity.isDead()) {
//                            point = physics.getEntityAABB(entity).getCenter();
//                        }
//
//                        return point;
//                    }
//                };
//            } else {
//                Vector point = targetHit.getPoint();
//                target = () -> point;
//            }
//        } else {
//            Vector point = end.clone();
//            target = () -> point;
//        }
//
//        for (int i = 0; i < 10; i++) {
//
//            Vector launchVelocity = Vector.getRandom().subtract(new Vector(0.5, 0.5, 0.5)).multiply(5);
//
//            super.fireHomingMissile(hero, true, 5,
//                    target,
//                    player.getEyeLocation().toVector(), launchVelocity, 0.5, 2, 0.5, 100,
//                    null, null,
//                    EnumSet.of(RayCastFlag.BLOCK_HIGH_DETAIL, RayCastFlag.BLOCK_IGNORE_NON_SOLID));
//        }


        // ----- Homing on what your looking at ----- //

//        super.fireHomingMissile(hero, true, 5,
//                () -> {
//                    Vector start = player.getEyeLocation().toVector();
//                    Vector end = player.getEyeLocation().getDirection().multiply(50).add(start);
//
//                    RayCastHit hit = physics.rayCast(world, player, start, end, RayCastFlag.BLOCK_HIGH_DETAIL, RayCastFlag.BLOCK_IGNORE_NON_SOLID);
//                    return hit != null ? hit.getPoint() : end;
//                },
//                player.getEyeLocation().toVector(), player.getEyeLocation().getDirection().multiply(0.1), 0.5, 2, 1, 600,
//                entity -> false, block -> false,
//                EnumSet.of(RayCastFlag.BLOCK_HIGH_DETAIL, RayCastFlag.BLOCK_HIT_FLUID_SOURCE, RayCastFlag.ENTITY_HIT_SPECTATORS));

        return SkillResult.NORMAL;
    }

    @Override
    protected void onEntityHit(Hero hero, Entity entity, Vector hitOrigin, Vector hitForce) {
        entity.getWorld().createExplosion(hitOrigin.getX(), hitOrigin.getY(), hitOrigin.getZ(), (int) hitForce.length() + 1, false, false);
    }

    @Override
    protected void onEntityPassed(Hero hero, Entity entity, Vector hitOrigin, Vector hitForce) {

        // ----- Homing on what your looking at ----- //

//        if (entity instanceof  LivingEntity) {
//            LivingEntity livingEntity = (LivingEntity) entity;
//
//            boolean canDamage = damageCheck(livingEntity, hero.getPlayer());
//            hero.getPlayer().sendMessage("Passing Entity: " + entity + " : " + canDamage + " : " + hitOrigin + " : " + hitForce);
//
//            if (canDamage) {
//                damageEntity(livingEntity, hero.getPlayer(), 10.0, EntityDamageEvent.DamageCause.MAGIC, false);
//                livingEntity.setVelocity(livingEntity.getVelocity().add(hitForce));
//            }
//        }
    }

    @Override
    protected void onBlockHit(Hero hero, Block block, Vector hitPosition, Vector hitForce, BlockFace hitFace) {
        block.getWorld().createExplosion(hitPosition.getX(), hitPosition.getY(), hitPosition.getZ(), (int) hitForce.length() + 1, false, false);
    }

    @Override
    protected void onBlockPassed(Hero hero, Block block, Vector hitPosition, Vector hitForce, BlockFace hitFace) {

    }

    @Override
    protected void renderMissilePath(World world, Vector start, Vector end, double radius) {

        EffectManager em = new EffectManager(plugin);

        Location loc = start.clone().add(end.clone().subtract(start).multiply(0.5)).toLocation(world);
        loc.setDirection(end.clone().subtract(start));

        world.playSound(loc, Sound.ENTITY_FIREWORK_BLAST_FAR, 1, 1);

        CylinderEffect cyl = new CylinderEffect(em);
        cyl.setLocation(loc);
        cyl.asynchronous = true;

        cyl.radius = (float) (radius * 0.5);
        cyl.height = (float) (start.distance(end));
        cyl.particle = ParticleEffect.REDSTONE;
        cyl.particles = 20;
        cyl.solid = true;
        cyl.rotationX = Math.toRadians(loc.getPitch() + 90);
        cyl.rotationY = -Math.toRadians(loc.getYaw());
        cyl.angularVelocityX = 0;
        cyl.angularVelocityY = 0;
        cyl.angularVelocityZ = 0;
        cyl.iterations = 1;
        cyl.visibleRange = 100;

        cyl.start();
        em.disposeOnTermination();
    }

    @Override
    protected void onExpire(Hero hero, Vector position, Vector velocity, boolean hitSomething) {
        if (!hitSomething) {
            hero.getPlayer().getWorld().createExplosion(position.getX(), position.getY(), position.getZ(), (int) velocity.length() + 1, false, false);
        }
    }
}
