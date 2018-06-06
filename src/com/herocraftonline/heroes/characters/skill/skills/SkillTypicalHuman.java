package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.events.SkillDamageEvent;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.MaxHealthPercentIncreaseEffect;
import com.herocraftonline.heroes.characters.skill.*;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class SkillTypicalHuman extends PassiveSkill {

    public SkillTypicalHuman(Heroes plugin) {
        super(plugin, "TypicalHuman");
        //commented out as this skill currently on provides damage boost
//        setDescription("Passive: additional $1% damage to physical damage and $2% to health pool.");
        setDescription("Passive: additional $1% damage to physical damage.");
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
        double additionalPhysicalDamagePercent = SkillConfigManager.getUseSetting(hero,SkillTypicalHuman.this, "additional-physical-damage-percent", 0.05, false);
//        double additionalHealth = SkillConfigManager.getUseSetting(hero, this, "additional-health-percent", 0.05, false);

//        return getDescription().replace("$1",(additionalPhysicalDamagePercent*100) + "").replace("$2",(additionalHealth*100) + "");
        return getDescription().replace("$1",(additionalPhysicalDamagePercent*100) + "");
    }

    private class TypicalHumanListener implements Listener {
        private Skill skill;

        public TypicalHumanListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onSkillDamage(SkillDamageEvent event) {
            if (event.getDamage() == 0)
                return;

            //boost only physical damage
            Skill skill = event.getSkill();
            if (!skill.isType(SkillType.ABILITY_PROPERTY_PHYSICAL)){
                return;
            }

            // Handle outgoing
            if (event.getDamager() instanceof Hero) {
                Hero hero = (Hero) event.getDamager();

                if (hero.hasEffect(getName())) {
                    double additionalPhysicalDamagePercent = SkillConfigManager.getUseSetting(hero,
                            SkillTypicalHuman.this, "additional-physical-damage-percent", 0.05, false);

                    double originalDamage = event.getDamage();
                    event.setDamage(originalDamage * (1 + additionalPhysicalDamagePercent));
                }
            }
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onWeaponDamage(WeaponDamageEvent event) {
            if (event.getDamage() == 0)
                return;

            // Handle outgoing
            if (event.getDamager() instanceof Hero) {
                Hero hero = (Hero) event.getDamager();

                if (hero.hasEffect(getName())) {
                    double additionalPhysicalDamagePercent = SkillConfigManager.getUseSetting(hero,
                            SkillTypicalHuman.this, "additional-physical-damage-percent", 0.05, false);

                    double originalDamage = event.getDamage();
                    event.setDamage(originalDamage * (1 + additionalPhysicalDamagePercent));
                }
            }
        }

    }

    @Override
    protected void apply(Hero hero) {
        //super.apply(hero);
        addTypicalHumanEffect(hero);
        hero.resolveMaxHealth();
    }

    @Override
    protected void unapply(Hero hero) {
        // Remove effect
        super.unapply(hero);
        hero.resolveMaxHealth();
    }

    private void addTypicalHumanEffect(Hero hero) {
        //FIXME: need to first work out how to implement % health boost, as currently method doesn't seem to work
        //For reference this effect's health is applied in Hero.resolveMaxHealth()
        double additionalHealth = SkillConfigManager.getUseSetting(hero, this, "additional-health-percent", 0.05, false);

//            TypicalHumanEffect typicalHumanEffect = new Effect(this, "TypicalHumanEffect", EffectType.BENEFICIAL, EffectType.MAX_HEALTH_INCREASING);
        //test adding raw health
//            hero.addEffect(new MaxHealthIncreaseEffect(this,"TypicalHumanHealthEffect", hero.getPlayer(), -1, 50));

        Effect healthBoostEffect = new MaxHealthPercentIncreaseEffect(this, this.getName(), additionalHealth);
        healthBoostEffect.setPersistent(true);
        hero.addEffect(healthBoostEffect);
    }

}