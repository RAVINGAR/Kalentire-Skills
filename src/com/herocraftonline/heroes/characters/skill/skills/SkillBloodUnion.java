package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.events.SkillUseEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.BloodUnionEffect;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;

public class SkillBloodUnion extends PassiveSkill {

    //ScoreboardManager manager;
    //Scoreboard board;
    //HashMap<Hero, ScoreboardManager> bloodUnionManager;

    public SkillBloodUnion(Heroes plugin) {
        super(plugin, "BloodUnion");
        setDescription("Passive: Your damaging abilities form a Blood Union with your opponents. Blood Union allows you to use certain abilities, and also increases the effectiveness of others. Maximum Blood Union is $1. BloodUnion resets upon switching from monsters to players, and will expire completely if not increased after $2 seconds.");

        //ScoreboardManager manager = Bukkit.getScoreboardManager();
        //manager.getNewScoreboard();

        //bloodUnionManager = new HashMap<Hero, ScoreboardManager>();

        Bukkit.getPluginManager().registerEvents(new BloodUnionListener(this), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.APPLY_TEXT.node(), "");
        node.set(SkillSetting.UNAPPLY_TEXT.node(), "");
        node.set(SkillSetting.PERIOD.node(), 25000);
        node.set("max-blood-union", 4);

        return node;
    }

    @Override
    public String getDescription(Hero hero) {

        double period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 25000, false) / 1000;
        int maxBloodUnion = SkillConfigManager.getUseSetting(hero, this, "max-blood-union", 4, false);

        return getDescription().replace("$1", maxBloodUnion + "").replace("$2", period + "");
    }

    private class BloodUnionListener implements Listener {
        private Skill skill;

        public BloodUnionListener(Skill skill) {
            this.skill = skill;

        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onSkillUse(SkillUseEvent event) {
            Hero hero = event.getHero();

            if (hero.canUseSkill(skill)) {
                addBloodUnionEffect(hero);
            }
            else {
                removeBloodUnionEffect(hero);
            }
        }
    }

    public void addBloodUnionEffect(Hero hero) {
        if (!(hero.hasEffect("BloodUnionEffect"))) {
            int bloodUnionResetPeriod = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 25000, false);
            int maxBloodUnion = SkillConfigManager.getUseSetting(hero, this, "max-blood-union", 4, false);
            hero.addEffect(new BloodUnionEffect(this, bloodUnionResetPeriod, maxBloodUnion));
        }
    }

    public void removeBloodUnionEffect(Hero hero) {
        if (hero.hasEffect("BloodUnionEffect")) {
            hero.removeEffect(hero.getEffect("BloodUnionEffect"));
        }

    }
}