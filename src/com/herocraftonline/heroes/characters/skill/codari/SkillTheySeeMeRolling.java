package com.herocraftonline.heroes.characters.skill.codari;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.RecastData;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SkillTheySeeMeRolling extends ActiveSkill {

    private static final String FORWARD_RECAST_NAME = "Forward";
    private static final String SIDEWAYS_RECAST_NAME = "Sideways";
    private static final String BACKWARDS_RECAST_NAME = "Backwards";

    private static final int MIN_RECAST_DURATION = 500;

    private static final String FORWARDS_RECAST_DURATION_NODE = "forwards-recast-duration";
    private static final int DEFAULT_FORWARDS_RECAST_DURATION = 1500;

    private static final String SIDEWAYS_RECAST_DURATION_NODE = "sideways-recast-duration";
    private static final int DEFAULT_SIDEWAYS_RECAST_DURATION = 1500;

    private static final String BACKWARDS_RECAST_DURATION_NODE = "backwards-recast-duration";
    private static final int DEFAULT_BACKWARDS_RECAST_DURATION = 1500;

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
    }

    @Override
    public ConfigurationSection getDefaultConfig() {

        ConfigurationSection node = super.getDefaultConfig();

        node.set(FORWARDS_RECAST_DURATION_NODE, DEFAULT_FORWARDS_RECAST_DURATION);
        node.set(SIDEWAYS_RECAST_DURATION_NODE, DEFAULT_SIDEWAYS_RECAST_DURATION);
        node.set(BACKWARDS_RECAST_DURATION_NODE, DEFAULT_BACKWARDS_RECAST_DURATION);

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
                recastData = new RecastData(SIDEWAYS_RECAST_NAME);
                recastDuration = SkillConfigManager.getUseSetting(hero, this, SIDEWAYS_RECAST_DURATION_NODE, DEFAULT_SIDEWAYS_RECAST_DURATION, false);
            } else {
                // Backwards
                recastData = new RecastData(BACKWARDS_RECAST_NAME);
                recastDuration = SkillConfigManager.getUseSetting(hero, this, BACKWARDS_RECAST_DURATION_NODE, DEFAULT_BACKWARDS_RECAST_DURATION, false);
            }

            if (recastDuration < MIN_RECAST_DURATION) {
                recastDuration = MIN_RECAST_DURATION;
            }
            recastData.setReadyDelay(recastDuration);

            double yawMovementRadians = Math.toRadians(yawMovement + 90);
            Vector rollVelocity = new Vector(Math.cos(yawMovementRadians), 0.25, Math.sin(yawMovementRadians));

            player.setVelocity(rollVelocity);

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
            case SIDEWAYS_RECAST_NAME: {
                sidewaysRecast(hero);
                break;
            }
            case BACKWARDS_RECAST_NAME: {
                backwardsRecast(hero);
                break;
            }
        }
    }

    private void forwardsRecast(Hero hero) {

    }

    private void sidewaysRecast(Hero hero) {

    }

    private void backwardsRecast(Hero hero) {

    }
}
