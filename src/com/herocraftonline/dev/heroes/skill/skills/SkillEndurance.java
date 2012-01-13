package com.herocraftonline.dev.heroes.skill.skills;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;

import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.api.HeroesEventListener;
import com.herocraftonline.dev.heroes.api.SkillDamageEvent;
import com.herocraftonline.dev.heroes.api.SkillResult;
import com.herocraftonline.dev.heroes.api.WeaponDamageEvent;
import com.herocraftonline.dev.heroes.effects.EffectType;
import com.herocraftonline.dev.heroes.effects.common.FormEffect;
import com.herocraftonline.dev.heroes.hero.Hero;
import com.herocraftonline.dev.heroes.skill.ActiveSkill;
import com.herocraftonline.dev.heroes.skill.Skill;
import com.herocraftonline.dev.heroes.skill.SkillConfigManager;
import com.herocraftonline.dev.heroes.skill.SkillType;
import com.herocraftonline.dev.heroes.util.Setting;
import com.herocraftonline.dev.heroes.util.Util;

public class SkillEndurance extends ActiveSkill {

    private String expireText;

    public SkillEndurance(Heroes plugin) {
        super(plugin, "Endurance");
        setDescription("You take a defensive form, reducing damage taken by $1%, but reducing damage you deal by $2%.");
        setUsage("/skill endurance");
        setArgumentRange(0, 0);
        setIdentifiers("skill endurance");
        setTypes(SkillType.BUFF, SkillType.PHYSICAL);

        registerEvent(Type.CUSTOM_EVENT, new SkillHeroListener(this), Priority.Normal);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("incoming-multiplier", .9);
        node.set("outgoing-multiplier", .9);
        node.set("multiplier-per-level", .005);
        node.set(Setting.USE_TEXT.node(), "%hero% shifts into a defensive form!");
        node.set(Setting.EXPIRE_TEXT.node(), "%hero% has shifted out of their defensive form!");
        return node;
    }

    @Override
    public void init() {
        super.init();
        expireText = SkillConfigManager.getRaw(this, Setting.EXPIRE_TEXT, "%hero% has shifted out of their defensive form!").replace("%hero%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        if (hero.hasEffect("Endurance")) {
            hero.removeEffect(hero.getEffect("Endurance"));
            return SkillResult.REMOVED_EFFECT;
        }
        hero.addEffect(new EnduranceEffect(this));
        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

    public class EnduranceEffect extends FormEffect {
        public EnduranceEffect(Skill skill) {
            super(skill, "Endurance");
            types.add(EffectType.PHYSICAL);
        }

        @Override
        public void remove(Hero hero) {
            super.remove(hero);
            broadcast(hero.getPlayer().getLocation(), expireText, hero.getPlayer().getDisplayName());
        }
    }

    public class SkillHeroListener extends HeroesEventListener {

    	private Skill skill;
    	
    	SkillHeroListener(Skill skill) {
    		this.skill = skill;
    	}
    	
        @Override
        public void onSkillDamage(SkillDamageEvent event) {
            Heroes.debug.startTask("HeroesSkillListener");
            if (event.isCancelled() || event.getDamage() == 0) {
                Heroes.debug.stopTask("HeroesSkillListener");
                return;
            }

            if (event.getEntity() instanceof Player) {
                Hero hero = plugin.getHeroManager().getHero((Player) event.getEntity());
                if (hero.hasEffect(getName())) {
                    double levelMult = SkillConfigManager.getUseSetting(hero, skill, "multiplier-per-level", .005, false) * hero.getSkillLevel(skill);
                    int newDamage = (int) (event.getDamage() * (SkillConfigManager.getUseSetting(hero, skill, "incoming-multiplier", .9, true) - levelMult));
                    //Never go less than 1
                    if (newDamage == 0)
                        newDamage = 1;
                    event.setDamage(newDamage);
                }
            }

            if (event.getDamager() instanceof Player && event.getSkill().isType(SkillType.PHYSICAL)) {
                Hero hero = plugin.getHeroManager().getHero((Player) event.getDamager());
                if (hero.hasEffect(getName())) {
                    int newDamage = (int) (event.getDamage() * SkillConfigManager.getUseSetting(hero, skill, "outgoing-multiplier", .9, false));
                    if (newDamage == 0)
                        newDamage = 1;
                    event.setDamage(newDamage);
                }
            }
            Heroes.debug.stopTask("HeroesSkillListener");
        }

        @Override
        public void onWeaponDamage(WeaponDamageEvent event) {
            Heroes.debug.startTask("HeroesSkillListener");
            if (event.isCancelled()) {
                Heroes.debug.stopTask("HeroesSkillListener");
                return;
            }

            if (event.getEntity() instanceof Player) {
                Hero hero = plugin.getHeroManager().getHero((Player) event.getEntity());
                if (hero.hasEffect(getName())) {
                    double levelMult = SkillConfigManager.getUseSetting(hero, skill, "multiplier-per-level", .005, false) * hero.getSkillLevel(skill);
                    int newDamage = (int) (event.getDamage() * (SkillConfigManager.getUseSetting(hero, skill, "incoming-multiplier", .9, true) - levelMult));
                    //Always deal at least 1 damage
                    if (newDamage == 0)
                        newDamage = 1;
                    event.setDamage(newDamage);
                }
            }

            if (event.getDamager() instanceof Player) {
                Hero hero = plugin.getHeroManager().getHero((Player) event.getDamager());
                if (hero.hasEffect(getName())) {
                    int newDamage = (int) (event.getDamage() * SkillConfigManager.getUseSetting(hero, skill, "outgoing-multiplier", .9, false));
                    if (newDamage == 0)
                        newDamage = 1;
                    event.setDamage(newDamage);
                }
            }
            Heroes.debug.stopTask("HeroesSkillListener");
        }
    }

    @Override
    public String getDescription(Hero hero) {
        double out = 1 - SkillConfigManager.getUseSetting(hero, this, "outgoing-multiplier", .9, false);
        double inc = 1 - SkillConfigManager.getUseSetting(hero, this, "incoming-multiplier", .9, false);
        double perlev = SkillConfigManager.getUseSetting(hero, this, "multiplier-per-level", .005, false);
        int level = hero.getSkillLevel(this);
        if (level < 0)
            level = 0;
        inc -= (perlev * level);
        return getDescription().replace("$1", Util.stringDouble(inc * 100)).replace("$2", Util.stringDouble(out * 100));
    }
}
