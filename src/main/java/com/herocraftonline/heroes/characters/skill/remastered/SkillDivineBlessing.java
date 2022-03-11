package com.herocraftonline.heroes.characters.skill.remastered;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainHealthEvent;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.GeometryUtil;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

public class SkillDivineBlessing extends ActiveSkill {

	public SkillDivineBlessing(Heroes plugin) {
		super(plugin, "DivineBlessing");
		setDescription("You bestow upon your group a Divine Blessing, restoring $1 health to all nearby party members.");
		setUsage("/skill divineblessing");
		setArgumentRange(0, 0);
		setIdentifiers("skill divineblessing");
		setTypes(SkillType.HEALING, SkillType.AREA_OF_EFFECT, SkillType.ABILITY_PROPERTY_LIGHT, SkillType.SILENCEABLE);
	}

	@Override
	public String getDescription(Hero hero) {
		int healing = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING.node(), 125, false);
		healing = (int) getScaledHealing(hero, healing);
		double healingIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING_INCREASE_PER_WISDOM.node(), 2.0, false);
		healing += (int)(hero.getAttributeValue(AttributeType.WISDOM) * healingIncrease);

		return getDescription().replace("$1", healing + "");
	}

	@Override
	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection node = super.getDefaultConfig();

		node.set(SkillSetting.HEALING.node(), 120);
		node.set(SkillSetting.HEALING_INCREASE_PER_WISDOM.node(), 1.75);
		node.set(SkillSetting.RADIUS.node(), 8);

		return node;
	}

	public void onWarmup(Hero hero)
	{
		final Player player = hero.getPlayer();
		new BukkitRunnable() {
			private double time = 0;

			@Override
			public void run() 
			{
				final Location playerLoc = player.getLocation();
				if (time < 1.0) 
				{
					player.getLocation(playerLoc).add(0.7 * Math.sin(time * 16), time * 2.2, 0.7 * Math.cos(time * 16));
                    //player.getWorld().spigot().playEffect(playerLoc, Effect.INSTANT_SPELL, 0, 0, 0, 0, 0, 0.1f, 1, 16);
					player.getWorld().spawnParticle(Particle.SPELL_INSTANT, playerLoc, 1, 0, 0, 0, 0.1);
				} 
				else 
				{
					playerLoc.add(0, 2.3, 0);					
					for (double r = 1; r < 5 * 2; r++)
					{
						List<Location> particleLocations = GeometryUtil.circle(playerLoc, 36, r / 2);
						for (int i = 0; i < particleLocations.size(); i++)
						{
							//playerLoc.getWorld().spigot().playEffect(particleLocations.get(i), Effect.FIREWORKS_SPARK, 0, 0, 0, 0.1F, 0, 0.1F, 1, 16);
							playerLoc.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, particleLocations.get(i), 1, 0, 0.1, 0, 0.1);
						}
					}
					cancel();
				}
				time += 0.02;
			}
		}.runTaskTimer(plugin, 1, 1);
	}

	@Override
	public SkillResult use(Hero hero, String[] args) {
		Player player = hero.getPlayer();
		double healing = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING, 125, false);
		healing = getScaledHealing(hero, healing);
		double healingIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING_INCREASE_PER_WISDOM, 1.75, false);
		healing += (hero.getAttributeValue(AttributeType.WISDOM) * healingIncrease);

		if (hero.getParty() == null) {
			// Heal just the caster if he's not in a party
			HeroRegainHealthEvent hrhEvent = new HeroRegainHealthEvent(hero, healing, this, hero);
			plugin.getServer().getPluginManager().callEvent(hrhEvent);
			if (hrhEvent.isCancelled()) {
				player.sendMessage("Unable to heal the target at this time!");
				return SkillResult.CANCELLED;
			}

			hero.heal(hrhEvent.getDelta());
			//changed to hero.heal for bukkit events
		}
		else {
			int radiusSquared = (int) Math.pow(SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5, false), 2);
			Location heroLoc = player.getLocation();
			// Heal party members near the caster
			for (Hero partyHero : hero.getParty().getMembers()) {
				if (!player.getWorld().equals(partyHero.getPlayer().getWorld())) {
					continue;
				}
				if (partyHero.getPlayer().getLocation().distanceSquared(heroLoc) <= radiusSquared) {
					HeroRegainHealthEvent hrhEvent = new HeroRegainHealthEvent(partyHero, healing, this, hero);
					plugin.getServer().getPluginManager().callEvent(hrhEvent);
					if (hrhEvent.isCancelled()) {
						player.sendMessage("Unable to heal the target at this time!");
						return SkillResult.CANCELLED;
					}

					//old - partyHero.getPlayer().setHealth(partyHero.getPlayer().getHealth() + hrhEvent.getAmount());
					partyHero.heal(hrhEvent.getDelta());

				}
			}
		}

		broadcastExecuteText(hero);

		return SkillResult.NORMAL;
	}
}
