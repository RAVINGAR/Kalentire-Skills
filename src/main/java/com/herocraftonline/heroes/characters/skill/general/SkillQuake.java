package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.BlindEffect;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.skills.SkillBaseBlockWave;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class SkillQuake extends SkillBaseBlockWave {

	public SkillQuake(Heroes plugin) {
		super(plugin, "Quake");
		setDescription("The quake erupts and throws blocks around hitting all targets in a $1 degree arc in front of you, with a radius of $2, height of $3, and depth of $4, " +
				"Expanding at a rate of $5 block(s) per second. Targets can be hit hit a maximum of $6 time(s) each hit dealing $7 damage, blinding them for $8 second(s).");
		setUsage("/skill " + getName().toLowerCase());
		setIdentifiers("skill " + getName().toLowerCase());
		setArgumentRange(0, 0);
	}

	@Override
	public String getDescription(Hero hero) {
		double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5d, false);
		double expansionRate = SkillConfigManager.getUseSetting(hero, this, EXPANSION_RATE_NODE, 1d, false);
		double waveArc = SkillConfigManager.getUseSetting(hero, this, WAVE_ARC_NODE, 360d, false);
		int hitLimit = SkillConfigManager.getUseSetting(hero, this, HIT_LIMIT_NODE, 1, false);
		int depth =  SkillConfigManager.getUseSetting(hero, this, DEPTH_NODE, 5, false);
		int height = SkillConfigManager.getUseSetting(hero, this, HEIGHT_NODE, 3, false);


		double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 100d, false);
		double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 1d, false);
		final double totalDamage = damage + hero.getAttributeValue(AttributeType.STRENGTH) * damageIncrease;

		final long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 4000, false);

		return getDescription()
				.replace("$1", Util.decFormat.format(waveArc))
				.replace("$2", Util.decFormat.format(radius))
				.replace("$3", "" + depth)
				.replace("$4", "" + height)
				.replace("$5", Util.largeDecFormat.format(expansionRate))
				.replace("$6", "" + hitLimit)
				.replace("$7", Util.decFormat.format(totalDamage))
				.replace("$8", Util.decFormat.format((double) duration / 1000));
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
		node.set(SkillSetting.DAMAGE_INCREASE_PER_WISDOM.node(), 1);

		node.set(SkillSetting.DURATION.node(), 4000);

		return node;
	}

	@Override
	public SkillResult use(final Hero hero, String[] strings) {

		double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 100d, false);
		double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_WISDOM, 1d, false);
		final double totalDamage = damage + hero.getAttributeValue(AttributeType.WISDOM) * damageIncrease;
		final long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 4000, false);

		castBlockWave(hero, hero.getPlayer().getLocation().getBlock(), (hero1, target, center) -> {
            if (damageCheck(hero1.getPlayer(), target)) {
                damageEntity(target, hero1.getPlayer(), totalDamage, EntityDamageEvent.DamageCause.MAGIC, false);

                CharacterTemplate targetCt = plugin.getCharacterManager().getCharacter(target);

                BlindEffect blind = new BlindEffect(SkillQuake.this, hero1.getPlayer(), duration);
                targetCt.addEffect(blind);
            }
        });

		final World world = hero.getPlayer().getWorld();
		new BukkitRunnable() {

			float volume = 1;

			@Override
			public void run() {
				world.playSound(hero.getPlayer().getLocation(), Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, volume, 1f);
				volume -= 0.1;

				if (volume <= 0) {
					cancel();
				}
			}
		}.runTaskTimer(plugin, 0, 1);

		broadcastExecuteText(hero);
		return SkillResult.NORMAL;
	}
}
