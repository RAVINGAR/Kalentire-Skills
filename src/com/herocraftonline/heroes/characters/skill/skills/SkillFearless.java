package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.SkillDamageEvent;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.common.FormEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Setting;
import com.herocraftonline.heroes.util.Util;

public class SkillFearless extends ActiveSkill {

    private String expireText;

    public SkillFearless(Heroes plugin) {
        super(plugin, "Fearless");
        setDescription("You are fearless, dealing $1% more damage but taking $2% more damage!");
        setUsage("/skill fearless");
        setArgumentRange(0, 0);
        setIdentifiers("skill fearless");
        setTypes(SkillType.BUFF, SkillType.PHYSICAL);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillHeroListener(this), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("incoming-multiplier", 1.1);
        node.set("outgoing-multiplier", 1.1);
        node.set("multiplier-per-level", .005);
        node.set(Setting.USE_TEXT.node(), "%hero% is fearless!");
        node.set(Setting.EXPIRE_TEXT.node(), "%hero% is no longer fearless!");
        return node;
    }

    @Override
    public void init() {
        super.init();
        expireText = SkillConfigManager.getRaw(this, Setting.EXPIRE_TEXT, "%hero% is no longer fearless!").replace("%hero%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        if (hero.hasEffect("Fearless")) {
            hero.removeEffect(hero.getEffect("Fearless"));
            return SkillResult.REMOVED_EFFECT;
        }
        hero.addEffect(new FearlessEffect(this));
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.ENDERDRAGON_GROWL , 0.5F, 0.1F);
        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

    public class FearlessEffect extends FormEffect {
        public FearlessEffect(Skill skill) {
            super(skill, "Fearless");
            types.add(EffectType.PHYSICAL);
            //TODO Fix this so its just the particles//addMobEffect(5, (int) (1000) * 20, 127, false);
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
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
                Hero hero = plugin.getCharacterManager().getHero((Player) event.getEntity());
                if (hero.hasEffect(getName())) {
                    event.setDamage((int) (event.getDamage() * SkillConfigManager.getUseSetting(hero, skill, "incoming-multiplier", 1.1, true)));
                }
            }

            //TODO: store multiplier directly onto the effect so we can use it on monsters, re-check it on levelup etc.
            if (event.getSkill().isType(SkillType.PHYSICAL) && event.getDamager() instanceof Hero) {
                Hero hero = (Hero) event.getDamager();
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
                Hero hero = plugin.getCharacterManager().getHero((Player) event.getEntity());
                if (hero.hasEffect(getName())) {
                    event.setDamage((int) (event.getDamage() * SkillConfigManager.getUseSetting(hero, skill, "incoming-multiplier", 1.1, true)));
                }
            }

            if (event.getDamager() instanceof Hero) {
                Hero hero = (Hero) event.getDamager();
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
