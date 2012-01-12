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

public class SkillBerserk extends ActiveSkill {

    private String expireText;

    public SkillBerserk(Heroes plugin) {
        super(plugin, "Berserk");
        setDescription("You go berserk, dealing $1% more damage but taking $2% more damage!");
        setUsage("/skill berserk");
        setArgumentRange(0, 0);
        setIdentifiers("skill berserk");
        setTypes(SkillType.BUFF, SkillType.PHYSICAL);

        registerEvent(Type.CUSTOM_EVENT, new SkillHeroListener(this), Priority.Normal);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("incoming-multiplier", 1.1);
        node.set("outgoing-multiplier", 1.1);
        node.set("multiplier-per-level", .005);
        node.set(Setting.USE_TEXT.node(), "%hero% goes berserk!");
        node.set(Setting.EXPIRE_TEXT.node(), "%hero% is no longer berserking!");
        return node;
    }

    @Override
    public void init() {
        super.init();
        expireText = SkillConfigManager.getRaw(this, Setting.EXPIRE_TEXT, "%hero% is no longer berserking!").replace("%hero%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        if (hero.hasEffect("Berserk")) {
            hero.removeEffect(hero.getEffect("Berserk"));
            return SkillResult.REMOVED_EFFECT;
        }
        hero.addEffect(new BerserkEffect(this));
        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

    public class BerserkEffect extends FormEffect {
        public BerserkEffect(Skill skill) {
            super(skill, "Berserk");
            types.add(EffectType.PHYSICAL);
        }

        @Override
        public void remove(Hero hero) {
            super.remove(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), expireText, player.getDisplayName());
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
            if (event.isCancelled()) {
                Heroes.debug.stopTask("HeroesSkillListener");
                return;
            }
            
            if (event.getEntity() instanceof Player) {
                Hero hero = plugin.getHeroManager().getHero((Player) event.getEntity());
                if (hero.hasEffect(getName())) {
                    event.setDamage((int) (event.getDamage() * SkillConfigManager.getUseSetting(hero, skill, "incoming-multiplier", 1.1, true)));
                }
            }

            if (event.getDamager() instanceof Player && event.getSkill().isType(SkillType.PHYSICAL)) {
                Hero hero = plugin.getHeroManager().getHero((Player) event.getDamager());
                if (hero.hasEffect(getName())) {
                    double levelMult = SkillConfigManager.getUseSetting(hero, skill, "multiplier-per-level", .005, false) * hero.getSkillLevel(skill);
                    int newDamage = (int) (event.getDamage() * (SkillConfigManager.getUseSetting(hero, skill, "outgoing-multiplier", 1.1, false) + levelMult));
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
                    event.setDamage((int) (event.getDamage() * SkillConfigManager.getUseSetting(hero, skill, "incoming-multiplier", 1.1, true)));
                }
            }

            if (event.getDamager() instanceof Player) {
                Hero hero = plugin.getHeroManager().getHero((Player) event.getDamager());
                if (hero.hasEffect(getName())) {
                    double levelMult = SkillConfigManager.getUseSetting(hero, skill, "multiplier-per-level", .005, false) * hero.getSkillLevel(skill);
                    int newDamage = (int) (event.getDamage() * (SkillConfigManager.getUseSetting(hero, skill, "outgoing-multiplier", 1.1, false) + levelMult));
                    event.setDamage(newDamage);
                }
            }
            Heroes.debug.stopTask("HeroesSkillListener");
        }
    }

    @Override
    public String getDescription(Hero hero) {
        double incom = SkillConfigManager.getUseSetting(hero, this, "incoming-multiplier", 1.1, true);
        double out = SkillConfigManager.getUseSetting(hero, this, "outgoing-multiplier", 1.1, false);
        double perlev = SkillConfigManager.getUseSetting(hero, this, "multiplier-per-level", .005, false);
        int level = hero.getSkillLevel(this);
        if (level < 0)
            level = 0;
        out = (out + (level * perlev) - 1) * 100;
        return getDescription().replace("$1", out + "").replace("$2", (incom - 1) * 100 + "");
    }
}
