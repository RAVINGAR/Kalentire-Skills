package com.herocraftonline.heroes.characters.skill.skills;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;

public class SkillSoulSap extends PassiveSkill {

	public SkillSoulSap(Heroes plugin) {
		super(plugin, "SoulSap");
		setDescription("Passive: any player kills will automatically charge soul gems (emeralds) based on level if available in inventory");
		Bukkit.getServer().getPluginManager().registerEvents(new SoulSapListener(this), this.plugin);
	}

	@Override
	public String getDescription(Hero hero) {
		return getDescription();
	}
	public class SoulSapListener implements Listener {
		private Skill skill;

		public SoulSapListener(Skill skill) {
			this.skill = skill;
		}

		@EventHandler(priority = EventPriority.MONITOR)
		public void onPlayerDeath(PlayerDeathEvent event) {
			Player p = event.getEntity();
			Player k = p.getKiller();
			if(k==null) {
				return;
			}
			Hero h = skill.plugin.getCharacterManager().getHero(k);
			if(!h.hasEffect("SoulSap")) {
				return;
			}
			PlayerInventory pI = k.getInventory();
			if(!pI.contains(Material.EMERALD)) {
				return;
			}
			Iterator<Integer> emeralds = pI.all(Material.EMERALD).keySet().iterator();
			while(emeralds.hasNext()) {
				int next = emeralds.next();
				ItemStack emerald = pI.getItem(next);
				if(emerald.getAmount() > 1) {

					boolean creategem = createSoulGem(pI,p,skill.plugin);
					if(!creategem) {
						k.sendMessage("Not enough space in your inventory for a new soul gem!");
						return;
					}
					emerald.setAmount(emerald.getAmount()-1);
					break;
				}
				if(!emerald.getItemMeta().hasDisplayName()) {
					boolean creategem = createSoulGem(pI,p,skill.plugin);
					if(!creategem) {
						k.sendMessage("Not enough space in your inventory for a new soul gem!");
						return;
					}
					break;
				}
				if(emerald.getItemMeta().getDisplayName().toUpperCase().contains("GEM")) {
					continue;
				}
				boolean creategem = createSoulGem(pI,p,skill.plugin);
				if(!creategem) {
					k.sendMessage("Not enough space in your inventory for a new soul gem!");
					return;
				}
				break;
			}
		}

		private boolean createSoulGem(PlayerInventory pI, Player deadPlayer, Heroes heroes) {
			int empty = pI.firstEmpty();
			if(empty == -1) {
				return false;
			}
			ItemStack emerald = new ItemStack(Material.EMERALD,1);
			Hero h = heroes.getCharacterManager().getHero(deadPlayer);
			int level = h.getLevel();
			String rank = "";
			if(level >= 0 && level <= 20) {
				rank = "Petty";
			}
			if(level > 20 && level <= 30) {
				rank = "Weak";
			}
			if(level > 30 && level <= 40) {
				rank = "Common";
			}
			if(level > 40 && level <= 50) {
				rank = "Strong";
			}
			if(level > 50 && level < 65) {
				rank = "Major";
			}
			if(level >= 65 && level <75) {
				rank = "Master";
			}
			if(level == 75) {
				rank = "Legendary";
			}
			if(!h.getHeroClass().hasNoParents()) {
				rank = "Legendary";
			}
			ItemMeta meta = emerald.getItemMeta();
			meta.setDisplayName(rank + " Soul Gem");
			List<String> lore = new ArrayList<String>();
			lore.add(0,ChatColor.GRAY + "A gem that entraps the soul of a newly killed opponent.");
			lore.add(1,ChatColor.GRAY + "Can be used to attempt to install a random item mod.");
			meta.setLore(lore);
			emerald.setItemMeta(meta);
			pI.setItem(empty, emerald);
			((Player)((LivingEntity)pI.getHolder())).sendMessage(rank + " Soul Gem" + ChatColor.GRAY + " Successfully Created!");
			return true;
		}
	}

}
