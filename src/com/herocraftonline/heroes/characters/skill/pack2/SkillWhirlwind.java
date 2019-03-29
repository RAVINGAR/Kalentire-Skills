package com.herocraftonline.heroes.characters.skill.pack2;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;

public class SkillWhirlwind extends ActiveSkill {
	private String applyText;
	private String expireText;

	public SkillWhirlwind(Heroes plugin) {
		super(plugin, "Whirlwind");
		setDescription("Unleash a furious Whirlwind for $1 second(s). While active, you strike all enemies within $2 blocks every $3 second(s) for $4 physical damage. You are slowed during the effect.");
		setUsage("/skill whirlwind");
		setArgumentRange(0, 0);
		setIdentifiers("skill whirlwind");
		setTypes(SkillType.DAMAGING, SkillType.AREA_OF_EFFECT, SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.AGGRESSIVE);
	}

	@Override
	public String getDescription(Hero hero) {

		int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 3000, false);
		int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 1500, false);

		double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 60, false);
		double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 1.0, false);
		damage += damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH);

		int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5, false);

		String formattedDuration = Util.decFormat.format(duration / 1000.0);
		String formattedPeriod = Util.decFormat.format(period / 1000.0);
		String formattedDamage = Util.decFormat.format(damage);

		return getDescription().replace("$1", formattedDuration).replace("$2", radius + "").replace("$3", formattedPeriod).replace("$4", formattedDamage);
	}

	@Override
	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection node = super.getDefaultConfig();

		node.set(SkillSetting.RADIUS.node(), 5);
		node.set(SkillSetting.DAMAGE.node(), 60);
		node.set(SkillSetting.DAMAGE_INCREASE_PER_STRENGTH.node(), 1.0);
		node.set(SkillSetting.PERIOD.node(), 500);
		node.set(SkillSetting.DURATION.node(), 5000);
		node.set("slow-amplifier", 1);
		node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% is unleashing a powerful whirlwind!");
		node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% is no longer whirlwinding!");

		return node;
	}

	public void init() {
		super.init();

		applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%hero% is unleashing a powerful whirlwind!").replace("%hero%", "$1");
		expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%hero% is no longer whirlwinding!").replace("%hero%", "$1");
	}

	public ArrayList<Location> circle(Location centerPoint, int particleAmount, double circleRadius)
	{
		World world = centerPoint.getWorld();

		double increment = (2 * Math.PI) / particleAmount;

		ArrayList<Location> locations = new ArrayList<Location>();

		for (int i = 0; i < particleAmount; i++)
		{
			double angle = i * increment;
			double x = centerPoint.getX() + (circleRadius * Math.cos(angle));
			double z = centerPoint.getZ() + (circleRadius * Math.sin(angle));
			locations.add(new Location(world, x, centerPoint.getY(), z));
		}
		return locations;
	}

	@Override
	public SkillResult use(Hero hero, String[] args) {
		Player player = hero.getPlayer();

		broadcastExecuteText(hero);

		int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 3000, false);
		int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 1500, false);
		int slowAmplifier = SkillConfigManager.getUseSetting(hero, this, "slow-amplifier", 1, false);
		int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5, false);

		WhirlwindEffect effect = new WhirlwindEffect(this, player, period, duration, slowAmplifier, radius);

		hero.addEffect(effect);

		return SkillResult.NORMAL;
	}

	public class WhirlwindEffect extends PeriodicExpirableEffect {
		private int radius;

		public WhirlwindEffect(Skill skill, Player applier, long period, long duration, int slowAmplifier, int radius) {
			super(skill, "Whirlwind", applier, period, duration, applyText, expireText);

			types.add(EffectType.PHYSICAL);
			types.add(EffectType.BENEFICIAL);
			types.add(EffectType.SLOW);

			this.setRadius(radius);

			int tickDuration = (int) ((duration / 1000) * 20);
			addPotionEffect(new PotionEffect(PotionEffectType.SLOW, tickDuration, slowAmplifier), false);
			//addMobEffect(8, tickDuration, 254, false);
		}

		public int getRadius() {
			return radius;
		}

		public void setRadius(int radius) {
			this.radius = radius;
		}

		@Override
		public void tickHero(Hero hero) {
			Player player = hero.getPlayer();

			for (Effect effect : hero.getEffects()) {
				if (effect.isType(EffectType.STUN) || effect.isType(EffectType.DISABLE)) {
					hero.removeEffect(this);
					return;
				}
			}

			player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BAT_LOOP, 0.6F, 0.6F);

			// TORNADOOOO
			for (int h = 0; h < 2; h++)
			{
				ArrayList<Location> locations = circle(player.getLocation(), 36, (double) h + 1.2);
				for (int i = 0; i < locations.size(); i++)
				{
					//player.getWorld().spigot().playEffect(locations.get(i).add(0, (double) h + 0.2, 0), org.bukkit.Effect.CLOUD, 0, 0, 0, 0, 0, 0, 8, 16);
					player.getWorld().spawnParticle(Particle.CLOUD, locations.get(i).add(0, (double) h + 0.2, 0), 8, 0, 0, 0, 0);
				}
			}

			double damage = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE, 60, false);
			double damageIncrease = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 1.0, false);
			damage += damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH);

			boolean hitTarget = false;
			List<Entity> entities = hero.getPlayer().getNearbyEntities(radius, radius, radius);
			for (Entity entity : entities) {
				if (!(entity instanceof LivingEntity)) {
					continue;
				}

				LivingEntity target = (LivingEntity) entity;
				if (!damageCheck(player, target))
					continue;

				addSpellTarget(target, hero);
				damageEntity(target, player, damage, DamageCause.ENTITY_ATTACK, false);

				hitTarget = true;
			}

			if (hitTarget)
				player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.3F, 1.6F);
		}

		@Override
		public void tickMonster(Monster monster) {}
	}
}
