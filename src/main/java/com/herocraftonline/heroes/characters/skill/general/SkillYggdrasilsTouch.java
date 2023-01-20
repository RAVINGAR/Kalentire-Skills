package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.libs.slikey.effectlib.effect.LoveEffect;
import com.herocraftonline.heroes.libs.slikey.effectlib.util.DynamicLocation;
import com.herocraftonline.heroes.util.GeometryUtil;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.List;

public class SkillYggdrasilsTouch extends ActiveSkill {

    public SkillYggdrasilsTouch(final Heroes plugin) {
        super(plugin, "YggdrasilsTouch");
        setDescription("You mark all nearby Allies with Yggdrasil's touch. After a short period Yggdrasil's touch explodes and heals you and all nearby allies");
        setUsage("/skill yggdrasilstouch");
        setArgumentRange(0, 0);
        setIdentifiers("skill yggdrasilstouch", "skill ytouch");
        setTypes(SkillType.HEALING, SkillType.AREA_OF_EFFECT, SkillType.SILENCEABLE);
    }

    @Override
    public String getDescription(final Hero hero) {
        final double healing = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING, 60, false);
        final double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5.0, false);
        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);

        return getDescription()
                .replace("$1", healing + "")
                .replace("$2", radius + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.HEALING.node(), 60.0);
        config.set(SkillSetting.RADIUS.node(), 5.0);
        config.set(SkillSetting.DURATION.node(), 5000);
        return config;
    }

    @Override
    public SkillResult use(final Hero hero, final String[] args) {
        final Player player = hero.getPlayer();

        if (hero.getParty() == null) {
            player.sendMessage("    " + ChatComponents.GENERIC_SKILL + "You must be in a party to use this ability!");
            return SkillResult.CANCELLED;
        }

        broadcastExecuteText(hero);

        final double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5, false);
        final double radiusSquared = radius * radius;
        final double healing = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING, 60.0, false);
        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);

        final Location heroLoc = player.getLocation();
        for (final Hero partyHero : hero.getParty().getMembers()) {
            if (!player.getWorld().equals(partyHero.getPlayer().getWorld())) {
                continue;
            }
            if (!(partyHero.getPlayer().getLocation().distanceSquared(heroLoc) <= radiusSquared)) {
                continue;
            }

            partyHero.addEffect(new YggdrasilsMark(this, player, duration, radius, radiusSquared, healing));
        }

        return SkillResult.NORMAL;
    }

    public static class YggdrasilsMark extends ExpirableEffect {
        private final double radius;
        private final double radiusSquared;
        private final double healing;

        public YggdrasilsMark(final Skill skill, final Player applier, final long duration, final double radius, final double radiusSquared, final double healing) {
            super(skill, "YggdrasilsMark", applier, duration);

            this.radius = radius;
            this.radiusSquared = radiusSquared;
            this.healing = healing;

            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.HEALING);
        }

        @Override
        public void applyToHero(final Hero hero) {
            super.applyToHero(hero);
            playHeartVisual(hero.getPlayer());
        }

        @Override
        public void removeFromHero(final Hero hero) {
            super.removeFromHero(hero);

            final Player player = hero.getPlayer();
            // get everyone in the player's party
            final Location heroLoc = player.getLocation();
            if (hero.getParty() == null) {
                return;
            }

            for (final Hero partyHero : hero.getParty().getMembers()) {
                if (!player.getWorld().equals(partyHero.getPlayer().getWorld())) {
                    continue;
                }
                if (!(partyHero.getPlayer().getLocation().distanceSquared(heroLoc) <= radiusSquared)) {
                    continue;
                }

                if (partyHero.hasEffect("YggdrasilsMark")) {
                    if (!partyHero.tryHeal(hero, skill, healing)) {
                        continue;
                    }

                    // Our heal worked, explode
                    for (double r = 1; r < radius; r++) {
                        final List<Location> particleLocations = GeometryUtil.circle(partyHero.getPlayer().getLocation(), 45, r / 2.0);
                        for (final Location particleLocation : particleLocations) {
                            partyHero.getPlayer().getWorld().spawnParticle(Particle.TOTEM, particleLocation, 1, 0, 0.1, 0, 0.1);
                        }
                    }
                }
            }
        }

        //Visual Mark to signify all players with the heal explosion
        public void playHeartVisual(final LivingEntity target) {
            final int durationTicks = (int) this.getDuration() / 50;
            final int displayPeriod = 2;

            final LoveEffect visualEffect = new LoveEffect(effectLib);
            final DynamicLocation dynamicLoc = new DynamicLocation(target);
            visualEffect.setDynamicOrigin(dynamicLoc);
            visualEffect.disappearWithOriginEntity = true;
            visualEffect.color = Color.fromRGB(145, 178, 71);
            visualEffect.particle = Particle.REDSTONE;
            visualEffect.period = displayPeriod;
            visualEffect.particleSize = 15;

            visualEffect.iterations = durationTicks / displayPeriod;
            dynamicLoc.addOffset(new Vector(0, 0.8, 0));

            effectLib.start(visualEffect);
        }
    }
}