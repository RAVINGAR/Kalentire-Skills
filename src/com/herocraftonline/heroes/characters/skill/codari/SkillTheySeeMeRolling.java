package com.herocraftonline.heroes.characters.skill.codari;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.standard.SlownessEffect;
import com.herocraftonline.heroes.characters.effects.standard.SwiftnessEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.RecastData;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import java.util.*;

public class SkillTheySeeMeRolling extends ActiveSkill implements Listener {

    private static final String BACKWARDS_RECAST_PROJECTILE_META_KEY = "roll-recast-backwards-projectile";

    private static final String FORWARD_RECAST_NAME = "Forward";
    private static final String BACKWARDS_RECAST_NAME = "Backwards";

    private static final int MIN_RECAST_DURATION = 500;

    private static final String FORWARDS_RECAST_DURATION_NODE = "forwards-recast-duration";
    private static final int DEFAULT_FORWARDS_RECAST_DURATION = 1500;

    private static final String FORWARDS_RECAST_ATTACK_RANGE_NODE = "forwards-recast-attack-range";
    private static final double DEFAULT_FORWARDS_RECAST_ATTACK_RANGE = 6;

    private static final String FORWARDS_RECAST_ATTACK_DAMAGE_NODE = "forwards-recast-attack-damage";
    private static final double DEFAULT_FORWARDS_RECAST_ATTACK_DAMAGE = 10;

    private static final String FORWARDS_RECAST_ATTACK_BLEEDING_STACK_DURATION_NODE = "forwards-recast-attack-bleeding-stack-duration";
    private static final int DEFAULT_FORWARDS_RECAST_ATTACK_BLEEDING_STACK_DURATION = 3000;

    private static final String FORWARDS_RECAST_ATTACK_BLEEDING_STACK_AMOUNT_NODE = "forwards-recast-attack-bleeding-stack-amount";
    private static final int DEFAULT_FORWARDS_RECAST_ATTACK_BLEEDING_STACK_AMOUNT = 1;

    private static final String SIDEWAYS_SWIFTNESS_DURATION_NODE = "sideways-swiftness-duration";
    private static final int DEFAULT_SIDEWAYS_SWIFTNESS_DURATION = 3000;

    private static final String SIDEWAYS_SWIFTNESS_STRENGTH_NODE = "sideways-swiftness-strength";
    private static final int DEFAULT_SIDEWAYS_SWIFTNESS_STRENGTH = 2;

    private static final String SIDEWAYS_FLAT_COOLDOWN_REDUCTION_NODE = "sideways-flat-cooldown-reduction";
    private static final int DEFAULT_SIDEWAYS_FLAT_COOLDOWN_REDUCTION = 0;

    private static final String SIDEWAYS_PERCENT_COOLDOWN_REDUCTION_NODE = "sideways-percent-cooldown-reduction";
    private static final double DEFAULT_SIDEWAYS_PERCENT_COOLDOWN_REDUCTION = 0.25;

    private static final String BACKWARDS_RECAST_DURATION_NODE = "backwards-recast-duration";
    private static final int DEFAULT_BACKWARDS_RECAST_DURATION = 1500;

    private static final String BACKWARDS_RECAST_ATTACK_THROW_VELOCITY_NODE = "backwards-recast-attack-throw-velocity";
    private static final double DEFAULT_BACKWARDS_RECAST_ATTACK_THROW_VELOCITY = 2.5;
    private static final double MIN_BACKWARDS_RECAST_ATTACK_THROW_VELOCITY = 1;

    private static final String BACKWARDS_RECAST_ATTACK_DAMAGE_NODE = "backwards-recast-attack-damage";
    private static final double DEFAULT_BACKWARDS_RECAST_ATTACK_DAMAGE = 10;

    private static final String BACKWARDS_RECAST_ATTACK_DAMAGE_KNOCKBACKS_NODE = "backwards-recast-attack-damage-knockbacks";
    private static final boolean DEFAULT_BACKWARDS_RECAST_ATTACK_DAMAGE_KNOCKBACKS = false;

    private static final String BACKWARDS_RECAST_ATTACK_SLOWNESS_DURATION_NODE = "backwards-recast-attack-slowness-duration";
    private static final int DEFAULT_BACKWARDS_RECAST_ATTACK_SLOWNESS_DURATION = 5000;

    private static final String BACKWARDS_RECAST_ATTACK_SLOWNESS_STRENGTH_NODE = "backwards-recast-attack-slowness-strength";
    private static final int DEFAULT_BACKWARDS_RECAST_ATTACK_SLOWNESS_STRENGTH = 2;

    private Set<UUID> sidewaysMovement = new HashSet<>();

    public SkillTheySeeMeRolling(Heroes plugin) {
        super(plugin, "TheySeeMeRolling");
        setDescription("Stuff");

        setUsage("/skill " + getName());
        setArgumentRange(0, 0);
        setIdentifiers("skill " + getName());

    }

    @Override
    public void init() {
        super.init();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {

        ConfigurationSection node = super.getDefaultConfig();

        node.set(FORWARDS_RECAST_DURATION_NODE, DEFAULT_FORWARDS_RECAST_DURATION);
        node.set(SIDEWAYS_SWIFTNESS_DURATION_NODE, DEFAULT_SIDEWAYS_SWIFTNESS_DURATION);
        node.set(SIDEWAYS_SWIFTNESS_STRENGTH_NODE, DEFAULT_SIDEWAYS_SWIFTNESS_STRENGTH);
        node.set(SIDEWAYS_FLAT_COOLDOWN_REDUCTION_NODE, DEFAULT_SIDEWAYS_FLAT_COOLDOWN_REDUCTION);
        node.set(SIDEWAYS_PERCENT_COOLDOWN_REDUCTION_NODE, DEFAULT_SIDEWAYS_PERCENT_COOLDOWN_REDUCTION);
        node.set(BACKWARDS_RECAST_DURATION_NODE, DEFAULT_BACKWARDS_RECAST_DURATION);
        node.set(BACKWARDS_RECAST_ATTACK_THROW_VELOCITY_NODE, DEFAULT_BACKWARDS_RECAST_ATTACK_THROW_VELOCITY);
        node.set(BACKWARDS_RECAST_ATTACK_DAMAGE_NODE, DEFAULT_BACKWARDS_RECAST_ATTACK_DAMAGE);
        node.set(BACKWARDS_RECAST_ATTACK_DAMAGE_KNOCKBACKS_NODE, DEFAULT_BACKWARDS_RECAST_ATTACK_DAMAGE_KNOCKBACKS);
        node.set(BACKWARDS_RECAST_ATTACK_SLOWNESS_DURATION_NODE, DEFAULT_BACKWARDS_RECAST_ATTACK_SLOWNESS_DURATION);
        node.set(BACKWARDS_RECAST_ATTACK_SLOWNESS_STRENGTH_NODE, DEFAULT_BACKWARDS_RECAST_ATTACK_SLOWNESS_STRENGTH);

        return node;
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

    @Override
    public SkillResult use(Hero hero, String[] strings) {

        final Player player = hero.getPlayer();

        if (!player.isOnGround()) {
            player.sendMessage("Not on ground");
            return SkillResult.CANCELLED;
        }

        {
            RecastData recastData = new RecastData("Roll");
            recastData.setNeverReady();
        }

        final Vector origin = player.getLocation().toVector();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {

            double yawDirection = (player.getEyeLocation().getYaw() + 360) % 360;

            Vector movement = player.getLocation().toVector().subtract(origin);
            double yawMovement;
            if (movement.getX() != 0 && movement.getZ() != 0) {
                yawMovement = (float)Math.toDegrees((Math.atan2(-movement.getX(), movement.getZ()) + (Math.PI * 2)) % (Math.PI * 2));
            } else {
                yawMovement = yawDirection;
            }

            double yawDifference = Math.min(360 - Math.abs(yawDirection - yawMovement), Math.abs(yawDirection - yawMovement));

            RecastData recastData;
            int recastDuration;

            if (yawDifference <= 45) {
                // Forward
                recastData = new RecastData(FORWARD_RECAST_NAME);
                recastDuration = SkillConfigManager.getUseSetting(hero, this, FORWARDS_RECAST_DURATION_NODE, DEFAULT_FORWARDS_RECAST_DURATION, false);
            } else if (yawDifference < 135) {
                // Sideways
                recastData = null;
                recastDuration = 0;
                // No recast here
                sidewaysEffect(hero);
            } else {
                // Backwards
                recastData = new RecastData(BACKWARDS_RECAST_NAME);
                recastDuration = SkillConfigManager.getUseSetting(hero, this, BACKWARDS_RECAST_DURATION_NODE, DEFAULT_BACKWARDS_RECAST_DURATION, false);
            }

            double yawMovementRadians = Math.toRadians(yawMovement + 90);
            Vector rollVelocity = new Vector(Math.cos(yawMovementRadians), 0.25, Math.sin(yawMovementRadians));

            player.setVelocity(rollVelocity);

            if (recastData != null) {
                if (recastDuration < MIN_RECAST_DURATION) {
                    recastDuration = MIN_RECAST_DURATION;
                }
                startRecast(hero, recastDuration, recastData);
            } else {
                endRecast(hero);
            }

        }, 1);

        return SkillResult.NORMAL;
    }

    @Override
    protected void recast(Hero hero, RecastData data) {
        switch (data.getName()) {
            case FORWARD_RECAST_NAME: {
                forwardsRecast(hero);
                break;
            }
            case BACKWARDS_RECAST_NAME: {
                backwardsRecast(hero);
                break;
            }
        }
    }

    @Override
    protected int alterAppliedCooldown(Hero hero, int cooldown) {

        if (sidewaysMovement.contains(hero.getPlayer().getUniqueId())) {

            int flatCooldownReduction = SkillConfigManager.getUseSetting(hero, this,
                    SIDEWAYS_FLAT_COOLDOWN_REDUCTION_NODE, DEFAULT_SIDEWAYS_FLAT_COOLDOWN_REDUCTION, false);
            if (flatCooldownReduction < 0) {
                flatCooldownReduction = 0;
            }

            double percentCooldownReduction = SkillConfigManager.getUseSetting(hero, this,
                    SIDEWAYS_PERCENT_COOLDOWN_REDUCTION_NODE, DEFAULT_SIDEWAYS_PERCENT_COOLDOWN_REDUCTION, false);
            if (percentCooldownReduction < 0) {
                percentCooldownReduction = 0;
            } else if (percentCooldownReduction > 1) {
                percentCooldownReduction = 1;
            }

            return cooldown - (flatCooldownReduction + (int)(cooldown * percentCooldownReduction));
        } else {
            return cooldown;
        }
    }

    @Override
    protected void finalizeSkillUse(Hero hero) {
        sidewaysMovement.remove(hero.getPlayer().getUniqueId());
    }

    private void forwardsRecast(Hero hero) {

    }

    private void sidewaysEffect(Hero hero) {

        int swiftnessDuration = SkillConfigManager.getUseSetting(hero, this, SIDEWAYS_SWIFTNESS_DURATION_NODE, DEFAULT_SIDEWAYS_SWIFTNESS_DURATION, false);
        int swiftnessStrength = SkillConfigManager.getUseSetting(hero, this, SIDEWAYS_SWIFTNESS_STRENGTH_NODE, DEFAULT_SIDEWAYS_SWIFTNESS_STRENGTH, false);

        if (swiftnessDuration > 0 && swiftnessStrength > 0) {
            SwiftnessEffect.addDuration(hero, this, hero.getPlayer(), swiftnessDuration, swiftnessStrength);
        }

        sidewaysMovement.add(hero.getPlayer().getUniqueId());
    }

    private void backwardsRecast(Hero hero) {

        Player player = hero.getPlayer();

        Trident projectile = player.launchProjectile(Trident.class);
        projectile.setPickupStatus(Arrow.PickupStatus.DISALLOWED);

        double throwVelocity = SkillConfigManager.getUseSetting(hero, this, BACKWARDS_RECAST_ATTACK_THROW_VELOCITY_NODE, DEFAULT_BACKWARDS_RECAST_ATTACK_THROW_VELOCITY, false);
        if (throwVelocity < MIN_BACKWARDS_RECAST_ATTACK_THROW_VELOCITY) {
            throwVelocity = MIN_BACKWARDS_RECAST_ATTACK_THROW_VELOCITY;
        }

        projectile.setVelocity(projectile.getVelocity().normalize().multiply(throwVelocity));
        projectile.setMetadata(BACKWARDS_RECAST_PROJECTILE_META_KEY, new FixedMetadataValue(plugin, null));

        endRecast(hero);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onProjectileHit(ProjectileHitEvent e) {

        if (e.getEntity() instanceof Trident && e.getEntity().hasMetadata(BACKWARDS_RECAST_PROJECTILE_META_KEY)) {

            Player player = (Player) e.getEntity().getShooter();
            Hero hero = plugin.getCharacterManager().getHero(player);

            if (e.getHitEntity() != null && e.getHitEntity() instanceof LivingEntity) {

                LivingEntity target = (LivingEntity) e.getHitEntity();
                if (damageCheck(player, target)) {

                    CharacterTemplate targetCharacter = plugin.getCharacterManager().getCharacter(target);

                    double damage = SkillConfigManager.getUseSetting(hero, this, BACKWARDS_RECAST_ATTACK_DAMAGE_NODE, DEFAULT_BACKWARDS_RECAST_ATTACK_DAMAGE, false);
                    if (damage > 0) {
                        boolean knockback = SkillConfigManager.getUseSetting(hero, this, BACKWARDS_RECAST_ATTACK_DAMAGE_KNOCKBACKS_NODE, DEFAULT_BACKWARDS_RECAST_ATTACK_DAMAGE_KNOCKBACKS);
                        addSpellTarget(target, hero);
                        damageEntity(target, player, damage, knockback);
                    }

                    int slownessDuration = SkillConfigManager.getUseSetting(hero, this, BACKWARDS_RECAST_ATTACK_SLOWNESS_DURATION_NODE, DEFAULT_BACKWARDS_RECAST_ATTACK_SLOWNESS_DURATION, false);
                    int slownessStrength = SkillConfigManager.getUseSetting(hero, this, BACKWARDS_RECAST_ATTACK_SLOWNESS_STRENGTH_NODE, DEFAULT_BACKWARDS_RECAST_ATTACK_SLOWNESS_STRENGTH, false);

                    if (slownessDuration > 0 && slownessStrength > 0) {
                        SlownessEffect.addDuration(targetCharacter, this, player, slownessDuration, slownessStrength);
                    }
                }
            }

            if (e.getHitBlock() != null) {
                e.getEntity().remove();
            }
        }
    }
}
