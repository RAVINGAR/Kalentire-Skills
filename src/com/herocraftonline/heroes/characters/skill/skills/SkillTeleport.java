package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

public class SkillTeleport extends ActiveSkill {

    public SkillTeleport(Heroes plugin) {
        super(plugin, "Teleport");
        setDescription("Teleports you somewhere close by to a party member.");
        setUsage("/skill teleport <player>");
        setArgumentRange(1, 1);
        setIdentifiers("skill teleport");
        setTypes(SkillType.TELEPORT, SkillType.SILENCABLE);
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        if (!(hero.getParty() != null && hero.getParty().getMembers().size() > 0)) {
            Messaging.send(player, "Sorry, you need to be in a party with players!");
            return SkillResult.FAIL;
        }

        Player targetPlayer = plugin.getServer().getPlayer(args[0]);
        if (targetPlayer == null)
            return SkillResult.INVALID_TARGET;
        

        if (!hero.getParty().isPartyMember(plugin.getCharacterManager().getHero(targetPlayer))) {
            Messaging.send(player, "Sorry, that player isn't in your party!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }
        int level = hero.getSkillLevel(this);
        Location loc1 = targetPlayer.getLocation().add(Util.nextRand() * (-50 + level - (50 - level)), 0, Util.nextRand() * (-50 + level - (50 - level)));
        Double highestBlock = (double) targetPlayer.getWorld().getHighestBlockYAt(loc1);
        loc1.setY(highestBlock);
        player.teleport(loc1);
        player.getWorld().playEffect(player.getLocation(), Effect.SMOKE, 3);
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.PORTAL_TRAVEL , 10.0F, 1.0F); 
        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }
}
