package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.ConeEffect;
import de.slikey.effectlib.util.ParticleEffect;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

public abstract class SkillBaseSpike extends TargettedSkill {

	public SkillBaseSpike(Heroes plugin, String name) {
		super(plugin, name);
	}



	protected void renderSpike(Location target, double height, double radius, ParticleEffect particle, Color color) {

		EffectManager em = new EffectManager(plugin);
		ConeEffect effect = new ConeEffect(em);
	}

	protected void renderSpike(LivingEntity target, ParticleEffect particle) {

	}

	private void renderSpike(LivingEntity target, ConeEffect effect) {

	}
}
