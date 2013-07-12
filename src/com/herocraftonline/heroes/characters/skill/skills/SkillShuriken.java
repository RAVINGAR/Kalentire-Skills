package com.herocraftonline.heroes.characters.skill.skills;

import java.util.LinkedHashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;

public class SkillShuriken extends PassiveSkill {

	private Map<Arrow, Long> shurikens = new LinkedHashMap<Arrow, Long>(100) {
		private static final long serialVersionUID = -1L;

		protected boolean removeEldestEntry(Map.Entry<Arrow, Long> eldest) {
			return (size() > 60) || (((Long) eldest.getValue()).longValue() + 5000L <= System.currentTimeMillis());
		}
	};

	public SkillShuriken(Heroes plugin) {
		super(plugin, "Shuriken");
		setDescription("Right click with a flint in hand to throw a Shuriken! Shurikens deal $1 damage and can be thrown once every $2 seconds.");
		setArgumentRange(0, 0);
		setTypes(SkillType.HARMFUL, SkillType.DAMAGING, SkillType.ITEM, SkillType.PHYSICAL, SkillType.UNBINDABLE);

		Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(this), plugin);
	}

	public String getDescription(Hero hero) {

		int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 20, false);
		double cooldown = SkillConfigManager.getUseSetting(hero, this, "shuriken-toss-cooldown", 500, false) / 1000;

		return getDescription().replace("$1", damage + "").replace("$2", cooldown + "");
	}

	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection node = super.getDefaultConfig();

		node.set(SkillSetting.DAMAGE.node(), Integer.valueOf(20));
		node.set("stamina-cost", Integer.valueOf(1));
		node.set("shuriken-toss-cooldown", Integer.valueOf(500));
		node.set("velocity-multiplier", Double.valueOf(2.0D));

		return node;
	}

	public class SkillEntityListener implements Listener {

		private Skill skill;

		public SkillEntityListener(Skill skill) {
			this.skill = skill;
		}

		@EventHandler(priority = EventPriority.MONITOR)
		public void onPlayerInteract(PlayerInteractEvent event) {
			Player player = event.getPlayer();

			if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
				if (event.hasItem()) {
					ItemStack activatedItem = event.getItem();

					if (activatedItem.getType() == Material.FLINT) {

						// If the clicked block is null, we are clicking air. Air is a valid block that we do not need to validate
						if (event.getClickedBlock() != null) {

							// VALIDATE NON-AIR BLOCK
							if (!(isInteractableBlock(event.getClickedBlock().getType()))) {
								shurikenToss(player, activatedItem);
							}
						}
						else {
							// AIR BLOCK. NO VALIDATION
							shurikenToss(player, activatedItem);
						}
					}
				}
			}
		}

		private void shurikenToss(Player player, ItemStack activatedItem) {
			Hero hero = plugin.getCharacterManager().getHero(player);

			// Check if the player's class actually has the skill available
			if (!hero.canUseSkill(skill))
				return;					// Class does not have the skill. Do nothing.

			if (hero.hasEffect("ShurikenTossCooldownEffect"))
				return;		// Shuriken Toss is on cooldown. Do not continue.

			int staminaCost = SkillConfigManager.getUseSetting(hero, skill, "stamina-cost", 1, false);
			if (player.getFoodLevel() < staminaCost) {
				Messaging.send(player, "§7[§2Skill§7] You are too fatigued!");
				return;
			}

			// Add the cooldown effect
			int cdDuration = SkillConfigManager.getUseSetting(hero, skill, "shuriken-toss-cooldown", 500, false);
			hero.addEffect(new ShurikenTossCooldownEffect(skill, cdDuration));

			// Put the shuriken on our hashmap
			Arrow shuriken = (Arrow) player.launchProjectile(Arrow.class);
			shurikens.put(shuriken, Long.valueOf(System.currentTimeMillis()));

			// Fire the shuriken
			double velocityMultiplier = SkillConfigManager.getUseSetting(hero, skill, "velocity-multiplier", 1.5D, false);
			shuriken.setVelocity(shuriken.getVelocity().multiply(velocityMultiplier));
			shuriken.setShooter(player);
			//shuriken.setKnockbackStrength(0);

			// Remove a flint from their inventory
			PlayerInventory inventory = player.getInventory();
			activatedItem.setAmount(activatedItem.getAmount() - 1);

			if (activatedItem.getAmount() == 0) {
				inventory.clear(inventory.getHeldItemSlot());
			}

			// Reduce their stamina by the stamina cost value
			player.setFoodLevel(Math.min(player.getFoodLevel() - staminaCost, 20));
		}

		@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
		public void onEntityDamage(EntityDamageEvent event) {
			if ((!(event instanceof EntityDamageByEntityEvent)) || (!(event.getEntity() instanceof LivingEntity))) {
				return;
			}

			Entity projectile = ((EntityDamageByEntityEvent) event).getDamager();
			if ((!(projectile instanceof Arrow)) || (!(((Projectile) projectile).getShooter() instanceof Player))) {
				return;
			}

			if (!(shurikens.containsKey((Arrow) projectile))) {
				return;
			}

			Arrow shuriken = (Arrow) projectile;
			Player player = (Player) shuriken.getShooter();
			Hero hero = plugin.getCharacterManager().getHero(player);

			// Remove the shuriken from the hash map
			shurikens.remove(shuriken);

			LivingEntity target = (LivingEntity) event.getEntity();

			// Damage the target
			double damage = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE, 20, false);
			skill.plugin.getDamageManager().addSpellTarget(target, hero, skill);
			damageEntity(target, player, damage, EntityDamageEvent.DamageCause.MAGIC);

			// Prevent arrow from dealing damage
			shuriken.remove();
			event.setDamage(0);
			event.setCancelled(true);
		}
	}

	// Effect required for implementing an internal cooldown
	private class ShurikenTossCooldownEffect extends ExpirableEffect {
		public ShurikenTossCooldownEffect(Skill skill, long duration) {
			super(skill, "ShurikenTossCooldownEffect", duration);
		}
	}

	private boolean isInteractableBlock(Material material) {
		if (material == Material.CHEST)
			return true;
		else if (material == Material.LOCKED_CHEST)
			return true;
		else if (material == Material.IRON_DOOR_BLOCK)
			return true;
		else if (material == Material.SIGN)
			return true;
		else if (material == Material.WALL_SIGN)
			return true;
		else if (material == Material.SIGN_POST)
			return true;
		else if (material == Material.WORKBENCH)
			return true;
		else if (material == Material.STONE_BUTTON)
			return true;
		else if (material == Material.WOOD_BUTTON)
			return true;
		else if (material == Material.LEVER)
			return true;
		else if (material == Material.WOODEN_DOOR)
			return true;
		else if (material == Material.TRAP_DOOR)
			return true;
		else if (material == Material.TRAPPED_CHEST)
			return true;
		else if (material == Material.DIODE)	// Repeater
			return true;
		//		else if (material == Material.DIODE_BLOCK_ON)	// Repeater
		//			return true;
		//		else if (material == Material.DIODE_BLOCK_OFF)	// Repeater
		//			return true;
		else if (material == Material.DISPENSER)
			return true;
		else if (material == Material.HOPPER)
			return true;
		else if (material == Material.DROPPER)
			return true;
		else if (material == Material.REDSTONE_COMPARATOR)
			return true;
		//		else if (material == Material.REDSTONE_COMPARATOR_ON)
		//			return true;
		//		else if (material == Material.REDSTONE_COMPARATOR_OFF)
		//			return true;
		else if (material == Material.FURNACE)
			return true;
		else if (material == Material.BURNING_FURNACE)
			return true;
		else if (material == Material.CAULDRON)
			return true;
		else if (material == Material.JUKEBOX)
			return true;
		else if (material == Material.NOTE_BLOCK)
			return true;
		else if (material == Material.STORAGE_MINECART)
			return true;
		else if (material == Material.ENDER_CHEST)
			return true;
		else if (material == Material.FENCE_GATE)
			return true;
		else if (material == Material.ENCHANTMENT_TABLE)
			return true;
		else if (material == Material.BREWING_STAND)
			return true;
		else if (material == Material.ITEM_FRAME)
			return true;
		else if (material == Material.BOAT)
			return true;
		else if (material == Material.MINECART)
			return true;
		else if (material == Material.FLOWER_POT)
			return true;
		else if (material == Material.BEACON)
			return true;
		else if (material == Material.BED_BLOCK)
			return true;
		else if (material == Material.ANVIL)
			return true;
		else if (material == Material.COMMAND)
			return true;
		else
			return false;
	}
}