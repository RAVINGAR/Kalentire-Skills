package com.herocraftonline.heroes.characters.skill.unusedskills;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityShootBowEvent;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.common.ImbueEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;

public class SkillHeartshot extends ActiveSkill {
	
	public SkillHeartshot(Heroes plugin) {
		super(plugin, "HeartShot");
		setDescription("Your bow fires arrows straight at the heart, dealing $1 extra damage but costing $2 mana per shot!");
		setUsage("/skill heartshot");
		setArgumentRange(0, 0);
		setIdentifiers("skill heartshot", "skill hshot");
		setTypes(SkillType.BUFFING);
		plugin.getServer().getPluginManager().registerEvents(new HeartShotListener(this), plugin);
	}

	@Override
	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection conf = super.getDefaultConfig();
		conf.set(SkillSetting.MANA.node(), 20);
		conf.set(SkillSetting.COOLDOWN.node(), 3000);
		conf.set(SkillSetting.APPLY_TEXT.node(), ChatColor.GRAY.toString() + "%hero% imbused their weapon with the power of HeartShot!");
		conf.set(SkillSetting.DAMAGE.node(), 50);
		conf.set(SkillSetting.DAMAGE_INCREASE.node(), 0);
		conf.set("mana-per-shot", 20);
		return conf;
	}
	
	@Override
	public SkillResult use(Hero hero, String[] args) {
		if(hero.hasEffect("HeartShotBuff")) {
			hero.removeEffect(hero.getEffect("HeartShotBuff"));
			return SkillResult.SKIP_POST_USAGE;
		}
		hero.addEffect(new HeartShotBuff(this));
		broadcastExecuteText(hero);
		return SkillResult.NORMAL;
	}

	@Override
	public String getDescription(Hero hero) {
		Integer damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 50, true);
		Integer manaCost = SkillConfigManager.getUseSetting(hero, this, "mana-per-shot", 20, true);
		return getDescription().replace("$1", damage.toString()).replace("$2", manaCost.toString());
	}
	
	public class HeartShotBuff extends ImbueEffect {

		public HeartShotBuff(Skill skill) {
			super(skill, "HeartShotBuff");
			
			types.add(EffectType.BENEFICIAL);
		}
		
	}
	
	public class HeartShotListener implements Listener {
		
		private final Skill skill;
		
		public HeartShotListener(Skill skill) {
			this.skill = skill;
		}
		
	    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	    public void onEntityShootBow(EntityShootBowEvent event) {
	      if (!(event.getEntity() instanceof Player) || !(event.getProjectile() instanceof Arrow)) return;
	      Hero hero = plugin.getCharacterManager().getHero((Player) event.getEntity());
	      if (hero.hasEffect("HeartShotBuff")) {
	    	  int mana = SkillConfigManager.getUseSetting(hero, skill, "mana-per-shot", 20, true);
	    	  if(hero.getMana() < mana) hero.removeEffect(hero.getEffect("HeartShotBuff"));
	    	  else hero.setMana(hero.getMana() - mana);
	      }
	    }
		
		@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
		public void onEntityDamage(EntityDamageEvent event) {
			
			if(!(event.getEntity() instanceof LivingEntity)) return;
			if(!(event instanceof EntityDamageByEntityEvent)) return;
			
			LivingEntity target = (LivingEntity) event.getEntity();
			
			EntityDamageByEntityEvent subEvent = (EntityDamageByEntityEvent) event;
			
			if (!(subEvent.getDamager() instanceof Arrow)) return;
			
			Arrow arrow = (Arrow) subEvent.getDamager();
			if(!(arrow.getShooter() instanceof Player)) return;
			
			Player player = (Player) arrow.getShooter();
			Hero hero = plugin.getCharacterManager().getHero(player);
			
			if(hero.hasEffect("HeartShotBuff")) {
				if(!damageCheck(player, target)) return;
				double damage = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE, 50, false);
				damage += (SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE_INCREASE, 0, false) * hero.getSkillLevel(skill));
				damageEntity(target, player, damage, DamageCause.MAGIC, false);
			}
			
		}
	}
}
