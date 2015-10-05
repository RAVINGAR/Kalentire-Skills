package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;

public class SkillDivineRuination extends SkillBaseBlockWave {

	public SkillDivineRuination(Heroes plugin) {
		super(plugin, "DivineRuination");
		setDescription("");
		setUsage("/skill " + getName().toLowerCase());
		setIdentifiers("skill " + getName().toLowerCase());
		setArgumentRange(0, 0);
	}

	@Override
	public String getDescription(Hero hero) {
		return getDescription();
	}

	@Override
	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection node = super.getDefaultConfig();

		node.set(SkillSetting.RADIUS.node(), 5);
		node.set(HEIGHT_NODE, 3);
		node.set(DEPTH_NODE, 5);
		node.set(EXPANSION_RATE_NODE, 1);
		node.set(LAUNCH_FORCE_NODE, 0.2);
		node.set(HIT_LIMIT_NODE, 1);

		node.set(SkillSetting.DAMAGE.node(), 100d);
		node.set(SkillSetting.DAMAGE_INCREASE_PER_STRENGTH.node(), 1);

		return node;
	}

	@Override
	public SkillResult use(Hero hero, String[] strings) {

		double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 100d, false);
		double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 1d, false);
		final double totalDamge = damage + hero.getAttributeValue(AttributeType.STRENGTH) * damageIncrease;

		castBlockWave(hero, hero.getPlayer().getLocation().getBlock(), new WaveTargetAction() {
			@Override
			public void onTarget(Hero hero, LivingEntity target, Location center) {
				if (damageCheck(hero.getPlayer(), target)) {
					damageEntity(target, hero.getPlayer(), totalDamge, EntityDamageEvent.DamageCause.MAGIC, false);
				}
			}
		});
		broadcastExecuteText(hero);
		return SkillResult.NORMAL;
	}
}
