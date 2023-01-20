package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.events.SkillDamageEvent;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.StaminaRegenPercentIncreaseEffect;
import com.herocraftonline.heroes.characters.skill.*;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class SkillBitterComfort extends PassiveSkill {

    private static final double DEFAULT_PROJECTILE_DAMAGE_PERCENT = 0.05;
    private static final double DEFAULT_STAMINA_REGEN_PERCENT = 0.05;

    public SkillBitterComfort(Heroes plugin) {
        super(plugin, "BitterComfort");
        setDescription("additional $1% projectile damage and +$2% stamina regen.");
        setTypes(SkillType.ABILITY_PROPERTY_PROJECTILE, SkillType.STAMINA_INCREASING);

        Bukkit.getPluginManager().registerEvents(new BitterComfortListener(this), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.APPLY_TEXT.node(), "");
        node.set(SkillSetting.UNAPPLY_TEXT.node(), "");
        node.set("additional-projectile-damage-percent", DEFAULT_PROJECTILE_DAMAGE_PERCENT);
        node.set("additional-stamina-regen-percent", DEFAULT_STAMINA_REGEN_PERCENT);

        return node;
    }

    @Override
    public String getDescription(Hero hero) {
        double additionalProjectileDamagePercent = SkillConfigManager.getUseSetting(hero,this,
                "additional-projectile-damage-percent", DEFAULT_PROJECTILE_DAMAGE_PERCENT, false);
        double additionalStaminaRegenPercent = SkillConfigManager.getUseSetting(hero,this,
                "additional-stamina-regen-percent", DEFAULT_STAMINA_REGEN_PERCENT, false);

        return getDescription().replace("$1",(additionalProjectileDamagePercent*100) + "")
                .replace("$2",(additionalProjectileDamagePercent*100) + "");
    }

    private class BitterComfortListener implements Listener {
        private final Skill skill;

        public BitterComfortListener(Skill skill) {
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
                    double additionalDamagePercent = SkillConfigManager.getUseSetting(hero,SkillBitterComfort.this,
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
                    double additionalDamagePercent = SkillConfigManager.getUseSetting(hero,SkillBitterComfort.this,
                            "additional-projectile-damage-percent", DEFAULT_PROJECTILE_DAMAGE_PERCENT, false);

                    double originalDamage = event.getDamage();
                    event.setDamage(originalDamage * (1 + additionalDamagePercent));
                }
            }
        }
    }

    @Override
    public void apply(Hero hero) {
        addBitterComfortEffect(hero);
        hero.resolveStaminaRegen();
    }

    @Override
    public void unapply(Hero hero) {
        //Remove effect
        super.unapply(hero);
        hero.resolveStaminaRegen();
    }

    private void addBitterComfortEffect(Hero hero) {
        //For reference this effect's health is applied in Hero.resolveMaxMana()
        double additionalStaminaPercent = SkillConfigManager.getUseSetting(hero, this,
                "additional-stamina-regen-percent", DEFAULT_STAMINA_REGEN_PERCENT, false);
        Effect staminaRegenBoostEffect = new StaminaRegenPercentIncreaseEffect(this, this.getName(), additionalStaminaPercent);
        staminaRegenBoostEffect.setPersistent(true);
        hero.addEffect(staminaRegenBoostEffect);
    }
}