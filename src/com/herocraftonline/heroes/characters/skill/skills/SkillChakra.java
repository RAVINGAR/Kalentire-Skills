package com.herocraftonline.heroes.characters.skill.skills;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainHealthEvent;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.VisualEffect;
import com.herocraftonline.heroes.util.Util;

public class SkillChakra extends ActiveSkill {
	// This is for Firework Effects
	public VisualEffect fplayer = new VisualEffect();

	public SkillChakra(Heroes plugin) {
		super(plugin, "Chakra");
		setDescription("You restore $1 health and dispel up to $2 negative effects from all party-members within $3 blocks. You are only healed for $4 health from this ability however.");
		setUsage("/skill chakra");
		setArgumentRange(0, 0);
		setIdentifiers("skill chakra");
		setTypes(SkillType.SILENCEABLE, SkillType.AREA_OF_EFFECT, SkillType.HEALING, SkillType.DISPELLING, SkillType.ABILITY_PROPERTY_LIGHT);
	}

	@Override
	public String getDescription(Hero hero) {
		int wisdom = hero.getAttributeValue(AttributeType.WISDOM);

		int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5, false);
		double radiusIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS_INCREASE_PER_WISDOM, 0.125, false);
		radius += (int) (wisdom * radiusIncrease);

		double healing = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING, 75, false);
		double healingIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING_INCREASE_PER_WISDOM, 0.875, false);
		healing += (wisdom * healingIncrease);

		int removals = SkillConfigManager.getUseSetting(hero, this, "max-removals", 0, true);
		double removalsIncrease = SkillConfigManager.getUseSetting(hero, this, "max-removals-increase-per-wisdom", 0.05, false);
		removals += Math.floor((wisdom * removalsIncrease));     // Round down

		String formattedHealing = Util.decFormat.format(healing);
		String formattedSelfHealing = Util.decFormat.format(healing * Heroes.properties.selfHeal);

		return getDescription().replace("$1", formattedHealing).replace("$2", removals + "").replace("$3", radius + "").replace("$4", formattedSelfHealing);
	}

	@Override
	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection node = super.getDefaultConfig();

		node.set(SkillSetting.RADIUS.node(), 5);
		node.set(SkillSetting.RADIUS_INCREASE_PER_WISDOM.node(), 0.125);
		node.set(SkillSetting.HEALING.node(), 75);
		node.set(SkillSetting.HEALING_INCREASE_PER_WISDOM.node(), 0.875);
		node.set("max-removals", 0);
		node.set("max-removals-increase-per-wisdom", 0.05);

		return node;
	}

	public ArrayList<Location> circle(Location centerPoint, int particleAmount, double circleRadius)
	{
		World world = centerPoint.getWorld();

		double increment = (2 * Math.PI) / particleAmount;

		ArrayList<Location> locations = new ArrayList<Location>();

		for (int i = 0; i < particleAmount; i++)
		{
			double angle = i * increment;
			double x = centerPoint.getX() + (circleRadius * Math.cos(angle));
			double z = centerPoint.getZ() + (circleRadius * Math.sin(angle));
			locations.add(new Location(world, x, centerPoint.getY(), z));
		}
		return locations;
	}

	@Override
	public SkillResult use(Hero hero, String[] args) {
		Player player = hero.getPlayer();
		Location castLoc = player.getLocation().clone();

		int wisdom = hero.getAttributeValue(AttributeType.WISDOM);

		int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5, false);
		double radiusIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS_INCREASE_PER_WISDOM, 0.125, false);
		radius += (int) (wisdom * radiusIncrease);
		int radiusSquared = radius * radius;

		double healing = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING, 75, false);
		double healingIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING_INCREASE_PER_WISDOM, 0.875, false);
		healing += (wisdom * healingIncrease);

		int removals = SkillConfigManager.getUseSetting(hero, this, "max-removals", 0, true);
		double removalsIncrease = SkillConfigManager.getUseSetting(hero, this, "max-removals-increase-per-wisdom", 0.05, false);
		removals += Math.floor(wisdom * removalsIncrease);     // Round down

		if (hero.hasParty()) {
			for (Hero p : hero.getParty().getMembers()) {
				if (!castLoc.getWorld().equals(p.getPlayer().getWorld())) {
					continue;
				}
				if (castLoc.distanceSquared(p.getPlayer().getLocation()) <= radiusSquared) {
					healDispel(p, removals, healing, hero);
				}
			}
		}
		else
			healDispel(hero, removals, healing, hero);

		broadcastExecuteText(hero);

		// this is our fireworks shit
		/*try {
			fplayer.playFirework(player.getWorld(), player.getLocation().add(0, 1.5, 0), FireworkEffect.builder().flicker(false)
					.trail(true).with(FireworkEffect.Type.BALL).withColor(Color.FUCHSIA).withFade(Color.WHITE).build());
		} catch (Exception e) {
			e.printStackTrace();
		}*/

		for (int i = 0; i < circle(player.getLocation(), 72, radius).size(); i++)
		{
			player.getWorld().spigot().playEffect(circle(player.getLocation(), 72, radius).get(i), org.bukkit.Effect.INSTANT_SPELL, 0, 0, 0, 0, 0, 0, 16, 16);
		}
		
		player.getWorld().playSound(player.getLocation(), Sound.ORB_PICKUP, 0.8F, 1.0F);

		return SkillResult.NORMAL;
	}

	private void healDispel(Hero targetHero, int removals, double healAmount, Hero hero) {
		HeroRegainHealthEvent hrhEvent = new HeroRegainHealthEvent(targetHero, healAmount, this, hero);
		Bukkit.getPluginManager().callEvent(hrhEvent);
		if (!hrhEvent.isCancelled()) {
			targetHero.heal(hrhEvent.getAmount());
		}
		if (removals == 0)
			return;

		if (targetHero.getPlayer().getFireTicks() > 0) {
			removals--;
			targetHero.getPlayer().setFireTicks(0);
			if (removals == 0)
				return;
		}

		for (Effect effect : targetHero.getEffects()) {
			if (effect.isType(EffectType.HARMFUL) && effect.isType(EffectType.DISPELLABLE)) {
				targetHero.removeEffect(effect);
				removals--;
				if (removals == 0) {
					break;
				}
			}
		}
	}
}
