package com.herocraftonline.heroes.characters.skill.reborn.enderbeast;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;

public class SkillLungingBite extends ActiveSkill {
    private String applyText;

    public SkillLungingBite(Heroes plugin) {
        super(plugin, "LungingBite");
        setDescription("You lunge forward with transformed jaws, biting any targets in your path and dealing $1 damage to each target you pass. " +
                "You are healed by $2 for each target you kill with this ability. " +
                "While transformed, you are healed for $1 instead and any targets caught in your jaws will be held for $3 second(s).");
        setUsage("/skill lungingbite");
        setIdentifiers("skill lungingbite");
        setArgumentRange(0, 0);
        setTypes(SkillType.DAMAGING, SkillType.MOVEMENT_INCREASING, SkillType.VELOCITY_INCREASING, SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.AGGRESSIVE);
    }

    public String getDescription(Hero hero) {
        double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE, 35.0, true);
        int stuckInJawsDuration = SkillConfigManager.getUseSetting(hero, this, "transform-jaws-duration", 2500, false);

        double killHeal = damage / 2.0;
        double actualDuration = stuckInJawsDuration / 1000.0;

        return getDescription()
                .replace("$1", Util.decFormat.format(damage))
                .replace("$2", Util.decFormat.format(damage / 2.0))
                .replace("$3", actualDuration + "");
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL
                + "%hero% has clenched %target% in their jaws!")
                .replace("%hero%", "$2")
                .replace("%target%", "$1");
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.DAMAGE.node(), 35.0);
        config.set(SkillSetting.DAMAGE_INCREASE.node(), 0.5);
        config.set(SkillSetting.RADIUS.node(), 3.0);
        config.set("speed-mult", 1.0);
        config.set("speed-mult-per-level", 0.00125);
        config.set("speed-mult-vertical-multiplier", 0.75);
        config.set("transform-jaws-period", 200);
        config.set("transform-jaws-duration", 2500);
        config.set("transform-jaws-pull-power-reduction", 6.0);
        config.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% has clenched %target% in their jaws!");
        return config;
    }

    public SkillResult use(Hero hero, String[] args) {
        final Player player = hero.getPlayer();

        PerformLungingBite(hero, player);

        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

    private void PerformLungingBite(Hero hero, Player player) {
        final double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE, 35.0, true);
        final double dashPower = SkillConfigManager.getScaledUseSettingDouble(hero, this, "speed-mult", 1.0, true);
        final double dashYMultiplier = SkillConfigManager.getScaledUseSettingDouble(hero, this, "speed-mult-vertical-multiplier", 0.75, true);

        double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 3.0, true);

        int stuckInJawsPeriod = SkillConfigManager.getUseSetting(hero, this, "transform-jaws-period", 200, false);
        int stuckInJawsDuration = SkillConfigManager.getUseSetting(hero, this, "transform-jaws-duration", 2500, false);
        double stuckInJawsPullPowerReduction = SkillConfigManager.getUseSetting(hero, this, "transform-jaws-pull-power-reduction", 6.0, false);

        final Vector velocity = player.getLocation().getDirection().multiply(new Vector(dashPower, dashPower * dashYMultiplier, dashPower));
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 25, 0, 0.1, 0, 0.5);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERDRAGON_FLAP, 1.0F, 1.0F);

        final ArrayList<LivingEntity> lungeEntitiesToHit = new ArrayList<>();
        Skill skill = this;
        new BukkitRunnable() {
            int ticks = 0;
            int maxTicks = 5;

            public void run() {
                if (ticks == maxTicks) {
                    player.setFallDistance(-3f);
                    cancel();
                    return;
                }
                player.setVelocity(velocity);

                for (Entity ent : player.getNearbyEntities(radius, radius, radius)) {
                    if (!(ent instanceof LivingEntity))
                        continue;
                    LivingEntity target = (LivingEntity) ent;
                    if (!damageCheck(player, target))
                        continue;
                    if (lungeEntitiesToHit.contains(target))
                        continue;

                    addSpellTarget(target, hero);
                    lungeEntitiesToHit.add(target);

                    damageEntity(target, player, damage, EntityDamageEvent.DamageCause.ENTITY_ATTACK, false);
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EVOCATION_FANGS_ATTACK, 1.0F, 1.0F);

                    if (target.isDead()) {
                        hero.heal(damage / 2);
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_DRINK, 0.5F, 0.5F);
                    } else if (hero.hasEffect("EnderBeastTransformed")) {
                        CharacterTemplate targetCharacter = plugin.getCharacterManager().getCharacter(target);
                        StuckInJawsEffect effect = new StuckInJawsEffect(skill, player, stuckInJawsPeriod, stuckInJawsDuration, stuckInJawsPullPowerReduction);
                        targetCharacter.addEffect(effect);
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    public class StuckInJawsEffect extends PeriodicExpirableEffect {

        private final double pullPowerReduction;

        StuckInJawsEffect(Skill skill, Player applier, long period, long duration, double pullPowerReduction) {
            super(skill, "StuckInJaws", applier, period, duration, applyText, null);

            this.pullPowerReduction = pullPowerReduction;

            types.add(EffectType.HARMFUL);
            types.add(EffectType.PHYSICAL);
        }

        @Override
        public void tickHero(Hero hero) {
            Player player = hero.getPlayer();
            Location applierLocation = applier.getLocation();
            moveVictimToApplier(player, applierLocation);
        }

        @Override
        public void tickMonster(Monster monster) {
            LivingEntity victim = monster.getEntity();
            Location applierLocation = applier.getLocation();
            moveVictimToApplier(victim, applierLocation);
        }

        private void moveVictimToApplier(LivingEntity victim, Location applierLocation) {
            Location victimLocation = victim.getLocation();
            victim.setVelocity(new Vector(0, 0, 0));
            double xDir = (applierLocation.getX() - victimLocation.getX()) / pullPowerReduction;
            double yDir = (applierLocation.getY() - victimLocation.getY()) / pullPowerReduction;
            double zDir = (applierLocation.getZ() - victimLocation.getZ()) / pullPowerReduction;
            final Vector v = new Vector(xDir, yDir * 0.5, zDir);
            victim.setVelocity(victim.getVelocity().add(v));
        }
    }
}
