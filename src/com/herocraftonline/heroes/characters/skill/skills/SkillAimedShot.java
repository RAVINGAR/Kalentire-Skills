package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.util.Vector;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.effects.common.SneakEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Messaging;

public class SkillAimedShot extends TargettedSkill {

	private String applyText;
	private String expireTextFail;
	private String expireTextBadShot;
	private String expireTextSuccess;

	public SkillAimedShot(Heroes plugin) {
		super(plugin, "AimedShot");
		setDescription("Hone your aim in on a target. Once completed, the next shot you fire within $1 seconds will land §o§lwithout question§r§6. This shot will deal $2 damage to the target.");
		setUsage("/skill aimedshot");
		setArgumentRange(0, 0);
		setIdentifiers("skill aimedshot");
		setTypes(SkillType.PHYSICAL, SkillType.PHYSICAL, SkillType.HARMFUL, SkillType.DAMAGING);
		Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(this), plugin);
	}

	public String getDescription(Hero hero) {

		double gracePeriod = SkillConfigManager.getUseSetting(hero, this, "grace-period", 4000, false) / 1000;
		int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 250, false);

		return getDescription().replace("$1", gracePeriod + "").replace("$2", damage + "");
	}

	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection node = super.getDefaultConfig();

		node.set(SkillSetting.DAMAGE.node(), Integer.valueOf(250));
		node.set(SkillSetting.MAX_DISTANCE.node(), Integer.valueOf(30));
		node.set(SkillSetting.DELAY.node(), Integer.valueOf(3000));
		node.set("grace-period", Integer.valueOf(2000));
		node.set(SkillSetting.USE_TEXT.node(), String.valueOf("§7[§2Skill§7] %hero% begins to hone in his aim on %target%"));
		node.set(SkillSetting.APPLY_TEXT.node(), String.valueOf("§7[§2Skill§7] %hero% begins to hone in his aim on %target%"));
		node.set("grace-period", Integer.valueOf(2000));
		node.set("expire-text-fail", String.valueOf("§7[§2Skill§7] %hero% has lost sight of his target."));
		node.set("expire-text-bad-shot", String.valueOf("§7[§2Skill§7] %hero%'s shot was shallow."));
		node.set("expire-text-success", String.valueOf("§7[§2Skill§7] %hero% has unleashed a powerful §lAimed Shot§r§7 on %target%!"));

		return node;
	}

	public void init() {
		super.init();

		applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, "§7[§2Skill§7] %hero% is locked on!").replace("%hero%", "$1");
		expireTextFail = SkillConfigManager.getRaw(this, "expire-text-fail", "§7[§2Skill§7] %hero% has lost sight of his target.").replace("%hero%", "$1");
		expireTextBadShot = SkillConfigManager.getRaw(this, "expire-text-bad-shot", "§7[§2Skill§7] %hero%'s shot was shallow.").replace("%hero%", "$1");
		expireTextSuccess = SkillConfigManager.getRaw(this, "expire-text-success", "§7[§2Skill§7] %hero% has unleashed a powerful §lAimed Shot§r§7 on %target%!").replace("%hero%", "$1").replace("%target%", "$2");
	}

	public SkillResult use(Hero hero, LivingEntity target, String[] args) {

		Player player = hero.getPlayer();
		
		// Check line of sight, but only against other players.
		if (target instanceof Player) {
			
			Player targetPlayer = (Player) target;
			if (!inLineOfSight(player, targetPlayer) || !player.canSee(targetPlayer)) {
				hero.getPlayer().sendMessage("Your target is not within your line of sight!");
				return SkillResult.FAIL;
			}
		}
		
		// Sneak checks. Force vanilla sneaking.
		if (!player.isSneaking()) {
			Messaging.send(player, "You must be sneaking to use this ability!");
			return SkillResult.FAIL;
		}

		if (hero.hasEffect("Sneak")) {
			SneakEffect sEffect = (SneakEffect) hero.getEffect("Sneak");
			if (!sEffect.isVanillaSneaking()) {
				Messaging.send(player, "You must be crouch-sneaking to use this ability!");
				return SkillResult.FAIL;
			}
		}

		int gracePeriod = SkillConfigManager.getUseSetting(hero, this, "grace-period", 4000, false);
		hero.addEffect(new AimedShotEffect(this, target, gracePeriod));

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

			final Hero hero = plugin.getCharacterManager().getHero((Player) event.getEntity());
			if (hero.hasEffect("AimedShotEffect")) {

				// Player released arrow too soon--skill failure.
				AimedShotEffect asEffect = (AimedShotEffect) hero.getEffect("AimedShotEffect");

				if (event.getForce() < 1) {
					// Player released arrow too soon--skill failure.
					asEffect.setBadShot(true);
					hero.removeEffect(asEffect);
					return;
				}

				final LivingEntity target = asEffect.getTarget();

				Vector playerLocVec = hero.getPlayer().getLocation().toVector();
				Vector targetLocVec = target.getLocation().toVector();

				double distance = playerLocVec.distance(targetLocVec);
				int travelTime = (int) (0.05 * distance);

				// Tell the buff that we have a successful shot and then remove it
				asEffect.failed = false;
				hero.removeEffect(asEffect);

				// Remove the standard projectile
				Arrow actualArrow = (Arrow) event.getProjectile();
				actualArrow.remove();

				// Damage the target
				final int damage = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE, 250, false);
				Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
					public void run() {
						skill.plugin.getDamageManager().addSpellTarget(target, hero, skill);
						damageEntity(target, hero.getPlayer(), damage, EntityDamageEvent.DamageCause.MAGIC);
					}
				}, travelTime * 20);

			}
		}
	}

	// Buff effect used to keep track of warmup time
	public class AimedShotEffect extends ExpirableEffect {

		private boolean badShot = false;
		private boolean failed = true;
		private LivingEntity target;

		public AimedShotEffect(Skill skill, LivingEntity target, long duration) {
			super(skill, "AimedShotEffect", duration);

			this.target = target;
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

			if (badShot)
				broadcast(player.getLocation(), expireTextBadShot, player.getDisplayName());
			else if (failed)
				broadcast(player.getLocation(), expireTextFail, player.getDisplayName());
			else {
				if (target instanceof Monster)
					broadcast(player.getLocation(), expireTextSuccess, player.getDisplayName(), Messaging.getLivingEntityName((Monster) target));
				else if (target instanceof Player)
					broadcast(player.getLocation(), expireTextSuccess, player.getDisplayName(), ((Player) target).getDisplayName());
			}
		}

		public void setFailed(boolean failed) {
			this.failed = failed;
		}

		public void setBadShot(boolean badShot) {
			this.badShot = badShot;
		}

		public LivingEntity getTarget() {
			return target;
		}
	}
}