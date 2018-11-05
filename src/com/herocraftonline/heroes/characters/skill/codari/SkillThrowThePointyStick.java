package com.herocraftonline.heroes.characters.skill.codari;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.standard.SlownessEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.RecastData;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SkillThrowThePointyStick extends ActiveSkill implements Listener {

    private static final String PROJECTILE_METADATA_KEY = "thrown-pointy-stick";

    private static final String FRONTAL_DAMAGE_NODE = "frontal-damage";
    private static final double DEFAULT_FRONTAL_DAMAGE = 10;

    private static final String REAR_DAMAGE_NODE = "rear-damage";
    private static final double DEFAULT_REAR_DAMAGE = 10;

    private static final String THROW_VELOCITY_NODE = "throw-velocity";
    private static final double DEFAULT_THROW_VELOCITY = 2.5;

    private static final String FRONTAL_ARC_NODE = "frontal-arc";
    private static final double DEFAULT_FRONTAL_ARC = 90;

    private static final String REAR_HIT_SLOWNESS_DURATION_NODE = "rear-hit-slowness-duration";
    private static final int DEFAULT_REAR_HIT_SLOWNESS_DURATION = 3000;

    private static final String REAR_HIT_SLOWNESS_STRENGTH_NODE = "rear-hit-slowness-strength";
    private static final int DEFAULT_REAR_HIT_SLOWNESS_STRENGTH = 2;

    private static final String REAR_HIT_FLAT_COOLDOWN_REDUCTION_NODE = "rear-hit-flat-cooldown-reduction";
    private static final int DEFAULT_REAR_HIT_FLAT_COOLDOWN_REDUCTION = 0;

    private static final String REAR_HIT_PERCENT_COOLDOWN_REDUCTION_NODE = "rear-hit-percent-cooldown-reduction";
    private static final double DEFAULT_REAR_HIT_PERCENT_COOLDOWN_REDUCTION = 0.5;

    private Set<UUID> rearHit = new HashSet<>();

    public SkillThrowThePointyStick(Heroes plugin) {
        super(plugin, "ThrowThePointyStick");
        setDescription("");

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

        node.set(FRONTAL_DAMAGE_NODE, DEFAULT_FRONTAL_DAMAGE);
        node.set(REAR_DAMAGE_NODE, DEFAULT_REAR_DAMAGE);
        node.set(THROW_VELOCITY_NODE, DEFAULT_THROW_VELOCITY);
        node.set(FRONTAL_ARC_NODE, DEFAULT_FRONTAL_ARC);
        node.set(REAR_HIT_SLOWNESS_DURATION_NODE, DEFAULT_REAR_HIT_SLOWNESS_DURATION);
        node.set(REAR_HIT_SLOWNESS_STRENGTH_NODE, DEFAULT_REAR_HIT_SLOWNESS_STRENGTH);
        node.set(REAR_HIT_FLAT_COOLDOWN_REDUCTION_NODE, DEFAULT_REAR_HIT_FLAT_COOLDOWN_REDUCTION);
        node.set(REAR_HIT_PERCENT_COOLDOWN_REDUCTION_NODE, DEFAULT_REAR_HIT_PERCENT_COOLDOWN_REDUCTION);

        return node;
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

    @Override
    public SkillResult use(Hero hero, String[] strings) {

        Player player = hero.getPlayer();

        RecastData recastData = new RecastData("Spear Throw");
        recastData.setNeverReady();
        startRecast(hero, recastData);

        Trident projectile = player.launchProjectile(Trident.class);
        projectile.setPickupStatus(Arrow.PickupStatus.DISALLOWED);

        double throwVelocity = SkillConfigManager.getUseSetting(hero, this, THROW_VELOCITY_NODE, DEFAULT_THROW_VELOCITY, false);
        if (throwVelocity < 1) {
            throwVelocity = 1;
        }

        projectile.setVelocity(projectile.getVelocity().normalize().multiply(throwVelocity));

        projectile.setMetadata(PROJECTILE_METADATA_KEY, new FixedMetadataValue(plugin, null));

        broadcastExecuteText(hero);

        return SkillResult.NORMAL;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onProjectileHit(ProjectileHitEvent e) {

        if (e.getEntity() instanceof Trident && e.getEntity().hasMetadata(PROJECTILE_METADATA_KEY)) {

            Player player = (Player) e.getEntity().getShooter();
            Hero hero = plugin.getCharacterManager().getHero(player);

            if (e.getHitEntity() != null && e.getHitEntity() instanceof LivingEntity) {

                LivingEntity target = (LivingEntity) e.getHitEntity();
                if (damageCheck(player, target)) {

                    CharacterTemplate targetCharacter = plugin.getCharacterManager().getCharacter(target);

                    double targetYaw = (target.getLocation().getYaw() + 360) % 360;

                    Vector tridentVelocity = e.getEntity().getVelocity();
                    double tridentYaw;
                    if (tridentVelocity.getX() != 0 && tridentVelocity.getZ() != 0) {
                        tridentYaw = (float)Math.toDegrees((Math.atan2(-tridentVelocity.getX(), tridentVelocity.getZ()) + (Math.PI * 2)) % (Math.PI * 2));
                    } else {
                        tridentYaw = targetYaw;
                    }

                    double yawDifference = Math.min(360 - Math.abs(tridentYaw - targetYaw), Math.abs(tridentYaw - targetYaw));

                    double frontalArc = SkillConfigManager.getUseSetting(hero, this, FRONTAL_ARC_NODE, DEFAULT_FRONTAL_ARC, false);
                    if (frontalArc < 10) {
                        frontalArc = 10;
                    } else if (frontalArc > 180) {
                        frontalArc = 180;
                    }

                    double damage;

                    if (yawDifference >= (180 - (frontalArc / 2))) {
                        // Frontal Hit
                        damage = SkillConfigManager.getUseSetting(hero, this, FRONTAL_DAMAGE_NODE, DEFAULT_FRONTAL_DAMAGE, false);

                        if (targetCharacter instanceof Hero) {
                            ((Hero) targetCharacter).interruptDelayedSkill();
                        }
                    } else {
                        // Rear Hit
                        damage = SkillConfigManager.getUseSetting(hero, this, REAR_DAMAGE_NODE, DEFAULT_REAR_DAMAGE, false);

                        int slownessDuration = SkillConfigManager.getUseSetting(hero, this, REAR_HIT_SLOWNESS_DURATION_NODE, DEFAULT_REAR_HIT_SLOWNESS_DURATION, false);
                        int slownessStrength = SkillConfigManager.getUseSetting(hero, this, REAR_HIT_SLOWNESS_STRENGTH_NODE, DEFAULT_REAR_HIT_SLOWNESS_STRENGTH, false);

                        if (slownessDuration > 0 && slownessStrength > 0) {
                            SlownessEffect.addDuration(targetCharacter, this, player, slownessDuration, slownessStrength);
                        }

                        rearHit.add(player.getUniqueId());
                    }

                    if (damage > 0) {
                        addSpellTarget(target, hero);
                        damageEntity(target, player, damage);
                    }
                }
            }

            if (e.getHitBlock() != null) {
                e.getEntity().remove();
            }

            endRecast(hero);
        }
    }

    @Override
    protected int alterAppliedCooldown(Hero hero, int cooldown) {

        if (rearHit.contains(hero.getPlayer().getUniqueId())) {

            int flatCooldownReduction = SkillConfigManager.getUseSetting(hero, this,
                    REAR_HIT_FLAT_COOLDOWN_REDUCTION_NODE, DEFAULT_REAR_HIT_FLAT_COOLDOWN_REDUCTION, false);
            if (flatCooldownReduction < 0) {
                flatCooldownReduction = 0;
            }

            double percentCooldownReduction = SkillConfigManager.getUseSetting(hero, this,
                    REAR_HIT_PERCENT_COOLDOWN_REDUCTION_NODE, DEFAULT_REAR_HIT_PERCENT_COOLDOWN_REDUCTION, false);
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
        rearHit.remove(hero.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onWeaponDamage(WeaponDamageEvent e) {
        if (e.isProjectile() && e.getAttackerEntity() instanceof Trident && e.getAttackerEntity().hasMetadata(PROJECTILE_METADATA_KEY)) {
            e.setCancelled(true);
        }
    }
}
