package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;

public class SkillWaterbolt extends ActiveSkill
{
	private HashMap<Snowball, Player> bolts = new HashMap<Snowball, Player>();

	public SkillWaterbolt(Heroes plugin)
	{
		super(plugin, "Waterbolt");
		setDescription("You summon a ball of water and launch it, dealing $1 damage to your target and soaking them for 3 seconds. You regain half of this skill's mana cost on a successful hit.");
		setUsage("/skill waterbolt");
		setArgumentRange(0, 0);
		setIdentifiers("skill waterbolt");
		setTypes(SkillType.DAMAGING);
		Bukkit.getServer().getPluginManager().registerEvents(new WaterboltListener(this), plugin);
		Bukkit.getServer().getPluginManager().registerEvents(new SaturatedListener(this), plugin);
	}

	public String getDescription(Hero hero)
	{
		double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 40, true);
		damage += SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE, 1, true) * hero.getHeroLevel(this);
		return getDescription().replace("$1", damage + "");
	}

	public ConfigurationSection getDefaultConfig()
	{
		ConfigurationSection node = super.getDefaultConfig();
		node.set("mana-restored", 25);
		node.set(SkillSetting.DAMAGE.node(), 40);
		node.set(SkillSetting.DAMAGE_INCREASE.node(), 1);
		node.set("speed", 2.0);

		return node;
	}

	public SkillResult use(Hero hero, String[] args)
	{
		Player player = hero.getPlayer();
		float speed = (float) SkillConfigManager.getUseSetting(hero, this, "speed", 2.0, false);

		Snowball bolt = player.launchProjectile(Snowball.class);
		//ghost(bolt);
		final Vector velocity = player.getLocation().getDirection().normalize().multiply(speed);
		bolt.setVelocity(velocity);
		player.getWorld().playSound(player.getLocation(), Sound.WEATHER_RAIN, 1.0F, 1.3F);

		final Snowball theWaterbolt = bolt;
		bolts.put(bolt, player);

		new BukkitRunnable()
		{
			private int ticks = 0;
			private int maxTicks = 4;
			public void run()
			{
				if (ticks < maxTicks)
				{
					if (theWaterbolt.isDead()) 
					{
						bolts.remove(theWaterbolt);
						cancel();
					}
					ticks++;
				}
				else
				{
					bolts.remove(theWaterbolt);
					//theWaterbolt.getWorld().spigot().playEffect(theWaterbolt.getLocation(), Effect.SPLASH, 0, 0, 0.3F, 0.3F, 0.3F, 0.5F, 65, 64);
					theWaterbolt.getWorld().spawnParticle(Particle.WATER_SPLASH, theWaterbolt.getLocation(), 65, 0.3, 0.3, 0.3, 0.5, true);
					//theWaterbolt.getWorld().spigot().playEffect(theWaterbolt.getLocation(), Effect.TILE_BREAK, Material.WATER.getId(), 0, 0.3F, 0.3F, 0.3F, 0.0F, 35, 64);
					theWaterbolt.getWorld().spawnParticle(Particle.BLOCK_CRACK, theWaterbolt.getLocation(), 35, 0.3, 0.3, 0.3, 0, Bukkit.createBlockData(Material.WATER), true);
					theWaterbolt.getWorld().playSound(theWaterbolt.getLocation(), Sound.ENTITY_GENERIC_SPLASH, 1.0F, 1.3F);
					theWaterbolt.remove();
					cancel();
				}
			}
		}.runTaskTimer(plugin, 0, 20);

		new BukkitRunnable()
		{
			public void run()
			{
				if (theWaterbolt.isDead()) cancel();
				//theWaterbolt.getWorld().spigot().playEffect(theWaterbolt.getLocation(), Effect.SPLASH, 0, 0, 0.3F, 0.3F, 0.3F, 0.1F, 45, 64);
				theWaterbolt.getWorld().spawnParticle(Particle.WATER_SPLASH, theWaterbolt.getLocation(), 45, 0.3, 0.3, 0.3, 0.1, true);
				//theWaterbolt.getWorld().spigot().playEffect(theWaterbolt.getLocation(), Effect.TILE_BREAK, Material.WATER.getId(), 0, 0.1F, 0.1F, 0.1F, 0.0F, 25, 64);
				theWaterbolt.getWorld().spawnParticle(Particle.BLOCK_CRACK, theWaterbolt.getLocation(), 25, 0.1, 0.1, 0.1, 0, Bukkit.createBlockData(Material.WATER), true);
			}
		}.runTaskTimer(plugin, 0, 1);

		broadcastExecuteText(hero);

		return SkillResult.NORMAL;
	}
	
	public class SaturatedEffect extends ExpirableEffect
	{
		public SaturatedEffect(Skill skill, Player applier, long duration)
		{
			super(skill, "Saturated", applier, duration);
		}

		public void applyToHero(Hero hero)
		{
			super.applyToHero(hero);
			final Hero h = hero;
			new BukkitRunnable()
			{
				public void run()
				{
					if (!h.hasEffect("Saturated") || h.getPlayer().isDead()) cancel();
					//h.getPlayer().getWorld().spigot().playEffect(h.getPlayer().getLocation(), Effect.SPLASH, 0, 0, 0.3F, 1.0F, 0.3F, 0.0F, 25, 16);
					h.getPlayer().getWorld().spawnParticle(Particle.WATER_SPLASH, h.getPlayer().getLocation(), 25, 0.3, 1, 0.3, 0);
				}
			}.runTaskTimer(plugin, 0, 8);
		}

		public void applyToMonster(Monster monster)
		{
			super.applyToMonster(monster);
			final Monster m = monster;
			new BukkitRunnable()
			{
				public void run()
				{
					if (!m.hasEffect("Saturated") || m.getEntity().isDead()) cancel();
					//m.getEntity().getWorld().spigot().playEffect(m.getEntity().getLocation(), Effect.SPLASH, 0, 0, 0.3F, 1.0F, 0.3F, 0.0F, 25, 16);
					m.getEntity().getWorld().spawnParticle(Particle.WATER_SPLASH, m.getEntity().getLocation(), 25, 0.3, 1, 0.3, 0);
				}
			}.runTaskTimer(plugin, 0, 8);
		}
	}

	public class WaterboltListener implements Listener
	{
		Skill skill;
		public WaterboltListener(Skill skill)
		{
			this.skill = skill;
		}

		@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
		public void boltHitEntity(EntityDamageByEntityEvent event)
		{
			if (!(event.getDamager() instanceof Snowball) || !(event.getEntity() instanceof LivingEntity)) return;

			Snowball snowball = (Snowball) event.getDamager();
			LivingEntity target = (LivingEntity) event.getEntity();

			if (!bolts.containsKey(snowball)) 
			{
				return;
			}
			else
			{
				event.setCancelled(true);
			}

			Player player = bolts.get(snowball);
			Hero hero = plugin.getCharacterManager().getHero(player);
			double damage = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE, 40, true);
			int manaRestored = SkillConfigManager.getUseSetting(hero, skill, "mana-restored", 25, true);
			damage += SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE_INCREASE, 1, true) * hero.getHeroLevel(skill);

			if (target.getFireTicks() > 0)
			{
				//FIXME Effect is a sound but played like a particle?
				//target.getWorld().spigot().playEffect(target.getLocation(), Effect.EXTINGUISH, 0, 0, 0.5F, 1.0F, 0.5F, 0.2F, 25, 16);
				target.getWorld().playSound(target.getLocation(), Sound.ENTITY_CREEPER_HURT, 1.0F, 1.0F);
				target.setFireTicks(0);
			}

			addSpellTarget(target, hero);
			damageEntity(target, player, damage, DamageCause.MAGIC, false);
			hero.setMana((int) (hero.getMana() + (manaRestored)));

			CharacterTemplate targCT = plugin.getCharacterManager().getCharacter(target);
			targCT.addEffect(new SaturatedEffect(skill, player, 3000));

			//target.getWorld().spigot().playEffect(target.getLocation().add(0, 0.5, 0), Effect.SPLASH, 0, 0, 0.3F, 1.0F, 0.3F, 0.0F, 100, 16);
			target.getWorld().spawnParticle(Particle.WATER_SPLASH, target.getLocation().add(0, 0.5, 0), 100, 0.3, 1, 0.3, 0);
			target.getWorld().playSound(target.getLocation(), Sound.ENTITY_GENERIC_SPLASH, 1.0F, 1.3F);
			target.getWorld().playSound(target.getLocation(), Sound.WEATHER_RAIN, 1.0F, 1.0F);
			snowball.remove();
		}

		@EventHandler
		public void onWaterboltHit(ProjectileHitEvent event)
		{
			if (!(event.getEntity() instanceof Snowball)) return;

			Snowball snowball = (Snowball) event.getEntity();

			if (!bolts.containsKey(snowball)) 
			{
				return;
			}

			//snowball.getWorld().spigot().playEffect(snowball.getLocation(), Effect.SPLASH, 0, 0, 0.3F, 0.3F, 0.3F, 0.5F, 65, 64);
			snowball.getWorld().spawnParticle(Particle.WATER_SPLASH, snowball.getLocation(), 65, 0.3, 0.3, 0.3, 0.5, true);
			//snowball.getWorld().spigot().playEffect(snowball.getLocation(), Effect.TILE_BREAK, Material.WATER.getId(), 0, 0.3F, 0.3F, 0.3F, 0.0F, 35, 64);
			snowball.getWorld().spawnParticle(Particle.BLOCK_CRACK, snowball.getLocation(), 35, 0.3, 0.3, 0.3, 0, Bukkit.createBlockData(Material.WATER), true);
			snowball.getWorld().playSound(snowball.getLocation(), Sound.ENTITY_GENERIC_SPLASH, 1.0F, 1.3F);
			snowball.remove();
		}
	}

	public class SaturatedListener implements Listener
	{
		public SaturatedListener(Skill skill)
		{
		}

		@EventHandler
		public void entFireDamage(EntityDamageEvent event)
		{
			if (!(event.getEntity() instanceof LivingEntity) || (event.getCause() != DamageCause.FIRE_TICK && event.getCause() != DamageCause.FIRE)) return;
			CharacterTemplate entCT = plugin.getCharacterManager().getCharacter((LivingEntity) event.getEntity());
			if (!entCT.hasEffect("Saturated")) return;
			else event.setDamage(event.getDamage() / 2);
		}
	}
}