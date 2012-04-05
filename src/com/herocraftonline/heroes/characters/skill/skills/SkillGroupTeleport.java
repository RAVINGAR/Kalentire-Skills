package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;

public class SkillGroupTeleport extends ActiveSkill {

    public SkillGroupTeleport(Heroes plugin) {
        super(plugin, "GroupTeleport");
        setDescription("You summon your group to your location.");
        setUsage("/skill groupteleport");
        setArgumentRange(0, 0);
        setIdentifiers("skill groupteleport", "skill gteleport");
        setTypes(SkillType.TELEPORT, SkillType.SILENCABLE);
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        if (hero.getParty() != null && hero.getParty().getMembers().size() != 1) {
            for (Hero partyMember : hero.getParty().getMembers()) {
                if (!partyMember.getPlayer().getWorld().equals(player.getWorld())) {
                    continue;
                }
                partyMember.getPlayer().teleport(player);
            }
            broadcastExecuteText(hero);
            return SkillResult.NORMAL;
        }
        Messaging.send(player, "You must have a group to teleport your party members to you!");
        return SkillResult.FAIL;
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }
}
