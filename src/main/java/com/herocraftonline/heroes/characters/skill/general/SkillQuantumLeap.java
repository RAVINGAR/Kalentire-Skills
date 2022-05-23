package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SkillQuantumLeap extends TargettedSkill {
    public SkillQuantumLeap(Heroes plugin) {
        super(plugin, "QuantumLeap");
        setDescription("Through quantum physics, change places with your target.");
        setUsage("/skill quantumleap");
        setArgumentRange(0, 0);
        setIdentifiers("skill quantumleap");
        setTypes(SkillType.SILENCEABLE, SkillType.NO_SELF_TARGETTING, SkillType.MULTI_GRESSIVE, SkillType.TELEPORTING);
    }

    public String getDescription(Hero hero) {
        return getDescription();
    }

    public SkillResult use(Hero hero, LivingEntity target, String[] args) {

        if (!(target instanceof Player))
            return SkillResult.INVALID_TARGET;

        Player player = hero.getPlayer();
        Player targetPlayer = (Player) target;
        Hero targetHero = plugin.getCharacterManager().getHero(targetPlayer);

        if (targetHero.hasEffect("Root") || targetHero.hasEffectType(EffectType.STUN) || targetHero.hasEffect("Piggify"))
            return SkillResult.INVALID_TARGET;
        else if (hero.hasEffect("Root") || hero.hasEffectType(EffectType.STUN) || hero.hasEffect("Piggify"))
            return SkillResult.INVALID_TARGET;

        // Set the player's location
        Location pLocation = player.getLocation();

        // Set the target location
        Location tLocation = targetPlayer.getLocation();
        
        // Swap the locations yaw/pitch values
        pLocation.setYaw(targetPlayer.getLocation().getYaw());
        pLocation.setPitch(targetPlayer.getLocation().getPitch());

        tLocation.setYaw(player.getLocation().getYaw());
        tLocation.setPitch(player.getLocation().getPitch());

        
        // Teleport the player and his target
        player.teleport(tLocation);
        targetPlayer.teleport(pLocation);

        // Play effect
        player.getWorld().playEffect(player.getLocation(), Effect.ENDER_SIGNAL, 3);

        // Play sound
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.5F, 1.0F);

        broadcastExecuteText(hero, target);

        return SkillResult.NORMAL;
    }
}