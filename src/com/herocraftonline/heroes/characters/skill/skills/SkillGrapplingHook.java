package com.herocraftonline.heroes.characters.skill.skills;

import java.util.LinkedHashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.util.Vector;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.effects.common.SafeFallEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;

public class SkillGrapplingHook extends ActiveSkill {

	private Map<Arrow, Long> grapplingHooks = new LinkedHashMap<Arrow, Long>(100) {
		private static final long serialVersionUID = -1L;

		protected boolean removeEldestEntry(Map.Entry<Arrow, Long> eldest) {
			return (size() > 60) || (((Long) eldest.getValue()).longValue() + 5000L <= System.currentTimeMillis());
		}
	};

	private String applyText;
	private String expireText;

	public SkillGrapplingHook(Heroes plugin) {
		super(plugin, "GrapplingHook");
		setDescription("Apply a grappling hook to $1 of your arrows. Once attached, your $2 will grapple you to the targeted location! The grappling hook weighs down your arrows and reduces their velocity by $3%.");
		setUsage("/skill grapplinghook");
		setArgumentRange(0, 0);
		setIdentifiers("skill grapplinghook");
		setTypes(SkillType.PHYSICAL, SkillType.BUFF, SkillType.FORCE);
		Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(this), plugin);
	}

	public String getDescription(Hero hero) {
		int numShots = SkillConfigManager.getUseSetting(hero, this, "num-grapples", 1, false);

		String numShotsString = "";
		if (numShots > 1)
			numShotsString = "next " + numShots + " shots";
		else
			numShotsString = "next shot";

		int velocityMultiplier = (int) (SkillConfigManager.getUseSetting(hero, this, "velocity-multiplier", 0.5D, false) * 100);

		return getDescription().replace("$1", numShots + "").replace("$2", numShotsString + "").replace("$3", velocityMultiplier + "");
	}

	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection node = super.getDefaultConfig();

		node.set("num-grapples", Integer.valueOf(1));
		node.set("velocity-multiplier", Double.valueOf(0.5D));
		node.set("max-distance", Integer.valueOf(35));
		node.set("safe-fall-duration", Integer.valueOf(5000));
		node.set(SkillSetting.DURATION.node(), Integer.valueOf(12000));
		node.set(SkillSetting.APPLY_TEXT.node(), "§7[§2Skill§7] %hero% readies his grappling hook!");
		node.set(SkillSetting.EXPIRE_TEXT.node(), "§7[§2Skill§7] %hero% sheathes his grappling hook.");

		return node;
	}

	public void init() {
		super.init();

		applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, "§7[§2Skill§7] %hero% readies his grappling hook!").replace("%hero%", "$1");
		expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "§7[§2Skill§7] %hero% sheathes his grappling hook.").replace("%hero%", "$1");
	}

	public SkillResult use(Hero hero, String[] args) {

		int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 12000, false);
		int numShots = SkillConfigManager.getUseSetting(hero, this, "num-grapples", 1, false);
		hero.addEffect(new GrapplingHookBuffEffect(this, duration, numShots));

		return SkillResult.NORMAL;
	}

	public class SkillEntityListener implements Listener {

		private Skill skill;

		public SkillEntityListener(Skill skill) {
			this.skill = skill;
		}

		@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
		public void onEntityShootBow(EntityShootBowEvent event) {
			if ((!(event.getEntity() instanceof Player)) || (!(event.getProjectile() instanceof Arrow))) {
				return;
			}

			Hero hero = plugin.getCharacterManager().getHero((Player) event.getEntity());
			if (hero.hasEffect("GrapplingHookBuffEffect")) {

				// Lower the number of shots left on the buff
				GrapplingHookBuffEffect ghbEffect = (GrapplingHookBuffEffect) hero.getEffect("GrapplingHookBuffEffect");
				ghbEffect.setHooksLeft(ghbEffect.getHooksLeft() - 1);

				// If we're out of grapples, remove the buff.
				if (ghbEffect.getHooksLeft() < 1) {
					ghbEffect.setShowExpireText(false);		// Don't show 
					hero.removeEffect(ghbEffect);
				}

				// Modify the projectile
				double velocityMultiplier = SkillConfigManager.getUseSetting(hero, skill, "velocity-multiplier", 0.5D, false);
				Arrow grapplingHook = (Arrow) event.getProjectile();
				grapplingHook.setVelocity(grapplingHook.getVelocity().multiply(velocityMultiplier));
				grapplingHooks.put(grapplingHook, Long.valueOf(System.currentTimeMillis()));
			}
		}

		@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
		public void onProjectileHit(ProjectileHitEvent event) {
			if (!(event.getEntity() instanceof Arrow))
				return;

			Arrow grapplingHook = (Arrow) event.getEntity();
			if ((!(grapplingHook.getShooter() instanceof Player)))
				return;

			// Messaging.send((Player) grapplingHook.getShooter(), "A projectile has landed!");	// DEBUG

			if (!(grapplingHooks.containsKey(grapplingHook)))
				return;

			Player shooter = (Player) grapplingHook.getShooter();
			Hero hero = plugin.getCharacterManager().getHero(shooter);

			// Messaging.send(shooter, "The projectile is on the hash map.");	// DEBUG

			// Remove the arrow tracking buff
			GrapplingHookEffect ghEffect = (GrapplingHookEffect) hero.getEffect("GrapplingHookEffect");
			hero.removeEffect(ghEffect);

			// Grapple!
			grapplingHooks.remove(grapplingHook);
			grapple(hero, grapplingHook.getLocation());
		}

		// OLD CODE. NO LONGER NEEDED.
		//		@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
		//		public void onEntityDamage(EntityDamageEvent event) {
		//			if ((!(event instanceof EntityDamageByEntityEvent)) || (!(event.getEntity() instanceof LivingEntity))) {
		//				return;
		//			}
		//
		//			Entity projectile = ((EntityDamageByEntityEvent) event).getDamager();
		//			if ((!(projectile instanceof Arrow)) || (!(((Projectile) projectile).getShooter() instanceof Player))) {
		//				return;
		//			}
		//
		//			Arrow arrow = (Arrow) projectile;
		//			Player shooter = (Player) arrow.getShooter();
		//			Hero hero = plugin.getCharacterManager().getHero(shooter);
		//
		//			// Check to see if the person who shot the arrow is grappling
		//			if (!hero.hasEffect("GrapplingHookEffect")) {
		//				return;
		//			}
		//
		//			// Remove the arrow tracking buff
		//			GrapplingHookEffect ghEffect = (GrapplingHookEffect) hero.getEffect("GrapplingHookEffect");
		//			hero.removeEffect(ghEffect);
		//
		//			// Grapple!
		//			grapple(hero, arrow.getLocation());
		//
		//			return;
		//		}
	}

	private void grapple(Hero shooterHero, Location location) {

		Player shooter = shooterHero.getPlayer();

		Location playerLoc = shooter.getLocation();
		if (!(playerLoc.getWorld().equals(location.getWorld())))
			return;

		Vector playerLocVec = shooter.getLocation().toVector();
		Vector locVec = location.toVector();

		double distance = (int) playerLocVec.distance(locVec);
		//Messaging.send(shooter, "Distance: " + distance + ".", new Object[0]);

		int maxDistance = SkillConfigManager.getUseSetting(shooterHero, this, "max-distance", 35, false);
		if (distance > maxDistance) {
			Messaging.send(shooter, "Your target is too far, your line has snapped!", new Object[0]);
			return;
		}

		// If the player is aiming downwards, don't let him increase his y.
		boolean noY = false;
		if (locVec.getY() < playerLoc.getY())
			noY = true;

		// Messaging.send(shooter, "PlayerLoc Vector x: " + playerLoc.getX() + ", y: " + playerLoc.getY() + ", z: " + playerLoc.getZ(), new Object[0]);
		// Messaging.send(shooter, "TargetLoc Vector x: " + locVec.getX() + ", y: " + locVec.getY() + ", z: " + locVec.getZ(), new Object[0]);

		// Create our distance vector
		Vector dVector = locVec.subtract(playerLocVec);

		// Messaging.send(shooter, "Distance Vector Velocity x: " + dVector.getX() + ", y: " + dVector.getY() + ", z: " + dVector.getZ(), new Object[0]);

		// Store the block variables
		int dX = dVector.getBlockX();
		int dY = dVector.getBlockY();
		int dZ = dVector.getBlockZ();

		// Calculate pull vector
		int multiplier = (int) ((Math.abs(dX) + Math.abs(dY) + Math.abs(dZ)) / 6);
		int ymultiplier = (int) (Math.abs(dY) - (Math.abs(dX) + Math.abs(dZ)) / 30);
		Vector vec = new Vector(dX, Math.abs(dY) + ymultiplier, dZ).normalize().multiply(multiplier);
		if ((Math.abs(dY) + ymultiplier) * multiplier > 3) {
			vec.setY(vec.getY() / 2.0D);
		}

		// Prevent y velocity increase if told to.
		if (noY) {
			vec.setY(0);
			vec.multiply(0.5);	// Half the power of the grapple
		}

		// Messaging.send(shooter, "Vector Velocity x: " + vec.getX() + ", y: " + vec.getY() + ", z: " + vec.getZ(), new Object[0]);
		
		// Grapple!
		shooter.getWorld().playSound(shooter.getLocation(), Sound.MAGMACUBE_JUMP, 10.0F, 1.0F);
		shooter.setVelocity(vec);
		int safeFallDuration = SkillConfigManager.getUseSetting(shooterHero, this, "safe-fall-duration", 5000, false);
		shooterHero.addEffect(new SafeFallEffect(this, safeFallDuration));
	}

	// Effect used for detecting who shot the arrow
	public class GrapplingHookEffect extends Effect {

		public GrapplingHookEffect(Skill skill) {
			super(skill, "GrapplingHookEffect");

			this.types.add(EffectType.PHYSICAL);
			this.types.add(EffectType.BENEFICIAL);
		}
	}

	// Buff effect used to keep track of grappling hook uses
	public class GrapplingHookBuffEffect extends ExpirableEffect {

		protected int hooksLeft = 1;
		private boolean showExpireText = true;

		public GrapplingHookBuffEffect(Skill skill, long duration, int numShots) {
			super(skill, "GrapplingHookBuffEffect", duration);
			this.hooksLeft = numShots;

			this.types.add(EffectType.PHYSICAL);
			this.types.add(EffectType.BENEFICIAL);
		}

		@Override
		public void applyToHero(Hero hero) {
			super.applyToHero(hero);

			Player player = hero.getPlayer();
			broadcast(player.getLocation(), applyText, player.getDisplayName());
		}

		@Override
		public void removeFromHero(Hero hero) {
			super.removeFromHero(hero);

			Player player = hero.getPlayer();

			if (showExpireText)
				broadcast(player.getLocation(), expireText, player.getDisplayName());
		}

		public int getHooksLeft() {
			return hooksLeft;
		}

		public void setHooksLeft(int hooksLeft) {
			this.hooksLeft = hooksLeft;
		}

		public void setShowExpireText(boolean showExpireText) {
			this.showExpireText = showExpireText;
		}
	}
}