package com.herocraftonline.heroes.characters.skill.reborn.druid;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.PeriodicHealEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Util;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.LoveEffect;
import de.slikey.effectlib.util.DynamicLocation;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class SkillHealingBloom extends ActiveSkill {

    public SkillHealingBloom(Heroes plugin) {
        super(plugin, "HealingBloom");
        setDescription("Apply a Healing Bloom to party members within $1 blocks, healing them for $2 health over $3 second(s). " +
                "You are only healed for $3 health from this effect.");
        setUsage("/skill healingbloom");
        setIdentifiers("skill healingbloom");
        setArgumentRange(0, 0);
        setTypes(SkillType.SILENCEABLE, SkillType.AREA_OF_EFFECT, SkillType.HEALING, SkillType.ABILITY_PROPERTY_EARTH);
    }

    public String getDescription(Hero hero) {
        double radius = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.RADIUS, false);

        int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 2000, false);
        int duration = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.DURATION, false);

        double healing = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING_TICK, 17.0, false);
        healing = getScaledHealing(hero, healing);

        double tickMultiplier = (double) duration / (double) period;

        return getDescription()
                .replace("$1", Util.decFormat.format(radius))
                .replace("$2", Util.decFormat.format(healing * tickMultiplier))
                .replace("$3", Util.decFormat.format(duration / 1000.0))
                .replace("$4", Util.decFormat.format(healing * Heroes.properties.selfHeal * tickMultiplier));
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.DURATION.node(), 20000);
        config.set(SkillSetting.RADIUS.node(), 15);
        config.set(SkillSetting.PERIOD.node(), 2000);
        config.set(SkillSetting.HEALING_TICK.node(), 11);
        config.set(SkillSetting.HEALING_INCREASE_PER_WISDOM.node(), 0.0);
        return config;
    }

    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        double radius = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.RADIUS, false);
        double radiusSquared = radius * radius;
        int duration = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.DURATION, false);
        double healing = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING_TICK, 17.0, false);
        healing = getScaledHealing(hero, healing);

        int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 2000, false);

        broadcastExecuteText(hero);

        // Check if the hero has a party
        if (hero.hasParty()) {
            Location playerLocation = player.getLocation();
            // Loop through the player's party members and add the effect as necessary
            for (Hero member : hero.getParty().getMembers()) {
                // Ensure the party member is in the same world.
                if (member.getPlayer().getLocation().getWorld().equals(playerLocation.getWorld())) {
                    // Check to see if they are close enough to the player to receive the buff
                    if (member.getPlayer().getLocation().distanceSquared(playerLocation) <= radiusSquared) {
                        // Add the effect
                        member.addEffect(new PeriodicHealEffect(this, "HealingBloom", player, period, duration, healing));
                        playVisuals(member.getEntity(), duration);
                    }
                }
            }
        } else {
            // Add the effect to just the player
            hero.addEffect(new PeriodicHealEffect(this, "HealingBloom", player, period, duration, healing));
            playVisuals(hero.getEntity(), duration);
        }

        return SkillResult.NORMAL;
    }

    public void playVisuals(LivingEntity target, int duration) {
        EffectManager effectManager = new EffectManager(plugin);
        final int durationTicks = (int) duration / 50;
        final int displayPeriod = 2;

        LoveEffect visualEffect = new LoveEffect(effectManager);
        DynamicLocation dynamicLoc = new DynamicLocation(target);
        visualEffect.setDynamicOrigin(dynamicLoc);
        visualEffect.disappearWithOriginEntity = true;

        visualEffect.particle = Particle.VILLAGER_HAPPY;
        visualEffect.period = displayPeriod;
        visualEffect.particleSize = 15;

        visualEffect.iterations = durationTicks / displayPeriod;
        dynamicLoc.addOffset(new Vector(0, 0.8, 0));

        effectManager.start(visualEffect);
        effectManager.disposeOnTermination();
    }
}