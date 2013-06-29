package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Color;
import org.bukkit.Effect;
import org.bukkit.FireworkEffect;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.characters.skill.VisualEffect;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

public class SkillLunarLance extends TargettedSkill {
	private final int defDamage = 95;													// Default damage
	private final int defBurnValue = 20;												// Default mana burn amount
	private final String defFailText = "§fYou need a shovel to use this ability!";		// Default skill fail text

	public VisualEffect fplayer = new VisualEffect();									// Firework effect

	public SkillLunarLance(Heroes plugin) {
		super(plugin, "LunarLance");
		setDescription("Strike the target with a Lunar Lance dealing $1 Light damage and burning $2 mana from the target.");
		setUsage("/skill lunarlance");
		setArgumentRange(0, 0);
		setIdentifiers("skill lunarlance", "skill lance");
		setTypes(SkillType.DAMAGING, SkillType.SILENCABLE, SkillType.LIGHT, SkillType.MANA, SkillType.HARMFUL);
	}

	@Override
	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection node = super.getDefaultConfig();
		node.set("weapons", Util.shovels);
		node.set("burn-amount", defBurnValue);
		node.set("fail-text", defFailText);
		node.set(SkillSetting.DAMAGE.node(), defDamage);
		return node;
	}

	@Override
	public String getDescription(Hero hero) {
		int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, defDamage, false);
		int manaBurn = SkillConfigManager.getUseSetting(hero, this, "burn-amount", defBurnValue, false);
		return getDescription().replace("$1", damage + "").replace("$2", manaBurn + "");
	}

	@Override
	public SkillResult use(Hero hero, LivingEntity target, String[] args) {

		Player player = hero.getPlayer();
		ItemStack item = player.getItemInHand();

		// Ensure they have a weapon in hand
		if (!SkillConfigManager.getUseSetting(hero, this, "weapons", Util.shovels).contains(item.getType().name())) {
			// Notify them that they don't have a shovel equipped
			String failText = SkillConfigManager.getUseSetting(hero, this, "fail-text", defFailText);
			Messaging.send(hero.getPlayer(), failText, new Object[0]);

			return SkillResult.FAIL;
		}

		// If the target is a player, drain their mana
		if ((target instanceof Player)) {
			// Get the target hero
			Hero tHero = plugin.getCharacterManager().getHero((Player) target);

			// Burn their mana
			int burnValue = SkillConfigManager.getUseSetting(hero, this, "burn-amount", defBurnValue, false);
			if (tHero.getMana() > burnValue) {
				// Burn the target's mana
				int newMana = tHero.getMana() - burnValue;
				tHero.setMana(newMana);
			}
			else {
				// Burn all of their remaining mana
				tHero.setMana(0);
			}
		}

		// Deal damage
		int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, defDamage, false);
		damageEntity(target, hero.getPlayer(), damage, EntityDamageEvent.DamageCause.MAGIC);

		// Player Firework Effect
		try {
			fplayer.playFirework(player.getWorld(), target.getLocation().add(0, 1.5, 0), FireworkEffect.builder().flicker(true).trail(false).with(FireworkEffect.Type.BALL_LARGE).withColor(Color.BLUE).withFade(Color.AQUA).build());
		}
		catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		player.getWorld().playEffect(player.getLocation(), Effect.EXTINGUISH, 3);

		// Play Sound
		hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.CAT_HISS, 0.8F, 1.0F);

		// Broadcast skill usage
		broadcastExecuteText(hero, target);

		return SkillResult.NORMAL;
	}
}
