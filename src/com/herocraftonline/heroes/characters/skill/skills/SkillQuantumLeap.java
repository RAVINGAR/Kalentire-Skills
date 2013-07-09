package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;

public class SkillQuantumLeap extends TargettedSkill {
    public SkillQuantumLeap(Heroes plugin) {
        super(plugin, "QuantumLeap");
        setDescription("Through quantum physics, change places with your target.");
        setUsage("/skill quantumleap");
        setArgumentRange(0, 0);
        setIdentifiers("skill quantumleap");
        setTypes(SkillType.SILENCABLE, SkillType.DAMAGING, SkillType.HARMFUL, SkillType.TELEPORT);
    }

    public String getDescription(Hero hero) {
        return getDescription();
    }

    public SkillResult use(Hero hero, LivingEntity target, String[] args) {

        if (!(target instanceof Player))
            return SkillResult.INVALID_TARGET;

        Player player = hero.getPlayer();

        // Set the player's location
        Location pLocation = player.getLocation();

        // Set the target location
        Location tLocation = target.getLocation();

        /*
        
        //Better Method that Kainzo hates for some reason. :(
        
        // Swap the locations yaw/pitch values
        pLocation.setYaw(target.getLocation().getYaw());
        pLocation.setPitch(target.getLocation().getPitch());

        tLocation.setYaw(player.getLocation().getYaw());
        tLocation.setPitch(player.getLocation().getPitch());
        
        */

        // Teleport the player and his target
        player.teleport(tLocation);
        target.teleport(pLocation);

        // Play effect
        player.getWorld().playEffect(player.getLocation(), Effect.ENDER_SIGNAL, 3);

        // Play sound
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.PORTAL, 0.5F, 1.0F);

        broadcastExecuteText(hero, target);

        return SkillResult.NORMAL;
    }
}