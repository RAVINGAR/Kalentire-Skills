package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.DelayedSkill;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Messaging;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.command.TownCommand;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class SkillTownSpawn extends ActiveSkill {

    public SkillTownSpawn(Heroes plugin) {
        super(plugin, "TownSpawn");
        setDescription("Teleport to your town's spawn.");
        setUsage("/skill townspawn");
        setIdentifiers("skill townspawn");
        setArgumentRange(0, 0);
        setTypes(SkillType.SILENCEABLE, SkillType.TELEPORTING);
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.NO_COMBAT_USE.node(), true);
        node.set(SkillSetting.DELAY.node(), 10000);

        return node;
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        Location spawnLoc;
        // Check if player is in a town, and grab location of town spawn if they are.
        // Assumes the player is only allowed to teleport to their town, and that they are allowed to teleport to their own town.
        // Bypasses Towny logic to choose a town and determine ability to teleport.
        try {
            spawnLoc = TownyUniverse.getDataSource().getResident(player.getName()).getTown().getSpawn();
        }
        catch (NotRegisteredException e) {
            Messaging.send(player, "You don't belong to a Town!");
            return SkillResult.FAIL;
        }
        catch (TownyException e) {
            Messaging.send(player, e.getMessage());
            return SkillResult.FAIL;
        }

        broadcastExecuteText(hero);
        player.getWorld().playSound(player.getLocation(), Sound.WITHER_SPAWN, 0.5F, 1.0F); // Sound stolen from Recall
        player.teleport(spawnLoc);
        player.getWorld().playSound(spawnLoc, Sound.WITHER_SPAWN, 0.5F, 1.0F); // Sound stolen from Recall

        return SkillResult.NORMAL;
    }



}
