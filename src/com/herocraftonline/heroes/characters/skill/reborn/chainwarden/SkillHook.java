package com.herocraftonline.heroes.characters.skill.reborn.chainwarden;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.characters.skill.tools.BasicMissile;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.LineEffect;
import de.slikey.effectlib.util.DynamicLocation;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.logging.Level;

public class SkillHook extends ActiveSkill {
    public static String skillName = "Hook";
    public static String ownerEffectName = "HookOwner";

    public SkillHook(Heroes plugin) {
        super(plugin, "Hook");
        setDescription("You pull a chain from your chainbelt and attach a hook to it, then throw it forwards at high speeds. " +
                "The hook will latch onto whatever it hits for the next $1 seconds. " +
                "Enemies will be dealt $2 damage on hit. Any targets that run over $3 blocks away will be yanked back slightly and have their hook dislodged.");
        setUsage("/skill hook");
        setIdentifiers("skill hook");
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
        config.set("projectile-velocity", 45.0);
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

//        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_2, 1.0F, 0.7F);

        return SkillResult.NORMAL;
    }

    public static InvalidHookTargetReason hasValidHookLocation(Heroes plugin, Hero hookOwner, Location targetLoc, double grabRadius) {
        if (targetLoc == null)
            return InvalidHookTargetReason.NULL_LOCATION;

        HookOwnerEffect ownerEffect = (HookOwnerEffect) hookOwner.getEffect(ownerEffectName);
        if (ownerEffect == null)
            return InvalidHookTargetReason.OTHER;

        HookedLocationEffect validHookedLocEffect = ownerEffect.tryGetHookedLocationEffect(targetLoc, grabRadius);
        if (validHookedLocEffect == null) {
            return ownerEffect.getCurrentHookCount() == 0
                    ? InvalidHookTargetReason.NO_ACTIVE_HOOKS
                    : InvalidHookTargetReason.OUT_OF_RANGE;
        }
        return InvalidHookTargetReason.VALID_TARGET;
    }

    public static Location tryGetHookLocation(Heroes plugin, Hero hookOwner, Location targetLoc, double grabRadius, boolean removeHookIfFound) {
        if (targetLoc == null)
            return null;

        HookOwnerEffect ownerEffect = (HookOwnerEffect) hookOwner.getEffect(ownerEffectName);
        if (ownerEffect == null)
            return null;

        HookedLocationEffect validHookedLocEffect = ownerEffect.tryGetHookedLocationEffect(targetLoc, grabRadius);
        if (validHookedLocEffect == null)
            return null;

        if (removeHookIfFound) {
            hookOwner.removeEffect(validHookedLocEffect);
        }
        return validHookedLocEffect.hookLocation;
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

    // Broadcast more informative message to player, rather than "Invalid Target"
    public static void broadcastInvalidHookTargetText(Hero hookOwner, InvalidHookTargetReason invalidHookTargetReason){
        final Player player = hookOwner.getPlayer();
        if (player == null || invalidHookTargetReason == InvalidHookTargetReason.VALID_TARGET)
            return;

        String text = "    " + ChatComponents.GENERIC_SKILL;
        switch (invalidHookTargetReason){
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

    public static InvalidHookTargetReason tryRemoveHook(Heroes plugin, Hero hookOwner, LivingEntity target) {
        if (target == null)
            return InvalidHookTargetReason.NULL_TARGET_ENTITY;

        CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
        return tryRemoveHook(hookOwner, targetCT);
    }

    public static InvalidHookTargetReason tryRemoveHook(Hero hookOwner, CharacterTemplate targetCT) {
        if (targetCT == null)
            return InvalidHookTargetReason.NULL_CHARACTER;

        Player ownerPlayer = hookOwner.getPlayer();
        String baseEffectName = getBaseHookEffectName(hookOwner.getPlayer());

        List<ExpirableEffect> foundHooks = new ArrayList<ExpirableEffect>();
        for (Effect effect : targetCT.getEffects()) {
            if (effect.getName().startsWith(baseEffectName) && effect instanceof ExpirableEffect) {
                if (effect.getApplier().equals(ownerPlayer)) {
                    foundHooks.add((ExpirableEffect) effect);
                }
            }
        }
        if (foundHooks.isEmpty())
            return InvalidHookTargetReason.NO_HOOK;


        boolean isAlliedTo = hookOwner.isAlliedTo(targetCT.getEntity());
        if (!isAlliedTo && !damageCheck(hookOwner.getPlayer(), targetCT.getEntity()))
            return InvalidHookTargetReason.INVINCIBLE_TARGET;

        // Get the hook with the shortest remaining duration
        Effect effectToRemove = Collections.min(foundHooks, Comparator.comparing(ExpirableEffect::getRemainingTime));
        targetCT.removeEffect(effectToRemove);
        return InvalidHookTargetReason.VALID_TARGET;
    }

    public static String getBaseHookEffectName(Player hookOwner) {
        return hookOwner.getName() + "-Hooked";
    }

    // This is silly I know, but you gotta do what you gotta do.
    private static int hookedTargetIndex = 0;
    public static String getDynamicHookedTargetEffectName(Player hookOwner) {
        hookedTargetIndex++;
        if (hookedTargetIndex > 500) {
            hookedTargetIndex = -500;
        }
        return getBaseHookEffectName(hookOwner) + "-" + hookedTargetIndex;
    }

    // This is silly I know, but you gotta do what you gotta do.
    private static int hookLocationIndex = 0;
    private static String getDynamicHookedLocationName() {
        hookLocationIndex++;
        if (hookLocationIndex > 500) {
            hookLocationIndex = -500;
        }
        return "HookedLocation-" + hookLocationIndex;
    }


    public SkillHook.HookProjectile createHookProjectile(Hero hero) {
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

            Location hookedLoc = block.getLocation().add(0.5, 0.5, 0.5);
            HookedLocationEffect hookedEffect = new HookedLocationEffect(skill, player, hookedLoc, (long) (hookedDuration * 0.5), hookLeashDistance);
            hero.addEffect(hookedEffect);
        }

        @Override
        protected void onEntityHit(Entity entity, Vector hitOrigin, Vector hitForce) {
            if (!(entity instanceof LivingEntity))
                return;

//            player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_HIT, 0.5F, 0.5F);

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

            player.getWorld().spawnParticle(Particle.REDSTONE, target.getEyeLocation(), 5, 0.5F, 0.25F, 0.3F, 1.0F);
        }
    }

    // Has to be static so that other skills can instantiate it.
    public static class HookOwnerEffect extends Effect {
        private List<CharacterTemplate> hookedCharacters = new ArrayList<CharacterTemplate>();
        private List<HookedLocationEffect> hookedLocations = new ArrayList<HookedLocationEffect>();

        HookOwnerEffect(Skill skill, Player applier) {
            super(skill, ownerEffectName, applier);

            this.types.add(EffectType.INTERNAL);
        }

        public int getCurrentHookCount() {
            return (int) this.hookedCharacters.stream().distinct().count() + this.hookedLocations.size();
        }

        public int getCurrentHookedLocationsCount() {
            return this.hookedLocations.size();
        }

        public HookedLocationEffect tryGetHookedLocationEffect(Location location, double grabRadius) {
            if (hookedLocations.isEmpty())
                return null;

            double grabSquared = grabRadius * grabRadius;
            for (HookedLocationEffect locEffect : hookedLocations) {
                if (locEffect.hookLocation.distanceSquared(location) <= grabSquared)
                    return locEffect;
            }
            return null;
        }

        // Do not call this manually. This is only for the HookedEffect to call.
        void addHook(CharacterTemplate targetCT) {
            this.hookedCharacters.add(targetCT);
        }

        // Do not call this manually. This is only for the HookedEffect to call.
        void removeHook(CharacterTemplate targetCT) {
            this.hookedCharacters.remove(targetCT);
        }

        // Do not call this manually. This is only for the HookedLocationEffect to call.
        void addLocationHook(HookedLocationEffect locEffect) {
            this.hookedLocations.add(locEffect);
        }

        // Do not call this manually. This is only for the HookedLocationEffect to call.
        void removeLocationHook(HookedLocationEffect locEffect) {
            this.hookedLocations.remove(locEffect);
        }
    }

    public static class HookedLocationEffect extends PeriodicExpirableEffect {
        private final Location hookLocation;
        private final double snapDistSquared;

        private EffectManager effectManager;

        HookedLocationEffect(Skill skill, Player applier, Location location, long duration, double snapDist) {
            super(skill, getDynamicHookedLocationName(), applier, 1000, duration, null, null);
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
            ownerEffect.addLocationHook(this);

            this.effectManager = new EffectManager(plugin);
            LineEffect effect = getHookVisual(this.effectManager, applier, this.hookLocation);
            this.effectManager.start(effect);
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);

            if (hero.hasEffect(ownerEffectName)) {
                HookOwnerEffect ownerEffect = (HookOwnerEffect) hero.getEffect(ownerEffectName);
                ownerEffect.removeLocationHook(this);
            }

            if (this.effectManager != null)
                this.effectManager.dispose();
        }

        @Override
        public void tickHero(Hero hero) {
            Player player = hero.getPlayer();
            if (player.getWorld() != hookLocation.getWorld()) {
                hero.removeEffect(this);
                return;
            }

            Location playerLoc = player.getLocation();
            double distanceSquared = playerLoc.distanceSquared(hookLocation);
            if (distanceSquared > snapDistSquared) {
                hero.removeEffect(this);
            }
        }

        @Override
        public void tickMonster(Monster monster) {
            // Won't ever happen
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
            super(skill, getDynamicHookedTargetEffectName(applierHero.getPlayer()), applierHero.getPlayer(), 1000, duration, null, null);
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
            if (self.getWorld() != applier.getWorld()) {
                selfCT.removeEffect(this);
                return;
            }

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
        effect.setTargetLocation(targetLoc);

        return effect;
    }

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
