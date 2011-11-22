package com.herocraftonline.dev.heroes.skill.skills;

import org.bukkit.entity.Player;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.util.config.ConfigurationNode;

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
import com.herocraftonline.dev.heroes.skill.SkillType;
import com.herocraftonline.dev.heroes.util.Setting;

public class SkillEndurance extends ActiveSkill {

    private String expireText;

    public SkillEndurance(Heroes plugin) {
        super(plugin, "Endurance");
        setDescription("You shift into a defensive form!");
        setUsage("/skill endurance");
        setArgumentRange(0, 0);
        setIdentifiers("skill endurance");
        setTypes(SkillType.BUFF, SkillType.PHYSICAL);

        registerEvent(Type.CUSTOM_EVENT, new SkillHeroListener(this), Priority.Normal);
    }

    @Override
    public ConfigurationNode getDefaultConfig() {
        ConfigurationNode node = super.getDefaultConfig();
        node.setProperty("incoming-multiplier", .9);
        node.setProperty("outgoing-multiplier", .9);
        node.setProperty("multiplier-per-level", .005);
        node.setProperty(Setting.USE_TEXT.node(), "%hero% shifts into a defensive form!");
        node.setProperty(Setting.EXPIRE_TEXT.node(), "%hero% has shifted out of their defensive form!");
        return node;
    }

    @Override
    public void init() {
        super.init();
        expireText = getSetting(null, Setting.EXPIRE_TEXT.node(), "%hero% has shifted out of their defensive form!").replace("%hero%", "$1");
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
                    double levelMult = getSetting(hero, "multiplier-per-level", .005, false) * hero.getLevel(skill);
                    int newDamage = (int) (event.getDamage() * (getSetting(hero, "incoming-multiplier", .9, true) - levelMult));
                    //Never go less than 1
                    if (newDamage == 0)
                        newDamage = 1;
                    event.setDamage(newDamage);
                }
            }

            if (event.getDamager() instanceof Player && event.getSkill().isType(SkillType.PHYSICAL)) {
                Hero hero = plugin.getHeroManager().getHero((Player) event.getDamager());
                if (hero.hasEffect(getName())) {
                    int newDamage = (int) (event.getDamage() * getSetting(hero, "outgoing-multiplier", .9, false));
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
                    double levelMult = getSetting(hero, "multiplier-per-level", .005, false) * hero.getLevel(skill);
                    int newDamage = (int) (event.getDamage() * (getSetting(hero, "incoming-multiplier", .9, true) - levelMult));
                    //Always deal at least 1 damage
                    if (newDamage == 0)
                        newDamage = 1;
                    event.setDamage(newDamage);
                }
            }

            if (event.getDamager() instanceof Player) {
                Hero hero = plugin.getHeroManager().getHero((Player) event.getDamager());
                if (hero.hasEffect(getName())) {
                    int newDamage = (int) (event.getDamage() * getSetting(hero, "outgoing-multiplier", .9, false));
                    if (newDamage == 0)
                        newDamage = 1;
                    event.setDamage(newDamage);
                }
            }
            Heroes.debug.stopTask("HeroesSkillListener");
        }
    }
}
