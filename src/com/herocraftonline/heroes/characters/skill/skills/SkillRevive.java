package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

public class SkillRevive extends ActiveSkill {

    public SkillRevive(Heroes plugin) {
        super(plugin, "Revive");
        setDescription("Revives your target, teleporting them to their place of death.");
        setUsage("/skill revive <target>");
        setArgumentRange(1, 1);
        setIdentifiers("skill revive");
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

        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }
}
