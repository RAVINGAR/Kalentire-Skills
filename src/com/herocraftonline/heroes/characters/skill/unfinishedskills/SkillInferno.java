package com.herocraftonline.heroes.characters.skill.unfinishedskills;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.CombustEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.VisualEffect;

public class SkillInferno extends ActiveSkill {

	public VisualEffect fplayer = new VisualEffect();

	public SkillInferno(Heroes plugin) {
		super(plugin, "Inferno");
		setDescription("Unleash an inferno upon your enemies, dealing $1 fire damage and igniting them for $2 seconds.");
		setUsage("/skill inferno");
		setArgumentRange(0, 0);
		setIdentifiers("skill inferno");
		setTypes(SkillType.FIRE, SkillType.DAMAGING, SkillType.HARMFUL);
	}

	public String getDescription(Hero hero) {
		int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 50, false);
		damage += (int) (SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE, 0.0D, false) * hero.getSkillLevel(this));

		int duration = (SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 6000, false) * 50) / 1000;

		return getDescription().replace("$1", damage + "").replace("$2", duration + "");
	}

	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection node = super.getDefaultConfig();
		node.set(SkillSetting.DURATION.node(), Integer.valueOf(6000));
		node.set(SkillSetting.RADIUS.node(), Integer.valueOf(8));
		node.set(SkillSetting.DAMAGE.node(), Integer.valueOf(50));
		node.set(SkillSetting.DAMAGE_INCREASE.node(), Double.valueOf(0.0D));

		return node;
	}

	public SkillResult use(Hero hero, String[] args) {
		Player player = hero.getPlayer();

		int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 8, false);
		double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 50, false);
		int fireTicks = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 6000, false);

		broadcastExecuteText(hero);

		for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {

			// Check to see if the entity can be damaged
			if (!(entity instanceof LivingEntity) || !damageCheck(player, (LivingEntity) entity))
				continue;

			LivingEntity target = (LivingEntity) entity;

			addSpellTarget(target, hero);
			damageEntity(target, player, damage, EntityDamageEvent.DamageCause.FIRE);

			target.setFireTicks(fireTicks);
			plugin.getCharacterManager().getCharacter(target).addEffect(new CombustEffect(this, player));

			try {
				this.fplayer.playFirework(player.getWorld(), target.getLocation(), FireworkEffect.builder().flicker(false).trail(true).with(FireworkEffect.Type.BURST).withColor(Color.ORANGE).withFade(Color.RED).build());
			}
			catch (IllegalArgumentException e) {
				e.printStackTrace();
			}
			catch (Exception e) {
				e.printStackTrace();
			}

		}

		return SkillResult.NORMAL;
	}
}