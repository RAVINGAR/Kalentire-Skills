package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.Vector;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.effects.common.SlowEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

public class SkillImpale extends TargettedSkill {
    
    private String applyText;
    private String expireText;
    
    
    public SkillImpale(Heroes plugin) {
        super(plugin, "Impale");
        setDescription("You impale your target with your weapon, tossing them up in the air momentarily and slowing them for $1 seconds.");
        setUsage("/skill impale");
        setArgumentRange(0, 0);
        setIdentifiers("skill impale");
        setTypes(SkillType.PHYSICAL, SkillType.DAMAGING, SkillType.HARMFUL, SkillType.FORCE, SkillType.INTERRUPT);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("weapons", Util.shovels);
        node.set(SkillSetting.MAX_DISTANCE.node(), 6);
        node.set(SkillSetting.DURATION.node(), 5000);
        node.set("amplitude", 4);
        node.set(SkillSetting.APPLY_TEXT.node(), "%target% has been slowed by %hero%'s impale!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "%target% is no longer slowed!");
        node.set("force", 3);
        return node;
    }
    
    @Override
    public void init() {
        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, "%target% has been slowed by %hero%'s impale!").replace("%target%", "$1").replace("%hero%", "$2");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "%target% is no longer slowed!").replace("%target%", "$1");
    }
    
    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        Material item = player.getItemInHand().getType();
        if (!SkillConfigManager.getUseSetting(hero, this, "weapons", Util.swords).contains(item.name())) {
            Messaging.send(player, "You can't use impale with that weapon!");
            return SkillResult.FAIL;
        }
        
        int force = SkillConfigManager.getUseSetting(hero, this, "force", 3, false);
        int damage = plugin.getDamageManager().getItemDamage(item, player);
        damageEntity(target, player, damage, DamageCause.ENTITY_ATTACK);
        //Do a little knockup
        target.setVelocity(target.getVelocity().add(new Vector(0, force, 0)));
        //Add the slow effect
        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
        int amplitude = SkillConfigManager.getUseSetting(hero, this, "amplitude", 4, false);
        SlowEffect sEffect = new SlowEffect(this, duration, amplitude, false, applyText, expireText, hero);
        plugin.getCharacterManager().getCharacter(target).addEffect(new ImpaleEffect(this, 300, sEffect));
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.HURT , 0.8F, 1.0F); 
        broadcastExecuteText(hero, target);
        return SkillResult.NORMAL;
    }
    
    public class ImpaleEffect extends ExpirableEffect {

    	private final Effect effect;
		public ImpaleEffect(Skill skill, long duration, Effect afterEffect) {
			super(skill, "Impale", duration);
			this.effect = afterEffect;
			this.types.add(EffectType.HARMFUL);
			this.types.add(EffectType.DISABLE);
			this.types.add(EffectType.SLOW);
			addMobEffect(2, (int) (duration / 1000) * 20, 20, false);
		}
		
		@Override
		public void removeFromHero(Hero hero) {
		    super.removeFromHero(hero);
			hero.addEffect(effect);
		}
		
		@Override
		public void removeFromMonster(Monster monster) {
		    super.removeFromMonster(monster);
		    monster.addEffect(effect);
		}
    }
    
    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
        return getDescription().replace("$1", duration / 1000 + "");
    }
}
