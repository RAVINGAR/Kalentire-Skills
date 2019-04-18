package com.herocraftonline.heroes.characters.skill.reborn.enderbeast;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.ChatColor;
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

    private String buffEffectName = "HeroicCalling";
    private String debuffEffectName = "HeroicPurpose";
    private String applyText;
    private String expireText;

    public SkillHerosCall(Heroes plugin) {
        super(plugin, "HerosCall");
        setDescription("You loose a dreadful howl, imposing your beastly presence in a $1 block radius. " +
                "All those who hear the call will have their inner dragon slayer awoken. " +
                "The beast " + ChatColor.BOLD + ChatColor.ITALIC + "must" + ChatColor.RESET + " be slain. The effect lasts for $2 second(s). " +
                "Not very effective if you are in your human form.");
        setArgumentRange(0, 0);
        setUsage("/skill heroscall");
        setIdentifiers("skill heroscall");
        setTypes(SkillType.DEBUFFING, SkillType.AREA_OF_EFFECT, SkillType.AGGRESSIVE, SkillType.INTERRUPTING);
    }

    @Override
    public String getDescription(Hero hero) {
        double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 8.0, false);
        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 4000, false);

        return getDescription()
                .replace("$1", Util.decFormat.format(radius))
                .replace("$2", Util.decFormat.format((double) duration / 1000.0));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
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

    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT,
                ChatComponents.GENERIC_SKILL + "%target% is filled with a Hero's call and wishes to slay %hero%!")
                .replace("%target%", "$1")
                .replace("%hero%", "$2");

        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT,
                ChatComponents.GENERIC_SKILL + "%target% is no longer filled with heroic purpose!")
                .replace("%target%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 8.0, false);
        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 4000, false);

        int period = hero.hasEffect("EnderBeastTransformed")
                ? SkillConfigManager.getUseSetting(hero, this, "transform-period", 500, false)
                : SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 4000, false);

        double maxDistance = SkillConfigManager.getUseSetting(hero, this, "maximum-effective-distance", 40.0, false);
        double pullPowerReduction = SkillConfigManager.getUseSetting(hero, this, "pull-power-reduction", 6.0, false);
        double maxAngle = SkillConfigManager.getUseSetting(hero, this, "taunt-required-fov", 30.0, false);

        broadcastExecuteText(hero);

        List<CharacterTemplate> actualCallTargets = new ArrayList<CharacterTemplate>();
        List<Entity> entities = hero.getPlayer().getNearbyEntities(radius, radius, radius);
        for (Entity entity : entities) {
            if (!(entity instanceof LivingEntity)) {
                continue;
            }

            LivingEntity target = (LivingEntity) entity;
            if (!damageCheck(player, target))
                continue;

            CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
            target.getWorld().spawnParticle(Particle.SQUID_INK, target.getLocation().add(0, 1, 0), 5, 1.0F, 0.5F, 1.0F, 0);
            if (targetCT.hasEffect(debuffEffectName))
                targetCT.removeEffect(hero.getEffect(debuffEffectName));

            HeroicPurposeEffect callEffect = new HeroicPurposeEffect(this, player, period, duration, maxDistance, maxAngle, pullPowerReduction);
            targetCT.addEffect(callEffect);
            if (targetCT instanceof Hero) {
                ((Hero) targetCT).interruptDelayedSkill();
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

        public HeroicCallingEffect(Skill skill, Player applier, long duration, List<CharacterTemplate> callTargets) {
            super(skill, buffEffectName, applier, duration);
            this.callTargets = callTargets;
        }

        @Override
        public void removeFromHero(Hero hero) {
            for (CharacterTemplate target : callTargets) {
                if (target == null) // Does this happen if one of the players logs out? I have no idea tbh.
                    continue;
                if (target.hasEffect(debuffEffectName))
                    target.removeEffect(target.getEffect(debuffEffectName));
            }
        }
    }

    public class HeroicPurposeEffect extends PeriodicExpirableEffect {
        private final double maxDistanceSquared;
        private final double pullPowerReduction;
        private final double maxAngleDegrees;

        HeroicPurposeEffect(Skill skill, Player applier, long period, long duration, double maxDistance, double maxAngle, double pullPowerReduction) {
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
        public void tickHero(Hero hero) {
            Player victim = hero.getPlayer();
            Hero currentlyCallingHero = plugin.getCharacterManager().getHero((Player) applier);
            if (currentlyCallingHero == null || !currentlyCallingHero.hasEffect(buffEffectName) || victim.getLocation().distanceSquared(applier.getLocation()) > maxDistanceSquared)
                hero.removeEffect(this);

            Location targetLocation = applier.getLocation();
            faceTarget(victim, targetLocation);
            pullToTarget(victim, targetLocation);
        }

        @Override
        public void tickMonster(Monster monster) {
            LivingEntity victim = monster.getEntity();
            Hero currentlyCallingHero = plugin.getCharacterManager().getHero((Player) applier);
            if (currentlyCallingHero == null || !currentlyCallingHero.hasEffect(buffEffectName) || victim.getLocation().distanceSquared(applier.getLocation()) > maxDistanceSquared)
                monster.removeEffect(this);

            Location targetLocation = applier.getLocation();
            faceTarget(victim, targetLocation);
            pullToTarget(victim, targetLocation);

            monster.setTargetIfAble(applier);
        }

        private void faceTarget(LivingEntity victim, Location callerLocation) {
            Location victimLocation = victim.getLocation();
            Vector difference = callerLocation.toVector().subtract(victimLocation.toVector());
            double angleDegrees = Math.toDegrees(victimLocation.getDirection().angle(difference));

            if (angleDegrees > maxAngleDegrees) {
                Vector dir = callerLocation.clone().subtract(victim.getEyeLocation()).toVector();
                Location loc = victim.getLocation().setDirection(dir);
                victim.teleport(loc, PlayerTeleportEvent.TeleportCause.PLUGIN);
            }
        }

        private void pullToTarget(LivingEntity victim, Location callerLocation) {
            Location victimLocation = victim.getLocation();
            double xDir = (callerLocation.getX() - victimLocation.getX()) / pullPowerReduction;
            double zDir = (callerLocation.getZ() - victimLocation.getZ()) / pullPowerReduction;
            final Vector v = new Vector(xDir, 0, zDir);
            victim.setVelocity(victim.getVelocity().add(v));
        }
    }
}
