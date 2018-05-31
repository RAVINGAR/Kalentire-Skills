package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.common.MaxHealthIncreaseEffect;
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

    public SkillTypicalHuman(Heroes plugin) {
        super(plugin, "TypicalHuman");
        //TODO: set description
        setDescription("Passive: additional 5% damage to physical damage and 5% to health pool.");
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.BUFFING);

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
        if (!(hero.hasEffect("TypicalHumanHealthEffect"))) {
            // code just for reference:
//            int bloodUnionResetPeriod = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 25000, false);
//            int maxBloodUnion = SkillConfigManager.getUseSetting(hero, this, "max-blood-union", 4, false);
//            hero.addEffect(new BloodUnionEffect(this, bloodUnionResetPeriod, maxBloodUnion));

            //FIXME: additional helath not currently in use, need to first work out how to implement % health boost
            double additionalHealth = SkillConfigManager.getUseSetting(hero, this, "additional-health-percent", 0.05, false);

//            TypicalHumanEffect typicalHumanEffect = new Effect(this, "TypicalHumanEffect", EffectType.BENEFICIAL, EffectType.MAX_HEALTH_INCREASING);
            //TODO test adding raw health
            hero.addEffect(new MaxHealthIncreaseEffect(this,"TypicalHumanHealthEffect", hero.getPlayer(), -1, 50));
        }
    }

    public void removeTypicalHumanEffect(Hero hero) {
        if (hero.hasEffect("TypicalHumanHealthEffect")) {
            hero.removeEffect(hero.getEffect("TypicalHumanHealthEffect"));
        }

    }
}