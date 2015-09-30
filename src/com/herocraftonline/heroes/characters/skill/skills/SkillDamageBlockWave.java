package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.heroes.nms.physics.collision.AABB;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static com.herocraftonline.heroes.characters.skill.SkillType.*;
import static com.herocraftonline.heroes.characters.skill.SkillType.SILENCEABLE;
import static com.herocraftonline.heroes.characters.skill.SkillType.UNINTERRUPTIBLE;

public class SkillDamageBlockWave extends SkillBaseBlockWave {

	public SkillDamageBlockWave(Heroes plugin) {
		super(plugin, "DamageBlockWave");setDescription("Damage stuff in a beam");
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
		node.set(EXPANSION_RATE, 1);
		node.set("knockback", 0.75);

		return node;
	}

	@Override
	public SkillResult use(Hero hero, String[] strings) {
		if (castBlockWave(hero, hero.getPlayer().getLocation().getBlock().getRelative(BlockFace.DOWN), new WaveTargetAction() {

			Set<UUID> tracked = new HashSet<>();

			@Override
			public void onTarget(Hero hero, LivingEntity target, Location center) {
				if (damageCheck(hero.getPlayer(), target)) {
					if (!tracked.contains(target.getUniqueId())) {
						damageEntity(target, hero.getPlayer(), 10d, EntityDamageEvent.DamageCause.MAGIC, false);

						double knockback = SkillConfigManager.getUseSetting(hero, SkillDamageBlockWave.this, "knockback", 0.5, false);

						AABB targetAABB = NMSHandler.getInterface().getNMSPhysics().getEntityAABB(target);
						target.setVelocity(target.getVelocity().add(targetAABB.getCenter().subtract(center.toVector()).normalize().multiply(knockback)));
					}

					tracked.add(target.getUniqueId());
				}
			}
		})) {
			broadcastExecuteText(hero);
			return SkillResult.NORMAL;
		} else {
			return SkillResult.INVALID_TARGET_NO_MSG;
		}
	}
}
