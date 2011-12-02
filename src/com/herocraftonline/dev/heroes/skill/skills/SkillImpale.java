package com.herocraftonline.dev.heroes.skill.skills;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Creature;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.api.SkillResult;
import com.herocraftonline.dev.heroes.classes.HeroClass;
import com.herocraftonline.dev.heroes.effects.Effect;
import com.herocraftonline.dev.heroes.effects.EffectType;
import com.herocraftonline.dev.heroes.effects.ExpirableEffect;
import com.herocraftonline.dev.heroes.effects.common.SlowEffect;
import com.herocraftonline.dev.heroes.hero.Hero;
import com.herocraftonline.dev.heroes.skill.Skill;
import com.herocraftonline.dev.heroes.skill.SkillType;
import com.herocraftonline.dev.heroes.skill.TargettedSkill;
import com.herocraftonline.dev.heroes.util.Messaging;
import com.herocraftonline.dev.heroes.util.Setting;
import com.herocraftonline.dev.heroes.util.Util;

public class SkillImpale extends TargettedSkill {
    
    private String applyText;
    private String expireText;
    
    
    public SkillImpale(Heroes plugin) {
        super(plugin, "Impale");
        setDescription("You attempt to impale your target on your weapon, tossing them up in the air momentarily.");
        setUsage("/skill impale <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill impale");
        setTypes(SkillType.PHYSICAL, SkillType.DAMAGING, SkillType.HARMFUL, SkillType.FORCE);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("weapons", Util.shovels);
        node.set(Setting.MAX_DISTANCE.node(), 6);
        node.set(Setting.DURATION.node(), 5000);
        node.set("amplitude", 4);
        node.set(Setting.APPLY_TEXT.node(), "%target% has been slowed by %hero%'s impale!");
        node.set(Setting.EXPIRE_TEXT.node(), "%target% is no longer slowed!");
        node.set("force", 3);
        return node;
    }
    
    @Override
    public void init() {
        applyText = getSetting(null, Setting.APPLY_TEXT.node(), "%target% has been slowed by %hero%'s impale!").replace("%target%", "$1").replace("%hero%", "$2");
        expireText = getSetting(null, Setting.EXPIRE_TEXT.node(), "%target% is no longer slowed!").replace("%target%", "$1");
    }
    
    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        Material item = player.getItemInHand().getType();
        if (!getSetting(hero, "weapons", Util.swords).contains(item.name())) {
            Messaging.send(player, "You can't use impale with that weapon!");
            return SkillResult.FAIL;
        }

        HeroClass heroClass = hero.getHeroClass();
        int force = getSetting(hero, "force", 3, false);
        int damage = heroClass.getItemDamage(item) == null ? 0 : heroClass.getItemDamage(item);
        target.damage(damage, player);
        //Do a little knockup
        target.setVelocity(target.getVelocity().add(new Vector(0, force, 0)));
        //Add the slow effect
        long duration = getSetting(hero, Setting.DURATION.node(), 5000, false);
        int amplitude = getSetting(hero, "amplitude", 4, false);
        SlowEffect sEffect = new SlowEffect(this, duration, amplitude, false, applyText, expireText, hero);
        if (target instanceof Player) {
            ImpaleEffect iEffect = new ImpaleEffect(this, 300, sEffect);
            plugin.getHeroManager().getHero((Player) target).addEffect(iEffect);
        } else if (target instanceof Creature) {
            plugin.getEffectManager().addCreatureEffect((Creature) target, sEffect);
        }
        
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
		
		public void remove(Hero hero) {
			hero.addEffect(effect);
		}
    }
}
