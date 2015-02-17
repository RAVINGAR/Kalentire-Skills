package com.herocraftonline.heroes.characters.skill.skills;

import java.util.ArrayList;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.common.InvulnerabilityEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

public class SkillInvuln extends ActiveSkill {

	private String applyText;
	private String expireText;

	public SkillInvuln(Heroes plugin) {
		super(plugin, "Invuln");
		setDescription("You become immune to all attacks, and may not attack for $1 seconds.");
		setUsage("/skill invuln");
		setArgumentRange(0, 0);
		setIdentifiers("skill invuln");
		setTypes(SkillType.BUFFING, SkillType.SILENCEABLE);
	}

	@Override
	public String getDescription(Hero hero) {
		int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 6000, false);
		String formattedDuration = Util.decFormat.format(duration / 1000.0);

		return getDescription().replace("$1", formattedDuration);
	}

	@Override
	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection node = super.getDefaultConfig();

		node.set(SkillSetting.DURATION.node(), 6000);
		node.set(SkillSetting.APPLY_TEXT.node(), Messaging.getSkillDenoter() + "%hero% has become invulnerable!");
		node.set(SkillSetting.EXPIRE_TEXT.node(), Messaging.getSkillDenoter() + "%hero% is once again vulnerable!");
		node.set(SkillSetting.REAGENT.node(), 81);
		node.set(SkillSetting.REAGENT_COST.node(), 1);

		return node;
	}

	@Override
	public void init() {
		super.init();

		applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, Messaging.getSkillDenoter() + "%hero% has become invulnerable!").replace("%hero%", "$1");
		expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, Messaging.getSkillDenoter() + "%hero% is once again vulnerable!").replace("%hero%", "$1");
	}

	public ArrayList<Location> rectangle(Location center, double width, double effectSpacing)
	{
		ArrayList<Location> locations = new ArrayList<Location>();
		double minX = center.getX() - width;
		double maxX = center.getX() + width;
		double minZ = center.getZ() - width;
		double maxZ = center.getZ() + width;
		for (double x = minX; x < maxX; x += effectSpacing)
		{
			for (double z = minX; z < maxZ; z += effectSpacing)
			{
				if (z == minZ || z == maxZ || x == minX || x == maxX)
				{
					locations.add(new Location(center.getWorld(), x, center.getY(), z));
				}
			}
		}
		return locations;
	}

	@Override
	public SkillResult use(Hero hero, String[] args) {
		Player player = hero.getPlayer();
		final Player p = player;
		
        new BukkitRunnable() {
            
            private Location location = p.getLocation();

            private double height = 8;

            @SuppressWarnings("deprecation")
            @Override
            public void run() 
            {
            	ArrayList<Location> particleLocations = rectangle(location.add(0, height, 0), 2, 1);
            	for (Location l : particleLocations)
            	{
            		l.getWorld().spigot().playEffect(l, org.bukkit.Effect.TILE_BREAK, Material.QUARTZ_BLOCK.getId(), 0, 0.3F, 0.2F, 0.3F, 0.0F, 10, 16);
            	}
            	height -= 1;
            	if (height == 0)
            		cancel();
            }
        }.runTaskTimer(plugin, 1, 3);
		broadcastExecuteText(hero);

		// Remove any harmful effects on the caster
		for (Effect effect : hero.getEffects()) {
			if (effect.isType(EffectType.HARMFUL)) {
				hero.removeEffect(effect);
			}
		}

		int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 6000, false);
		hero.addEffect(new InvulnerabilityEffect(this, player, duration, applyText, expireText));

		player.getWorld().playSound(player.getLocation(), Sound.LEVEL_UP, 0.8F, 1.0F);

		return SkillResult.NORMAL;
	}
}
