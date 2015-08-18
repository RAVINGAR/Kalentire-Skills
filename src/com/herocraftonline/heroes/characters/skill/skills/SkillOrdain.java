package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import org.bukkit.configuration.ConfigurationSection;

public class SkillOrdain extends ActiveSkill {

	public SkillOrdain(Heroes plugin) {
		super(plugin, "Ordain");
		setDescription("You summon the calling of order to project a beam that heals $1 health in its path.");
		setUsage("/skill ordain");
		setIdentifiers("skill ordain");
	}

	@Override
	public String getDescription(Hero hero) {
		return null;
	}

	@Override
	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection node = super.getDefaultConfig();

		node.set(SkillSetting.MAX_DISTANCE.node(), 15);
		node.set(SkillSetting.HEALING.node(), 200);
		node.set(SkillSetting.HEALING_INCREASE_PER_WISDOM.node(), 5);
		node.set(SkillSetting.COOLDOWN.node(), 10);

		return node;
	}

	@Override
	public SkillResult use(Hero hero, String[] strings) {
		return null;
	}
}
