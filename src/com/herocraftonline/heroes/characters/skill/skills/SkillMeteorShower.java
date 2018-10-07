package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.chat.ChatComponents;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

public class SkillMeteorShower extends ActiveSkill implements Listener
{
	private ArrayList<Player> showers = new ArrayList<Player>();
	private HashMap<FallingBlock, Player> meteors = new HashMap<FallingBlock, Player>();
	
	public SkillMeteorShower(Heroes plugin)
	{
		super(plugin, "MeteorShower");
		setDescription("You call upon a deep inner power and shake the very stars, causing $1 meteors to rain down at your target location within a radius of $2 meters. Each meteor deals $3 damage to targets within $4 meters upon impact.");
		setUsage("/skill meteorshower");
		setArgumentRange(0, 0);
		setIdentifiers("skill meteorshower");
		setTypes(SkillType.DAMAGING);
		Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
	}

	public String getDescription(Hero hero)
	{
		int meteors = SkillConfigManager.getUseSetting(hero, this, "meteors", 20, false);
		meteors += SkillConfigManager.getUseSetting(hero, this, "meteors-per-level", 0.5, false) * hero.getHeroLevel(this);
		int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 12, false);
		double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 50, false);
		damage += SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 2, false)
				* hero.getAttributeValue(AttributeType.INTELLECT);
		double blastRadius = SkillConfigManager.getUseSetting(hero, this, "blast-radius", 2, false);
		return getDescription().replace("$1", meteors + "").replace("$2", radius + "").replace("$3", damage + "").replace("$4", blastRadius + "");
	}
	public ConfigurationSection getDefaultConfig() 
	{
		ConfigurationSection node = super.getDefaultConfig();
		node.set(SkillSetting.RADIUS.node(), 12);

		node.set(SkillSetting.DAMAGE.node(), 50);

		node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 2);

		node.set("meteors", 30);

		node.set("meteors-per-level", 0.5);

		node.set("blast-radius", 2);

		return node;
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
	
	public SkillResult use(Hero hero, String[] args)
	{
		final int maxMeteors = (int) Math.floor(SkillConfigManager.getUseSetting(hero, this, "meteors", 20, true)
				+ ((SkillConfigManager.getUseSetting(hero, this, "firebolts-per-level", 0.5, false) * hero.getHeroLevel(this))));
		final int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 12, false);
		int distance = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 20, false);

		ArrayList<Location> meteorLocs = new ArrayList<Location>();

		Player player = hero.getPlayer();

		Block targetBlock = player.getTargetBlock((HashSet<Material>)null, distance);
		Location center = targetBlock.getLocation().clone().add(0, 40, 0);

		for (int i = 1; i <= radius; i++)
		{
			ArrayList<Location> concentric = circle(center, 12, (double) i);
			meteorLocs.addAll(concentric);
		}

		player.getWorld().playSound(player.getLocation(), Sound.BLOCK_PORTAL_TRAVEL, 1.0F, 2.0F);

		final ArrayList<Location> finalLocs = meteorLocs;
		final Player p = player;

        showers.add(p);

		new BukkitRunnable()
		{
			private Random rand = new Random();
			private int meteorCount = 0;

			public void run()
			{
				if (meteorCount < maxMeteors)
				{
					int index = rand.nextInt(finalLocs.size());
					Location meteorLocation = finalLocs.get(index).clone().setDirection(new Vector(0, -1, 0));
					FallingBlock meteor = meteorLocation.getWorld().spawnFallingBlock(meteorLocation, Material.DRAGON_EGG.createBlockData());
					meteor.setDropItem(false);
					meteors.put(meteor, p);

					final FallingBlock theMeteor = meteor;

					new BukkitRunnable() {
						public void run() {
							if (theMeteor.isDead()) cancel();
							//theMeteor.getWorld().spigot().playEffect(theMeteor.getLocation(), Effect.LARGE_SMOKE, 0, 0, 1.0F, 1.0F, 1.0F, 0.2F, 40, 128);
							theMeteor.getWorld().spawnParticle(Particle.SMOKE_LARGE, theMeteor.getLocation(), 40, 1, 1, 1, 0.2, true);
							//theMeteor.getWorld().spigot().playEffect(theMeteor.getLocation(), Effect.FLAME, 0, 0, 0.2F, 1.0F, 0.2F, 0.0F, 30, 128);
							theMeteor.getWorld().spawnParticle(Particle.FLAME, theMeteor.getLocation(), 30, 0.2, 1, 0.2, 0, true);
						}
					}.runTaskTimer(plugin, 0, 1);
					meteorCount++;
				}
				else
				{
					showers.remove(p);
					cancel();
				}
			}
		}.runTaskTimer(plugin, 1, 5);

		broadcast(player.getLocation(), ChatComponents.GENERIC_SKILL + ChatColor.WHITE + hero.getName()
				+ ChatColor.GRAY + " causes a " + ChatColor.WHITE + "Meteor Shower" + ChatColor.GRAY + "!");

		return SkillResult.NORMAL;
	}
	
	@EventHandler
	public void onFallingBlockLand(final EntityChangeBlockEvent event)
	{
		if (event.getEntity() instanceof FallingBlock)
		{
			FallingBlock fallingBlock = (FallingBlock) event.getEntity();

			Block b = event.getBlock();

			if (meteors.containsKey(fallingBlock))
			{
				fallingBlock.setDropItem(false);				
				fallingBlock.remove();
				Hero hero = plugin.getCharacterManager().getHero(meteors.get(fallingBlock));
				final double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 50, false)
						+ (SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 2, false)
						* hero.getAttributeValue(AttributeType.INTELLECT));
				final double blastRadius = SkillConfigManager.getUseSetting(hero, this, "blast-radius", 2, true);
				b.getWorld().playSound(b.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.75F, 1.0F);
				b.getWorld().playSound(b.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 0.5F, 1.0F);
				//b.getWorld().spigot().playEffect(b.getLocation(), Effect.EXPLOSION_LARGE, fallingBlock.getBlockId(), 0, (float) blastRadius / 2, (float) blastRadius / 2, (float) blastRadius / 2, 0.0F, 45, 128);
				b.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, b.getLocation(), 45, blastRadius, blastRadius, blastRadius, 0, true);
				//b.getWorld().spigot().playEffect(b.getLocation(), Effect.LARGE_SMOKE, fallingBlock.getBlockId(), 0, (float) blastRadius, (float) blastRadius, (float) blastRadius, 0.0F, 45, 128);
				b.getWorld().spawnParticle(Particle.SMOKE_LARGE, b.getLocation(), 45, blastRadius, blastRadius, blastRadius, 0, true);
				//b.getWorld().spigot().playEffect(b.getLocation(), Effect.FLAME, fallingBlock.getBlockId(), 0, (float) blastRadius, (float) blastRadius, (float) blastRadius, 0.3F, 145, 128);
				b.getWorld().spawnParticle(Particle.FLAME, b.getLocation(), 140, blastRadius, blastRadius, blastRadius, 0.3, true);
				
				for (Entity e : hero.getPlayer().getNearbyEntities(50, 50, 50)) {
					if (e.getLocation().distance(b.getLocation()) > blastRadius) continue;
					if (!(e instanceof LivingEntity)) continue;
					LivingEntity le = (LivingEntity) e;
					if (!damageCheck(hero.getPlayer(), le)) continue;
					addSpellTarget(le, hero);
					damageEntity(le, hero.getPlayer(), damage, DamageCause.MAGIC, false);
					Vector force = (fallingBlock.getLocation().toVector().subtract(le.getLocation().toVector()).multiply(-1).setY(0.6));
					le.setVelocity(force);
				}
				
				event.setCancelled(true);
			}
		}
	}
}
