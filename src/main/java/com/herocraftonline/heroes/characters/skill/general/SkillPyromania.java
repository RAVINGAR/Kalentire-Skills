package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.events.SkillDamageEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.logging.Level;

public class SkillPyromania extends PassiveSkill {

    public SkillPyromania(final Heroes plugin) {
        super(plugin, "Pyromania");
        setDescription("You passively take $1% less damage from Fire. " +
                "Additionally, you deal $2% increased damage with fire abilities for each second of burning damage you have stored up on yourself, up to a maximum of $3%.");
        setEffectTypes(EffectType.BENEFICIAL, EffectType.FIRE);

        Bukkit.getPluginManager().registerEvents(new SkillHeroListener(this), plugin);
    }

    @Override
    public String getDescription(final Hero hero) {
        final double reductionPercent = SkillConfigManager.getUseSetting(hero, this, "damage-reduction-percent", 0.2, false);
        final double increasePerFireTickSecond = SkillConfigManager.getUseSetting(hero, this, "damage-percent-increase-per-fire-tick-second", 0.05, false);
        final double maxPercentIncrease = SkillConfigManager.getUseSetting(hero, this, "max-damage-increase-percent", 0.25, false);

        return super.getDescription()
                .replace("$1", Util.decFormat.format(reductionPercent * 100))
                .replace("$2", Util.decFormat.format(increasePerFireTickSecond * 100))
                .replace("$3", Util.decFormat.format(maxPercentIncrease * 100));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.APPLY_TEXT.node(), "");
        config.set(SkillSetting.UNAPPLY_TEXT.node(), "");
        config.set("damage-reduction-percent", 0.2);
        config.set("damage-percent-increase-per-fire-tick-second", 0.05);
        config.set("max-damage-increase-percent", 0.25);
        return config;
    }

    @Override
    public void apply(final Hero hero) {
        final String applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT,
                        ChatComponents.GENERIC_SKILL + "%hero% gained %skill%!")
                .replace("%hero%", "$1").replace("$hero$", "$1").replace("%skill%", getName());
        final String unapplyText = SkillConfigManager.getRaw(this, SkillSetting.UNAPPLY_TEXT,
                        ChatComponents.GENERIC_SKILL + "%hero% lost %skill%!")
                .replace("%hero%", "$1").replace("$hero$", "$1").replace("%skill%", getName());

        final PyromaniaEffect effect = new PyromaniaEffect(this, hero.getPlayer(), applyText, unapplyText);
        hero.addEffect(effect);
    }

    public static class PyromaniaEffect extends Effect {
        private double reductionPercent;
        private double increasePerFireTickSecond;
        private double maxPercentIncrease;

        PyromaniaEffect(final Skill skill, final Player player, final String applyText, final String unapplyText) {
            super(skill, skill.getName(), player, applyText, unapplyText);

            types.add(EffectType.INTERNAL);
            setPersistent(true);
        }

        @Override
        public void applyToHero(final Hero hero) {
            super.applyToHero(hero);

            this.reductionPercent = SkillConfigManager.getUseSetting(hero, skill, "damage-reduction-percent", 0.2, false);
            this.increasePerFireTickSecond = SkillConfigManager.getUseSetting(hero, skill, "damage-percent-increase-per-fire-tick-second", 0.05, false);
            this.maxPercentIncrease = SkillConfigManager.getUseSetting(hero, skill, "max-damage-increase-percent", 0.25, false);
        }

        public double getReductionPercent() {
            return this.reductionPercent;
        }

        public double getDamageIncrease() {
            if (applier.getFireTicks() < 1) {
                return 0.0;
            }

            final int fireTickSeconds = (int) (applier.getFireTicks() / 20);
            return Math.min(maxPercentIncrease, increasePerFireTickSecond * fireTickSeconds);
        }
    }

    private class SkillHeroListener implements Listener {
        private final Skill skill;

        SkillHeroListener(final Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onSkillDamage(final SkillDamageEvent event) {
            if (event.getSkill() == null || !event.getSkill().getTypes().contains(SkillType.ABILITY_PROPERTY_FIRE)) {
                return;
            }
            if (event.getDamage() <= 0.0) {
                return;
            }
            if (!event.getDamager().hasEffect(skill.getName())) {
                return;
            }

            final PyromaniaEffect effect = (PyromaniaEffect) event.getDamager().getEffect(skill.getName());
            Heroes.log(Level.INFO, "Pyromania burn damage increase: " + effect.getDamageIncrease());
            if (effect.getDamageIncrease() <= 0.0) {
                return;
            }

            final double newDamage = event.getDamage() * (1.0 + effect.getDamageIncrease());
            final double damageDifference = (newDamage - event.getDamage());
            Heroes.log(Level.INFO, "Pyromania - PreDamage: " + event.getDamage() + ", New Damage: " + newDamage);
            if (damageDifference <= 0.0) {
                return;
            }

            event.setDamage(newDamage);
            event.getDamager().getEntity().sendMessage("    " + ChatComponents.GENERIC_SKILL + ChatColor.GOLD + "Pyromania Damage Boost: " + damageDifference + "!");
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onFireDamage(final EntityDamageEvent event) {
            if (event.getCause() != EntityDamageEvent.DamageCause.FIRE_TICK
                    && event.getCause() != EntityDamageEvent.DamageCause.FIRE
                    && event.getCause() != EntityDamageEvent.DamageCause.HOT_FLOOR
                    && event.getCause() != EntityDamageEvent.DamageCause.LAVA) {
                return;
            }

            if (!(event.getEntity() instanceof Player)) {
                return;
            }

            final Player player = (Player) event.getEntity();
            final Hero hero = plugin.getCharacterManager().getHero(player);
            final EntityDamageEvent.DamageCause cause = event.getCause();
            if (!hero.hasEffect(skill.getName())) {
                return;
            }
            if (event.getDamage() <= 0) {
                return;
            }

            final PyromaniaEffect effect = (PyromaniaEffect) hero.getEffect(skill.getName());
            event.setDamage(event.getDamage() * (1.0 - effect.getReductionPercent()));
        }
    }
}