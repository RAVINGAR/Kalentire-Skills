package com.herocraftonline.heroes.characters.skill.codari;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.UUID;

public class SkillTheySeeMeRolling extends ActiveSkill {

    public SkillTheySeeMeRolling(Heroes plugin) {
        super(plugin, "TheySeeMeRolling");
        setDescription("Stuff");

        setUsage("/skill " + getName());
        setArgumentRange(0, 0);
        setIdentifiers("skill " + getName());

    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

    @Override
    public SkillResult use(Hero hero, String[] strings) {

        Player player = hero.getPlayer();

        if (!player.isOnGround()) {
            player.sendMessage("Not on ground");
            return SkillResult.CANCELLED;
        }

        double yawDirection = player.getEyeLocation().getYaw();
        player.sendMessage("DIRECTION: " + yawDirection);

        Vector velocity = player.getVelocity();
        double yawVelocity = (float)Math.toDegrees((Math.atan2(-velocity.getX(), velocity.getZ()) + (Math.PI * 2)) % (Math.PI * 2));
        player.sendMessage("VELOCITY: " + velocity + " : " + yawVelocity);

        double yawDifference = Math.min(360 - Math.abs(yawDirection - yawVelocity), Math.abs(yawDirection - yawVelocity));
        player.sendMessage("DIFFERENCE: " + yawDifference);

//        if (yawDifference < 45) {
//            // Forward
//            player.sendMessage("FORWARD: " + yawDifference);
//        } else if (yawDifference <= 135) {
//            player.sendMessage("SIDEWAYS: " + yawDifference);
//        } else {
//            player.sendMessage("BACKWARDS: " + yawDifference);
//        }

        Vector rollVelocity = velocity.setY(0).normalize().setY(0.25).multiply(3);
        player.setVelocity(rollVelocity);

        return SkillResult.NORMAL;
    }
}
