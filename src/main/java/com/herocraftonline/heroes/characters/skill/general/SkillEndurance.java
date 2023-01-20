package com.herocraftonline.heroes.characters.skill.general;

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
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class SkillEndurance extends ActiveSkill {

    private String expireText;

    public SkillEndurance(final Heroes plugin) {
        super(plugin, "Endurance");
        setDescription("You take a defensive form, reducing damage taken by $1%, but reducing damage you deal by $2%.");
        setUsage("/skill endurance");
        setArgumentRange(0, 0);
        setIdentifiers("skill endurance");
        setTypes(SkillType.BUFFING, SkillType.ABILITY_PROPERTY_PHYSICAL);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillHeroListener(this), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();
        node.set("incoming-multiplier", .9);
        node.set("outgoing-multiplier", .9);
        node.set("multiplier-per-level", .005);
        node.set(SkillSetting.USE_TEXT.node(), "%hero% shifts into a defensive form!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "%hero% has shifted out of their defensive form!");
        return node;
    }

    @Override
    public void init() {
        super.init();
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "%hero% has shifted out of their defensive form!").replace("%hero%", "$1").replace("$hero$", "$1");
    }

    @Override
    public SkillResult use(final Hero hero, final String[] args) {
        if (hero.hasEffect("Endurance")) {
            hero.removeEffect(hero.getEffect("Endurance"));
            return SkillResult.REMOVED_EFFECT;
        }
        hero.addEffect(new EnduranceEffect(this));
        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(final Hero hero) {
        final double out = 1 - SkillConfigManager.getUseSetting(hero, this, "outgoing-multiplier", .9, false);
        double inc = 1 - SkillConfigManager.getUseSetting(hero, this, "incoming-multiplier", .9, false);
        final double perlev = SkillConfigManager.getUseSetting(hero, this, "multiplier-per-level", .005, false);
        int level = hero.getHeroLevel(this);
        if (level < 0) {
            level = 0;
        }
        inc -= (perlev * level);
        return getDescription().replace("$1", Util.stringDouble(inc * 100)).replace("$2", Util.stringDouble(out * 100));
    }

    public class EnduranceEffect extends FormEffect {
        public EnduranceEffect(final Skill skill) {
            super(skill, "Endurance");
            types.add(EffectType.PHYSICAL);
        }

        @Override
        public void removeFromHero(final Hero hero) {
            super.removeFromHero(hero);
            broadcast(hero.getPlayer().getLocation(), "    " + expireText, hero.getPlayer().getName());
        }
    }

    public class SkillHeroListener implements Listener {

        private final Skill skill;

        SkillHeroListener(final Skill skill) {
            this.skill = skill;
        }

        @EventHandler()
        public void onSkillDamage(final SkillDamageEvent event) {
            if (event.isCancelled() || event.getDamage() == 0) {
                return;
            }

            if (event.getEntity() instanceof Player) {
                final Hero hero = plugin.getCharacterManager().getHero((Player) event.getEntity());
                if (hero.hasEffect(getName())) {
                    final double levelMult = SkillConfigManager.getUseSetting(hero, skill, "multiplier-per-level", .005, false) * hero.getHeroLevel(skill);
                    int newDamage = (int) (event.getDamage() * (SkillConfigManager.getUseSetting(hero, skill, "incoming-multiplier", .9, true) - levelMult));
                    //Never go less than 1
                    if (newDamage == 0) {
                        newDamage = 1;
                    }
                    event.setDamage(newDamage);
                }
            }

            //TODO: change to allow monsters
            if (event.getSkill().isType(SkillType.ABILITY_PROPERTY_PHYSICAL) && event.getDamager() instanceof Hero) {
                final Hero hero = (Hero) event.getDamager();
                if (hero.hasEffect(getName())) {
                    int newDamage = (int) (event.getDamage() * SkillConfigManager.getUseSetting(hero, skill, "outgoing-multiplier", .9, false));
                    if (newDamage == 0) {
                        newDamage = 1;
                    }
                    event.setDamage(newDamage);
                }
            }
        }

        @EventHandler()
        public void onWeaponDamage(final WeaponDamageEvent event) {
            if (event.isCancelled()) {
                return;
            }

            if (event.getEntity() instanceof Player) {
                final Hero hero = plugin.getCharacterManager().getHero((Player) event.getEntity());
                if (hero.hasEffect(getName())) {
                    final double levelMult = SkillConfigManager.getUseSetting(hero, skill, "multiplier-per-level", .005, false) * hero.getHeroLevel(skill);
                    int newDamage = (int) (event.getDamage() * (SkillConfigManager.getUseSetting(hero, skill, "incoming-multiplier", .9, true) - levelMult));
                    //Always deal at least 1 damage
                    if (newDamage == 0) {
                        newDamage = 1;
                    }
                    event.setDamage(newDamage);
                }
            }

            if (event.getDamager() instanceof Hero) {
                final Hero hero = (Hero) event.getDamager();
                if (hero.hasEffect(getName())) {
                    int newDamage = (int) (event.getDamage() * SkillConfigManager.getUseSetting(hero, skill, "outgoing-multiplier", .9, false));
                    if (newDamage == 0) {
                        newDamage = 1;
                    }
                    event.setDamage(newDamage);
                }
            }
        }
    }
}
