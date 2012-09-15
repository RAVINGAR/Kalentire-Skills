package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Material;
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
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Setting;
import com.herocraftonline.heroes.util.Util;

public class SkillIceblade extends TargettedSkill {
    
    private String applyText;
    private String expireText;
    
    
    public SkillIceblade(Heroes plugin) {
        super(plugin, "Iceblade");
        setDescription("You freeze your target with your weapon, damaging and slowing them for $1 seconds.");
        setUsage("/skill iceblade <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill iceblade");
        setTypes(SkillType.ICE, SkillType.DAMAGING, SkillType.HARMFUL, SkillType.FORCE, SkillType.INTERRUPT);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("weapons", Util.shovels);
        node.set(Setting.MAX_DISTANCE.node(), 6);
        node.set(Setting.DURATION.node(), 5000);
        node.set("amplitude", 4);
        node.set(Setting.APPLY_TEXT.node(), "%target% has been slowed by %hero%'s iceblade!");
        node.set(Setting.EXPIRE_TEXT.node(), "%target% is no longer slowed!");
        node.set("force", 3);
        return node;
    }
    
    @Override
    public void init() {
        applyText = SkillConfigManager.getRaw(this, Setting.APPLY_TEXT, "%target% has been slowed by %hero%'s iceblade!").replace("%target%", "$1").replace("%hero%", "$2");
        expireText = SkillConfigManager.getRaw(this, Setting.EXPIRE_TEXT, "%target% is no longer slowed!").replace("%target%", "$1");
    }
    
    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        Material item = player.getItemInHand().getType();
        if (!SkillConfigManager.getUseSetting(hero, this, "weapons", Util.swords).contains(item.name())) {
            Messaging.send(player, "You can't use iceblade with that weapon!");
            return SkillResult.FAIL;
        }
        
        int force = SkillConfigManager.getUseSetting(hero, this, "force", 0, false);
        int damage = plugin.getDamageManager().getItemDamage(item, player);
        damageEntity(target, player, damage, DamageCause.ENTITY_ATTACK);
        //Do a little knock-up
        target.setVelocity(target.getVelocity().add(new Vector(0, force, 0)));
        //Add the slow effect
        long duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 5000, false);
        int amplitude = SkillConfigManager.getUseSetting(hero, this, "amplitude", 4, false);
        SlowEffect sEffect = new SlowEffect(this, duration, amplitude, false, applyText, expireText, hero);
        plugin.getCharacterManager().getCharacter(target).addEffect(new IcebladeEffect(this, 300, sEffect));
        broadcastExecuteText(hero, target);
        return SkillResult.NORMAL;
    }
    
    public class IcebladeEffect extends ExpirableEffect {

    	private final Effect effect;
		public IcebladeEffect(Skill skill, long duration, Effect afterEffect) {
			super(skill, "Iceblade", duration);
			this.effect = afterEffect;
			this.types.add(EffectType.HARMFUL);
			this.types.add(EffectType.ICE);
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
        int duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 10000, false);
        return getDescription().replace("$1", duration / 1000 + "");
    }
}
