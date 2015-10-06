package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.SlowEffect;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;

public class SkillSeismicAssault extends SkillBaseBlockWave {

	public SkillSeismicAssault(Heroes plugin) {
		super(plugin, "SeismicAssault");
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

		node.set(SkillSetting.RADIUS.node(), 8);
		node.set(HEIGHT_NODE, 3);
		node.set(DEPTH_NODE, 5);
		node.set(EXPANSION_RATE_NODE, 1);
		node.set(WAVE_ARC_NODE, 60d);
		node.set(LAUNCH_FORCE_NODE, 0.2);
		node.set(HIT_LIMIT_NODE, 1);

		node.set(SkillSetting.DAMAGE.node(), 100d);
		node.set(SkillSetting.DAMAGE_INCREASE_PER_STRENGTH.node(), 1);

		node.set(SkillSetting.DURATION.node(), 4000);

		return node;
	}

	@Override
	public SkillResult use(Hero hero, String[] strings) {

		double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 100d, false);
		double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 1d, false);
		final double totalDamage = damage + hero.getAttributeValue(AttributeType.STRENGTH) * damageIncrease;
		final long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 4000, false);

		castBlockWave(hero, hero.getPlayer().getLocation().getBlock(), new WaveTargetAction() {
			@Override
			public void onTarget(Hero hero, LivingEntity target, Location center) {
				if (damageCheck(hero.getPlayer(), target)) {
					damageEntity(target, hero.getPlayer(), totalDamage, EntityDamageEvent.DamageCause.MAGIC, false);

					CharacterTemplate targetCt = plugin.getCharacterManager().getCharacter(target);

					SlowEffect slow = new SlowEffect(SkillSeismicAssault.this, hero.getPlayer(), duration, 2);
					targetCt.addEffect(slow);
				}
			}
		});

		World world = hero.getPlayer().getWorld();
		world.playSound(hero.getPlayer().getLocation(), Sound.EXPLODE, 1f, 0.1f);
		world.playSound(hero.getPlayer().getLocation(), Sound.FIRE_IGNITE, 1f, 1f);
		world.playSound(hero.getPlayer().getLocation(), Sound.FALL_BIG, 1f, 1f);

		broadcastExecuteText(hero);
		return SkillResult.NORMAL;
	}
}
