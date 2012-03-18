package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.util.Vector;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;

public class SkillFirewave extends ActiveSkill {

	public SkillFirewave(Heroes plugin) {
		super(plugin, "Firewave");
		setDescription("You throw a wave of fire in all directions!");
		setUsage("/skill firewave");
		setArgumentRange(0, 0);
		setTypes(SkillType.FIRE, SkillType.SILENCABLE, SkillType.DAMAGING, SkillType.HARMFUL);
		setIdentifiers("skill firewave");
	}

	@Override
	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection node = super.getDefaultConfig();
		node.set("fireballs", 8);
		node.set("fireballs-per-level", .2);
		return node;
	}

	@Override
	public SkillResult use(Hero hero, String[] args) {
		Player player = hero.getPlayer();
		int numFireballs = SkillConfigManager.getUseSetting(hero, this, "fireballs", 8, false);
		numFireballs += (SkillConfigManager.getUseSetting(hero, this, "fireballs-per-level", .2, false) * hero.getSkillLevel(this));

		double diff = 2 * Math.PI / numFireballs;
		for (double a = 0; a < 2 * Math.PI; a += diff) {
			Vector vel = new Vector(Math.cos(a), 0, Math.sin(a));
			Snowball snowball = player.launchProjectile(Snowball.class);
			snowball.setVelocity(vel);
			snowball.setFireTicks(100);
		}
		broadcastExecuteText(hero);
		return SkillResult.NORMAL;
	}

	@Override
	public String getDescription(Hero hero) {
		return getDescription();
	}
}
