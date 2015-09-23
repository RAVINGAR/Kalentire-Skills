package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.command.TownCommand;
import com.palmergames.bukkit.towny.exceptions.TownyException;

public class SkillTownSpawn extends ActiveSkill {

    public SkillTownSpawn(Heroes plugin) {
        super(plugin, "townspawn");
        setDescription("Teleport to your town's spawn.");
        setUsage("/skill townspawn");
        setIdentifiers("skill townspawn");
        setArgumentRange(0, 0);
        setTypes(SkillType.SILENCEABLE, SkillType.TELEPORTING);
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        try {
            TownCommand.townSpawn(hero.getPlayer(), args, false);
        }
        catch (TownyException e) {
            TownyMessaging.sendErrorMsg(hero.getPlayer(), e.getMessage());
        }
        return SkillResult.NORMAL;
    }


}
