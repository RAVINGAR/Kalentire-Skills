package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import org.bukkit.configuration.ConfigurationSection;

public class SkillDisarray extends ActiveSkill {
	
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

		node.set(SkillSetting.MAX_DISTANCE.node(), 15);
		node.set(SkillSetting.DAMAGE.node(), 150);
		node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 5);
		node.set(SkillSetting.COOLDOWN.node(), 10);

		return node;
	}

	@Override
	public SkillResult use(Hero hero, String[] strings) {
		return null;
	}
}
