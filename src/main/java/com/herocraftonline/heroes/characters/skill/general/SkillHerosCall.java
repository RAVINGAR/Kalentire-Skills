package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class SkillHerosCall extends ActiveSkill {

    private final String buffEffectName = "HeroicCalling";
    private final String debuffEffectName = "HeroicPurpose";
    private String applyText;
    private String expireText;

    public SkillHerosCall(final Heroes plugin) {
        super(plugin, "HerosCall");
        setDescription("You taunt all nearby enemies in a $1 block radius around you. " +
                "The effect lasts for $2 second(s). ");
        setArgumentRange(0, 0);
        setUsage("/skill heroscall");
        setIdentifiers("skill heroscall");
        setTypes(SkillType.DEBUFFING, SkillType.AREA_OF_EFFECT, SkillType.AGGRESSIVE, SkillType.INTERRUPTING);
    }

    @Override
    public String getDescription(final Hero hero) {
        final double radius = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.RADIUS, false);
        final long duration = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.DURATION, false);

        return getDescription()
                .replace("$1", Util.decFormat.format(radius))
                .replace("$2", Util.decFormat.format((double) duration / 1000.0));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.ON_INTERRUPT_FORCE_COOLDOWN.node(), 2000);
        config.set(SkillSetting.RADIUS.node(), 8.0);
        config.set("transform-period", 500);
        config.set(SkillSetting.PERIOD.node(), 3000);
        config.set(SkillSetting.DURATION.node(), 6000);
        config.set("maximum-effective-distance", 25.0);
        config.set("pull-power-reduction", 6.0);
        config.set("taunt-required-fov", 30.0);
        config.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% is filled with a Hero's call and wishes to slay %hero%!");
        config.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% is no longer filled with heroic purpose!");
        return config;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT,
                        ChatComponents.GENERIC_SKILL + "%target% is filled with a Hero's call and wishes to slay %hero%!")
                .replace("%target%", "$1").replace("$target$", "$1")
                .replace("%hero%", "$2").replace("$hero$", "$2");

        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT,
                        ChatComponents.GENERIC_SKILL + "%target% is no longer filled with heroic purpose!")
                .replace("%target%", "$1").replace("$target$", "$1");
    }

    @Override
    public SkillResult use(final Hero hero, final String[] args) {
        final Player player = hero.getPlayer();

        final double radius = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.RADIUS, false);
        final long duration = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.DURATION, false);

        final int period = hero.hasEffect("EnderBeastTransformed")
                ? SkillConfigManager.getUseSetting(hero, this, "transform-period", 500, false)
                : SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 4000, false);

        final double maxDistance = SkillConfigManager.getUseSetting(hero, this, "maximum-effective-distance", 40.0, false);
        final double pullPowerReduction = SkillConfigManager.getUseSetting(hero, this, "pull-power-reduction", 6.0, false);
        final double maxAngle = SkillConfigManager.getUseSetting(hero, this, "taunt-required-fov", 30.0, false);

        broadcastExecuteText(hero);

        final List<CharacterTemplate> actualCallTargets = new ArrayList<>();
        final List<Entity> entities = hero.getPlayer().getNearbyEntities(radius, radius, radius);
        for (final Entity entity : entities) {
            if (!(entity instanceof LivingEntity)) {
                continue;
            }

            final LivingEntity target = (LivingEntity) entity;
            if (!damageCheck(player, target)) {
                continue;
            }

            final CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
            target.getWorld().spawnParticle(Particle.SQUID_INK, target.getLocation().add(0, 1, 0), 5, 1.0F, 0.5F, 1.0F, 0);
            if (targetCT.hasEffect(debuffEffectName)) {
                targetCT.removeEffect(hero.getEffect(debuffEffectName));
            }

            final HeroicPurposeEffect callEffect = new HeroicPurposeEffect(this, player, period, duration, maxDistance, maxAngle, pullPowerReduction);
            targetCT.addEffect(callEffect);
            if (targetCT instanceof Hero) {
                final long interruptCd = SkillConfigManager.getUseSetting(hero, this, SkillSetting.ON_INTERRUPT_FORCE_COOLDOWN, 2000, false);
                ((Hero) targetCT).interruptDelayedSkill(interruptCd);
            }
            actualCallTargets.add(targetCT);
        }

        hero.addEffect(new HeroicCallingEffect(this, player, duration + 1000, actualCallTargets));

        //player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WOLF_HOWL, 0.5F, 0.8F);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_AMBIENT, 0.5f, 0.5f);

        return SkillResult.NORMAL;
    }

    public class HeroicCallingEffect extends ExpirableEffect {

        private final List<CharacterTemplate> callTargets;

        public HeroicCallingEffect(final Skill skill, final Player applier, final long duration, final List<CharacterTemplate> callTargets) {
            super(skill, buffEffectName, applier, duration);
            this.callTargets = callTargets;
        }

        @Override
        public void removeFromHero(final Hero hero) {
            for (final CharacterTemplate target : callTargets) {
                if (target == null) // Does this happen if one of the players logs out? I have no idea tbh.
                {
                    continue;
                }
                if (target.hasEffect(debuffEffectName)) {
                    target.removeEffect(target.getEffect(debuffEffectName));
                }
            }
        }
    }

    public class HeroicPurposeEffect extends PeriodicExpirableEffect {
        private final double maxDistanceSquared;
        private final double pullPowerReduction;
        private final double maxAngleDegrees;

        HeroicPurposeEffect(final Skill skill, final Player applier, final long period, final long duration, final double maxDistance, final double maxAngle, final double pullPowerReduction) {
            super(skill, debuffEffectName, applier, period, duration, applyText, expireText);
            this.maxDistanceSquared = maxDistance * maxDistance;
            this.maxAngleDegrees = maxAngle * 0.5;

            this.pullPowerReduction = pullPowerReduction;

            types.add(EffectType.HARMFUL);
            types.add(EffectType.PHYSICAL);
            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.TAUNT);

            addPotionEffect(new PotionEffect(PotionEffectType.SLOW, (int) (duration / 1000 * 20), 1));
        }

        @Override
        public void tickHero(final Hero hero) {
            final Player victim = hero.getPlayer();
            final Hero currentlyCallingHero = plugin.getCharacterManager().getHero((Player) applier);
            if (currentlyCallingHero == null || !currentlyCallingHero.hasEffect(buffEffectName) || victim.getLocation().distanceSquared(applier.getLocation()) > maxDistanceSquared) {
                hero.removeEffect(this);
            }

            final Location callerLoc = applier.getLocation();
            faceTargetIfNecessary(victim, applier.getEyeLocation());
            pullToTarget(victim, callerLoc);
        }

        @Override
        public void tickMonster(final Monster monster) {
            final LivingEntity victim = monster.getEntity();
            final Hero currentlyCallingHero = plugin.getCharacterManager().getHero((Player) applier);
            if (currentlyCallingHero == null || !currentlyCallingHero.hasEffect(buffEffectName) || victim.getLocation().distanceSquared(applier.getLocation()) > maxDistanceSquared) {
                monster.removeEffect(this);
            }

            final Location callerLoc = applier.getLocation();
            faceTargetIfNecessary(victim, applier.getEyeLocation());
            pullToTarget(victim, callerLoc);

            monster.setTargetIfAble(applier);
        }

        private void faceTargetIfNecessary(final LivingEntity victim, final Location callerLocation) {
            final Location victimLocation = victim.getEyeLocation();
            final Vector difference = callerLocation.toVector().subtract(victimLocation.toVector());
            final double angleDegrees = Math.toDegrees(victimLocation.getDirection().angle(difference));

            if (angleDegrees > maxAngleDegrees) {
                final Vector dir = callerLocation.clone().subtract(victim.getEyeLocation()).toVector();
                final Location loc = victim.getLocation().setDirection(dir);
                victim.teleport(loc, PlayerTeleportEvent.TeleportCause.PLUGIN);
            }
        }

        private void pullToTarget(final LivingEntity victim, final Location callerLocation) {
            final Location victimLocation = victim.getLocation();
            final double xDir = (callerLocation.getX() - victimLocation.getX()) / pullPowerReduction;
            final double zDir = (callerLocation.getZ() - victimLocation.getZ()) / pullPowerReduction;
            final Vector v = new Vector(xDir, 0, zDir);
            victim.setVelocity(victim.getVelocity().add(v));
        }
    }
}
