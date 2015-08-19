package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class SkillDisarray extends ActiveSkill {

	private static final double DEFAULT_MAX_DISTANCE = 15d;
	
	public SkillDisarray(Heroes plugin) {
		super(plugin, "Disarray");
		setDescription("Surfing with chaos, you fire off a beam that deals $1 damage to everything in its path.");
		setUsage("/skill disarray");
		setIdentifiers("skill disarray");
	}

	@Override
	public String getDescription(Hero hero) {
		return null;
	}

	@Override
	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection node = super.getDefaultConfig();

		node.set(SkillSetting.MAX_DISTANCE.node(), DEFAULT_MAX_DISTANCE);
		node.set(SkillSetting.DAMAGE.node(), 150d);
		node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 5d);
		node.set(SkillSetting.COOLDOWN.node(), 10d);

		return node;
	}

	@Override
	public SkillResult use(Hero hero, String[] strings) {
		Player player = hero.getPlayer();

		double beamLength = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, DEFAULT_MAX_DISTANCE, false);

		return SkillResult.NORMAL;
	}
}
