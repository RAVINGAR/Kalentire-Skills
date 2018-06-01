package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.characters.effects.MaxHealthPercentIncreaseEffect;
import com.herocraftonline.heroes.characters.skill.*;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.events.SkillUseEvent;
import com.herocraftonline.heroes.characters.Hero;

public class SkillTypicalHuman extends PassiveSkill {

    private static final String TYPICAL_HUMAN_HEALTH_EFFECT_NAME = "TypicalHumanHealthEffect";

    public SkillTypicalHuman(Heroes plugin) {
        super(plugin, "TypicalHuman");
        //TODO: set description
        setDescription("Passive: additional 5% damage to physical damage and 5% to health pool.");
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.BUFFING, SkillType.MAX_HEALTH_INCREASING);

        Bukkit.getPluginManager().registerEvents(new TypicalHumanListener(this), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.APPLY_TEXT.node(), "");
        node.set(SkillSetting.UNAPPLY_TEXT.node(), "");
        node.set("additional-physical-damage-percent", 0.05);
        node.set("additional-health-percent", 0.05);

        return node;
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

    private class TypicalHumanListener implements Listener {
        private Skill skill;

        public TypicalHumanListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onSkillUse(SkillUseEvent event) {
            Hero hero = event.getHero();

            if (hero.canUseSkill(skill)) {
                addTypicalHumanEffect(hero);
            }
            else {
                removeTypicalHumanEffect(hero);
            }
        }
    }

    public void addTypicalHumanEffect(Hero hero) {
        if (!(hero.hasEffect(TYPICAL_HUMAN_HEALTH_EFFECT_NAME))) {

            //FIXME: additional health not currently in use, need to first work out how to implement % health boost
            double additionalHealth = SkillConfigManager.getUseSetting(hero, this, "additional-health-percent", 0.05, false);

//            TypicalHumanEffect typicalHumanEffect = new Effect(this, "TypicalHumanEffect", EffectType.BENEFICIAL, EffectType.MAX_HEALTH_INCREASING);
            //TODO test adding raw health
//            hero.addEffect(new MaxHealthIncreaseEffect(this,"TypicalHumanHealthEffect", hero.getPlayer(), -1, 50));

            //FIXME: remove try catch when MaxHealthPercentIncreaseEffect is recognised. (It exists in another repository, therefore this is just to catch existence issues)
//            try {
                hero.addEffect(new MaxHealthPercentIncreaseEffect(this, TYPICAL_HUMAN_HEALTH_EFFECT_NAME, additionalHealth));
//            } catch (Exception e){
//                 Catch exceptions if "MaxHealthPercentIncreaseEffect" isn't a defined class
//            }
        }
    }

    public void removeTypicalHumanEffect(Hero hero) {
        if (hero.hasEffect(TYPICAL_HUMAN_HEALTH_EFFECT_NAME)) {
            hero.removeEffect(hero.getEffect(TYPICAL_HUMAN_HEALTH_EFFECT_NAME));
        }

    }
}