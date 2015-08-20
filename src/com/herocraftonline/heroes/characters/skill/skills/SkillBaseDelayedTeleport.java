package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.SkillUseEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public abstract class SkillBaseDelayedTeleport extends TargettedSkill {

	private static final String DELAYED_TELEPORT_EFFECT_NAME = "DelayedTeleport";

	public SkillBaseDelayedTeleport(Heroes plugin, String name) {
		super(plugin, name);
	}

	public final class DelayedTeleportEffect extends ExpirableEffect {

		private final LivingEntity target;
		private final Location location;

		public DelayedTeleportEffect(LivingEntity target, Location location, Player applier, long duration) {
			super(SkillBaseDelayedTeleport.this, DELAYED_TELEPORT_EFFECT_NAME, applier, duration);
			this.target = target;
			this.location = location;
		}

		public LivingEntity getTarget() {
			return target;
		}

		public Location getLocation() {
			return location.clone();
		}
	}
}
