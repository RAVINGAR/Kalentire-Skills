package com.herocraftonline.heroes.characters.skill.public1;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.nms.NMSHandler;

public class SkillShield extends PassiveSkill {

	/** I'm terrible at my job so this is my temporary fix for placing custom shields. - lordofroosters */
	//private ArrayList<String> shieldNames = new ArrayList<String>();
	private ArrayList<Material> shieldItems = new ArrayList<Material>();

	public SkillShield(Heroes plugin) {
		super(plugin, "Shield");
		setDescription("You are able to use doors as shields to absorb damage!");
		setArgumentRange(0, 0);
		setEffectTypes(EffectType.BENEFICIAL, EffectType.PHYSICAL);
		setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL);
		Bukkit.getServer().getPluginManager().registerEvents(new CustomListener(this), plugin);

		//shieldNames.add("Worn Shield of Ironpass"); // I always put ArrayList components in the class constructor itself :P
		shieldItems.add(Material.IRON_DOOR);
		shieldItems.add(Material.OAK_DOOR);
		shieldItems.add(Material.BIRCH_DOOR);
		shieldItems.add(Material.SPRUCE_DOOR);
		shieldItems.add(Material.JUNGLE_DOOR);
		shieldItems.add(Material.ACACIA_DOOR);
		shieldItems.add(Material.DARK_OAK_DOOR);

		shieldItems.add(Material.IRON_TRAPDOOR);
		shieldItems.add(Material.OAK_TRAPDOOR);
		shieldItems.add(Material.BIRCH_TRAPDOOR);
		shieldItems.add(Material.SPRUCE_TRAPDOOR);
		shieldItems.add(Material.JUNGLE_TRAPDOOR);
		shieldItems.add(Material.ACACIA_TRAPDOOR);
		shieldItems.add(Material.DARK_OAK_TRAPDOOR);
	}

	@Override
	public String getDescription(Hero hero) {
		return getDescription();
	}

	@Override
	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection node = super.getDefaultConfig();

		node.set(SkillSetting.APPLY_TEXT.node(), "");
		node.set(SkillSetting.UNAPPLY_TEXT.node(), "");
		node.set("iron-door", 0.75);
		node.set("wooden-door", 0.85);
		node.set("iron-trapdoor", 0.90);
		node.set("wooden-trapdoor", 0.95);

		return node;
	}

	public List<Material> getShieldItems() {
		return new ArrayList<>(shieldItems);
	}

	public class CustomListener implements Listener {

		private final Skill skill;

		public CustomListener(Skill skill) {
			this.skill = skill;
		}

		@EventHandler(priority = EventPriority.HIGHEST)
		public void onWeaponDamage(WeaponDamageEvent event) {
			if (event.getCause() != DamageCause.ENTITY_ATTACK || event.getDamage() == 0 || !(event.getEntity() instanceof Player)) {
				return;
			}

			Player player = (Player) event.getEntity();
			Hero hero = plugin.getCharacterManager().getHero(player);
			if (hero.hasEffect(getName())) {
				Material type = NMSHandler.getInterface().getItemInOffHand(player.getInventory()).getType();
				double multiplier = 1;
				switch (type) {
					case IRON_DOOR:
						multiplier = SkillConfigManager.getUseSetting(hero, skill, "iron-door", 0.75, true);
						break;
					case OAK_DOOR:
					case BIRCH_DOOR:
					case SPRUCE_DOOR:
					case JUNGLE_DOOR:
					case ACACIA_DOOR:
					case DARK_OAK_DOOR:
						multiplier = SkillConfigManager.getUseSetting(hero, skill, "wooden-door", 0.85, true);
						break;
					case IRON_TRAPDOOR:
						multiplier = SkillConfigManager.getUseSetting(hero, skill, "iron-trapdoor", 0.90, true);
						break;
					case OAK_TRAPDOOR:
					case BIRCH_TRAPDOOR:
					case SPRUCE_TRAPDOOR:
					case JUNGLE_TRAPDOOR:
					case ACACIA_TRAPDOOR:
					case DARK_OAK_TRAPDOOR:
						multiplier = SkillConfigManager.getUseSetting(hero, skill, "wooden-trapdoor", 0.95, true);
						break;
				}
				event.setDamage((event.getDamage() * multiplier));
			}
		}

		@EventHandler(priority = EventPriority.NORMAL)
		public void onBlockPlaced(BlockPlaceEvent event) {
			Hero hero = plugin.getCharacterManager().getHero(event.getPlayer());
			if (hero.hasEffect(getName()) && getShieldItems().contains(event.getBlock().getType()) && hero.isInCombat()) {
				event.setCancelled(true);
			}
		}
	}
}
