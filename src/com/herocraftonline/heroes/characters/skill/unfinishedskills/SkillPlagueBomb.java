package com.herocraftonline.heroes.characters.skill.unfinishedskills;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.server.v1_6_R2.EntityLiving;
import net.minecraft.server.v1_6_R2.MobEffect;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.craftbukkit.v1_6_R2.entity.CraftLivingEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;

public class SkillPlagueBomb extends ActiveSkill {
	private Map<Integer, Player> sheepMap = new HashMap<Integer, Player>();

	public SkillPlagueBomb(Heroes plugin) {
		super(plugin, "PlagueBomb");
		setDescription("You spawn a diseased explosive sheep. The sheep will detonate after $1 seconds, or after taking non-poison based damage. Upon detonation, the sheep will deal $2 damage to all enemies within $3 blocks.");
		setUsage("/skill plaguebomb");
		setArgumentRange(0, 0);
		setIdentifiers("skill plaguebomb");
		setTypes(SkillType.HARMFUL, SkillType.DAMAGING, SkillType.SUMMON, SkillType.SILENCABLE);

		Bukkit.getPluginManager().registerEvents(new SkillListener(), plugin);
	}

	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection node = super.getDefaultConfig();

		node.set(SkillSetting.DAMAGE.node(), Integer.valueOf(210));
		node.set(SkillSetting.RADIUS.node(), Integer.valueOf(5));
		node.set("fuse-time", Integer.valueOf(5000));
		node.set("velocity", Double.valueOf(1.0D));

		return node;
	}

	public String getDescription(Hero hero) {
		int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 210, false);
		double fuseTime = SkillConfigManager.getUseSetting(hero, this, "fuse-time", 5000, true) / 1000;
		int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5, false);

		return getDescription().replace("$1", fuseTime + "").replace("$2", damage + "").replace("$3", radius + "");
	}

	public SkillResult use(Hero hero, String[] args) {
		Player player = hero.getPlayer();
		Vector pLoc = player.getLocation().toVector();
		Vector direction = player.getLocation().getDirection();
		Vector spawnLoc = pLoc.add(direction);
		World world = player.getWorld();

		final LivingEntity sheep = (LivingEntity) world.spawnEntity(spawnLoc.toLocation(world), EntityType.SHEEP);
		sheepMap.put(sheep.getEntityId(), player);

		EntityLiving cbSheep = ((CraftLivingEntity) sheep).getHandle();
		cbSheep.addEffect(new MobEffect(19, 10000, 0));
		cbSheep.setHealth(10000);

		double velocity = SkillConfigManager.getUseSetting(hero, this, "velocity", 1.0D, false);
		sheep.setVelocity(direction.multiply(velocity).add(new Vector(0.0D, 0.15D, 0.0D)));

		int fuse = SkillConfigManager.getUseSetting(hero, this, "fuse-time", 6000, true);

		Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
			public void run() {
				explodeSheep(sheep);
			}
		}, fuse / 1000 * 20);

		broadcastExecuteText(hero);
		return SkillResult.NORMAL;
	}

	private class SkillListener implements Listener {
		private SkillListener() {
		}

		@EventHandler
		public void onEntityDamage(EntityDamageEvent event) {
			Entity entity = event.getEntity();

			if (((entity instanceof Sheep)) && (event.getCause() != EntityDamageEvent.DamageCause.POISON))
				explodeSheep((Sheep) entity);
		}
	}

	private void explodeSheep(LivingEntity sheep) {
		int id = sheep.getEntityId();
		if (sheepMap.containsKey(id)) {
			Player player = (Player) sheepMap.get(id);
			Hero hero = plugin.getCharacterManager().getHero(player);
			double damage = 1;
			if (hero != null) {
				damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 210, false);
			}

			if (!sheep.isDead()) {
				sheep.getWorld().createExplosion(sheep.getLocation(), 0.0F, false);
                sheep.damage(20000.0);

				int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5, false);
				List<Entity> nearby = sheep.getNearbyEntities(radius, radius, radius);

				for (Entity entity : nearby) {
					if (entity instanceof LivingEntity) {
						LivingEntity livingEntity = (LivingEntity) entity;
						if (hero != null) {
							plugin.getDamageManager().addSpellTarget(livingEntity, hero, this);
						}
						damageEntity(livingEntity, player, damage, EntityDamageEvent.DamageCause.MAGIC);
					}
				}
			}

			sheepMap.remove(id);
		}
	}
}