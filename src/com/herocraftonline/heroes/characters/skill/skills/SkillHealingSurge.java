package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicHealEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SkillHealingSurge extends TargettedSkill {
    // This is for Firework Effects
    public VisualEffect fplayer = new VisualEffect();
    private String expireText;
    private String applyText;

    public SkillHealingSurge(Heroes plugin) {
        super(plugin, "HealingSurge");
        setDescription("You restore $1 health and $2% of mana/stamina to the target over $3 seconds. You are only healed for $4 health and $5% of mana/stamina from this effect.");
        setUsage("/skill healingsurge <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill healingsurge");
        setTypes(SkillType.BUFFING, SkillType.HEALING, SkillType.MANA_INCREASING, SkillType.STAMINA_INCREASING, SkillType.SILENCABLE);
    }

    public String getDescription(Hero hero) {
        int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, Integer.valueOf(2000), false);
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), Integer.valueOf(20000), false);

        double healing = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING_TICK, Integer.valueOf(10), false);
        double healingIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING_INCREASE_PER_WISDOM, Double.valueOf(0.25), false);
        healing += (hero.getAttributeValue(AttributeType.WISDOM) * healingIncrease);

        double manaStaminaPercent = SkillConfigManager.getUseSetting(hero, this, "mana-stamina-recover-percent", Double.valueOf(1), false);
        double manaStaminaPercentIncrease = SkillConfigManager.getUseSetting(hero, this, "mana-stamina-recover-percent-per-intellect", Double.valueOf(1), false);
        manaStaminaPercent += (hero.getAttributeValue(AttributeType.INTELLECT) * manaStaminaPercentIncrease);
        
        String formattedHealing = Util.decFormat.format(healing * ((double) duration / (double) period));
        String formattedSelfHealing = Util.decFormat.format((healing * ((double) duration / (double) period)) * Heroes.properties.selfHeal);
        String formattedDuration = Util.decFormat.format(duration / 1000.0);
        String formattedManaStaminaPercent = Util.decFormat.format(manaStaminaPercent * ((double) duration / (double) period));
        String formattedSelfManaStaminaPercent = Util.decFormat.format(manaStaminaPercent * ((double) duration / (double) period) * Heroes.properties.selfHeal);

        return getDescription().replace("$1", formattedHealing).replace("$2", formattedManaStaminaPercent).replace("$3", formattedDuration).replace("$4", formattedSelfHealing).replace("$5", formattedSelfManaStaminaPercent);
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), Integer.valueOf(10));
        node.set(SkillSetting.DURATION.node(), Integer.valueOf(20000));
        node.set(SkillSetting.PERIOD.node(), Integer.valueOf(2000));
        node.set(SkillSetting.HEALING_TICK.node(), Integer.valueOf(10));
        node.set(SkillSetting.HEALING_INCREASE_PER_WISDOM.node(), Double.valueOf(0.25));
        node.set("mana-stamina-recover-percent", Double.valueOf(1));
        node.set("mana-stamina-recover-percent-per-intellect", Double.valueOf(0.1));
        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, Messaging.getSkillDenoter() + "%target% feels a healing surge!").replace("%target%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, Messaging.getSkillDenoter() + "%target%'s healing surge has ended!").replace("%target%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();
        if (!(target instanceof Player))
            return SkillResult.INVALID_TARGET;

        Hero targetHero = plugin.getCharacterManager().getHero((Player) target);

        double healing = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING_TICK, Integer.valueOf(10), false);
        double healingIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING_INCREASE_PER_WISDOM, Double.valueOf(0.25), false);
        healing += (hero.getAttributeValue(AttributeType.WISDOM) * healingIncrease);

        double manaStaminaPercent = SkillConfigManager.getUseSetting(hero, this, "mana-stamina-recover-percent", Double.valueOf(1), false);
        double manaStaminaPercentIncrease = SkillConfigManager.getUseSetting(hero, this, "mana-stamina-recover-percent-per-intellect", Double.valueOf(1), false);
        manaStaminaPercent += (hero.getAttributeValue(AttributeType.INTELLECT) * manaStaminaPercentIncrease);
        
        int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, Integer.valueOf(2000), false);
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), Integer.valueOf(20000), false);

        HealingSurgeEffect hEffect = new HealingSurgeEffect(this, player, period, duration, healing, manaStaminaPercent);
        targetHero.addEffect(hEffect);

        // Could possibly put an effect in here. Rather leave it in the ticking though.

        return SkillResult.NORMAL;
    }

    public class HealingSurgeEffect extends PeriodicHealEffect {

        private double tickManaStamina;
        
        public HealingSurgeEffect(Skill skill, Player applier, long period, long duration, double tickHealth, double tickManaStaminaPercent) {
            super(skill, "Rejuvenate", applier, period, duration, tickHealth);

            types.add(EffectType.MAGIC);
            types.add(EffectType.MANA_INCREASING);
            types.add(EffectType.STAMINA_INCREASING);
            types.add(EffectType.DISPELLABLE);
            
            tickManaStamina = tickManaStaminaPercent / 100;
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), applyText, player.getDisplayName());
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), expireText, player.getDisplayName());
        }
        
        @Override
        public void tickHero(Hero hero) {
            super.tickHero(hero);
            
            double manaToRestore = hero.getMaxMana() * tickManaStamina;
            double staminaToRestore = hero.getMaxStamina() * tickManaStamina;
            if(hero.getPlayer() == applier) {
                manaToRestore *= Heroes.properties.selfHeal;
                staminaToRestore *= Heroes.properties.selfHeal;
            }
            
            // Math.ceil to round up. If the combination of low % and self heal nerf causes it to be less than 1, that wouldn't be very nice.
            hero.setMana(hero.getMana() + (int) Math.ceil(manaToRestore));
            hero.setStamina(hero.getStamina() + (int) Math.ceil(staminaToRestore));
            
            // This would be a good place for an effect.
        }
    }
}