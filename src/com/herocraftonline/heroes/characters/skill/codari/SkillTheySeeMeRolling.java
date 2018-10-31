package com.herocraftonline.heroes.characters.skill.codari;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.RecastData;
import org.bukkit.Bukkit;
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

    private Map<UUID, Vector> playerMovement = new HashMap<>();

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

        RecastData recastData = new RecastData("Roll");
        recastData.setNeverReady();

        final Vector origin = player.getLocation().toVector();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {

            //double yawDirection = (player.getEyeLocation().getYaw() + 360) % 360;
            double yawDirection = (player.getEyeLocation().getYaw() + 360) % 360;
            player.sendMessage("DIRECTION YAW: " + yawDirection + " : " + player.getLocation().getYaw());

            Vector movement = player.getLocation().toVector().subtract(origin);
            player.sendMessage("MOVEMENT: " + movement);
            double yawMovement;

            if (movement.getX() != 0 && movement.getZ() != 0) {
                yawMovement = (float)Math.toDegrees((Math.atan2(-movement.getX(), movement.getZ()) + (Math.PI * 2)) % (Math.PI * 2));
            } else {
                yawMovement = yawDirection;
            }

            player.sendMessage("MOVEMENT YAW: " + yawMovement);

            double yawDifference = Math.min(360 - Math.abs(yawDirection - yawMovement), Math.abs(yawDirection - yawMovement));
            player.sendMessage("DIFFERENCE YAW: " + yawDifference);

            if (yawDifference < 45) {
                player.sendMessage("FORWARD");
            } else if (yawDifference <= 135) {
                player.sendMessage("SIDEWAYS");
            } else {
                player.sendMessage("BACKWARDS");
            }

            double yawMovementRadians = Math.toRadians(yawMovement + 90);
            Vector rollVelocity = new Vector(Math.cos(yawMovementRadians), 0.25, Math.sin(yawMovementRadians));

            player.setVelocity(rollVelocity);

        }, 1);

        return SkillResult.NORMAL;
    }
}
