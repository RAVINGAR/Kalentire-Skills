package com.herocraftonline.heroes.characters.skill.reborn.chainwarden;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.characters.skill.tools.BasicMissile;
import com.herocraftonline.heroes.util.Util;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.LineEffect;
import de.slikey.effectlib.util.DynamicLocation;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SkillHookshot extends ActiveSkill {
    public static String skillName = "Hookshot";
    public static String ownerEffectName = "HookOwner";

    public SkillHookshot(Heroes plugin) {
        super(plugin, "Hookshot");
        setDescription("You launch a hook and chain outward at high speeds. The hook will latch onto whatever it hits for the next $1 seconds. " +
                "Enemies will be dealt $2 damage on hit. Any targets that run over $3 blocks away will be yanked back slightly and have their hook dislodged.");
        setUsage("/skill hookshot");
        setIdentifiers("skill hookshot");
        setArgumentRange(0, 0);
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.DAMAGING, SkillType.MULTI_GRESSIVE);
    }

    @Override
    public String getDescription(Hero hero) {
        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 1, false);
        double hookLeashDistance = SkillConfigManager.getUseSetting(hero, this, "hook-leash-distance", 20.0, false);

        return getDescription()
                .replace("$1", Util.decFormat.format((double) duration / 1000))
                .replace("$2", Util.decFormat.format(damage))
                .replace("$3", Util.decFormat.format(hookLeashDistance));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.DAMAGE.node(), 40.0);
        config.set(SkillSetting.DURATION.node(), 18000);
        config.set("projectile-size", 0.35);
        config.set("projectile-velocity", 35.0);
        config.set("projectile-gravity", 2.5);
        config.set("projectile-max-ticks-lived", 20);
        config.set("hook-leash-distance", 25.0);
        config.set("hook-leash-power", 1.25);
        return config;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        // This is necessary for compatibility with AoE versions of this skill.
        boolean shouldBroadcast = args == null || args.length == 0 || Arrays.stream(args).noneMatch(x -> x.equalsIgnoreCase("NoBroadcast"));

        if (!SkillChainBelt.tryRemoveChain(this, hero, shouldBroadcast)) {
            return SkillResult.FAIL;
        }

        if (shouldBroadcast)
            broadcastExecuteText(hero);

        HookProjectile missile = createHookProjectile(hero);
        missile.fireMissile();

        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_2, 1.0F, 0.7F);

        return SkillResult.NORMAL;
    }

    public static Location tryGetHookLocation(Heroes plugin, Hero hookOwner, Location targetLoc, double grabRadius, boolean removeHookIfFound) {
        if (targetLoc == null)
            return null;
        HookOwnerEffect ownerEffect = (HookOwnerEffect) hookOwner.getEffect(ownerEffectName);
        if (ownerEffect == null)
            return null;
        Location validHookLoc = ownerEffect.tryConverToHookedLocation(targetLoc, grabRadius);
        if (validHookLoc == null)
            return null;

        if (removeHookIfFound) {
            ownerEffect.removeLocationHook(validHookLoc);
        }
        return validHookLoc;
    }

    public static boolean tryRemoveHook(Heroes plugin, Hero hookOwner, LivingEntity target) {
        if (target == null)
            return false;

        CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
        return tryRemoveHook(hookOwner, targetCT);
    }

    public static boolean tryRemoveHook(Hero hookOwner, CharacterTemplate targetCT) {
        if (targetCT == null)
            return false;

        String effectName = getHookedEffectName(hookOwner.getPlayer());
        if (!targetCT.hasEffect(effectName))
            return false;

        boolean isAlliedTo = hookOwner.isAlliedTo(targetCT.getEntity());
        if (!isAlliedTo && !damageCheck(hookOwner.getPlayer(), targetCT.getEntity()))
            return false;

        targetCT.removeEffect(targetCT.getEffect(effectName));
        return true;
    }

    public static String getHookedEffectName(Player hookOwner) {
        return hookOwner.getName() + "-Hooked";
    }

    // Lol.
    private static int hookLocationIndex = 0;
    private static String getHookedLocationName() {
        hookLocationIndex++;
        if (hookLocationIndex > 500) {
            hookLocationIndex = -500;
        }
        return "HookedLocation-" + hookLocationIndex;
    }

    public SkillHookshot.HookProjectile createHookProjectile(Hero hero) {
        double projSize = SkillConfigManager.getUseSetting(hero, this, "projectile-size", 0.25, false);
        double projVelocity = SkillConfigManager.getUseSetting(hero, this, "projectile-velocity", 20, false);

        return new HookProjectile(plugin, this, hero, projSize, projVelocity);
    }

    public class HookProjectile extends BasicMissile {
        private final long hookedDuration;
        private final double hookLeashDistance;
        private final double hookLeashPower;

        HookProjectile(Plugin plugin, Skill skill, Hero hero, double projectileSize, double projVelocity) {
            super(plugin, skill, hero, projectileSize, projVelocity);

            setRemainingLife(SkillConfigManager.getUseSetting(hero, skill, "projectile-max-ticks-lived", 20, false));
            setGravity(SkillConfigManager.getUseSetting(hero, skill, "projectile-gravity", 2.5, false));
            this.damage = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE, 50.0, false);
            this.hookLeashDistance = SkillConfigManager.getUseSetting(hero, skill, "hook-leash-distance", 20.0, false);
            this.hookLeashPower = SkillConfigManager.getUseSetting(hero, skill, "hook-leash-power", 1.25, false);
            this.hookedDuration = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DURATION, 18000, false);

            this.visualEffect = getHookVisual(this.effectManager, player, getLocation());
        }

        @Override
        protected void onTick() {
            this.visualEffect.setTargetLocation(getLocation());
        }

        @Override
        protected boolean onCollideWithEntity(Entity entity) {
            return !player.equals(entity);  // Don't let them hook themselves.
        }

        @Override
        protected void onBlockHit(Block block, Vector hitPoint, BlockFace hitFace, Vector hitForce) {
            if (block == null || hitFace == null)
                return;

            Location hookedLoc = block.getRelative(hitFace).getLocation();
            HookedLocationEffect hookedEffect = new HookedLocationEffect(skill, player, hookedLoc, hookedDuration, hookLeashDistance);
            hero.addEffect(hookedEffect);
        }

        @Override
        protected void onEntityHit(Entity entity, Vector hitOrigin, Vector hitForce) {
            if (!(entity instanceof LivingEntity))
                return;

            player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_HIT, 0.5F, 0.5F);

            LivingEntity target = (LivingEntity)entity;
            CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
            HookedEffect hookedEffect = new HookedEffect(skill, hero, hookedDuration, hookLeashDistance, hookLeashPower);

            // If we're an ally, make the effect beneficial and add it to them.
            if (hero.isAlliedTo(target)) {
                hookedEffect.types.add(EffectType.BENEFICIAL);
                targetCT.addEffect(hookedEffect);
                return;
            }

            // We're dealing with an enemy now
            if (!damageCheck(this.player, target))
                return;

            skill.addSpellTarget(target, this.hero);
            skill.damageEntity(target, this.player, this.damage, DamageCause.ENTITY_ATTACK);

            hookedEffect.types.add(EffectType.HARMFUL);
            targetCT.addEffect(hookedEffect);

            player.getWorld().spawnParticle(Particle.REDSTONE, target.getEyeLocation(), 5, 0.5F, 0.25F, 0.3F, 1.0F, new Particle.DustOptions(Color.RED, 1));
        }
    }

    // Has to be static so that other skills can instantiate it.
    public static class HookOwnerEffect extends Effect {
        private List<CharacterTemplate> hookedCharacters = new ArrayList<CharacterTemplate>();
        private List<Location> hookedLocations = new ArrayList<Location>();

        HookOwnerEffect(Skill skill, Player applier) {
            super(skill, ownerEffectName, applier);

            this.types.add(EffectType.INTERNAL);
        }

        public int getCurrentHookCount() {
            return this.hookedCharacters.size() + this.hookedLocations.size();
        }

        public void addHook(CharacterTemplate targetCT) {
            if (!this.hookedCharacters.contains(targetCT))
                this.hookedCharacters.add(targetCT);
        }

        public void removeHook(CharacterTemplate targetCT) {
            this.hookedCharacters.remove(targetCT);
        }

        public Location tryConverToHookedLocation(Location location, double grabRadius) {
            if (hookedLocations.isEmpty())
                return null;

            double grabSquared = grabRadius * grabRadius;
            for (Location hookedLoc : hookedLocations) {
                if (hookedLoc.distanceSquared(location) <= grabSquared)
                    return hookedLoc;
            }
            return null;
        }

        public void addLocationHook(Location location) {
            if (!this.hookedLocations.contains(location))
                this.hookedLocations.add(location);
        }

        public void removeLocationHook(Location location) {
            this.hookedLocations.remove(location);
        }
    }

    public static class HookedLocationEffect extends PeriodicExpirableEffect {
        private final Location hookLocation;
        private final double snapDistSquared;

        private EffectManager effectManager;

        HookedLocationEffect(Skill skill, Player applier, Location location, long duration, double snapDist) {
            super(skill, getHookedLocationName(), applier, 1000, duration, null, null);
            this.hookLocation = location;
            this.snapDistSquared = snapDist * snapDist;

            types.add(EffectType.INTERNAL);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);

            HookOwnerEffect ownerEffect = null;
            if (hero.hasEffect(ownerEffectName)) {
                ownerEffect = (HookOwnerEffect) hero.getEffect(ownerEffectName);
            } else {
                ownerEffect = new HookOwnerEffect(skill, applier);
                hero.addEffect(ownerEffect);
            }
            ownerEffect.addLocationHook(hookLocation);

            this.effectManager = new EffectManager(plugin);
            LineEffect effect = getHookVisual(this.effectManager, applier, this.hookLocation);
            effect.disappearWithOriginEntity = true;
            effect.disappearWithTargetEntity = false;

            this.effectManager.start(effect);
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);

            if (hero.hasEffect(ownerEffectName)) {
                HookOwnerEffect ownerEffect = (HookOwnerEffect) hero.getEffect(ownerEffectName);
                ownerEffect.removeLocationHook(hookLocation);
            }

            if (this.effectManager != null)
                this.effectManager.dispose();
        }

        @Override
        public void tickHero(Hero hero) {
            tryToLeashOrSnap(hero, hero.getPlayer());
        }

        @Override
        public void tickMonster(Monster monster) {
            tryToLeashOrSnap(monster, monster.getEntity());
        }

        private void tryToLeashOrSnap(CharacterTemplate selfCT, LivingEntity self) {
            Location playerLoc = self.getLocation();
            Location applierLoc = applier.getLocation();
            double distanceSquared = playerLoc.distanceSquared(applierLoc);
            if (distanceSquared > snapDistSquared) {
                selfCT.removeEffect(this);
            }
        }
    }

    // Has to be static so that other skills can instantiate it.
    public static class HookedEffect extends PeriodicExpirableEffect {
        private final Hero applierHero;
        private final double leashDistSquared;
        private final double snapDistSquared;
        private final double hookLeashPower;
        private EffectManager effectManager;

        public HookedEffect(Skill skill, Hero applierHero, long duration, double leashDist, double hookLeashPower) {
            super(skill, getHookedEffectName(applierHero.getPlayer()), applierHero.getPlayer(), 1000, duration, null, null);
            this.applierHero = applierHero;
            double snapDist = leashDist * 1.5;
            this.leashDistSquared = leashDist * leashDist;
            this.snapDistSquared = snapDist * snapDist;
            this.hookLeashPower = hookLeashPower;

            types.add(EffectType.PHYSICAL);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);

            if (applierHero.hasEffect(ownerEffectName)) {
                HookOwnerEffect ownerEffect = (HookOwnerEffect) applierHero.getEffect(ownerEffectName);
                ownerEffect.addHook(hero);
            } else {
                HookOwnerEffect ownerEffect = new HookOwnerEffect(skill, applier);
                applierHero.addEffect(ownerEffect);
                ownerEffect.addHook(hero);
            }

            this.effectManager = new EffectManager(plugin);
            LineEffect effect = getHookVisual(this.effectManager, applier, hero.getPlayer());
            effect.disappearWithOriginEntity = true;
            effect.disappearWithTargetEntity = true;
            this.effectManager.start(effect);
        }

        @Override
        public void applyToMonster(Monster monster) {
            super.applyToMonster(monster);

            if (applierHero.hasEffect(ownerEffectName)) {
                HookOwnerEffect ownerEffect = (HookOwnerEffect) applierHero.getEffect(ownerEffectName);
                ownerEffect.addHook(monster);
            } else {
                HookOwnerEffect ownerEffect = new HookOwnerEffect(skill, applier);
                applierHero.addEffect(ownerEffect);
                ownerEffect.addHook(monster);
            }

            this.effectManager = new EffectManager(plugin);
            LineEffect effect = getHookVisual(this.effectManager, applier, monster.getEntity());
            effect.disappearWithOriginEntity = true;
            effect.disappearWithTargetEntity = true;

            this.effectManager.start(effect);
        }

        @Override
        public void removeFromMonster(Monster monster) {
            super.removeFromMonster(monster);

            if (applierHero.hasEffect(ownerEffectName)) {
                HookOwnerEffect ownerEffect = (HookOwnerEffect) applierHero.getEffect(ownerEffectName);
                ownerEffect.removeHook(monster);
            }

            if (this.effectManager != null)
                this.effectManager.dispose();
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);

            if (applierHero.hasEffect(ownerEffectName)) {
                HookOwnerEffect ownerEffect = (HookOwnerEffect) applierHero.getEffect(ownerEffectName);
                ownerEffect.removeHook(hero);
            }

            this.effectManager.dispose();
        }

        @Override
        public void tickHero(Hero hero) {
            tryToLeashOrSnap(hero, hero.getPlayer());
        }

        @Override
        public void tickMonster(Monster monster) {
            tryToLeashOrSnap(monster, monster.getEntity());
        }

        private void tryToLeashOrSnap(CharacterTemplate selfCT, LivingEntity self) {
            Location playerLoc = self.getLocation();
            Location applierLoc = applier.getLocation();
            double distanceSquared = playerLoc.distanceSquared(applierLoc);
            if (distanceSquared > snapDistSquared) {
                selfCT.removeEffect(this);
            } else if (distanceSquared > leashDistSquared) {
                // Only leash them if it's a harmful hook. Otherwise we'll just snap instead.
                if (types.contains(EffectType.HARMFUL)) {
                    Vector locDiff = applierLoc.toVector().subtract(playerLoc.toVector()).normalize().setY(0.25).multiply(hookLeashPower);
                    self.setVelocity(locDiff);
                }

                selfCT.removeEffect(this);
            }
        }
    }

    private static LineEffect getHookVisual(EffectManager effectManager, Player owner, LivingEntity target) {
        LineEffect effect = getBaseHookVisual(effectManager);

        DynamicLocation dynamicOwnerLoc = new DynamicLocation(owner);
        effect.setDynamicOrigin(dynamicOwnerLoc);

        DynamicLocation dynamicTargetLoc = new DynamicLocation(target);
        dynamicOwnerLoc.addOffset(new Vector(0, -0.5, 0));
        effect.setDynamicTarget(dynamicTargetLoc);

        return effect;
    }

    private static LineEffect getHookVisual(EffectManager effectManager, Player owner, Location targetLoc) {
        LineEffect effect = getBaseHookVisual(effectManager);

        DynamicLocation dynamicOwnerLoc = new DynamicLocation(owner);
        effect.setDynamicOrigin(dynamicOwnerLoc);
        effect.setLocation(targetLoc);

        return effect;
    }

    @NotNull
    private static LineEffect getBaseHookVisual(EffectManager effectManager) {
        LineEffect effect = new LineEffect(effectManager);
        effect.particle = Particle.CRIT_MAGIC;
        effect.particles = 5;
//        effect.particleSize = 0.5f;
//        effect.speed = 2.0f;
        effect.iterations = 9999;
        return effect;
    }
}
