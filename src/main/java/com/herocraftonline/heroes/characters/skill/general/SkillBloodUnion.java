package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.events.SkillDamageEvent;
import com.herocraftonline.heroes.api.events.SkillUseEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.BloodUnionEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.util.GeometryUtil;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.List;

public class SkillBloodUnion extends PassiveSkill implements Listenable {
    private final static String bloodUnionEffectName = "BloodUnionEffect";
    private static final Particle.DustOptions skillEffectDustOptions = new Particle.DustOptions(Color.RED, 1);
    private final Listener listener;

    public SkillBloodUnion(Heroes plugin) {
        super(plugin, "BloodUnion");
        setDescription("Your damaging abilities form a Blood Union with your opponents. Blood Union allows you to use " +
                "certain abilities, and also increases the effectiveness of others. Maximum Blood Union is $1. " +
                "BloodUnion resets upon switching from monsters to players, and will expire completely if not " +
                "increased after $2 second(s)." +
                "Additionally you convert $3% of your magic damage into health for you and all party members within a " +
                "$4 block radius.");
        setTypes(SkillType.BUFFING, SkillType.SILENCEABLE, SkillType.AREA_OF_EFFECT, SkillType.HEALING,
                SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.ABILITY_PROPERTY_DARK);

        listener = new BloodUnionListener(this);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.APPLY_TEXT.node(), "");
        config.set(SkillSetting.UNAPPLY_TEXT.node(), "");
        config.set(SkillSetting.PERIOD.node(), 25000);
        config.set("max-blood-union", 6);
        config.set("magic-heal-percent", 0.15);
        config.set("heal-party-radius", 12.0);
        return config;
    }

    @Override
    public String getDescription(Hero hero) {
        int period = SkillConfigManager.getUseSettingInt(hero, this, SkillSetting.PERIOD, false);
        int maxBloodUnion = SkillConfigManager.getUseSettingInt(hero, this, "max-blood-union", false);

        double healPercent = SkillConfigManager.getUseSettingDouble(hero, this, "magic-heal-percent", false);
        double radius = SkillConfigManager.getUseSettingDouble(hero, this, "heal-party-radius", false);

        return getDescription()
                .replace("$1", maxBloodUnion + "")
                .replace("$2", Util.decFormat.format(period / 1000.0))
                .replace("$3", Util.decFormat.format(healPercent * 100))
                .replace("$4", Util.decFormat.format(radius));
    }

    @Override
    public Listener getListener() {
        return listener;
    }

    private class BloodUnionListener implements Listener {
        private PassiveSkill skill;

        public BloodUnionListener(PassiveSkill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onSkillUse(SkillUseEvent event) {
            Hero hero = event.getHero();

            if (skill.hasPassive(hero)) {
                addBloodUnionEffect(hero);
            } else {
                removeBloodUnionEffect(hero);
            }
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onSkillDamage(SkillDamageEvent event) {
            if (!(event.getDamager().getEntity() instanceof Player) || !event.getSkill().isType(SkillType.ABILITY_PROPERTY_MAGICAL))
                return;

            // Make sure the hero has this skill
            Hero hero = plugin.getCharacterManager().getHero((Player) event.getDamager().getEntity());
            if (skill.hasPassive(hero)) {
                healHeroParty(hero, event.getDamage());
            }
        }
    }

    public void addBloodUnionEffect(Hero hero) {
        if (!hero.hasEffect(bloodUnionEffectName)) {
            int bloodUnionResetPeriod = SkillConfigManager.getUseSettingInt(hero, this, SkillSetting.PERIOD,false);
            int maxBloodUnion = SkillConfigManager.getUseSettingInt(hero, this, "max-blood-union", false);
            hero.addEffect(new BloodUnionEffect(this, bloodUnionResetPeriod, maxBloodUnion));
        }
    }

    public void removeBloodUnionEffect(Hero hero) {
        if (hero.hasEffect(bloodUnionEffectName)) {
            hero.removeEffect(hero.getEffect(bloodUnionEffectName));
        }
    }

    // Heals the hero and his party based on the specified damage
    private void healHeroParty(Hero hero, double damage) {
        double radius = SkillConfigManager.getUseSettingDouble(hero, this, "heal-party-radius", false);
        double radiusSquared = radius * radius;
        double healPercent = SkillConfigManager.getUseSettingDouble(hero, this, "magic-heal-percent", false);
        double healAmount = healPercent * damage;

        // Check if the hero has a party
        final Player healer = hero.getPlayer();
        if (!hero.hasParty()) {
            hero.tryHeal(hero, this, healAmount);
        } else {
            Location playerLocation = healer.getLocation();
            final World healerWorld = healer.getWorld();

            for (Hero member : hero.getParty().getMembers()) {
                final Player memberPlayer = member.getPlayer();
                if (!memberPlayer.getWorld().equals(healerWorld))
                    continue;
                if (memberPlayer.getLocation().distanceSquared(playerLocation) > radiusSquared)
                    continue;

                member.tryHeal(hero, this, healAmount);
            }
        }

        List<Location> circle = GeometryUtil.circle(healer.getLocation(), 36, 1.5);
        for (int i = 0; i < circle.size(); i++) {
            //healer.getWorld().spigot().playEffect(circle.get(i), org.bukkit.Effect.COLOURED_DUST, 0, 0, 0.2F, 1.5F, 0.2F, 0, 4, 16);
            healer.getWorld().spawnParticle(Particle.REDSTONE, circle.get(i), 4, 0.2F, 1.5F, 0.2F, 0, skillEffectDustOptions);
        }
    }
}