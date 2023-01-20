package com.herocraftonline.heroes.characters.skill.general;

import java.util.Map;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.Vector;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.SkillResult.ResultType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;

public class SkillBarrage extends ActiveSkill {

	public SkillBarrage(Heroes plugin) {
		super(plugin, "Barrage");
		setDescription("You fire a barrage of arrows in all directions.");
		setUsage("/skill barrage");
		setArgumentRange(0, 0);
		setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.DAMAGING, SkillType.AGGRESSIVE);
		setIdentifiers("skill barrage");
	}

	@Override
	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection node = super.getDefaultConfig();

		node.set("velocity-multiplier", 2);

		return node;
	}

	@Override
	public String getDescription(Hero hero) {
		return getDescription();
	}

	@Override
	public SkillResult use(Hero hero, String[] args) {
		Player player = hero.getPlayer();
		PlayerInventory inv = player.getInventory();

		Map<Integer, ? extends ItemStack> arrowSlots = inv.all(Material.ARROW);

		int numArrows = 0;
		for (Map.Entry<Integer, ? extends ItemStack> entry : arrowSlots.entrySet()) {
			numArrows += entry.getValue().getAmount();
		}

		if (numArrows == 0) {
			player.sendMessage("You have no arrows.");
			return new SkillResult(ResultType.MISSING_REAGENT, false);
		}

		numArrows = numArrows > 24 ? 24 : numArrows;

		int removedArrows = 0;
		for (Map.Entry<Integer, ? extends ItemStack> entry : arrowSlots.entrySet()) {
			int amount = entry.getValue().getAmount();
			int remove = amount;
			if (removedArrows + remove > numArrows) {
				remove = numArrows - removedArrows;
			}
			removedArrows += remove;
			if (remove == amount) {
				inv.clear(entry.getKey());
			}
			else {
				inv.getItem(entry.getKey()).setAmount(amount - remove);
			}

			if (removedArrows >= numArrows) {
				break;
			}
		}
		player.updateInventory();

		double velocityMultiplier = SkillConfigManager.getUseSetting(hero, this, "velocity-multiplier", 2, false);
		double diff = 2 * Math.PI / numArrows;
		for (double a = 0; a < 2 * Math.PI; a += diff) {
            Vector vel = new Vector(Math.cos(a), 0, Math.sin(a));

			Arrow arrow = player.launchProjectile(Arrow.class);
			//arrow.setVelocity(vel);			// Old method
			arrow.setVelocity(vel.multiply(velocityMultiplier));		// New method for increased damage
			arrow.setShooter(player);
		}

		broadcastExecuteText(hero);
		return SkillResult.NORMAL;
	}
}
