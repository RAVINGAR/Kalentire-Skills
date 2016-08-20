package com.herocraftonline.heroes.characters.skill.unusedskills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.base.SkillBaseBlockWave;
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.heroes.nms.physics.collision.AABB;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;

import static com.herocraftonline.heroes.characters.skill.SkillType.*;
import static com.herocraftonline.heroes.characters.skill.SkillType.SILENCEABLE;
import static com.herocraftonline.heroes.characters.skill.SkillType.UNINTERRUPTIBLE;

public class SkillDamageBlockWave extends SkillBaseBlockWave {

	public SkillDamageBlockWave(Heroes plugin) {
		super(plugin, "DamageBlockWave");
		setDescription("Damage stuff in a beam");
		setUsage("/skill damageblockwave");
		setIdentifiers("skill damageblockwave");
		setTypes(DAMAGING, MULTI_GRESSIVE, AREA_OF_EFFECT, NO_SELF_TARGETTING, UNINTERRUPTIBLE, SILENCEABLE);
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
		node.set("knockback", 0.75);

		return node;
	}

	@Override
	public SkillResult use(Hero hero, String[] strings) {
		castBlockWave(hero, hero.getPlayer().getLocation().getBlock(), new WaveTargetAction() {

			@Override
			public void onTarget(Hero hero, LivingEntity target, Location center) {
				if (damageCheck(hero.getPlayer(), target)) {
					damageEntity(target, hero.getPlayer(), 10d, EntityDamageEvent.DamageCause.MAGIC, false);

					double knockback = SkillConfigManager.getUseSetting(hero, SkillDamageBlockWave.this, "knockback", 0.5, false);

					AABB targetAABB = NMSHandler.getInterface().getNMSPhysics().getEntityAABB(target);
					target.setVelocity(
							target.getVelocity()
									.add(targetAABB.getCenter()
											.subtract(center.toVector())
											.normalize().multiply(knockback)
											.add(new Vector(0, 0.25, 0))));
				}
			}
		});

		broadcastExecuteText(hero);
		return SkillResult.NORMAL;
	}
}
