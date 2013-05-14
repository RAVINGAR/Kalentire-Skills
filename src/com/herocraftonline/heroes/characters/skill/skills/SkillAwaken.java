package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Color;
import org.bukkit.Effect;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.VisualEffect;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

public class SkillAwaken extends ActiveSkill {
    public VisualEffect fplayer = new VisualEffect();
    public SkillAwaken(Heroes plugin) {
        super(plugin, "Awaken");
        setDescription("Awakens your target, teleporting them to their place of death.");
        setUsage("/skill awaken <target>");
        setArgumentRange(1, 1);
        setIdentifiers("skill awaken");
        setTypes(SkillType.HEAL, SkillType.SILENCABLE);
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        Player target = plugin.getServer().getPlayer(args[0]);

        if (target == null)
        	return SkillResult.INVALID_TARGET;
        

        String targetName = target.getName();
        if (!Util.deaths.containsKey(targetName)) {
            Messaging.send(player, "$1 has not died recently.", targetName);
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        Location deathLoc = Util.deaths.get(targetName);
        Location playerLoc = player.getLocation();
        if (!playerLoc.getWorld().equals(deathLoc.getWorld()) || playerLoc.distance(deathLoc) > 50.0) {
            Messaging.send(player, "You are out of range.");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        if (target.isDead()) {
            Messaging.send(player, "$1 is still dead.", targetName);
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        Hero targetHero = plugin.getCharacterManager().getHero(target);
        if (!hero.hasParty() || !hero.getParty().isPartyMember(targetHero)) {
            Messaging.send(player, "The person needs to be in your party to do that!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        target.teleport(playerLoc);
        player.getWorld().playEffect(player.getLocation(), Effect.MOBSPAWNER_FLAMES, 3);
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.WITHER_SPAWN , 0.5F, 1.0F);
        broadcastExecuteText(hero);
        // this is our fireworks
        try {
            fplayer.playFirework(player.getWorld(), 
            		target.getLocation().add(0,1.5,0), 
            		FireworkEffect.builder()
            		.flicker(false)
            		.trail(false)
            		.with(FireworkEffect.Type.CREEPER)
            		.withColor(Color.PURPLE)
            		.withFade(Color.FUCHSIA)
            		.build());
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }
}
