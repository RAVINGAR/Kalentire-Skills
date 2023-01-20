package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.tools.BasicMissile;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.libs.slikey.effectlib.effect.LineEffect;
import com.herocraftonline.heroes.libs.slikey.effectlib.util.DynamicLocation;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.ChatColor;
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

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SkillHook extends ActiveSkill {
    public static final String skillName = "Hook";
    public static final String ownerEffectName = "HookOwner";
    // Lol.
    private static int hookLocationIndex = 0;

    public SkillHook(final Heroes plugin) {
        super(plugin, "Hook");
        setDescription("You launch a hook and chain outward at high speeds. The hook will latch onto whatever it hits for the next $1 seconds. " +
                "Enemies will be dealt $2 damage on hit. Any targets that run over $3 blocks away will be yanked back slightly and have their hook dislodged.");
        setUsage("/skill hook");
        setIdentifiers("skill hook");
        setArgumentRange(0, 0);
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.DAMAGING, SkillType.MULTI_GRESSIVE);
    }

    public static InvalidHookTargetReason hasValidHookLocation(final Heroes plugin, final Hero hookOwner, final Location targetLoc, final double grabRadius) {
        if (targetLoc == null) {
            return InvalidHookTargetReason.NULL_LOCATION;
        }

        final HookOwnerEffect ownerEffect = (HookOwnerEffect) hookOwner.getEffect(ownerEffectName);
        if (ownerEffect == null) {
            return InvalidHookTargetReason.OTHER;
        }

        final HookedLocationEffect validHookedLocEffect = ownerEffect.tryGetHookedLocationEffect(targetLoc, grabRadius);
        if (validHookedLocEffect == null) {
            return ownerEffect.getCurrentHookCount() == 0
                    ? InvalidHookTargetReason.NO_ACTIVE_HOOKS
                    : InvalidHookTargetReason.OUT_OF_RANGE;
        }
        return InvalidHookTargetReason.VALID_TARGET;
    }

    public static Location tryGetHookLocation(final Heroes plugin, final Hero hookOwner, final Location targetLoc, final double grabRadius, final boolean removeHookIfFound) {
        if (targetLoc == null) {
            return null;
        }

        final HookOwnerEffect ownerEffect = (HookOwnerEffect) hookOwner.getEffect(ownerEffectName);
        if (ownerEffect == null) {
            return null;
        }

        final HookedLocationEffect validHookedLocEffect = ownerEffect.tryGetHookedLocationEffect(targetLoc, grabRadius);
        if (validHookedLocEffect == null) {
            return null;
        }

        if (removeHookIfFound) {
            hookOwner.removeEffect(validHookedLocEffect);
        }
        return validHookedLocEffect.hookLocation;
    }

    // Broadcast more informative message to player, rather than "Invalid Target"
    public static void broadcastInvalidHookTargetText(final Hero hookOwner, final InvalidHookTargetReason invalidHookTargetReason) {
        final Player player = hookOwner.getPlayer();
        if (player == null || invalidHookTargetReason == InvalidHookTargetReason.VALID_TARGET) {
            return;
        }

        String text = ChatColor.GRAY + "    " + ChatComponents.GENERIC_SKILL;
        switch (invalidHookTargetReason) {
            case NO_HOOK:
                text += "That target has no hook!";
                break;
            case NO_ACTIVE_HOOKS:
                text += "You have no active hooks!";
                break;
            case OUT_OF_RANGE:
                text += "You are aiming out of range of the hook(s)!";
                break;
            case INVINCIBLE_TARGET:
                text += "You cannot damage that target right now!";
                break;
            case OTHER:
            case NULL_LOCATION:
            case NULL_TARGET_ENTITY:
            case NULL_CHARACTER:
            default:
                text += "Invalid Target!";
        }
        player.sendMessage(text);
    }

    public static InvalidHookTargetReason tryRemoveHook(final Heroes plugin, final Hero hookOwner, final LivingEntity target) {
        if (target == null) {
            return InvalidHookTargetReason.NULL_TARGET_ENTITY;
        }

        final CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
        return tryRemoveHook(hookOwner, targetCT);
    }

    public static InvalidHookTargetReason tryRemoveHook(final Hero hookOwner, final CharacterTemplate targetCT) {
        if (targetCT == null) {
            return InvalidHookTargetReason.NULL_CHARACTER;
        }

        final String effectName = getHookedEffectName(hookOwner.getPlayer());
        if (!targetCT.hasEffect(effectName)) {
            return InvalidHookTargetReason.NO_HOOK;
        }

        final boolean isAlliedTo = hookOwner.isAlliedTo(targetCT.getEntity());
        if (!isAlliedTo && !damageCheck(hookOwner.getPlayer(), targetCT.getEntity())) {
            return InvalidHookTargetReason.INVINCIBLE_TARGET;
        }

        targetCT.removeEffect(targetCT.getEffect(effectName));
        return InvalidHookTargetReason.VALID_TARGET;
    }

    public static String getHookedEffectName(final Player hookOwner) {
        return hookOwner.getName() + "-Hooked";
    }

    private static String getDynamicHookedLocationName() {
        hookLocationIndex++;
        if (hookLocationIndex > 500) {
            hookLocationIndex = -500;
        }
        return "HookedLocation-" + hookLocationIndex;
    }

    private static LineEffect getHookVisual(final Player owner, final LivingEntity target) {
        final LineEffect effect = getBaseHookVisual();

        final DynamicLocation dynamicOwnerLoc = new DynamicLocation(owner);
        effect.setDynamicOrigin(dynamicOwnerLoc);

        final DynamicLocation dynamicTargetLoc = new DynamicLocation(target);
        dynamicOwnerLoc.addOffset(new Vector(0, -0.5, 0));
        effect.setDynamicTarget(dynamicTargetLoc);

        return effect;
    }

    private static LineEffect getHookVisual(final Player owner, final Location targetLoc) {
        final LineEffect effect = getBaseHookVisual();

        final DynamicLocation dynamicOwnerLoc = new DynamicLocation(owner);
        effect.setDynamicOrigin(dynamicOwnerLoc);
        effect.setTargetLocation(targetLoc);

        return effect;
    }

    @Nonnull
    private static LineEffect getBaseHookVisual() {
        final LineEffect effect = new LineEffect(effectLib);
        effect.particle = Particle.CRIT_MAGIC;
        effect.particles = 5;
//        effect.particleSize = 0.5f;
//        effect.speed = 2.0f;
        effect.iterations = 9999;
        return effect;
    }

    @Override
    public String getDescription(final Hero hero) {
        final long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
        final double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 1, false);
        final double hookLeashDistance = SkillConfigManager.getUseSetting(hero, this, "hook-leash-distance", 20.0, false);

        return getDescription()
                .replace("$1", Util.decFormat.format((double) duration / 1000))
                .replace("$2", Util.decFormat.format(damage))
                .replace("$3", Util.decFormat.format(hookLeashDistance));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.DAMAGE.node(), 40.0);
        config.set(SkillSetting.DURATION.node(), 18000);
        config.set("projectile-size", 0.35);
        config.set("projectile-velocity", 45.0);
        config.set("projectile-gravity", 2.5);
        config.set("projectile-max-ticks-lived", 20);
        config.set("hook-leash-distance", 25.0);
        config.set("hook-leash-power", 1.25);
        return config;
    }

    @Override
    public SkillResult use(final Hero hero, final String[] args) {
        final Player player = hero.getPlayer();

        // This is necessary for compatibility with AoE versions of this skill.
        final boolean shouldBroadcast = args == null || args.length == 0 || Arrays.stream(args).noneMatch(x -> x.equalsIgnoreCase("NoBroadcast"));

        if (!SkillChainBelt.tryRemoveChain(this, hero, shouldBroadcast)) {
            return SkillResult.FAIL;
        }

        if (shouldBroadcast) {
            broadcastExecuteText(hero);
        }

        final HookProjectile missile = createHookProjectile(hero);
        missile.fireMissile();

        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_2, 1.0F, 0.7F);

        return SkillResult.NORMAL;
    }

    public SkillHook.HookProjectile createHookProjectile(final Hero hero) {
        return new HookProjectile(plugin, this, hero);
    }

    public enum InvalidHookTargetReason {
        OTHER,
        NULL_TARGET_ENTITY,
        NULL_CHARACTER,
        NO_HOOK,
        NULL_LOCATION,
        NO_ACTIVE_HOOKS,
        INVINCIBLE_TARGET,
        //ALLIED_TARGET,
        OUT_OF_RANGE,
        VALID_TARGET
    }

    public static class HookProjectile extends BasicMissile {
        private final long hookedDuration;
        private final double hookLeashDistance;
        private final double hookLeashPower;
        private final double damage;

        HookProjectile(final Plugin plugin, final Skill skill, final Hero hero) {
            super((Heroes) plugin, skill, hero, Particle.CRIT_MAGIC, Color.GRAY, true);


            setRemainingLife(SkillConfigManager.getUseSetting(hero, skill, "projectile-max-ticks-lived", 20, false));
            setGravity(SkillConfigManager.getUseSetting(hero, skill, "projectile-gravity", 2.5, false));
            this.damage = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE, 50.0, false);
            this.hookLeashDistance = SkillConfigManager.getUseSetting(hero, skill, "hook-leash-distance", 20.0, false);
            this.hookLeashPower = SkillConfigManager.getUseSetting(hero, skill, "hook-leash-power", 1.25, false);
            this.hookedDuration = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DURATION, 18000, false);

            this.visualEffect = getHookVisual(player, getLocation());
        }

        @Override
        protected void onTick() {
            this.visualEffect.setTargetLocation(getLocation());
        }

        @Override
        protected boolean onCollideWithEntity(final Entity entity) {
            return !player.equals(entity);  // Don't let them hook themselves.
        }

        @Override
        protected void onBlockHit(final Block block, final Vector hitPoint, final BlockFace hitFace, final Vector hitForce) {
            if (block == null || hitFace == null) {
                return;
            }

            final Location hookedLoc = block.getLocation().add(0.5, 0.5, 0.5);
            final HookedLocationEffect hookedEffect = new HookedLocationEffect(skill, player, hookedLoc, (long) (hookedDuration * 0.5), hookLeashDistance);
            hero.addEffect(hookedEffect);
        }

        @Override
        protected void onEntityHit(final Entity entity, final Vector hitOrigin, final Vector hitForce) {
            if (!(entity instanceof LivingEntity)) {
                return;
            }

            player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_HIT, 0.5F, 0.5F);

            final LivingEntity target = (LivingEntity) entity;
            final CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
            final HookedEffect hookedEffect = new HookedEffect(skill, hero, hookedDuration, hookLeashDistance, hookLeashPower);

            // If we're an ally, make the effect beneficial and add it to them.
            if (hero.isAlliedTo(target)) {
                hookedEffect.types.add(EffectType.BENEFICIAL);
                targetCT.addEffect(hookedEffect);
                return;
            }

            // We're dealing with an enemy now
            if (!damageCheck(this.player, target)) {
                return;
            }

            skill.addSpellTarget(target, this.hero);
            skill.damageEntity(target, this.player, this.damage, DamageCause.ENTITY_ATTACK);

            hookedEffect.types.add(EffectType.HARMFUL);
            targetCT.addEffect(hookedEffect);

            player.getWorld().spawnParticle(Particle.REDSTONE, target.getEyeLocation(), 5, 0.5F, 0.25F, 0.3F, 1.0F, new Particle.DustOptions(Color.RED, 1));
        }
    }

    // Has to be static so that other skills can instantiate it.
    public static class HookOwnerEffect extends Effect {
        private final List<CharacterTemplate> hookedCharacters = new ArrayList<>();
        private final List<HookedLocationEffect> hookedLocations = new ArrayList<>();

        HookOwnerEffect(final Skill skill, final Player applier) {
            super(skill, ownerEffectName, applier);

            this.types.add(EffectType.INTERNAL);
        }

        public int getCurrentHookCount() {
            return this.hookedCharacters.size() + this.hookedLocations.size();
        }

        public int getCurrentHookedLocationsCount() {
            return this.hookedLocations.size();
        }

        public HookedLocationEffect tryGetHookedLocationEffect(final Location location, final double grabRadius) {
            if (hookedLocations.isEmpty()) {
                return null;
            }

            final double grabSquared = grabRadius * grabRadius;
            for (final HookedLocationEffect locEffect : hookedLocations) {
                if (locEffect.hookLocation.distanceSquared(location) <= grabSquared) {
                    return locEffect;
                }
            }
            return null;
        }

        // Do not call this manually. This is only for the HookedLocationEffect to call.
        void addHook(final CharacterTemplate targetCT) {
            if (!this.hookedCharacters.contains(targetCT)) {
                this.hookedCharacters.add(targetCT);
            }
        }

        // Do not call this manually. This is only for the HookedLocationEffect to call.
        void removeHook(final CharacterTemplate targetCT) {
            this.hookedCharacters.remove(targetCT);
        }

        // Do not call this manually. This is only for the HookedLocationEffect to call.
        void addLocationHook(final HookedLocationEffect locEffect) {
            if (!this.hookedLocations.contains(locEffect)) {
                this.hookedLocations.add(locEffect);
            }
        }

        // Do not call this manually. This is only for the HookedLocationEffect to call.
        void removeLocationHook(final HookedLocationEffect locEffect) {
            if (this.hookedLocations.contains(locEffect)) {
                this.hookedLocations.remove(locEffect);
            }
        }
    }

    public static class HookedLocationEffect extends PeriodicExpirableEffect {
        private final Location hookLocation;
        private final double snapDistSquared;

        HookedLocationEffect(final Skill skill, final Player applier, final Location location, final long duration, final double snapDist) {
            super(skill, getDynamicHookedLocationName(), applier, 1000, duration, null, null);
            this.hookLocation = location;
            this.snapDistSquared = snapDist * snapDist;

            types.add(EffectType.INTERNAL);
        }

        @Override
        public void applyToHero(final Hero hero) {
            super.applyToHero(hero);

            HookOwnerEffect ownerEffect = null;
            if (hero.hasEffect(ownerEffectName)) {
                ownerEffect = (HookOwnerEffect) hero.getEffect(ownerEffectName);
            } else {
                ownerEffect = new HookOwnerEffect(skill, applier);
                hero.addEffect(ownerEffect);
            }
            ownerEffect.addLocationHook(this);

            final LineEffect effect = getHookVisual(applier, this.hookLocation);
            effectLib.start(effect);
        }

        @Override
        public void removeFromHero(final Hero hero) {
            super.removeFromHero(hero);

            if (hero.hasEffect(ownerEffectName)) {
                final HookOwnerEffect ownerEffect = (HookOwnerEffect) hero.getEffect(ownerEffectName);
                ownerEffect.removeLocationHook(this);
            }
        }

        @Override
        public void tickHero(final Hero hero) {
            final Player player = hero.getPlayer();
            if (player.getWorld() != hookLocation.getWorld()) {
                hero.removeEffect(this);
                return;
            }

            final Location playerLoc = player.getLocation();
            final double distanceSquared = playerLoc.distanceSquared(hookLocation);
            if (distanceSquared > snapDistSquared) {
                hero.removeEffect(this);
            }
        }

        @Override
        public void tickMonster(final Monster monster) {
            // Won't ever happen
        }
    }

    // Has to be static so that other skills can instantiate it.
    public static class HookedEffect extends PeriodicExpirableEffect {
        private final Hero applierHero;
        private final double leashDistSquared;
        private final double snapDistSquared;
        private final double hookLeashPower;

        public HookedEffect(final Skill skill, final Hero applierHero, final long duration, final double leashDist, final double hookLeashPower) {
            super(skill, getHookedEffectName(applierHero.getPlayer()), applierHero.getPlayer(), 1000, duration, null, null);
            this.applierHero = applierHero;
            final double snapDist = leashDist * 1.5;
            this.leashDistSquared = leashDist * leashDist;
            this.snapDistSquared = snapDist * snapDist;
            this.hookLeashPower = hookLeashPower;

            types.add(EffectType.PHYSICAL);
        }

        @Override
        public void applyToHero(final Hero hero) {
            super.applyToHero(hero);

            if (applierHero.hasEffect(ownerEffectName)) {
                final HookOwnerEffect ownerEffect = (HookOwnerEffect) applierHero.getEffect(ownerEffectName);
                ownerEffect.addHook(hero);
            } else {
                final HookOwnerEffect ownerEffect = new HookOwnerEffect(skill, applier);
                applierHero.addEffect(ownerEffect);
                ownerEffect.addHook(hero);
            }

            final LineEffect effect = getHookVisual(applier, hero.getPlayer());
            effect.disappearWithOriginEntity = true;
            effect.disappearWithTargetEntity = true;
            effectLib.start(effect);
        }

        @Override
        public void applyToMonster(final Monster monster) {
            super.applyToMonster(monster);

            if (applierHero.hasEffect(ownerEffectName)) {
                final HookOwnerEffect ownerEffect = (HookOwnerEffect) applierHero.getEffect(ownerEffectName);
                ownerEffect.addHook(monster);
            } else {
                final HookOwnerEffect ownerEffect = new HookOwnerEffect(skill, applier);
                applierHero.addEffect(ownerEffect);
                ownerEffect.addHook(monster);
            }
            final LineEffect effect = getHookVisual(applier, monster.getEntity());
            effect.disappearWithOriginEntity = true;
            effect.disappearWithTargetEntity = true;

            effectLib.start(effect);
        }

        @Override
        public void removeFromMonster(final Monster monster) {
            super.removeFromMonster(monster);

            if (applierHero.hasEffect(ownerEffectName)) {
                final HookOwnerEffect ownerEffect = (HookOwnerEffect) applierHero.getEffect(ownerEffectName);
                ownerEffect.removeHook(monster);
            }
        }

        @Override
        public void removeFromHero(final Hero hero) {
            super.removeFromHero(hero);

            if (applierHero.hasEffect(ownerEffectName)) {
                final HookOwnerEffect ownerEffect = (HookOwnerEffect) applierHero.getEffect(ownerEffectName);
                ownerEffect.removeHook(hero);
            }
        }

        @Override
        public void tickHero(final Hero hero) {
            tryToLeashOrSnap(hero, hero.getPlayer());
        }

        @Override
        public void tickMonster(final Monster monster) {
            tryToLeashOrSnap(monster, monster.getEntity());
        }

        private void tryToLeashOrSnap(final CharacterTemplate selfCT, final LivingEntity self) {
            if (self.getWorld() != applier.getWorld()) {
                selfCT.removeEffect(this);
                return;
            }

            final Location playerLoc = self.getLocation();
            final Location applierLoc = applier.getLocation();
            final double distanceSquared = playerLoc.distanceSquared(applierLoc);
            if (distanceSquared > snapDistSquared) {
                selfCT.removeEffect(this);
            } else if (distanceSquared > leashDistSquared) {
                // Only leash them if it's a harmful hook. Otherwise we'll just snap instead.
                if (types.contains(EffectType.HARMFUL)) {
                    final Vector locDiff = applierLoc.toVector().subtract(playerLoc.toVector()).normalize().setY(0.25).multiply(hookLeashPower);
                    self.setVelocity(locDiff);
                }

                selfCT.removeEffect(this);
            }
        }
    }
}
