package com.herocraftonline.heroes.characters.skill.reborn.pyromancer;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.events.HeroRegainManaEvent;
import com.herocraftonline.heroes.api.events.SkillDamageEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.*;
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

public class SkillPyromania extends PassiveSkill {

    public SkillPyromania(Heroes plugin) {
        super(plugin, "Pyromania");
        setDescription("You passively take $1% less damage from Fire. " +
                "You passively deal $2% increased damage with fire abilities for each second of burning damage you have stored up on yourself, up to a maximum of $3%. " +
                "You passively gain $4 mana for every point of fire tick damage that you take.");
        setEffectTypes(EffectType.BENEFICIAL, EffectType.FIRE);

        Bukkit.getPluginManager().registerEvents(new SkillHeroListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        double reductionPercent = SkillConfigManager.getUseSetting(hero, this, "damage-reduction-percent", 0.2, false);
        double increasePerFireTickSecond = SkillConfigManager.getUseSetting(hero, this, "damage-percent-increase-per-fire-tick", 0.0025, false);
        double maxPercentIncrease = SkillConfigManager.getUseSetting(hero, this, "max-damage-increase-percent", 0.25, false);
        double manaRegainPerDamage = SkillConfigManager.getUseSetting(hero, this, "mana-regain-per-fire-damage", 1.0, false);

        return super.getDescription()
                .replace("$1", Util.decFormat.format(reductionPercent * 100))
                .replace("$2", Util.decFormat.format(increasePerFireTickSecond * 20 * 100))
                .replace("$3", Util.decFormat.format(maxPercentIncrease * 100))
                .replace("$4", Util.decFormat.format(manaRegainPerDamage));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.APPLY_TEXT.node(), "");
        config.set(SkillSetting.UNAPPLY_TEXT.node(), "");
        config.set("damage-reduction-percent", 0.2);
        config.set("damage-percent-increase-per-fire-tick", 0.0025);
        config.set("max-damage-increase-percent", 0.25);
        config.set("mana-regain-per-fire-damage", 1.0);
        return config;
    }

    @Override
    public void apply(Hero hero) {
        String applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT,
                ChatComponents.GENERIC_SKILL + "%hero% gained %skill%!")
                .replace("%hero%", "$1").replace("%skill%", getName());
        String unapplyText = SkillConfigManager.getRaw(this, SkillSetting.UNAPPLY_TEXT,
                ChatComponents.GENERIC_SKILL + "%hero% lost %skill%!")
                .replace("%hero%", "$1").replace("%skill%", getName());

        PyromaniaEffect effect = new PyromaniaEffect(this, hero.getPlayer(), applyText, unapplyText);
        hero.addEffect(effect);
    }

    public class PyromaniaEffect extends Effect {
        private double reductionPercent;
        private double increasePerFireTick;
        private double maxPercentIncrease;
        private double manaRegainPerDamage;

        PyromaniaEffect(Skill skill, Player player, String applyText, String unapplyText) {
            super(skill, skill.getName(), player, applyText, unapplyText);

            types.add(EffectType.INTERNAL);
            setPersistent(true);
        }

        public void applyToHero(Hero hero) {
            super.applyToHero(hero);

            this.reductionPercent = SkillConfigManager.getUseSetting(hero, skill, "damage-reduction-percent", 0.2, false);
            this.increasePerFireTick = SkillConfigManager.getUseSetting(hero, skill, "damage-percent-increase-per-fire-tick", 0.0025, false);
            this.maxPercentIncrease = SkillConfigManager.getUseSetting(hero, skill, "max-damage-increase-percent", 0.25, false);
            this.manaRegainPerDamage = SkillConfigManager.getUseSetting(hero, skill, "mana-regain-per-fire-damage", 1.0, false);
        }

        public double getReductionPercent() {
            return this.reductionPercent;
        }

        public double getDamageIncrease() {
            if (applier.getFireTicks() < 1)
                return 0.0;

            return Math.min(maxPercentIncrease, increasePerFireTick * (applier.getFireTicks() + 1));    // FireTicks are 0 based
        }

        public double getManaRegainPerDamage() {
            return manaRegainPerDamage;
        }
    }

    private class SkillHeroListener implements Listener {
        private Skill skill;

        SkillHeroListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
        public void onSkillDamage(SkillDamageEvent event) {
            if (event.getSkill() == null || !event.getSkill().getTypes().contains(SkillType.ABILITY_PROPERTY_FIRE))
                return;
            if (event.getDamage() <= 0.0)
                return;
            if (!(event.getDamager() instanceof Hero) || !event.getDamager().hasEffect(skill.getName()))
                return;

            Hero hero = (Hero) event.getDamager();
            Player player = hero.getPlayer();
            PyromaniaEffect effect = (PyromaniaEffect) hero.getEffect(skill.getName());
            if (effect.getDamageIncrease() <= 0.0)
                return;

            double newDamage = event.getDamage() * (1.0 + effect.getDamageIncrease());
            double damageDifference = (newDamage - event.getDamage());
            if (damageDifference <= 0.0)
                return;

            event.setDamage(newDamage);
            player.sendMessage("    " + ChatComponents.GENERIC_SKILL + ChatColor.GOLD + "Pyromania Damage Boost: "
                    + Util.decFormat.format(damageDifference) + "!");
        }

        @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
        public void onFireDamageTaken(EntityDamageEvent event) {
            if (event.getCause() != EntityDamageEvent.DamageCause.FIRE_TICK
                    && event.getCause() != EntityDamageEvent.DamageCause.FIRE
                    && event.getCause() != EntityDamageEvent.DamageCause.HOT_FLOOR
                    && event.getCause() != EntityDamageEvent.DamageCause.LAVA) {
                return;
            }

            if (!(event.getEntity() instanceof Player))
                return;

            Player player = (Player) event.getEntity();
            Hero hero = plugin.getCharacterManager().getHero(player);
            EntityDamageEvent.DamageCause cause = event.getCause();
            if (!hero.hasEffect(skill.getName()))
                return;
            if (event.getDamage() <= 0)
                return;

            PyromaniaEffect effect = (PyromaniaEffect) hero.getEffect(skill.getName());
            event.setDamage(event.getDamage() * (1.0 - effect.getReductionPercent()));

            int manaIncrease = (int) (effect.getManaRegainPerDamage() * event.getDamage());
            HeroRegainManaEvent regainEvent = new HeroRegainManaEvent(hero, manaIncrease, skill);
            if (!regainEvent.isCancelled()) {
                hero.setMana(regainEvent.getDelta() + hero.getMana());

                if (hero.isVerboseMana())
                    hero.getPlayer().sendMessage(ChatComponents.Bars.mana(hero.getMana(), hero.getMaxMana(), true));
            }
        }
    }
}