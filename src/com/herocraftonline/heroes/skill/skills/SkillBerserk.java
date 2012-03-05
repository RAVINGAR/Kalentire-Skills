package com.herocraftonline.heroes.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillDamageEvent;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.WeaponDamageEvent;
import com.herocraftonline.heroes.effects.EffectType;
import com.herocraftonline.heroes.effects.common.FormEffect;
import com.herocraftonline.heroes.hero.Hero;
import com.herocraftonline.heroes.skill.ActiveSkill;
import com.herocraftonline.heroes.skill.Skill;
import com.herocraftonline.heroes.skill.SkillConfigManager;
import com.herocraftonline.heroes.skill.SkillType;
import com.herocraftonline.heroes.util.Setting;
import com.herocraftonline.heroes.util.Util;

public class SkillBerserk extends ActiveSkill {

    private String expireText;

    public SkillBerserk(Heroes plugin) {
        super(plugin, "Berserk");
        setDescription("You go berserk, dealing $1% more damage but taking $2% more damage!");
        setUsage("/skill berserk");
        setArgumentRange(0, 0);
        setIdentifiers("skill berserk");
        setTypes(SkillType.BUFF, SkillType.PHYSICAL);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillHeroListener(this), plugin);
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

    public class SkillHeroListener implements Listener {

    	private Skill skill;
    	
    	SkillHeroListener(Skill skill) {
    		this.skill = skill;
    	}
    	
    	@EventHandler()
        public void onSkillDamage(SkillDamageEvent event) {
            if (event.isCancelled()) {
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
        }

    	@EventHandler()
        public void onWeaponDamage(WeaponDamageEvent event) {
            if (event.isCancelled()) {
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
        return getDescription().replace("$1", Util.stringDouble(out)).replace("$2", Util.stringDouble((incom - 1) * 100));
    }
}
