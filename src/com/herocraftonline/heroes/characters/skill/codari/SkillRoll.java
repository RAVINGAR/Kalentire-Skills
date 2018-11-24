package com.herocraftonline.heroes.characters.skill.codari;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.standard.BleedingEffect;
import com.herocraftonline.heroes.characters.effects.standard.SlownessEffect;
import com.herocraftonline.heroes.characters.effects.standard.SwiftnessEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.RecastData;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.nms.physics.FluidCollision;
import com.herocraftonline.heroes.nms.physics.NMSPhysics;
import com.herocraftonline.heroes.nms.physics.RayCastHit;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
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

public class SkillRoll extends ActiveSkill implements Listener {

    private static final double MIN_LAUNCH_VELOCITY = 0.5;

    private static final String FORWARDS_LAUNCH_VELOCITY_NODE = "forwards-launch-velocity";
    private static final double DEFAULT_FORWARDS_LAUNCH_VELOCITY = 1;

    private static final String FORWARDS_LAUNCH_HEIGHT_NODE = "forwards-launch-height";
    private static final double DEFAULT_FORWARDS_LAUNCH_HEIGHT = 0.25;

    private static final String FORWARDS_SWIFTNESS_DURATION_NODE = "forwards-swiftness-duration";
    private static final int DEFAULT_FORWARDS_SWIFTNESS_DURATION = 3000;

    private static final String FORWARDS_SWIFTNESS_STRENGTH_NODE = "forwards-swiftness-strength";
    private static final int DEFAULT_FORWARDS_SWIFTNESS_STRENGTH = 2;

    private static final String SIDEWAYS_LAUNCH_VELOCITY_NODE = "sideways-launch-velocity";
    private static final double DEFAULT_SIDEWAYS_LAUNCH_VELOCITY = 1;

    private static final String SIDEWAYS_LAUNCH_HEIGHT_NODE = "sideways-launch-height";
    private static final double DEFAULT_SIDEWAYS_LAUNCH_HEIGHT = 0.25;

    private static final String SIDEWAYS_FLAT_COOLDOWN_REDUCTION_NODE = "sideways-flat-cooldown-reduction";
    private static final int DEFAULT_SIDEWAYS_FLAT_COOLDOWN_REDUCTION = 0;

    private static final String SIDEWAYS_PERCENT_COOLDOWN_REDUCTION_NODE = "sideways-percent-cooldown-reduction";
    private static final double DEFAULT_SIDEWAYS_PERCENT_COOLDOWN_REDUCTION = 0.25;

    private static final String BACKWARDS_LAUNCH_VELOCITY_NODE = "backwards-launch-velocity";
    private static final double DEFAULT_BACKWARDS_LAUNCH_VELOCITY = 1;

    private static final String BACKWARDS_LAUNCH_HEIGHT_NODE = "backwards-launch-height";
    private static final double DEFAULT_BACKWARDS_LAUNCH_HEIGHT = 0.25;

    private Set<UUID> sidewaysMovement = new HashSet<>();

    public SkillRoll(Heroes plugin) {
        super(plugin, "Roll");
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

        node.set(FORWARDS_LAUNCH_VELOCITY_NODE, DEFAULT_FORWARDS_LAUNCH_VELOCITY);
        node.set(FORWARDS_LAUNCH_HEIGHT_NODE, DEFAULT_FORWARDS_LAUNCH_HEIGHT);

        node.set(SIDEWAYS_LAUNCH_VELOCITY_NODE, DEFAULT_SIDEWAYS_LAUNCH_VELOCITY);
        node.set(SIDEWAYS_LAUNCH_HEIGHT_NODE, DEFAULT_SIDEWAYS_LAUNCH_HEIGHT);
        node.set(SIDEWAYS_FLAT_COOLDOWN_REDUCTION_NODE, DEFAULT_SIDEWAYS_FLAT_COOLDOWN_REDUCTION);
        node.set(SIDEWAYS_PERCENT_COOLDOWN_REDUCTION_NODE, DEFAULT_SIDEWAYS_PERCENT_COOLDOWN_REDUCTION);

        node.set(BACKWARDS_LAUNCH_VELOCITY_NODE, DEFAULT_BACKWARDS_LAUNCH_VELOCITY);
        node.set(BACKWARDS_LAUNCH_HEIGHT_NODE, DEFAULT_BACKWARDS_LAUNCH_HEIGHT);

        return node;
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

    @Override
    public SkillResult use(Hero hero, String[] strings) {

        final Player player = hero.getPlayer();
        Material blockTypeAtLegs = player.getLocation().getBlock().getType();

        if (!player.isOnGround() && blockTypeAtLegs != Material.WATER && blockTypeAtLegs != Material.LAVA) {
            player.sendMessage("Not on ground");
            return SkillResult.CANCELLED;
        }

        {
            RecastData recastData = new RecastData("Roll");
            recastData.setNeverReady();
            startRecast(hero, recastData);
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

            double launchVelocity;
            double launchHeight;

            if (yawDifference <= 45) {
                // Forward
                launchVelocity = SkillConfigManager.getUseSetting(hero, this, FORWARDS_LAUNCH_VELOCITY_NODE, DEFAULT_FORWARDS_LAUNCH_VELOCITY, false);
                launchHeight = SkillConfigManager.getUseSetting(hero, this, FORWARDS_LAUNCH_HEIGHT_NODE, DEFAULT_FORWARDS_LAUNCH_HEIGHT, false);

                int swiftnessDuration = SkillConfigManager.getUseSetting(hero, this, FORWARDS_SWIFTNESS_DURATION_NODE, DEFAULT_FORWARDS_SWIFTNESS_DURATION, false);
                int swiftnessStrength = SkillConfigManager.getUseSetting(hero, this, FORWARDS_SWIFTNESS_STRENGTH_NODE, DEFAULT_FORWARDS_SWIFTNESS_STRENGTH, false);

                if (swiftnessDuration > 0 && swiftnessStrength > 0) {
                    SwiftnessEffect.addDuration(hero, this, hero.getPlayer(), swiftnessDuration, swiftnessStrength);
                }
            } else if (yawDifference < 135) {
                // Sideways
                launchVelocity = SkillConfigManager.getUseSetting(hero, this, SIDEWAYS_LAUNCH_VELOCITY_NODE, DEFAULT_SIDEWAYS_LAUNCH_VELOCITY, false);
                launchHeight = SkillConfigManager.getUseSetting(hero, this, SIDEWAYS_LAUNCH_HEIGHT_NODE, DEFAULT_SIDEWAYS_LAUNCH_HEIGHT, false);

                sidewaysMovement.add(hero.getPlayer().getUniqueId());
            } else {
                // Backwards
                launchVelocity = SkillConfigManager.getUseSetting(hero, this, BACKWARDS_LAUNCH_VELOCITY_NODE, DEFAULT_BACKWARDS_LAUNCH_VELOCITY, false);
                launchHeight = SkillConfigManager.getUseSetting(hero, this, BACKWARDS_LAUNCH_HEIGHT_NODE, DEFAULT_BACKWARDS_LAUNCH_HEIGHT, false);

                SlownessEffect.removeAll(hero);
            }

            if (launchVelocity < MIN_LAUNCH_VELOCITY) {
                launchVelocity = MIN_LAUNCH_VELOCITY;
            }
            if (launchHeight < 0) {
                launchHeight = 0;
            }

            double yawMovementRadians = Math.toRadians(yawMovement + 90);
            Vector launchVector = new Vector(Math.cos(yawMovementRadians), 0, Math.sin(yawMovementRadians)).multiply(launchVelocity).setY(launchHeight);

            player.setVelocity(launchVector);
            endRecast(hero);

        }, 1);

        broadcastExecuteText(hero);

        return SkillResult.NORMAL;
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
}
