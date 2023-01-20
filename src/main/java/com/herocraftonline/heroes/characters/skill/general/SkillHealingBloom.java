package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.PeriodicHealEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.libs.slikey.effectlib.effect.LoveEffect;
import com.herocraftonline.heroes.libs.slikey.effectlib.util.DynamicLocation;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class SkillHealingBloom extends ActiveSkill {

    public SkillHealingBloom(final Heroes plugin) {
        super(plugin, "HealingBloom");
        setDescription("Apply a Healing Bloom to party members within $1 blocks, healing them for $2 health over $3 second(s). You are only healed for $3 health from this effect.");
        setUsage("/skill healingbloom");
        setIdentifiers("skill healingbloom");
        setTypes(SkillType.SILENCEABLE, SkillType.AREA_OF_EFFECT, SkillType.HEALING, SkillType.ABILITY_PROPERTY_EARTH);
        setArgumentRange(0, 0);
    }

    @Override
    public String getDescription(final Hero hero) {
        final int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 15, false);

        final int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 2000, false);
        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 20000, false);

        double healing = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING_TICK, 17, false);
        healing = getScaledHealing(hero, healing);

        final String formattedHealing = Util.decFormat.format(healing * ((double) duration / (double) period));
        final String formattedSelfHealing = Util.decFormat.format((healing * ((double) duration / (double) period)) * Heroes.properties.selfHeal);
        final String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription().replace("$1", radius + "").replace("$2", formattedHealing).replace("$3", formattedDuration).replace("$4", formattedSelfHealing);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DURATION.node(), 20000);
        node.set(SkillSetting.RADIUS.node(), 15);
        node.set(SkillSetting.PERIOD.node(), 2000);
        node.set(SkillSetting.HEALING_TICK.node(), 11);
        node.set(SkillSetting.HEALING_INCREASE_PER_WISDOM.node(), 0.275);

        return node;
    }

    @Override
    public SkillResult use(final Hero hero, final String[] args) {
        final Player player = hero.getPlayer();

        final int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 15, false);
        final int radiusSquared = radius * radius;
        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 20000, false);
        double healing = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING_TICK, 17, false);
        healing = getScaledHealing(hero, healing);


        final int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 2000, false);


        broadcastExecuteText(hero);

        // Check if the hero has a party
        if (hero.hasParty()) {
            final Location playerLocation = player.getLocation();
            // Loop through the player's party members and add the effect as necessary
            for (final Hero member : hero.getParty().getMembers()) {
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

    public void playVisuals(final LivingEntity target, final int duration) {
        final int durationTicks = duration / 50;
        final int displayPeriod = 2;

        final LoveEffect visualEffect = new LoveEffect(effectLib);
        final DynamicLocation dynamicLoc = new DynamicLocation(target);
        visualEffect.setDynamicOrigin(dynamicLoc);
        visualEffect.disappearWithOriginEntity = true;

        visualEffect.particle = Particle.VILLAGER_HAPPY;
        visualEffect.period = displayPeriod;
        visualEffect.particleSize = 15;

        visualEffect.iterations = durationTicks / displayPeriod;
        dynamicLoc.addOffset(new Vector(0, 0.8, 0));

        effectLib.start(visualEffect);
    }
}