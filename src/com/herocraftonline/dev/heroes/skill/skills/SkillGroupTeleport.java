package com.herocraftonline.dev.heroes.skill.skills;

import org.bukkit.entity.Player;

import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.api.SkillResult;
import com.herocraftonline.dev.heroes.hero.Hero;
import com.herocraftonline.dev.heroes.skill.ActiveSkill;
import com.herocraftonline.dev.heroes.skill.SkillType;
import com.herocraftonline.dev.heroes.util.Messaging;

public class SkillGroupTeleport extends ActiveSkill {

    public SkillGroupTeleport(Heroes plugin) {
        super(plugin, "GroupTeleport");
        setDescription("Summons your group to your location");
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
                partyMember.getPlayer().teleport(player);
            }
            broadcastExecuteText(hero);
            return SkillResult.NORMAL;
        }
        Messaging.send(player, "You must have a group to teleport your party members to you!");
        return SkillResult.FAIL;
    }
}
