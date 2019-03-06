package com.herocraftonline.heroes.characters.skill.skills;

//src=http://pastie.org/private/oeherulcmebfy0lerywsw
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.SafeFallEffect;
import com.herocraftonline.heroes.characters.skill.*;
import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.hooks.NCPExemptionManager;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Random;

import static com.herocraftonline.heroes.util.GeometryUtil.circle;

public class SkillThickenWinds extends ActiveSkill 
{
	private boolean ncpEnabled = false;

	public SkillThickenWinds(Heroes plugin) 
	{
		super(plugin, "ThickenWinds");
		setDescription("You thicken the winds, creating an updraft that allows your party (within 12 blocks) to float safely to the ground for $1 second(s).");
		setUsage("/skill thickenwinds");
		setArgumentRange(0, 0);
		setIdentifiers("skill thickenwinds");
		setTypes(SkillType.BUFFING, SkillType.SILENCEABLE);

		if (Bukkit.getServer().getPluginManager().getPlugin("NoCheatPlus") != null) 
		{
			ncpEnabled = true;
		}
	}

	@Override
	public String getDescription(Hero hero) {
		int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 10000, false);

		String formattedDuration = String.valueOf(duration / 1000.0);

		return getDescription().replace("$1", formattedDuration);
	}

	@Override
	public ConfigurationSection getDefaultConfig() 
	{
		ConfigurationSection node = super.getDefaultConfig();

		node.set(SkillSetting.RADIUS.node(), Integer.valueOf(12));
		node.set(SkillSetting.DURATION.node(), Integer.valueOf(10000));
		node.set(SkillSetting.APPLY_TEXT.node(), " %hero% thickens the winds!");
		node.set(SkillSetting.EXPIRE_TEXT.node(), " The winds have returned to normal.");

		return node;
	}

	@Override
	public SkillResult use(Hero hero, String[] args) 
	{
		final Player player = hero.getPlayer();

		broadcastExecuteText(hero);

		int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 10000, false);
		int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS.node(), 15, false);
		String applyText = SkillConfigManager.getUseSetting(hero, this, SkillSetting.APPLY_TEXT, " %hero% thickens the winds!").replace("%hero%", hero.getName());
		String expireText = SkillConfigManager.getUseSetting(hero, this, SkillSetting.EXPIRE_TEXT, " The winds have returned to normal.");

		double radiusSquared = radius * radius;

		if (hero.hasParty()) 
		{
			for (Hero member : hero.getParty().getMembers()) 
			{
				Player memberPlayer = member.getPlayer();
				if (memberPlayer.getWorld() != player.getWorld()) continue;

				if (memberPlayer.getLocation().distanceSquared(player.getLocation()) <= radiusSquared) 
				{
					member.addEffect(new ThickenWindsEffect(this, player, duration, applyText, expireText));
				}
			}
		}
		else hero.addEffect(new ThickenWindsEffect(this, player, duration, applyText, expireText));

		Location center = player.getLocation().add(0, 0.5, 0);
		final ArrayList<Location> windLocs = new ArrayList<Location>();

		for (int i = 1; i <= radius / 4; i++)
		{
			ArrayList<Location> concentric = circle(center, 12, (double) i);
			windLocs.addAll(concentric);
		}

		new BukkitRunnable()
		{
			private int ticks = 0;
			private int maxTicks = 5;
			private Random rand = new Random();

			public void run()
			{
				if (ticks < maxTicks)
				{
					int index = rand.nextInt(windLocs.size() + 1);
					Location loc = windLocs.get(index).clone().setDirection(new Vector(0, 1, 0));
					//loc.getWorld().spigot().playEffect(loc, Effect.CLOUD, 0, 0, 0.2F, 5.5F, 0.2F, 0.0F, 250, 25);
					loc.getWorld().spawnParticle(Particle.CLOUD, loc, 250, 0.2, 5.5, 0.2, 0, true);
					loc.getWorld().playSound(loc, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.3F, (float) (1 / (ticks + 1)));
					ticks++;
				}
				else
				{
					cancel();
				}
			}
		}.runTaskTimer(plugin, 0, 10);

		return SkillResult.NORMAL;
	}

	private class ThickenWindsEffect extends SafeFallEffect 
	{
		private String applyText;
		private String expireText;

		public ThickenWindsEffect(Skill skill, Player applier, long duration, String at, String et) 
		{
			super(skill, applier, duration);
			applyText = at;
			expireText = et;
		}

		@Override
		public void applyToHero(Hero hero) 
		{
			super.applyToHero(hero);
			final Hero h = hero;
			final Player player = hero.getPlayer();
			player.sendMessage(applyText);
			final String name = this.getName();
			new BukkitRunnable() {
				public void run() {
					if (h.hasEffect(name)) cancel();
					if (player.getVelocity().getY() < -0.1f) player.setVelocity(new Vector(player.getVelocity().getX(),
							-0.1f, player.getVelocity().getZ()));
				}
			}.runTaskTimer(plugin, 0, 1);
			if (ncpEnabled) NCPExemptionManager.exemptPermanently(player, CheckType.MOVING_NOFALL);
		}

		@Override
		public void removeFromHero(Hero hero) 
		{
			super.removeFromHero(hero);
			final Player player = hero.getPlayer();
			player.sendMessage(expireText);
			if (ncpEnabled) NCPExemptionManager.unexempt(player, CheckType.MOVING_NOFALL);
		}
	}
}
