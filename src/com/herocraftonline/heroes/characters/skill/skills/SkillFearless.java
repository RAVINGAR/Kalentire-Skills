package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
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
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;

public class SkillFearless extends ActiveSkill {
    private String expireText;

    public SkillFearless(Heroes plugin) {
        super(plugin, "Fearless");
        setDescription("You become fearless, gaining $1% more weapon damage. However, your wreckless behavior causes you to take an additional $2% damage from all sources!");
        setUsage("/skill fearless");
        setArgumentRange(0, 0);
        setIdentifiers("skill fearless");
        setTypes(SkillType.BUFF, SkillType.PHYSICAL);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillHeroListener(this), plugin);
    }

    public String getDescription(Hero hero) {
        double incomingMultiplier = SkillConfigManager.getUseSetting(hero, this, "incoming-multiplier", 1.1D, false);
        double outgoingMultiplier = SkillConfigManager.getUseSetting(hero, this, "outgoing-multiplier", 1.1D, false);
        double multPerLevel = SkillConfigManager.getUseSetting(hero, this, "multiplier-per-level", 0.005D, false);

        int level = hero.getSkillLevel(this);

        if (level < 0)
            level = 0;

        double levelMult = level * multPerLevel;
        outgoingMultiplier = (outgoingMultiplier + levelMult - 1) * 100;
        incomingMultiplier = (incomingMultiplier - 1) * 100;

        return getDescription().replace("$1", outgoingMultiplier + "").replace("$2", incomingMultiplier + "");
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("incoming-multiplier", Double.valueOf(1.1D));
        node.set("outgoing-multiplier", Double.valueOf(1.1D));
        node.set("multiplier-per-level", Double.valueOf(0.005D));
        node.set(SkillSetting.USE_TEXT.node(), "§7[§2Skill§7] %hero% is fearless!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "§7[§2Skill§7] %hero% is no longer fearless!");

        return node;
    }

    public void init() {
        super.init();
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "§7[§2Skill§7] %hero% is no longer fearless!").replace("%hero%", "$1");
    }

    public SkillResult use(Hero hero, String[] args) {
        if (hero.hasEffect("FearlessEffect")) {
            hero.removeEffect(hero.getEffect("FearlessEffect"));
            return SkillResult.REMOVED_EFFECT;
        }

        broadcastExecuteText(hero);

        hero.addEffect(new FearlessEffect(this));
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.ENDERDRAGON_GROWL, 0.5F, 0.1F);

        return SkillResult.NORMAL;
    }

    public class SkillHeroListener implements Listener {
        private Skill skill;

        public SkillHeroListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onSkillDamage(SkillDamageEvent event) {

            if ((event.getEntity() instanceof Player)) {
                Hero hero = plugin.getCharacterManager().getHero((Player) event.getEntity());
                if (hero.hasEffect("FearlessEffect")) {
                    double damageMult = SkillConfigManager.getUseSetting(hero, skill, "incoming-multiplier", 1.1D, false);
                    double newDamage = (damageMult * event.getDamage());

                    event.setDamage(newDamage);
                }
            }

            // FEARLESS SKILL DAMAGE INCREASE DISABLED
            /*
            if ((event.getSkill().isType(SkillType.PHYSICAL)) && ((event.getDamager() instanceof Hero))) {
            	Hero hero = (Hero) event.getDamager();
            	if (hero.hasEffect("FearlessEffect")) {
            		double levelMult = SkillConfigManager.getUseSetting(hero, skill, "multiplier-per-level", 0.005D, false) * hero.getSkillLevel(skill);
            		double damageMult = SkillConfigManager.getUseSetting(hero, skill, "outgoing-multiplier", 1.1D, false) + levelMult;
            		int newDamage = (int) (damageMult * event.getDamage());
            		event.setDamage(newDamage);
            	}
            }
            */
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onWeaponDamage(WeaponDamageEvent event) {

            if ((event.getEntity() instanceof Player)) {
                Hero hero = plugin.getCharacterManager().getHero((Player) event.getEntity());
                if (hero.hasEffect("FearlessEffect")) {
                    event.setDamage((event.getDamage() * SkillConfigManager.getUseSetting(hero, skill, "incoming-multiplier", 1.1D, false)));
                }
            }

            if ((event.getDamager() instanceof Hero)) {
                Hero hero = (Hero) event.getDamager();
                if (hero.hasEffect("FearlessEffect")) {
                    double levelMult = SkillConfigManager.getUseSetting(hero, skill, "multiplier-per-level", 0.005D, false) * hero.getSkillLevel(skill);
                    double newDamage = (event.getDamage() * (SkillConfigManager.getUseSetting(hero, skill, "outgoing-multiplier", 1.1D, false) + levelMult));
                    event.setDamage(newDamage);
                }
            }
        }
    }

    public class FearlessEffect extends FormEffect {
        public FearlessEffect(Skill skill) {
            super(skill, "FearlessEffect");
            this.types.add(EffectType.PHYSICAL);
        }

        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), expireText, player.getDisplayName());
        }
    }
}