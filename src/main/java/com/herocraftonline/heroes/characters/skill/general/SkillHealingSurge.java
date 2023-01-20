package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicHealEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SkillHealingSurge extends TargettedSkill {

    private String expireText;
    private String applyText;

    public SkillHealingSurge(final Heroes plugin) {
        super(plugin, "HealingSurge");
        setDescription("You restore $1 health and $2% of mana/stamina to the target over $3 second(s). You are only healed for $4 health and $5% of mana/stamina from this effect.");
        setUsage("/skill healingsurge <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill healingsurge");
        setTypes(SkillType.BUFFING, SkillType.HEALING, SkillType.MANA_INCREASING, SkillType.STAMINA_INCREASING, SkillType.SILENCEABLE);
    }

    @Override
    public String getDescription(final Hero hero) {
        final int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 2000, false);
        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 20000, false);

        double healing = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING_TICK, 10, false);
        final double healingIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING_INCREASE_PER_WISDOM, 0.25, false);
        healing += (hero.getAttributeValue(AttributeType.WISDOM) * healingIncrease);

        double manaStaminaPercent = SkillConfigManager.getUseSetting(hero, this, "mana-stamina-recover-percent", (double) 1, false);
        final double manaStaminaPercentIncrease = SkillConfigManager.getUseSetting(hero, this, "mana-stamina-recover-percent-per-intellect", (double) 1, false);
        manaStaminaPercent += (hero.getAttributeValue(AttributeType.INTELLECT) * manaStaminaPercentIncrease);

        final String formattedHealing = Util.decFormat.format(healing * ((double) duration / (double) period));
        final String formattedSelfHealing = Util.decFormat.format((healing * ((double) duration / (double) period)) * Heroes.properties.selfHeal);
        final String formattedDuration = Util.decFormat.format(duration / 1000.0);
        final String formattedManaStaminaPercent = Util.decFormat.format(manaStaminaPercent * ((double) duration / (double) period));
        final String formattedSelfManaStaminaPercent = Util.decFormat.format(manaStaminaPercent * ((double) duration / (double) period) * Heroes.properties.selfHeal);

        return getDescription().replace("$1", formattedHealing).replace("$2", formattedManaStaminaPercent).replace("$3", formattedDuration).replace("$4", formattedSelfHealing).replace("$5", formattedSelfManaStaminaPercent);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 10);
        node.set(SkillSetting.DURATION.node(), 20000);
        node.set(SkillSetting.PERIOD.node(), 2000);
        node.set(SkillSetting.HEALING_TICK.node(), 10);
        node.set(SkillSetting.HEALING_INCREASE_PER_WISDOM.node(), 0.25);
        node.set("mana-stamina-recover-percent", (double) 1);
        node.set("mana-stamina-recover-percent-per-intellect", 0.1);
        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%target% feels a healing surge!").replace("%target%", "$1").replace("$target$", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%target%'s healing surge has ended!").replace("%target%", "$1").replace("$target$", "$1");
    }

    @Override
    public SkillResult use(final Hero hero, final LivingEntity target, final String[] args) {
        final Player player = hero.getPlayer();
        if (!(target instanceof Player)) {
            return SkillResult.INVALID_TARGET;
        }

        final Hero targetHero = plugin.getCharacterManager().getHero((Player) target);

        double healing = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING_TICK, 10, false);
        final double healingIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING_INCREASE_PER_WISDOM, 0.25, false);
        healing += (hero.getAttributeValue(AttributeType.WISDOM) * healingIncrease);

        double manaStaminaPercent = SkillConfigManager.getUseSetting(hero, this, "mana-stamina-recover-percent", (double) 1, false);
        final double manaStaminaPercentIncrease = SkillConfigManager.getUseSetting(hero, this, "mana-stamina-recover-percent-per-intellect", (double) 1, false);
        manaStaminaPercent += (hero.getAttributeValue(AttributeType.INTELLECT) * manaStaminaPercentIncrease);

        final int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 2000, false);
        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 20000, false);

        final HealingSurgeEffect hEffect = new HealingSurgeEffect(this, player, period, duration, healing, manaStaminaPercent);
        targetHero.addEffect(hEffect);

        // Could possibly put an effect in here. Rather leave it in the ticking though.

        return SkillResult.NORMAL;
    }

    public class HealingSurgeEffect extends PeriodicHealEffect {

        private final double tickManaStamina;

        public HealingSurgeEffect(final Skill skill, final Player applier, final long period, final long duration, final double tickHealth, final double tickManaStaminaPercent) {
            super(skill, "Rejuvenate", applier, period, duration, tickHealth);

            types.add(EffectType.MAGIC);
            types.add(EffectType.MANA_INCREASING);
            types.add(EffectType.STAMINA_INCREASING);
            types.add(EffectType.DISPELLABLE);

            tickManaStamina = tickManaStaminaPercent / 100;
        }

        @Override
        public void applyToHero(final Hero hero) {
            super.applyToHero(hero);
            final Player player = hero.getPlayer();
            broadcast(player.getLocation(), "    " + applyText, player.getName());
        }

        @Override
        public void removeFromHero(final Hero hero) {
            super.removeFromHero(hero);
            final Player player = hero.getPlayer();
            broadcast(player.getLocation(), "    " + expireText, player.getName());
        }

        @Override
        public void tickHero(final Hero hero) {
            super.tickHero(hero);

            double manaToRestore = hero.getMaxMana() * tickManaStamina;
            double staminaToRestore = hero.getMaxStamina() * tickManaStamina;
            if (hero.getPlayer() == applier) {
                manaToRestore *= Heroes.properties.selfHeal;
                staminaToRestore *= Heroes.properties.selfHeal;
            }

            // Math.ceil to round up. If the combination of low % and self heal nerf causes it to be less than 1, that wouldn't be very nice.
            hero.setMana(hero.getMana() + (int) Math.ceil(manaToRestore));
            hero.setStamina(hero.getStamina() + (int) Math.ceil(staminaToRestore));

            // This would be a good place for an effect.
            final Player player = hero.getPlayer();
            //player.getWorld().spigot().playEffect(player.getLocation(), Effect.SPLASH, 0, 0, 0.5F, 1.0F, 0.5F, 0.2F, 35, 16);
            player.getWorld().spawnParticle(Particle.WATER_SPLASH, player.getLocation(), 35, 0.5, 1, 0.5, 0.2);
            //player.getWorld().spigot().playEffect(player.getLocation(), Effect.HAPPY_VILLAGER, 0, 0, 0.5F, 1.0F, 0.5F, 0.2F, 15, 16);
            player.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, player.getLocation(), 15, 0.5, 1, 0.5, 0.2);
        }
    }
}
