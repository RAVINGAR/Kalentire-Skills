package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.events.SkillDamageEvent;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.*;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class SkillBitterComfort extends PassiveSkill {

    private static final double DEFAULT_PROJECTILE_DAMAGE_PERCENT = 0.05;

    public SkillBitterComfort(Heroes plugin) {
        super(plugin, "BitterComfort");
        setDescription("Passive: additional $1% projectile damage.");
        setTypes(SkillType.ABILITY_PROPERTY_PROJECTILE);

        Bukkit.getPluginManager().registerEvents(new BitterComfortListener(this), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.APPLY_TEXT.node(), "");
        node.set(SkillSetting.UNAPPLY_TEXT.node(), "");
        node.set("additional-projectile-damage-percent", DEFAULT_PROJECTILE_DAMAGE_PERCENT);

        return node;
    }

    @Override
    public String getDescription(Hero hero) {
        double additionalProjectileDamagePercent = SkillConfigManager.getUseSetting(hero,SkillBitterComfort.this,
                "additional-projectile-damage-percent", DEFAULT_PROJECTILE_DAMAGE_PERCENT, false);

        return getDescription().replace("$1",(additionalProjectileDamagePercent*100) + "");
    }

    private class BitterComfortListener implements Listener {
        private Skill skill;

        public BitterComfortListener(Skill skill) {
            this.skill = skill;
        }

        //TODO: test overriding HeroRegainStaminaEvent to boost stamina regen rate

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
}