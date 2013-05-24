package com.herocraftonline.heroes.characters.skill.skills;

/*
 * Coded by: Delfofthebla - Last updated on 5 / 5 / 2013
 * 
 * OVERVIEW:
 * ------------------------------------------------
 * This is the "Fire" Rune ability. It is one of the many Rune abilities available to the Runeblade.
 * It functions similar to a Rogue's Envenom, applying a buff to the player's weapon that will activate upon left clicking an enemy.
 * However, unlike Envenom, the RuneWord system allows for multiple effects to be applied to their weapon at once.
 * These are added in a queue, up to a maximum of 3 (determined by the SkillRuneAbsoprtipn Heroes Skill.)
 * 
 * Due to the unique nature of the Rune system, this ability, and all other Runeblade abilities, speak to a hosting skill called "SkillAbsorbRunes".
 * This is necessary in order to keep track of the Rune Queue for each individual player.
 * However, no programmer should ever have to worry about this unless they are changing the system itself.
 * Each of these Rune Abilities are stand-alone and are very clean to code.
 * 
 * 
 * ACTUAL SKILL INFORMATION:
 * ------------------------------------------------
 * Upon Rune activation, this ability causes a specified amount of fire damage to the target.
 */

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.skills.absorbrunes.Rune;
import com.herocraftonline.heroes.characters.skill.skills.absorbrunes.RuneActivationEvent;
import com.herocraftonline.heroes.characters.skill.skills.absorbrunes.RuneApplicationEvent;
import com.herocraftonline.heroes.util.Util;

public class SkillFireRune extends ActiveSkill
{
	// Default skill values
	private final int defDamage = 105;
	private final int defManaCost = 25;

	// Default text values
	private final String runeChatColor = "§c";				// Red
	private final String skillText = "§7[§2Skill§7] ";		// Used to add "[Skill]" text to all skill related messages

	private final String defUseText = skillText + "%hero% imbues his blade with a Rune of " + runeChatColor + "Fire.";
	private final String defApplyText = skillText + "%target% has been burned by a Rune of Fire!";

	public SkillFireRune(Heroes plugin)
	{
		// Heroes stuff
		super(plugin, "FireRune");
		setDescription("Imbue your blade with the Rune of Fire. Upon Rune application, this Rune will deal $1 fire damage to the target.");
		setUsage("/skill firerune");
		setIdentifiers("skill firerune");
		setTypes(SkillType.FIRE, SkillType.HARMFUL, SkillType.DAMAGING, SkillType.SILENCABLE);
		setArgumentRange(0, 0);

		// Start up the listener for Runeword skill usage
		Bukkit.getServer().getPluginManager().registerEvents(new FireRuneListener(this), plugin);
	}

	@Override
	public ConfigurationSection getDefaultConfig()
	{
		ConfigurationSection node = super.getDefaultConfig();

		node.set("weapons", Util.swords);
		node.set(SkillSetting.DAMAGE.node(), defDamage);
		node.set(SkillSetting.USE_TEXT.node(), defUseText);
		node.set(SkillSetting.APPLY_TEXT.node(), defApplyText);

		return node;
	}

	@Override
	public String getDescription(Hero hero)
	{
		int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, defDamage, false);
		return getDescription().replace("$1", damage + "");
	}

	@Override
	public SkillResult use(Hero hero, String[] args)
	{
		// Create the Rune
		int manaCost = (SkillConfigManager.getUseSetting(hero, this, SkillSetting.MANA, defManaCost, false));
		Rune fireRune = new Rune("FireRune", manaCost, runeChatColor);

		// Add the Rune to the RuneWord queue here
		Bukkit.getServer().getPluginManager().callEvent(new RuneActivationEvent(hero, fireRune));

		// Play Firework
		// CODE HERE

		// Play Sound
		hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.WITHER_IDLE, 0.5F, 1.0F);

		// Let the world know that the hero has activated a Rune.
		broadcastExecuteText(hero);

		return SkillResult.NORMAL;
	}

	/*
	 * This listener is the main controller for the FireRune ability. The primary function is to listen to the Rune Application event.
	 * It could be used to listen to other things as well, but that won't typically be necessary.
	 */
	private class FireRuneListener implements Listener
	{
		private final Skill skill;

		public FireRuneListener(Skill skill)
		{
			this.skill = skill;
		}

		// Listen for the Fire rune application
		@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
		public void onRuneApplication(RuneApplicationEvent event)
		{
			// Get Hero information
			Hero hero = event.getHero();

			// Check to see if this is the correct rune to apply, and that the player actually has the rune applied.
			if (!(event.getRuneList().getHead().name == "FireRune"))
				return;

			// Ensure that the target is a living entity
			Entity targEnt = event.getTarget();
			if (!(targEnt instanceof LivingEntity))
				return;

			// Prep variables
			CharacterTemplate targCT = skill.plugin.getCharacterManager().getCharacter((LivingEntity) targEnt);

			int damage = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE, defDamage, false);
			String applyText = SkillConfigManager.getRaw(skill, SkillSetting.APPLY_TEXT, defApplyText).replace("%target%", "$1");

			// Damage and silence the target
			skill.plugin.getDamageManager().addSpellTarget(targEnt, hero, skill);
			damageEntity((LivingEntity) targEnt, hero.getPlayer(), damage, EntityDamageEvent.DamageCause.FIRE);

			// Announce that the player has been hit with the skill	
			broadcast(targEnt.getLocation(), applyText, targCT.getName());

			// Play Firework effect
			// CODE HERE

			// Play sound
			hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.FIZZ, 0.5F, 1.0F);

			return;
		}
	}
}