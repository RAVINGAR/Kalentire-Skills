package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.util.Util;
import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.hooks.NCPExemptionManager;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class SkillFlood extends ActiveSkill {
	private boolean ncpEnabled = false;
	
	public SkillFlood(Heroes plugin) {
		super(plugin, "Flood");
		setDescription("You let loose a flood up to $1 blocks in front of you, striking all enemies in its path for " +
				"$2 damage and washing them away. Targets hit are soaked for 5 second(s).");
		setUsage("/skill flood");
		setIdentifiers("skill flood");
		setArgumentRange(0, 0);
		setTypes(SkillType.SILENCEABLE, SkillType.FORCE, SkillType.ABILITY_PROPERTY_WATER, SkillType.DAMAGING);
		if (Bukkit.getServer().getPluginManager().getPlugin("NoCheatPlus") != null) ncpEnabled = true;
		Bukkit.getServer().getPluginManager().registerEvents(new SaturatedListener(this), plugin);
	}

	public String getDescription(Hero hero) {
		int distance = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.MAX_DISTANCE, false);
		double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE, false);
		return getDescription().replace("$1", distance + "")
				.replace("$2", Util.decFormat.format(damage));
	}	

	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection node = super.getDefaultConfig();

		node.set(SkillSetting.MAX_DISTANCE.node(), 8);
		node.set(SkillSetting.DAMAGE.node(), 50);
		node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 0.7);
		node.set("wave-delay", 2);
		node.set(SkillSetting.RADIUS.node(), 4);
		node.set("horizontal-power", 0.5);
		node.set("horizontal-power-increase-per-level", 0.05);
		node.set("vertical-power", 0.15);
		node.set("vertical-power-increase-per-level", 0.0045);
		node.set("ncp-exemption-duration", 1500);

		return node;
	}

	public SkillResult use(final Hero hero, String[] args) {
		final Player player = hero.getPlayer();

		final Skill skill = this;

		int distance = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.MAX_DISTANCE, false);

		Block tempBlock;
		BlockIterator iter = null;
		try {
			iter = new BlockIterator(player, distance);
		}
		catch (IllegalStateException e) {
			return SkillResult.INVALID_TARGET_NO_MSG;
		}

		broadcastExecuteText(hero);

		final double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE, false);
		final int radius = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.RADIUS, false);
		final int radiusSquared = radius * radius;

		final double hPower = SkillConfigManager.getScaledUseSettingDouble(hero, skill, "horizontal-power", false);
		final double vPower = SkillConfigManager.getScaledUseSettingDouble(hero, skill, "vertical-power", false);

		int delay = SkillConfigManager.getUseSetting(hero, this, "wave-delay", 2, false);

		final List<Entity> nearbyEntities = player.getNearbyEntities(distance * 2, distance, distance * 2);
		final List<Entity> hitEnemies = new ArrayList<Entity>();

		int numBlocks = 0;

		for (float f = 0.1F; f < 2.0F; f += 0.1F) {
			player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_SPLASH, 0.4F, f);
		}

		while (iter.hasNext()) {
			tempBlock = iter.next();
			Material tempBlockType = tempBlock.getType();
			if (Util.transparentBlocks.contains(tempBlockType)) {
				final Location targetLocation = tempBlock.getLocation().clone().add(new Vector(.5, 0, .5));

				Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable()
				{
					public void run() 
					{
						targetLocation.getWorld().playSound(targetLocation, Sound.WEATHER_RAIN, 0.6F, 0.7F);
						//targetLocation.getWorld().spigot().playEffect(targetLocation.add(0, 1.0, 0), Effect.SPLASH, 0, 0, (float) radius / 2, (float) radius / 2, (float) radius / 2, 0.0F, 150, 32);
						targetLocation.getWorld().spawnParticle(Particle.WATER_SPLASH, targetLocation.add(0, 1, 0), 150, radius / 2f, radius / 2f, radius / 2f, 0, true);
						//targetLocation.getWorld().spigot().playEffect(targetLocation.add(0, 1.0, 0), Effect.TILE_BREAK, Material.WATER.getId(), 0, (float) radius / 2, (float) radius / 2, (float) radius / 2, 0.0F, 100, 32);
						//targetLocation.getWorld().spawnParticle(Particle.BLOCK_CRACK, targetLocation.add(0, 1, 0), 100, radius / 2f, radius / 2f, radius / 2f, 0, Bukkit.createBlockData(Material.WATER), true);

						//FIXME remove this debug
						// couldn't get any messages, must be a issue getting to this.
//						String entitiesString = "none";
//						if (nearbyEntities.size() > 0) {
//							StringBuilder entitiesStringBuilder = new StringBuilder();
//							for (Entity entity : nearbyEntities) {
//								entitiesStringBuilder.append(entity.getName()).append(", ");
//							}
//							entitiesString = entitiesStringBuilder.toString();
//						}
//						Heroes.log(Level.INFO, entitiesString);

						for (Entity entity : nearbyEntities) {
							if (!(entity instanceof LivingEntity) || hitEnemies.contains(entity) || entity.getLocation().distanceSquared(targetLocation) > radiusSquared)
								continue;

							if (!damageCheck(player, (LivingEntity) entity))
								continue;

							LivingEntity target = (LivingEntity) entity;
							
							target.getWorld().playSound(target.getLocation(), Sound.ENTITY_GENERIC_SPLASH, 1.0F, 1.0F);
							target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0F, 1.0F);
							//target.getWorld().spigot().playEffect(target.getLocation().add(0, 0.3, 0), Effect.TILE_BREAK, Material.WATER.getId(), 0, 0.4F, 0.7F, 0.4F, 0.3F, 25, 32);
							//target.getWorld().spawnParticle(Particle.BLOCK_CRACK, target.getLocation().add(0, 0.3, 0), 25, 0.4, 0.7, 0.4, 0.3, Bukkit.createBlockData(Material.WATER), true);
							//target.getWorld().spigot().playEffect(target.getLocation().add(0, 0.3, 0), Effect.CRIT, 0, 0, 0.4F, 0.7F, 0.4F, 0.7F, 25, 32);
							target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 0.3, 0), 25, 0.4, 0.7, 0.4, 0.7, true);

							addSpellTarget(target, hero);
							damageEntity(target, player, damage, DamageCause.MAGIC);
							
							if (target.getFireTicks() > 0) {
								//FIXME This effect is a sound but why is it played like a particle.
								//target.getWorld().spigot().playEffect(target.getLocation(), Effect.EXTINGUISH, 0, 0, 0.5F, 1.0F, 0.5F, 0.2F, 25, 16);
								target.getWorld().playSound(target.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1.0F, 1.0F);
								target.setFireTicks(0);
							}
							
							CharacterTemplate targCT = plugin.getCharacterManager().getCharacter(target);
							targCT.addEffect(new SaturatedEffect(skill, player, 5000));
							
							Location playerLoc = player.getLocation();
							Location targetLoc = target.getLocation();

							double xDir = targetLoc.getX() - playerLoc.getX();
							double zDir = targetLoc.getZ() - playerLoc.getZ();
							double magnitude = Math.sqrt(xDir * xDir + zDir * zDir);

							xDir = xDir / magnitude * hPower;
							zDir = zDir / magnitude * hPower;

							if (ncpEnabled)	{
								if (target instanceof Player) {
									Player targetPlayer = (Player) target;
									Hero targetHero = plugin.getCharacterManager().getHero(targetPlayer);
									if (!targetPlayer.isOp()) {
										long ncpDuration = SkillConfigManager.getUseSetting(hero, skill, "ncp-exemption-duration", 500, false);
										NCPExemptionEffect ncpExemptEffect = new NCPExemptionEffect(skill, targetPlayer, ncpDuration);
										targetHero.addEffect(ncpExemptEffect);
									}
								}
							}

							target.setVelocity(new Vector(xDir, vPower, zDir));

							hitEnemies.add(entity);
						}
					}
				}, numBlocks * delay);

				numBlocks++;
			}
			else
				break;
		}
		return SkillResult.NORMAL;
	}
	
	private class NCPExemptionEffect extends ExpirableEffect {

		public NCPExemptionEffect(Skill skill, Player applier, long duration) {
			super(skill, "NCPExemptionEffect_MOVING", applier, duration);
		}

		@Override
		public void applyToHero(Hero hero) {
			super.applyToHero(hero);
			final Player player = hero.getPlayer();

			NCPExemptionManager.exemptPermanently(player, CheckType.MOVING);
		}

		@Override
		public void removeFromHero(Hero hero) {
			super.removeFromHero(hero);
			final Player player = hero.getPlayer();

			NCPExemptionManager.unexempt(player, CheckType.MOVING);
		}
	}
	
	public class SaturatedEffect extends ExpirableEffect {
		public SaturatedEffect(Skill skill, Player applier, long duration) {
			super(skill, "Saturated", applier, duration);
		}
		
		public void applyToHero(Hero hero) {
			super.applyToHero(hero);
			final Hero h = hero;
			new BukkitRunnable() {
				public void run() {
					if (!h.hasEffect("Saturated") || h.getPlayer().isDead()) cancel();
					//h.getPlayer().getWorld().spigot().playEffect(h.getPlayer().getLocation(), Effect.SPLASH, 0, 0, 0.3F, 1.0F, 0.3F, 0.0F, 25, 16);
					h.getPlayer().getWorld().spawnParticle(Particle.WATER_SPLASH, h.getPlayer().getLocation(), 25, 0.3, 1, 0.3, 0);
				}
			}.runTaskTimer(plugin, 0, 8);
		}
		
		public void applyToMonster(Monster monster)	{
			super.applyToMonster(monster);
			final Monster m = monster;
			new BukkitRunnable() {
				public void run() {
					if (!m.hasEffect("Saturated") || m.getEntity().isDead()) cancel();
					//m.getEntity().getWorld().spigot().playEffect(m.getEntity().getLocation(), Effect.SPLASH, 0, 0, 0.3F, 1.0F, 0.3F, 0.0F, 25, 16);
					m.getEntity().getWorld().spawnParticle(Particle.WATER_SPLASH, m.getEntity().getLocation(), 25, 0.3, 1, 0.3, 0);
				}
			}.runTaskTimer(plugin, 0, 8);
		}
	}
	
	public class SaturatedListener implements Listener {
		@SuppressWarnings("unused")
		private Skill skill;
		public SaturatedListener(Skill skill) {
			this.skill = skill;
		}
		
		@EventHandler
		public void entFireDamage(EntityDamageEvent event) {
			if (!(event.getEntity() instanceof LivingEntity) || (event.getCause() != DamageCause.FIRE_TICK && event.getCause() != DamageCause.FIRE)) return;
			CharacterTemplate entCT = plugin.getCharacterManager().getCharacter((LivingEntity) event.getEntity());
			if (!entCT.hasEffect("Saturated")) return;
			else event.setDamage(event.getDamage() / 2);
		}
	}
}
