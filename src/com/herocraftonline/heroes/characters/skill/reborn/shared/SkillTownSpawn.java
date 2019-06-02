package com.herocraftonline.heroes.characters.skill.reborn.shared;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.townships.Townships;
import com.herocraftonline.townships.towns.Town;
import com.herocraftonline.townships.users.TownshipsUser;
import com.herocraftonline.townships.users.UserManager;
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

        Plugin townyPlugin = plugin.getServer().getPluginManager().getPlugin("Townships");
        if(!(townyPlugin instanceof Townships)) {
            Heroes.log(Level.SEVERE, "SkillTownSpawn: Could not get Townships plugin! TownSpawn will not work!");
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

        TownshipsUser user = UserManager.fromOfflinePlayer(player);
        if(!user.hasTown())
            player.sendMessage(ChatColor.RED + "Warning:" + ChatColor.GRAY + " You are not a member of a town. Teleport will fail!");
        else if(!user.getTown().hasSpawnLocation())
            player.sendMessage(ChatColor.RED + "Warning:" + ChatColor.GRAY + " Your town does not have a spawn location. Teleport will fail!");

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

        // Check if player is in a town, and grab location of town spawn if they are.
        // Assumes the player is only allowed to teleport to their town, and that they are allowed to teleport to their own town.
        TownshipsUser user = UserManager.fromOfflinePlayer(player);

        if(!user.hasTown()) {
            player.sendMessage(ChatColor.GRAY + "No town found, teleport cancelled.");
            player.sendMessage(ChatColor.GRAY + "You must be a member of a town in order to teleport to your town's spawn.");
            return SkillResult.FAIL;
        }

        Town town = user.getTown();
        if(!town.hasSpawnLocation()) {
            player.sendMessage(ChatColor.GRAY + "No spawn location found, teleport cancelled.");
            player.sendMessage(ChatColor.GRAY + "Your town must have a spawn to teleport to! Ask a ranked member to set one.");
            return SkillResult.FAIL;
        }

        Location spawnLoc = town.getSpawnLocation();

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
            town.getBank().addBalance(costToTown);
        }
        return SkillResult.NORMAL;
    }
}