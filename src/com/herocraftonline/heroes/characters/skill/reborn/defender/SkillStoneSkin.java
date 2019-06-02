package com.herocraftonline.heroes.characters.skill.reborn.defender;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.SkillDamageEvent;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class SkillStoneSkin extends ActiveSkill {

    private String applyText;
    private String expireText;

    public SkillStoneSkin(Heroes plugin) {
        super(plugin, "StoneSkin");
        setDescription("Your skin turns to stone for $1 second(s). While active, enemies receive damage on attack! Additionally protects you $2% from $3$4.");
        setArgumentRange(0, 0);
        setUsage("/skill stoneskin");
        setIdentifiers("skill stoneskin", "skill sskin");
        setTypes(SkillType.ABILITY_PROPERTY_EARTH, SkillType.SILENCEABLE, SkillType.DEBUFFING, SkillType.AGGRESSIVE);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillHeroListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();

        config.set("damage-reduction", 0.05);
        config.set("reduce-magic-damage", false);
        config.set("resist-lightning", true);
        config.set(SkillSetting.COOLDOWN.node(), 25000);
        config.set(SkillSetting.DURATION.node(), 15000);
        return config;
    }

    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT,
                ChatComponents.GENERIC_SKILL + "%hero%'s skin has turned to stone.")
                .replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT,
                ChatComponents.GENERIC_SKILL + "%hero%'s skin softens.")
                .replace("%hero%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        int duration = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.DURATION, false);
        double damageReduction = SkillConfigManager.getUseSetting(hero, this, "damage-reduction", 0.05, false);

        hero.addEffect(new StoneSkinEffect(this, player, duration, damageReduction, applyText, expireText));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_THUNDER, 0.8F, 1.0F);

        return SkillResult.NORMAL;
    }

    public class SkillHeroListener implements Listener {

        private Skill skill;
        public SkillHeroListener(Skill skill){
            this.skill = skill;
        }

        //FIXME: should I used monitor or just highest? As it possibly may cancel the event.
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onSkillDamage(SkillDamageEvent event) {
            if (event.getDamage() == 0 || !(event.getEntity() instanceof Player))
                return;

            Hero defenderHero = plugin.getCharacterManager().getHero((Player) event.getEntity());
            if (!defenderHero.hasEffect("StoneSkin"))
                return;

            // Handle reduction of magic based damage.
            // Also Consider resisting lightning over the requirement to reduce magic damage
            boolean reduceMagicDamage = SkillConfigManager.getUseSetting(defenderHero, skill, "reduce-magic-damage",false);
            boolean resistLightning = SkillConfigManager.getUseSetting(defenderHero, skill, "resist-lightning",true);
            if (!( (event.getSkill().isType(SkillType.ABILITY_PROPERTY_LIGHTNING) && resistLightning)
                    || (event.getSkill().isType(SkillType.ABILITY_PROPERTY_MAGICAL) && reduceMagicDamage) ))
                return;
                // If skill is lightning and not resisting lightning -> Don't modify
                // OR? If skill is magical and not reducing magic damage -> Don't modify

            // If skill is lightning and resisting lightning
            // OR skill is magical and reducing magic damage
            // OR skill is not lightning or magical (e.g. physical or projectile)

            if (resistLightning && event.getSkill().isType(SkillType.ABILITY_PROPERTY_LIGHTNING)){
                event.setCancelled(true);
            } else {
                double damageFactor = 1.0 - ((StoneSkinEffect) defenderHero.getEffect("StoneSkin")).getDamageReduction();
                event.setDamage((event.getDamage() * damageFactor));
            }
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onWeaponDamage(WeaponDamageEvent event){
            if (event.getDamage() == 0 || !(event.getEntity() instanceof Player))
                return;

            Hero defenderHero = plugin.getCharacterManager().getHero((Player) event.getEntity());
            if (!defenderHero.hasEffect("StoneSkin"))
                return;

            double damageFactor = 1.0 - ((StoneSkinEffect) defenderHero.getEffect("StoneSkin")).getDamageReduction();
            event.setDamage((event.getDamage() * damageFactor));

        }

    }

    public class StoneSkinEffect extends ExpirableEffect {
        private final double damageReduction;

        public StoneSkinEffect(Skill skill, Player applier, long duration, double damageReduction,
                               String applyText, String expireText) {
            super(skill, "StoneSkin", applier, duration, applyText, expireText);

            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.EARTH);
            types.add(EffectType.BENEFICIAL);

            this.damageReduction = damageReduction;
        }

        public double getDamageReduction() {
            return damageReduction;
        }
    }
}
