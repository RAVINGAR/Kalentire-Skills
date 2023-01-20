package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.BloodUnionEffect;
import com.herocraftonline.heroes.characters.skill.*;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;

public class SkillBloodbolt extends ActiveSkill
{
	private final HashMap<Snowball, Player> bolts = new HashMap<>();

	public SkillBloodbolt(Heroes plugin)
	{
		super(plugin, "Bloodbolt");
		setDescription("You launch an orb of foul blood, dealing $1 damage to your target and healing for $2% of the damage dealt. Increases Blood Union by $3 if it hits a target.");
		setUsage("/skill bloodbolt");
		setArgumentRange(0, 0);
		setIdentifiers("skill bloodbolt");
		setTypes(SkillType.DAMAGING);
		Bukkit.getServer().getPluginManager().registerEvents(new BloodboltListener(this), plugin);
	}

	public String getDescription(Hero hero)
	{
		double healthPerc = SkillConfigManager.getUseSetting(hero, this, "health-restore-percent", 50, true);
		double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 40, true);
		damage += SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE, 1, true) * hero.getAttributeValue(AttributeType.INTELLECT);
		int bloodUnionIncrease = SkillConfigManager.getUseSetting(hero, this, "blood-union-increase", 1, false);
		return getDescription().replace("$1", damage + "")
				.replace("$2", healthPerc + "")
				.replace("$3", bloodUnionIncrease + "");
	}

	public ConfigurationSection getDefaultConfig()
	{
		ConfigurationSection node = super.getDefaultConfig();
		node.set("health-restore-percent", 50);
		node.set(SkillSetting.DAMAGE.node(), 40);
		node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 1);
		node.set("speed", 2.0);

		return node;
	}

	public SkillResult use(Hero hero, String[] args)
	{
		Player player = hero.getPlayer();
		float speed = (float) SkillConfigManager.getUseSetting(hero, this, "speed", 2.0, false);

		Snowball bolt = player.launchProjectile(Snowball.class);
		//EntityUtil.ghost(bolt);
		final Vector velocity = player.getLocation().getDirection().normalize().multiply(speed);
		bolt.setVelocity(velocity);
		player.getWorld().playSound(player.getLocation(), Sound.WEATHER_RAIN, 0.6F, 1.3F);
		player.getWorld().playSound(player.getLocation(), Sound.BLOCK_LAVA_AMBIENT, 1.0F, 1.3F);

		final Snowball theBloodbolt = bolt;
		bolts.put(bolt, player);

		new BukkitRunnable()
		{
			private int ticks = 0;
			private final int maxTicks = 4;
			public void run()
			{
				if (ticks < maxTicks)
				{
					if (theBloodbolt.isDead()) cancel();
					ticks++;
				}
				else
				{
					bolts.remove(theBloodbolt);
					//theBloodbolt.getWorld().spigot().playEffect(theBloodbolt.getLocation(), Effect.COLOURED_DUST, 0, 0, 0.3F, 0.3F, 0.3F, 0.0F, 65, 64);
					theBloodbolt.getWorld().spawnParticle(Particle.REDSTONE, theBloodbolt.getLocation(), 65, 0.3, 0.3, 0.3, 0.0, new Particle.DustOptions(Color.RED, 1), true);
					//theBloodbolt.getWorld().spigot().playEffect(theBloodbolt.getLocation(), Effect.TILE_BREAK, Material.NETHER_WARTS.getId(), 0, 0.3F, 0.3F, 0.3F, 0.0F, 35, 64);
					theBloodbolt.getWorld().spawnParticle(Particle.BLOCK_CRACK, theBloodbolt.getLocation(), 35, 0.3, 0.3, 0.3, 0.0, Bukkit.createBlockData(Material.NETHER_WART_BLOCK), true);
					theBloodbolt.getWorld().playSound(theBloodbolt.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 1.0F, 1.3F);
					theBloodbolt.remove();
					cancel();
				}
			}
		}.runTaskTimer(plugin, 0, 20);

		//FIXME Well fuck
		//EntityUtil.persistentFX(theBloodbolt, new Vector(0, 0, 0), Effect.COLOURED_DUST, 0, 0, 0.3F, 0.3F, 0.3F, 0.0F, 45, 64, plugin, 0, 1);
		//EntityUtil.persistentFX(theBloodbolt, new Vector(0, 0, 0), Effect.TILE_BREAK, Material.NETHER_WARTS.getId(), 0, 0.1F, 0.1F, 0.1F, 0.0F, 25, 64, plugin, 0, 1);

		broadcastExecuteText(hero);

		return SkillResult.NORMAL;
	}

	public class BloodboltListener implements Listener
	{
		final Skill skill;
		public BloodboltListener(Skill skill)
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
				return;
			else event.setCancelled(true);

			Player player = bolts.get(snowball);
			bolts.remove(snowball);
			Hero hero = plugin.getCharacterManager().getHero(player);
			double damage = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE, 40, true);
			double healthRestored = SkillConfigManager.getUseSetting(hero, skill, "health-restore-percent", 50, true);
			damage += SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE_INCREASE, 1, true) * hero.getAttributeValue(AttributeType.INTELLECT);
			int bloodUnionIncrease = SkillConfigManager.getUseSetting(hero, skill, "blood-union-increase", 1, false);

			if (hero.hasEffect("BloodUnionEffect")) {
				BloodUnionEffect buEffect = (BloodUnionEffect) hero.getEffect("BloodUnionEffect");

                buEffect.addBloodUnion(bloodUnionIncrease, target instanceof Player);
			}

			addSpellTarget(target, hero);
			damageEntity(target, player, damage, DamageCause.MAGIC, false);
			hero.heal(damage * (healthRestored/100));

			//target.getWorld().spigot().playEffect(target.getLocation().add(0, 0.5, 0), Effect.COLOURED_DUST, 0, 0, 0.3F, 1.0F, 0.3F, 0.0F, 100, 16);
			target.getWorld().spawnParticle(Particle.REDSTONE, target.getLocation().add(0, 0.5, 0), 100, 0.3, 0.1, 0.3, new Particle.DustOptions(Color.RED, 1));
			target.getWorld().playSound(target.getLocation(), Sound.ENTITY_GENERIC_SPLASH, 0.6F, 1.3F);
			target.getWorld().playSound(target.getLocation(), Sound.WEATHER_RAIN, 1.0F, 1.0F);
			target.getWorld().playSound(target.getLocation(), Sound.BLOCK_LAVA_AMBIENT, 1.0F, 1.0F);
			snowball.remove();
		}

		@EventHandler
		public void onBloodboltHit(ProjectileHitEvent event)
		{
			if (!(event.getEntity() instanceof Snowball)) return;

			Snowball snowball = (Snowball) event.getEntity();

			if (!bolts.containsKey(snowball)) 
			{
				return;
			}

			//snowball.getWorld().spigot().playEffect(snowball.getLocation(), Effect.COLOURED_DUST, 0, 0, 0.3F, 0.3F, 0.3F, 0.0F, 65, 64);
			snowball.getWorld().spawnParticle(Particle.REDSTONE, snowball.getLocation(), 65, 0.3, 0.3, 0.3, 0.0, new Particle.DustOptions(Color.RED, 1), true);
			//snowball.getWorld().spigot().playEffect(snowball.getLocation(), Effect.TILE_BREAK, Material.NETHER_WARTS.getId(), 0, 0.3F, 0.3F, 0.3F, 0.0F, 35, 64);
			snowball.getWorld().spawnParticle(Particle.BLOCK_CRACK, snowball.getLocation(), 35, 0.3, 0.3, 0.3, 0.0, Bukkit.createBlockData(Material.NETHER_WART_BLOCK), true);
			snowball.getWorld().playSound(snowball.getLocation(), Sound.ENTITY_GENERIC_SPLASH, 0.6F, 1.3F);
			snowball.getWorld().playSound(snowball.getLocation(), Sound.WEATHER_RAIN, 1.0F, 1.0F);
			snowball.getWorld().playSound(snowball.getLocation(), Sound.BLOCK_LAVA_AMBIENT, 1.0F, 1.0F);
			snowball.remove();
		}
	}
}