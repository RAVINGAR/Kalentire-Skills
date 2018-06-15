package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.events.SkillDamageEvent;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.skill.*;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class SkillSinisterUrge extends PassiveSkill {

    private static final double DEFAULT_PROJECTILE_DAMAGE_PERCENT = 0.05;
    private static final double DEFAULT_MANA_REGEN_PERCENT = 0.05;

    public SkillSinisterUrge(Heroes plugin) {
        super(plugin, "SinisterUrge");
        setDescription("Passive: additional $1% projectile damage and $2% mana regen.");
        setTypes(SkillType.ABILITY_PROPERTY_PROJECTILE, SkillType.MANA_INCREASING);

        Bukkit.getPluginManager().registerEvents(new SinisterUrgeListener(this), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.APPLY_TEXT.node(), "");
        node.set(SkillSetting.UNAPPLY_TEXT.node(), "");
        node.set("additional-projectile-damage-percent", DEFAULT_PROJECTILE_DAMAGE_PERCENT);
        node.set("additional-mana-regen-percent", DEFAULT_MANA_REGEN_PERCENT);

        return node;
    }

    @Override
    public String getDescription(Hero hero) {
        double additionalProjectileDamagePercent = SkillConfigManager.getUseSetting(hero,this,
                "additional-projectile-damage-percent", DEFAULT_PROJECTILE_DAMAGE_PERCENT, false);
        double additionalManaRegenPercent = SkillConfigManager.getUseSetting(hero,this,
                "additional-mana-regen-percent", DEFAULT_MANA_REGEN_PERCENT, false);

        return getDescription().replace("$1",(additionalProjectileDamagePercent*100) + "")
                .replace("$2",(additionalManaRegenPercent*100) + "");
    }

    private class SinisterUrgeListener implements Listener {
        private Skill skill;

        public SinisterUrgeListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onSkillDamage(SkillDamageEvent event) {
            if (event.getDamage() == 0)
                return;

            //boost only ranged damage
            Skill skill = event.getSkill();
            if (!skill.isType(SkillType.ABILITY_PROPERTY_PROJECTILE)){
                return;
            }

            // Handle outgoing
            if (event.getDamager() instanceof Hero) {
                Hero hero = (Hero) event.getDamager();

                if (hero.hasEffect(getName())) {
                    double additionalDamagePercent = SkillConfigManager.getUseSetting(hero,SkillSinisterUrge.this,
                            "additional-projectile-damage-percent", DEFAULT_PROJECTILE_DAMAGE_PERCENT, false);

                    double originalDamage = event.getDamage();
                    event.setDamage(originalDamage * (1 + additionalDamagePercent));
                }
            }
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onWeaponDamage(WeaponDamageEvent event) {
            if (event.getDamage() == 0)
                return;

            if (!event.isProjectile()){
                return;
            }

            // Handle outgoing
            if (event.getDamager() instanceof Hero) {
                Hero hero = (Hero) event.getDamager();

                if (hero.hasEffect(getName())) {
                    double additionalDamagePercent = SkillConfigManager.getUseSetting(hero,SkillSinisterUrge.this,
                            "additional-projectile-damage-percent", DEFAULT_PROJECTILE_DAMAGE_PERCENT, false);

                    double originalDamage = event.getDamage();
                    event.setDamage(originalDamage * (1 + additionalDamagePercent));
                }
            }
        }
    }

//FIXME: Uncomment once heroes maven is updated (so effect exists)
//    @Override
//    protected void apply(Hero hero) {
//        addSinisterUrgeEffect(hero);
//        hero.resolveManaRegen();
//    }
//
//    @Override
//    protected void unapply(Hero hero) {
//        //Remove effect
//        super.unapply(hero);
//        hero.resolveManaRegen();
//    }
//
//    private void addSinisterUrgeEffect(Hero hero) {
//        //For reference this effect's health is applied in Hero.resolveMaxHealth()
//        double additionalManaRegenPercent = SkillConfigManager.getUseSetting(hero, this,
//                "additional-mana-regen-percent", DEFAULT_MANA_REGEN_PERCENT, false);
//        Effect manaRegenBoostEffect = new ManaRegenPercentIncreaseEffect(this, this.getName(), additionalManaRegenPercent);
//        manaRegenBoostEffect.setPersistent(true);
//        hero.addEffect(manaRegenBoostEffect);
//    }
}