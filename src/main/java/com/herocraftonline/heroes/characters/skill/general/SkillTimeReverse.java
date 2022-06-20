package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.skills.SkillBaseMarkedTeleport;
import com.herocraftonline.heroes.util.Util;
import de.slikey.effectlib.util.ParticleEffect;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;

public class SkillTimeReverse extends SkillBaseMarkedTeleport {

	private static final String HEALING_PERCENTAGE_NODE = "healing-percentage";
	private static final String HEALING_PERCENTAGE_PER_WISDOM_NODE = "healing-percentage-per-wisdom";

	public SkillTimeReverse(Heroes plugin) {
		super(plugin, "TimeReverse", true, new EffectType[]{
				EffectType.HEALING,
				EffectType.BENEFICIAL,
		}, Particle.REDSTONE, new Color[]{
				Color.BLUE,
				Color.SILVER
		});
		setDescription("Mark your current position in time for the next $1 second(s). At any point during that time you may re activate the skill to teleport" +
				" your self back to that location healing you for an amount starting at $3 ($2% of max health) and decaying towards 0 as the skills duration reaches end. If you do not" +
				" re activate the skill within the duration no healing is applied and no teleport occurs.");
		setUsage("/skill timereverse");
		setIdentifiers("skill timereverse");

		setTypes(SkillType.HEALING, SkillType.TELEPORTING);
	}

	@Override
	public String getDescription(Hero hero) {
		double healing = SkillConfigManager.getUseSetting(hero, this, HEALING_PERCENTAGE_NODE, 0.25d, false);
		double healingIncrease = SkillConfigManager.getUseSetting(hero, this, HEALING_PERCENTAGE_PER_WISDOM_NODE, 0.005d, false);
		healing += hero.getAttributeValue(AttributeType.WISDOM) * healingIncrease;

		double totalDuration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);

		return getDescription()
				.replace("$1", Util.decFormat.format(totalDuration / 1000))
				.replace("$2", Util.largeDecFormat.format(healing * 100))
				.replace("$3", Util.decFormat.format(hero.getPlayer().getMaxHealth() * healing));
	}

	@Override
	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection node = super.getDefaultConfig();

		node.set(SkillSetting.DURATION.node(), 10000);
		node.set(HEALING_PERCENTAGE_NODE, 0.25d);
		node.set(HEALING_PERCENTAGE_PER_WISDOM_NODE, 0.005d);

		node.set(PRESERVE_LOOK_DIRECTION_NODE, true);
		node.set(PRESERVE_VELOCITY_NODE, true);

		return node;
	}

	@Override
	protected void onMarkerActivate(SkillBaseMarkedTeleport.Marker marker, long activateTime) {
		double healing = SkillConfigManager.getUseSetting(marker.getHero(), this, HEALING_PERCENTAGE_NODE, 0.25d, false);
		double healingIncrease = SkillConfigManager.getUseSetting(marker.getHero(), this, HEALING_PERCENTAGE_PER_WISDOM_NODE, 0.005d, false);
		healing += marker.getHero().getAttributeValue(AttributeType.WISDOM) * healingIncrease;

		double maxHeal = marker.getTarget().getEntity().getMaxHealth() * healing;
		long reCastDelay = SkillConfigManager.getUseSetting(marker.getHero(), this, RE_CAST_DELAY_NODE, 0, false);

		double totalDuration = SkillConfigManager.getUseSetting(marker.getHero(), this, SkillSetting.DURATION, 10000, false);
		double healScale = 1 - ((activateTime - marker.getCreateTime() + reCastDelay) / (totalDuration - reCastDelay));
		if (healScale < 0) {
			healScale = 0;
		}

		((Hero) marker.getTarget()).heal(maxHeal * healScale);
	}
}
