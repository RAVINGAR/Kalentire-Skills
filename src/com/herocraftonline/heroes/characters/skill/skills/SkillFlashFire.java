package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.SmallFireball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class SkillFlashFire extends ActiveSkill 
{
	public ArrayList<SmallFireball> tests = new ArrayList<SmallFireball>();
	public SkillFlashFire(Heroes plugin) {
		super(plugin, "FlashFire");
		setDescription("You flash through space in a burst of flame, teleporting up to $1 blocks away and dealing $2 damage to any enemies you pass through.");
		setUsage("/skill flashfire");
		setArgumentRange(0, 0);
		setIdentifiers("skill flashfire");
		setTypes(SkillType.TELEPORTING, SkillType.ABILITY_PROPERTY_FIRE, SkillType.DAMAGING, SkillType.SILENCEABLE);
		Bukkit.getServer().getPluginManager().registerEvents(new TestFireballCanceller(), plugin);
	}

	@Override
	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection node = super.getDefaultConfig();
		node.set(SkillSetting.MAX_DISTANCE.node(), 8);
		node.set(SkillSetting.DAMAGE.node(), 20);
		node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 0.4);
		node.set("restrict-ender-pearl", true);
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

	@Override
	public SkillResult use(Hero hero, String[] args) {
		Player player = hero.getPlayer();
		Location loc = player.getLocation();
		if (loc.getBlockY() > loc.getWorld().getMaxHeight() || loc.getBlockY() < 1) {
			player.sendMessage("You can't teleport into the void!");
			return SkillResult.FAIL;
		}
		int distance = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 8, true);
		double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 1.5, true);
		double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 20, true);
		damage += SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 0.4, true)
				* hero.getAttributeValue(AttributeType.INTELLECT);

		for (Location l : circle(loc.add(0, 0.5, 0), 36, 2))
			//l.getWorld().spigot().playEffect(l, Effect.FLAME, 0, 0, 0.1F, 0.5F, 0.1F, 0.0F, 20, 16);
			l.getWorld().spawnParticle(Particle.FLAME, l, 20, 0.1, 0.5, 0.1, 0);
		
		final ArrayList<LivingEntity> damaged = new ArrayList<LivingEntity>();		

		Block prev = null;
		Block b;
		BlockIterator iter = null;
		try {
			iter = new BlockIterator(player, distance);
		} catch (IllegalStateException e) {
			player.sendMessage("You can't flash there!");
			return SkillResult.INVALID_TARGET_NO_MSG;
		}
		while (iter.hasNext()) {
			b = iter.next();
			// I didn't use a distance check to the block location since the getNearbyEntities method seems to be a little more consistent.
			SmallFireball test = b.getWorld().spawn(b.getLocation(), SmallFireball.class);
			tests.add(test);
			List<Entity> nearby = test.getNearbyEntities(radius, radius, radius);
			for (Entity e : nearby)
			{
				if (!(e instanceof LivingEntity)) continue;
				LivingEntity target = (LivingEntity) e;
				if (!damageCheck(player, target)) continue;

				if (!damaged.contains(target))
				{
					addSpellTarget(target, hero);
					damageEntity(target, player, damage, DamageCause.MAGIC, false);
					damaged.add(target);
					target.setFireTicks(40);
				}
			}
			test.remove();
			tests.remove(test);
			//b.getWorld().spigot().playEffect(b.getLocation().add(0.5, 0, 0.5), Effect.FLAME, 0, 0, 0.5F, 0.5F, 0.5F, 0.0F, 35, 16);
			b.getWorld().spawnParticle(Particle.FLAME, b.getLocation().add(0.5, 0, 0.5), 35, 0.5, 0.5, 0.5, 0);
			if (Util.transparentBlocks.contains(b.getType()) && (Util.transparentBlocks.contains(b.getRelative(BlockFace.UP).getType())
					|| Util.transparentBlocks.contains(b.getRelative(BlockFace.DOWN).getType())))
				prev = b;
			else break;
		}
		if (prev != null) {
			Location teleport = prev.getLocation().clone();
			teleport.add(new Vector(.5, .5, .5));
			// Set the blink location yaw/pitch to that of the player
			teleport.setPitch(player.getLocation().getPitch());
			teleport.setYaw(player.getLocation().getYaw());
			Vector old = player.getVelocity();
			player.teleport(teleport);
			player.setVelocity(old);
			hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT , 0.8F, 1.0F);
			hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.ENTITY_GENERIC_EXPLODE , 0.8F, 1.0F);
			hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.BLOCK_FIRE_AMBIENT , 0.8F, 1.0F);
			loc = player.getLocation();
			for (Location l : circle(loc.add(0, 0.5, 0), 36, 2))
			{
				//l.getWorld().spigot().playEffect(l, Effect.FLAME, 0, 0, 0.1F, 0.5F, 0.1F, 0.0F, 20, 16);
				l.getWorld().spawnParticle(Particle.FLAME, l, 20, 0.1, 0.5, 0.1, 0);
			}
			return SkillResult.NORMAL;
		} else {
			player.sendMessage("No location to flash to.");
			return SkillResult.INVALID_TARGET_NO_MSG;
		}
	}

	@Override
	public String getDescription(Hero hero) {
		int distance = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 8, true);
		double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 20, true);
		damage += SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 0.4, true)
				* hero.getAttributeValue(AttributeType.INTELLECT);
		return getDescription().replace("$1", distance + "").replace("$2", damage + "");
	}

	public class TestFireballCanceller implements Listener
	{
		/**
		 * Just cancels any damage/fire caused by the test entities used
		 */
		public TestFireballCanceller() {}
		
		@EventHandler
		public void cancelFireball(EntityDamageByEntityEvent event)
		{
			if (!(event.getDamager() instanceof SmallFireball)) return;
			SmallFireball test = (SmallFireball) event.getDamager();
			if (tests.contains(test))
			{
				test.remove();
				tests.remove(test);
				event.setCancelled(true);
				return;
			}
		}
	}
}