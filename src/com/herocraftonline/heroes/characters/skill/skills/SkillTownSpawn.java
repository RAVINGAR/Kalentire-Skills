/*package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.*;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.exceptions.EconomyException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.logging.Level;

public class SkillTownSpawn extends ActiveSkill {

    public SkillTownSpawn(Heroes plugin) {
        super(plugin, "TownSpawn");
        setDescription("Teleport to your town's spawn. Costs $1 to use. $2 goes to the town bank.");
        setUsage("/skill townspawn");
        setIdentifiers("skill townspawn");
        setArgumentRange(0, 1);
        setTypes(SkillType.SILENCEABLE, SkillType.TELEPORTING);

        Plugin townyPlugin = plugin.getServer().getPluginManager().getPlugin("Towny");
        if(!(townyPlugin instanceof Towny)) {
            Heroes.log(Level.SEVERE, "SkillTownSpawn: Could not get Towny plugin! TownSpawn will not work!");
            plugin.getSkillManager().removeSkill(this);
            return;
        }

        if(Heroes.econ == null) {
            Heroes.log(Level.SEVERE, "SkillTownSpawn: Could not get Vault or Economy plugin! Monetary cost may not work!");
        }
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.NO_COMBAT_USE.node(), true);
        node.set(SkillSetting.DELAY.node(), 10000);

        node.set("money-cost", 25.00D);
        node.set("money-to-town", 5.00D);

        return node;
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription()
                .replace("$1", Heroes.econ.format(SkillConfigManager.getUseSetting(hero, this, "money-cost", 25.00D, false)))
                .replace("$2", Heroes.econ.format(SkillConfigManager.getUseSetting(hero, this, "money-to-town", 5.00D, false)));
    }

    // Money as a skill cost isn't really a thing in heroes. Easiest way to be nice to players is to warn them on warmup if they don't have enough.
    @Override
    public void onWarmup(Hero hero) {
        Player player = hero.getPlayer();
        double cost = SkillConfigManager.getUseSetting(hero, this, "money-cost", 25.00D, false);
        if(cost > 0) {
            double balance = Heroes.econ.getBalance(player);
            if (balance < cost) {
                player.sendMessage(ChatColor.RED + "Warning:" + ChatColor.GRAY + " Teleport costs " + Heroes.econ.format(cost) + ". You have " + Heroes.econ.format(balance) + ". Teleport will fail!");
            }
        }
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        Town town;
        Location spawnLoc;
        // Check if player is in a town, and grab location of town spawn if they are.
        // Assumes the player is only allowed to teleport to their town, and that they are allowed to teleport to their own town.
        // Bypasses Towny logic to choose a town and determine ability to teleport.
        try {
            town = TownyUniverse.getDataSource().getResident(player.getName()).getTown();
            spawnLoc = town.getSpawn();
        }
        catch (NotRegisteredException e) {
            player.sendMessage(ChatColor.GRAY + "You don't belong to a Town!");
            return SkillResult.FAIL;
        }
        catch (TownyException e) {
            player.sendMessage(ChatColor.RED + e.getMessage());
            return SkillResult.FAIL;
        }

        double cost = SkillConfigManager.getUseSetting(hero, this, "money-cost", 25.00D, false);
        double costToTown = SkillConfigManager.getUseSetting(hero, this, "money-to-town", 5.00D, false);

        // Check
        if(cost > 0) {
            EconomyResponse response = Heroes.econ.withdrawPlayer(player, cost);
            if (!response.transactionSuccess()) {
                player.sendMessage(ChatColor.GRAY + "Transaction failed, teleport cancelled.");
                player.sendMessage(ChatColor.GRAY + "You need " + Heroes.econ.format(cost) + " to teleport. You have " + Heroes.econ.format(Heroes.econ.getBalance(player)) + ".");
                return SkillResult.FAIL;
            }
            else {
                player.sendMessage(ChatColor.GRAY + "Charged " + Heroes.econ.format(cost) + " to teleport to your town's spawn.");
            }
        }

        broadcastExecuteText(hero);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5F, 1.0F); // Sound stolen from Recall
        player.teleport(spawnLoc);
        player.getWorld().playSound(spawnLoc, Sound.ENTITY_WITHER_SPAWN, 0.5F, 1.0F); // Sound stolen from Recall

        // Give money to town if we can. If we can't, the teleport was already paid for so oh well.
        if(costToTown > 0) {
            try {
                town.collect(costToTown);
            } catch (EconomyException e) {
                player.sendMessage(ChatColor.RED + "Error depositing money in town bank!");
                player.sendMessage(e.getError());
            }
        }
        return SkillResult.NORMAL;
    }
}
*/