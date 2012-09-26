package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.SkillUseEvent;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.DelayedSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Setting;

public class SkillDivineStun extends TargettedSkill {
	
	private String applyText;
	private String expireText;
	
	public SkillDivineStun(Heroes plugin) {
		super(plugin, "DivineStun");
		setDescription("You stun your target for $1 seconds, preventing them from using skills or moving and dealing $2 damage");
		setUsage("/skill divinestun <target>");
		setArgumentRange(0, 1);
        setIdentifiers("skill divinestun");
        setTypes(SkillType.LIGHT, SkillType.SILENCABLE, SkillType.DEBUFF, SkillType.DAMAGING, SkillType.HARMFUL, SkillType.INTERRUPT);
        Bukkit.getServer().getPluginManager().registerEvents(new DivineStunListener(), plugin);
	}
	
	@Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(Setting.DURATION.node(), 5000);
        node.set(Setting.DAMAGE.node(), 50);
        node.set(Setting.APPLY_TEXT.node(), "%target% has been stunned!");
        node.set(Setting.EXPIRE_TEXT.node(), "%target% is no longer stunned!");
        return node;
    }
	
	@Override
	public void init() {
		super.init();
		applyText = SkillConfigManager.getRaw(this, Setting.APPLY_TEXT, "%target% has been stunned!").replace("%target%", "$1");
        expireText = SkillConfigManager.getRaw(this, Setting.EXPIRE_TEXT, "%target% is no longer stunned!").replace("%target%", "$1");

	}

	@Override
	public SkillResult use(Hero hero, LivingEntity target, String[] args) {
		int duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 5000, false);
		int damage = SkillConfigManager.getUseSetting(hero, this, Setting.DAMAGE, 50, false);
		plugin.getCharacterManager().getCharacter(target).addEffect(new DivineStunEffect(duration));
		damageEntity(target, hero.getEntity(), damage);
		return SkillResult.NORMAL;
	}

	@Override
	public String getDescription(Hero hero) {
		int duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 5000, false);
		int damage = SkillConfigManager.getUseSetting(hero, this, Setting.DAMAGE, 50, false);
		return getDescription().replace("$1", (duration / 1000) + "").replace("$2", damage + "");
	}
	
	private class DivineStunEffect extends ExpirableEffect {
		
		private String applyText = SkillDivineStun.this.applyText;
		private String expireText = SkillDivineStun.this.expireText;
		
		public DivineStunEffect(int duration) {
			super(SkillDivineStun.this, "DivineStun", duration);
			this.types.add(EffectType.DISPELLABLE);
	        this.types.add(EffectType.ROOT);
	        this.types.add(EffectType.HARMFUL);
	        this.types.add(EffectType.SILENCE);
	        int effectDuration = (int) duration / 1000 * 20;
	        this.addMobEffect(2, effectDuration, 5, false);
	        this.addMobEffect(8, effectDuration, -5, false);
		}
		
		@Override
	    public void applyToMonster(Monster monster) {
	        super.applyToMonster(monster);
	        broadcast(monster.getEntity().getLocation(), applyText, Messaging.getLivingEntityName(monster));
	    }

	    @Override
	    public void applyToHero(Hero hero) {
	        super.applyToHero(hero);
	        DelayedSkill dSkill = hero.getDelayedSkill();
	        if (dSkill != null && dSkill.getSkill().isType(SkillType.SILENCABLE)) {
	            hero.cancelDelayedSkill();
	        }
	        Player player = hero.getPlayer();
	        broadcast(player.getLocation(), applyText, player.getDisplayName());
	    }

	    @Override
	    public void removeFromMonster(Monster monster) {
	        super.removeFromMonster(monster);
	        broadcast(monster.getEntity().getLocation(), expireText, Messaging.getLivingEntityName(monster));
	    }

	    @Override
	    public void removeFromHero(Hero hero) {
	        super.removeFromHero(hero);
	        Player player = hero.getPlayer();
	        broadcast(player.getLocation(), expireText, player.getDisplayName());
	    }
	}
	
	private class DivineStunListener implements Listener {
		
		@EventHandler
		public void onSkillUse(SkillUseEvent event) {
			if(event.getHero().hasEffect("DivineStun")) {
				event.setCancelled(true);
			}
		}
		
		@EventHandler
		public void onWeaponDamage(WeaponDamageEvent event) {
			if(event.getDamager().hasEffect("DivineStun")) {
				event.setCancelled(true);
			}
		}
		
	}

}
