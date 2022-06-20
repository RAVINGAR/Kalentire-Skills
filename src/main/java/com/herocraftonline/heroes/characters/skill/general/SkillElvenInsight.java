package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.events.SkillDamageEvent;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.MaxManaPercentIncreaseEffect;
import com.herocraftonline.heroes.characters.skill.*;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class SkillElvenInsight extends PassiveSkill {

    private static final double DEFAULT_MAGICAL_DAMAGE_PERCENT = 0.05;
    private static final double DEFAULT_PROJECTILE_DAMAGE_PERCENT = 0.05;
    private static final double DEFAULT_MANA_PERCENT = 0.05;

    public SkillElvenInsight(Heroes plugin) {
        super(plugin, "ElvenInsight");
        setDescription("additional $1% magic damage and $2% projectile damage. Also provides +$3% mana.");
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.ABILITY_PROPERTY_PROJECTILE, SkillType.MAX_MANA_INCREASING);

        Bukkit.getPluginManager().registerEvents(new ElvenInsightListener(this), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.APPLY_TEXT.node(), "");
        node.set(SkillSetting.UNAPPLY_TEXT.node(), "");
        node.set("additional-magical-damage-percent", DEFAULT_MAGICAL_DAMAGE_PERCENT);
        node.set("additional-projectile-damage-percent", DEFAULT_PROJECTILE_DAMAGE_PERCENT);
        node.set("additional-mana-percent", DEFAULT_MANA_PERCENT);

        return node;
    }

    @Override
    public String getDescription(Hero hero) {
        double additionalMagicalDamagePercent = SkillConfigManager.getUseSetting(hero,this,
                "additional-magical-damage-percent", DEFAULT_MAGICAL_DAMAGE_PERCENT, false);
        double additionalProjectileDamagePercent = SkillConfigManager.getUseSetting(hero,this,
                "additional-projectile-damage-percent", DEFAULT_PROJECTILE_DAMAGE_PERCENT, false);
        double additionalManaPercent = SkillConfigManager.getUseSetting(hero,this,
                "additional-mana-percent", DEFAULT_MANA_PERCENT, false);

        return getDescription().replace("$1",(additionalMagicalDamagePercent*100) + "")
                .replace("$2",(additionalProjectileDamagePercent*100) + "")
                .replace("$3",(additionalManaPercent*100) + "");
    }

    private class ElvenInsightListener implements Listener {
        private Skill skill;

        public ElvenInsightListener(Skill skill) {
            this.skill = skill;
        }

        //TODO test overriding HeroRegainHealthEvent for % increased healing rate

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onSkillDamage(SkillDamageEvent event) {
            if (event.getDamage() == 0)
                return;

            //boost only magical and ranged damage
            Skill skill = event.getSkill();
            if (!(skill.isType(SkillType.ABILITY_PROPERTY_MAGICAL) || skill.isType(SkillType.ABILITY_PROPERTY_PROJECTILE))){
                return;
            }

            // Handle outgoing
            if (event.getDamager() instanceof Hero) {
                Hero hero = (Hero) event.getDamager();

                if (hero.hasEffect(getName())) {
                    double additionalDamagePercent = 0;
                    if (skill.isType(SkillType.ABILITY_PROPERTY_MAGICAL)) {
                        additionalDamagePercent = SkillConfigManager.getUseSetting(hero,SkillElvenInsight.this,
                                "additional-magical-damage-percent", DEFAULT_MAGICAL_DAMAGE_PERCENT, false);
                    }
                    else if (skill.isType(SkillType.ABILITY_PROPERTY_PROJECTILE)){
                        additionalDamagePercent = SkillConfigManager.getUseSetting(hero,SkillElvenInsight.this,
                                "additional-projectile-damage-percent", DEFAULT_PROJECTILE_DAMAGE_PERCENT, false);
                    }

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
                    double additionalDamagePercent = SkillConfigManager.getUseSetting(hero,SkillElvenInsight.this,
                            "additional-projectile-damage-percent", DEFAULT_PROJECTILE_DAMAGE_PERCENT, false);

                    double originalDamage = event.getDamage();
                    event.setDamage(originalDamage * (1 + additionalDamagePercent));
                }
            }
        }

    }

    @Override
    public void apply(Hero hero) {
        addElvenInsightEffect(hero);
        hero.resolveMaxMana();
    }

    @Override
    public void unapply(Hero hero) {
        // Remove effect
        super.unapply(hero);
        hero.resolveMaxMana();
    }

    private void addElvenInsightEffect(Hero hero) {
        //For reference this effect's health is applied in Hero.resolveMaxMana()
        double additionalManaPercent = SkillConfigManager.getUseSetting(hero, this,
                "additional-mana-percent", DEFAULT_MANA_PERCENT, false);
        Effect manaBoostEffect = new MaxManaPercentIncreaseEffect(this, this.getName(), additionalManaPercent);
        manaBoostEffect.setPersistent(true);
        hero.addEffect(manaBoostEffect);
    }
}