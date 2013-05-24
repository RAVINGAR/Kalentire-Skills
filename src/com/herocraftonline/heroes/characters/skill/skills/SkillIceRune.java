package com.herocraftonline.heroes.characters.skill.skills;

/*
 * Coded by: Delfofthebla - Last updated on 5 / 5 / 2013
 * 
 * OVERVIEW:
 * ------------------------------------------------
 * This is the "Ice" Rune ability. It is one of the many Rune abilities available to the Runeblade.
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
 * Upon Rune activation, this ability damages the target by a specified amount and applies a slow effect for a set duration.
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
import com.herocraftonline.heroes.characters.effects.common.SlowEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.skills.absorbrunes.Rune;
import com.herocraftonline.heroes.characters.skill.skills.absorbrunes.RuneActivationEvent;
import com.herocraftonline.heroes.characters.skill.skills.absorbrunes.RuneApplicationEvent;

public class SkillIceRune extends ActiveSkill
{
	// Default skill values
	private final int defDamage = 40;
	private final int defSlowValue = 2;
	private final int defDuration = 2000;
	private final int defManaCost = 30;

	// Default text values
	private final String runeChatColor = "§b";				// Aqua
	private final String skillText = "§7[§2Skill§7] ";		// Used to add "[Skill]" text to all skill related messages

	private final String defUseText = skillText + "%hero% imbues his blade with a Rune of " + runeChatColor + "Ice.";
	private final String defApplyText = skillText + "%target% has been slowed by a Rune of Ice!";
	private final String defExpireText = skillText + "%target% is no longer slowed!";

	public SkillIceRune(Heroes plugin)
	{
		// Heroes stuff
		super(plugin, "IceRune");
		setDescription("Imbue your blade with the Rune of Ice. Upon Rune application, this Rune will deal $1 magic damage and slow the target for $2 seconds.");
		setUsage("/skill icerune");
		setIdentifiers("skill icerune");
		setTypes(SkillType.HARMFUL, SkillType.DEBUFF, SkillType.INTERRUPT, SkillType.SILENCABLE);
		setArgumentRange(0, 0);

		// Start up the listener for skill usage
		Bukkit.getServer().getPluginManager().registerEvents(new IceRuneListener(this), plugin);
	}

	@Override
	public ConfigurationSection getDefaultConfig()
	{
		ConfigurationSection node = super.getDefaultConfig();

		node.set("speed-multiplier", defSlowValue);
		node.set(SkillSetting.DAMAGE.node(), defDamage);
		node.set(SkillSetting.DURATION.node(), defDuration);
		node.set(SkillSetting.USE_TEXT.node(), defUseText);
		node.set(SkillSetting.APPLY_TEXT.node(), defApplyText);
		node.set(SkillSetting.EXPIRE_TEXT.node(), defExpireText);

		return node;
	}

	@Override
	public String getDescription(Hero hero)
	{
		int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, defDuration, false) / 1000;
		int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, defDamage, false);
		return getDescription().replace("$1", damage + "").replace("$2", duration + "");
	}

	@Override
	public SkillResult use(Hero hero, String[] args)
	{
		// Create the Rune
		int manaCost = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MANA, defManaCost, false);
		Rune iceRune = new Rune("IceRune", manaCost, runeChatColor);

		// Add the Rune to the RuneWord queue here
		Bukkit.getServer().getPluginManager().callEvent(new RuneActivationEvent(hero, iceRune));

		// Play Firework
		// CODE HERE

		// Play sound
		hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.WITHER_IDLE, 0.5F, 1.0F);

		// Let the world know that the hero has activated a Rune.
		broadcastExecuteText(hero);

		return SkillResult.NORMAL;
	}

	/*
	 * This listener is the main controller for the IceRune ability. The primary function is to listen to the Rune Application event.
	 * It could be used to listen to other things as well, but that won't typically be necessary.
	 */
	private class IceRuneListener implements Listener
	{
		private final Skill skill;

		public IceRuneListener(Skill skill)
		{
			this.skill = skill;
		}

		// Listen for the Ice rune application
		@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
		public void onRuneApplication(RuneApplicationEvent event)
		{
			// Get Hero information
			Hero hero = event.getHero();

			// Check to see if this is the correct rune to apply, and that the player actually has the rune applied.
			if (!(event.getRuneList().getHead().name == "IceRune"))
				return;

			// Ensure that the target is a living entity
			Entity targEnt = event.getTarget();
			if (!(targEnt instanceof LivingEntity))
				return;

			// Prep variables
			CharacterTemplate targCT = skill.plugin.getCharacterManager().getCharacter((LivingEntity) targEnt);

			int amplifier = SkillConfigManager.getUseSetting(hero, skill, "speed-multiplier", defSlowValue, false);
			long duration = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DURATION, defDuration, false);
			int damage = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE, defDamage, false);

			String applyText = SkillConfigManager.getRaw(skill, SkillSetting.APPLY_TEXT, defApplyText).replace("%target%", "$1");
			String expireText = SkillConfigManager.getRaw(skill, SkillSetting.EXPIRE_TEXT, defExpireText).replace("%target%", "$1");

			// Create the effect and slow the target
			SlowEffect sEffect = new SlowEffect(skill, duration, amplifier, false, applyText, expireText, hero);

			// Damage and silence the target
			skill.plugin.getDamageManager().addSpellTarget(targEnt, hero, skill);
			damageEntity((LivingEntity) targEnt, hero.getPlayer(), damage, EntityDamageEvent.DamageCause.MAGIC);
			targCT.addEffect(sEffect);

			// Play firework effect
			//FireworkEffect fireworkEffect = new FireworkEffect(false, false, null, null, null);
			//VisualEffect ve = new VisualEffect();

			// Play sound
			hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.FIZZ, 0.5F, 1.0F);

			return;
		}
	}
}