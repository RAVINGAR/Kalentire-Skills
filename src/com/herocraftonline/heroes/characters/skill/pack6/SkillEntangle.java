package com.herocraftonline.heroes.characters.skill.pack6;


import java.util.ArrayList;

import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.RootEffect;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;

public class SkillEntangle extends TargettedSkill {

	private String applyText;
	private String expireText;

	public SkillEntangle(Heroes plugin) {
		// Heroes stuff
		super(plugin, "Entangle");
		setDescription("Roots your target in place for $1 seconds. The effect breaks when the target takes damage.");
		setUsage("/skill entangle");
		setIdentifiers("skill entangle");
		setTypes(SkillType.AGGRESSIVE, SkillType.MOVEMENT_PREVENTING, SkillType.DEBUFFING, SkillType.INTERRUPTING, SkillType.SILENCEABLE, SkillType.ABILITY_PROPERTY_EARTH);
		setArgumentRange(0, 0);

		// Start up the listener for root skill usage
		Bukkit.getServer().getPluginManager().registerEvents(new RootListener(), plugin);
	}

	@Override
	public String getDescription(Hero hero) {
		double duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 3000, false);
		String formattedDuration = Util.decFormat.format(duration / 1000.0);

		return getDescription().replace("$1", formattedDuration);
	}

	@Override
	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection node = super.getDefaultConfig();

		node.set(SkillSetting.MAX_DISTANCE.node(), 10);
		node.set(SkillSetting.PERIOD.node(), 100);
		node.set(SkillSetting.DURATION.node(), 3000);
		node.set(SkillSetting.USE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% used %skill% on %target%!");
		node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% has been rooted!");
		node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% has broken free from the root!");

		return node;
	}

	public void init() {
		super.init();

		applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%target% has been rooted!").replace("%target%", "$1");
		expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%target% has broken free from the root!").replace("%target%", "$1");
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
	public SkillResult use(Hero hero, LivingEntity target, String[] args) {

		// Broadcast use text
		broadcastExecuteText(hero, target);

		// Play Sound
		Player player = hero.getPlayer();

		int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 4000, false);
		int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 100, false);

		//EntangleEffect EntangleEffect = new EntangleEffect(this, hero.getPlayer(), duration);
		RootEffect rootEffect = new RootEffect(this, player, period, duration, applyText, expireText);

		// Add root effect to the target
		CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
		targetCT.addEffect(rootEffect);

		player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 0.8F, 1.0F);

		ArrayList<Location> particleLocations = circle(player.getLocation(), 36, 1.5);
		for (int i = 0; i < particleLocations.size(); i++)
		{
			//player.getWorld().spigot().playEffect(particleLocations.get(i), Effect.TILE_BREAK, Material.WOOD.getId(), 0, 0, 0.1F, 0, 0.0F, 1, 16);
			player.getWorld().spawnParticle(Particle.BLOCK_CRACK, particleLocations.get(i), 1, 0, 0.1, 0, Bukkit.createBlockData(Material.OAK_WOOD));
		}

		return SkillResult.NORMAL;
	}

	private class RootListener implements Listener {

		//        private Skill skill;
		//
		//        public RootListener(Skill skill) {
		//            this.skill = skill;
		//        }

		@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
		public void onEntityDamage(EntityDamageEvent event) {
			if (event.getDamage() == 0)
				return;

			if (!(event.getEntity() instanceof LivingEntity))
				return;

			final CharacterTemplate defenderCT = plugin.getCharacterManager().getCharacter((LivingEntity) event.getEntity());

			if (defenderCT.hasEffect("Root"))
				defenderCT.removeEffect(defenderCT.getEffect("Root"));
		}

		// Below is my attempt at preventing sprinting. None of it worked as much as I would have hoped. Keeping it here for future attempts.

		//        @EventHandler(priority = EventPriority.MONITOR)
		//        public void onFoodLevelChangeEvent(FoodLevelChangeEvent event) {
		//
		//            if (!(event.getEntity() instanceof Player))
		//                return;
		//
		//            // We always set to 6 to disable sprinting. If we aren't at 6 we don't need to perform any further checks
		//            if (event.getFoodLevel() != 6)
		//                return;
		//
		//            Player player = (Player) event.getEntity();
		//            Hero hero = plugin.getCharacterManager().getHero(player);
		//            if (hero.hasEffect("Root")) {
		//                event.setCancelled(true);
		//                Messaging.send(player, "Cancelled a food change event.");
		//            }
		//        }

		//        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
		//        public void onPlayerSprintToggle(PlayerToggleSprintEvent event) {
		//            if (!event.isSprinting())
		//                return;
		//
		//            Hero hero = plugin.getCharacterManager().getHero(event.getPlayer());
		//            if (hero.hasEffect("Root")) {
		//                final Player player = event.getPlayer();
		//
		//                // Don't allow an entangled player to sprint. If they are sprinting, turn it off.
		//                final int currentHunger = player.getFoodLevel();
		//                player.setFoodLevel(1);
		//                player.setSprinting(false);
		//
		//                Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable()
		//                {
		//                    public void run()
		//                    {
		//                        player.setFoodLevel(currentHunger);
		//                    }
		//                }, 0L);
		//
		//            }
		//        }

		//        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
		//        public void onPlayerSprintToggle(PlayerToggleSprintEvent event) {
		//            if (!event.isSprinting())
		//                return;
		//
		//            Hero hero = plugin.getCharacterManager().getHero(event.getPlayer());
		//            if (hero.hasEffect("Root")) {
		//                if (!(hero.getEffect("Root") instanceof EntangleEffect))
		//                    return;
		//
		//                Player player = event.getPlayer();
		//
		//                // Store some temp variables
		//                EntangleEffect oldEntangleEffect = (EntangleEffect) hero.getEffect("Root");
		//                Player applier = oldEntangleEffect.getApplier();
		//                long duration = oldEntangleEffect.getDuration();
		//
		//                // Damage them for sprinting
		//                double damage = SkillConfigManager.getUseSetting(hero, skill, "sprint-damage", 10, false);
		//                damageEntity((LivingEntity) player, applier, damage, EntityDamageEvent.DamageCause.MAGIC);
		//
		//                // Put the entangle effect back on them
		//                EntangleEffect newEntangleEffect = new EntangleEffect(skill, applier, duration);
		//                hero.addEffect(newEntangleEffect);
		//            }
		//        }
		//    }
	}

	// Below is the effect used for a "normal" root that doesn't use teleportation as a base. Kept here for future attempts to tweak the skill.

	//    public class EntangleEffect extends ExpirableEffect {
	//
	//        private Player applier;
	//
	//        public EntangleEffect(Skill skill, Player applier, long duration) {
	//            super(skill, "Root", duration);
	//            this.setApplier(applier);
	//
	//            types.add(EffectType.ROOT);
	//            types.add(EffectType.HARMFUL);
	//            types.add(EffectType.MAGIC);
	//            types.add(EffectType.DISPELLABLE);
	//
	//            addMobEffect(2, (int) (duration / 1000) * 20, 127, false);      // Max slowness
	//            addMobEffect(8, (int) (duration / 1000) * 20, 128, false);      // Max negative jump boost
	//        }
	//
	//        @Override
	//        public void applyToMonster(Monster monster) {
	//            super.applyToMonster(monster);
	//            broadcast(monster.getEntity().getLocation(), "    " + applyText, CustomNameManager.getName(monster));
	//        }
	//
	//        @Override
	//        public void applyToHero(Hero hero) {
	//            super.applyToHero(hero);
	//            final Player player = hero.getPlayer();
	//
	//            // Don't allow an entangled player to sprint. If they are sprinting, turn it off.
	//            if (player.isSprinting()) {
	//                final int currentHunger = player.getFoodLevel();
	//                player.setFoodLevel(1);
	//                player.setSprinting(false);
	//
	//                Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable()
	//                {
	//                    public void run()
	//                    {
	//                        player.setFoodLevel(currentHunger);
	//                    }
	//                }, 0L);
	//            }
	//
	//            broadcast(player.getLocation(), "    " + applyText, player.getName());
	//        }
	//
	//        @Override
	//        public void removeFromHero(Hero hero) {
	//            super.removeFromHero(hero);
	//            final Player player = hero.getPlayer();
	//            broadcast(player.getLocation(), "    " + expireText, player.getName());
	//        }
	//
	//        @Override
	//        public void removeFromMonster(Monster monster) {
	//            super.removeFromMonster(monster);
	//            broadcast(monster.getEntity().getLocation(), "    " + expireText, CustomNameManager.getName(monster));
	//        }
	//
	//        public Player getApplier() {
	//            return applier;
	//        }
	//
	//        public void setApplier(Player applier) {
	//            this.applier = applier;
	//        }
}