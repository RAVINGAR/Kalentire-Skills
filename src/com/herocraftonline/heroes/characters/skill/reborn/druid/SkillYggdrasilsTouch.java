package com.herocraftonline.heroes.characters.skill.reborn.druid;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.GeometryUtil;
import com.herocraftonline.heroes.util.Util;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.LoveEffect;
import de.slikey.effectlib.util.DynamicLocation;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class SkillYggdrasilsTouch extends ActiveSkill {

    public SkillYggdrasilsTouch(Heroes plugin) {
        super(plugin, "YggdrasilsTouch");
        setDescription("You mark all nearby Allies with Yggdrasil's touch. After a short period Yggdrasil's touch explodes and heals you and all nearby allies for $1 in a $2 radius. This effect can stack if players group up for the explosion.");
        setNotes("/bind ytouch", "/skill ytouch");
        setUsage("/skill yggdrasilstouch");
        setIdentifiers("skill yggdrasilstouch", "skill ytouch");
        setArgumentRange(0, 0);
        setTypes(SkillType.HEALING, SkillType.AREA_OF_EFFECT, SkillType.SILENCEABLE);
    }

    @Override
    public String getDescription(Hero hero) {
        double healing = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.HEALING, false);
        double radius = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.RADIUS, false);
        int duration = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.DURATION, false);

        return getDescription()
                .replace("$1", Util.decFormat.format(healing))
                .replace("$2", Util.decFormat.format(radius));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.HEALING.node(), 60.0);
        config.set(SkillSetting.RADIUS.node(), 5.0);
        config.set(SkillSetting.DURATION.node(), 5000);
        return config;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero);

        final double radius = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.RADIUS, false);
        final double radiusSquared = radius * radius;
        double healing = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.HEALING, false);
        int duration = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.DURATION, false);

        final Location heroLoc = player.getLocation();
        if (hero.getParty() == null) {
            hero.addEffect(new YggdrasilsMark(this, player, duration, radius, radiusSquared, healing));
        } else {
            for (final Hero partyHero : hero.getParty().getMembers()){
                if (!player.getWorld().equals(partyHero.getPlayer().getWorld()))
                    continue;
                if (!(partyHero.getPlayer().getLocation().distanceSquared(heroLoc) <= radiusSquared))
                    continue;

                partyHero.addEffect(new YggdrasilsMark(this, player, duration, radius, radiusSquared, healing));
            }
        }

        return SkillResult.NORMAL;
    }

    public class YggdrasilsMark extends ExpirableEffect {
        private final double radius;
        private final double radiusSquared;
        private final double healing;

        private EffectManager effectManager;

        public YggdrasilsMark(Skill skill, Player applier, long duration, double radius, double radiusSquared, double healing) {
            super(skill, "YggdrasilsMark", applier, duration);

            this.radius = radius;
            this.radiusSquared = radiusSquared;
            this.healing = healing;

            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.HEALING);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);

            this.effectManager = new EffectManager(plugin);
            playHeartVisual(hero.getPlayer());
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);

            Player player = hero.getPlayer();
            // get everyone in the player's party
            final Location heroLoc = player.getLocation();

            if (!hero.hasParty())
                return;

            for (final Hero partyHero : hero.getParty().getMembers()) {
                if (!player.getWorld().equals(partyHero.getPlayer().getWorld()))
                    continue;
                if (!(partyHero.getPlayer().getLocation().distanceSquared(heroLoc) <= radiusSquared))
                    continue;

                if (partyHero.hasEffect("YggdrasilsMark")) {
                    if (!partyHero.tryHeal(hero, skill, healing))
                        continue;

                    // Our heal worked, explode
                    for (double r = 1; r < radius; r++) {
                        List<Location> particleLocations = GeometryUtil.circle(partyHero.getPlayer().getLocation(), 45, r / 2.0);
                        for (Location particleLocation : particleLocations) {
                            partyHero.getPlayer().getWorld().spawnParticle(Particle.TOTEM, particleLocation, 1, 0, 0.1, 0, 0.1);
                        }
                    }
                }
            }
        }

        //Visual Mark to signify all players with the heal explosion
        public void playHeartVisual(LivingEntity target) {
            final int durationTicks = (int) this.getDuration() / 50;
            final int displayPeriod = 2;

            LoveEffect visualEffect = new LoveEffect(effectManager);
            DynamicLocation dynamicLoc = new DynamicLocation(target);
            visualEffect.setDynamicOrigin(dynamicLoc);
            visualEffect.disappearWithOriginEntity = true;
            visualEffect.color = Color.fromRGB(208 ,255, 104);
            visualEffect.particle = Particle.REDSTONE;
            visualEffect.period = displayPeriod;
            visualEffect.particleSize = 15;

            visualEffect.iterations = durationTicks / displayPeriod;
            dynamicLoc.addOffset(new Vector(0, 0.8, 0));

            effectManager.start(visualEffect);
            effectManager.disposeOnTermination();
        }
    }
}