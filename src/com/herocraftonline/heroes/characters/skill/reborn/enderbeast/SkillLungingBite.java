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
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;

public class SkillLungingBite extends ActiveSkill
{
    String applyText;
    String expireText;

    public SkillLungingBite(Heroes plugin)
    {
        super(plugin, "LungingBite");
        setUsage("/skill lungingbite");
        setIdentifiers("skill lungingbite");
        setArgumentRange(0, 0);
        setDescription("You lunge forward with transformed jaws, biting any targets in your path and dealing $1 damage. "
                + "You are healed by $2 for each target you kill with this ability. "
                + "While fully transformed, you are healed for $1 instead, and any targets caught in your jaws will be held for $3 seconds.");
        setTypes(SkillType.DAMAGING, SkillType.MOVEMENT_INCREASING, SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.AGGRESSIVE);
    }

    public String getDescription(Hero hero)
    {
        double baseDamage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 35, true);
        double bonusDmg = SkillConfigManager.getUseSetting(hero, this, "bonus-baseDamage-per-level", 5, true);
        int stuckInJawsDuration = SkillConfigManager.getUseSetting(hero, this, "transform-jaws-duration", 2500, false);

        double damage = baseDamage + bonusDmg;
        double normalHeal = damage / 2d;
        double actualDuration = stuckInJawsDuration / 1000d;

        return getDescription().replace("$1", damage + "").replace("$2", damage / 2d + "").replace("$3", actualDuration + "");
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL
                + "%hero% has clenched %target% in their jaws!")
                .replace("%hero%", "$2")
                .replace("%target%", "$1");
    }

    public ConfigurationSection getDefaultConfig()
    {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.DAMAGE.node(), 35.0);
        config.set("damage-radius", 3.0);
        config.set("bonus-damage-per-level", 5.0);
        config.set("speed-boost-per-level", 0.000125);
        config.set("speed-mult", 1.0001);
        config.set("transform-jaws-period", 200);
        config.set("transform-jaws-duration", 2500);
        config.set("transform-jaws-pull-power-reduction", 6.0);
        config.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% has clenched %target% in their jaws!");
        return config;
    }

    public SkillResult use(Hero hero, String[] args)
    {
        final Player player = hero.getPlayer();

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 35, true);
        double bonusDmg = SkillConfigManager.getUseSetting(hero, this, "bonus-damage-per-level", 5, true);

        final double speedMult = SkillConfigManager.getUseSetting(hero, this, "speed-mult", 1.0, true);
        double boost = hero.getHeroLevel() * SkillConfigManager.getUseSetting(hero, this, "speed-boost-per-level", 0.000125, true);
        double radius = SkillConfigManager.getUseSetting(hero, this, "damage-radius", 3.0, true);

        int stuckInJawsPeriod = SkillConfigManager.getUseSetting(hero, this, "transform-jaws-period", 200, false);
        int stuckInJawsDuration = SkillConfigManager.getUseSetting(hero, this, "transform-jaws-duration", 2500, false);
        double stuckInJawsPullPowerReduction = SkillConfigManager.getUseSetting(hero, this, "transform-jaws-pull-power-reduction", 6.0, false);

        PerformLungingBite(hero, player, damage, speedMult, boost, radius, stuckInJawsPeriod, stuckInJawsDuration, stuckInJawsPullPowerReduction);

        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

    private void PerformLungingBite(Hero hero, Player player, double damage, double speedMult,
                                    double boost, double radius, int stuckInJawsPeriod,
                                    int stuckInJawsDuration, double stuckInJawsPullPowerReduction) {

        final Vector velocity = player.getLocation().getDirection().clone().setY(0.0D).multiply(speedMult + boost);
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 25, 0, 0.1, 0, 0.5);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0F, 1.0F);

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

                for (Entity ent : player.getNearbyEntities(radius, radius, radius))
                {
                    if (!(ent instanceof LivingEntity))
                        continue;
                    LivingEntity target = (LivingEntity) ent;
                    if (!damageCheck(player, target))
                        continue;
                    if (lungeEntitiesToHit.contains(target))
                        continue;

                    int currentHitCount = lungeEntitiesToHit.size();
                    final double finalDamage = ApplyAoEDiminishingReturns(damage, currentHitCount);

                    addSpellTarget(target, hero);
                    lungeEntitiesToHit.add(target);

                    damageEntity(target, player, finalDamage, EntityDamageEvent.DamageCause.ENTITY_ATTACK, false);
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PHANTOM_BITE, 1.0F, 1.0F);

                    if (target.isDead()) {
                        hero.heal(damage / 2);
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_DRINK, 0.5F, 0.5F);
                    } else if (hero.hasEffect("EnderBeastTransformed")){
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

        private void moveVictimToApplier(LivingEntity victim, Location applierLocation)
        {
            Location victimLocation = victim.getLocation();
            victim.setVelocity(new Vector(0,0,0));
            double xDir = (applierLocation.getX() - victimLocation.getX()) / pullPowerReduction;
            double zDir = (applierLocation.getZ() - victimLocation.getZ()) / pullPowerReduction;
            final Vector v = new Vector(xDir, 0, zDir);
            victim.setVelocity(victim.getVelocity().add(v));
        }
    }
    private double ApplyAoEDiminishingReturns(double damage, int numberOfTargets)
    {
        return ApplyAoEDiminishingReturns(damage, numberOfTargets, 3, 0.15, 0.75);
    }

    private double ApplyAoEDiminishingReturns(double damage, int numberOfTargets, int maxTargetsBeforeDiminish, double diminishPercent, double maxDiminishPercent)
    {
        if (numberOfTargets > maxTargetsBeforeDiminish) {
            double totalDiminishPercent = (diminishPercent * numberOfTargets);
            if (totalDiminishPercent > maxDiminishPercent)
                totalDiminishPercent = maxDiminishPercent;
            return totalDiminishPercent / damage * 100;
        } else {
            return damage;
        }
    }
}
