package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
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
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;

public class SkillLungingBite extends ActiveSkill {
    String applyText;
    String expireText;

    public SkillLungingBite(final Heroes plugin) {
        super(plugin, "LungingBite");
        setDescription("You lunge forward with transformed jaws, biting any targets in your path and dealing $1 damage. " +
                "You are healed by $2 for each target you kill with this ability. " +
                "While transformed, you are healed for $1 instead and any targets caught in your jaws will be held for $3 second(s).");
        setUsage("/skill lungingbite");
        setIdentifiers("skill lungingbite");
        setArgumentRange(0, 0);
        setTypes(SkillType.DAMAGING, SkillType.MOVEMENT_INCREASING, SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.AGGRESSIVE);
    }

    @Override
    public String getDescription(final Hero hero) {
        final double baseDamage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 35.0, true);
        final double bonusDmg = SkillConfigManager.getUseSetting(hero, this, "bonus-damage-per-level", 5.0, true);
        final int stuckInJawsDuration = SkillConfigManager.getUseSetting(hero, this, "transform-jaws-duration", 2500, false);

        final double damage = baseDamage + bonusDmg;
        final double normalHeal = damage / 2.0;
        final double actualDuration = stuckInJawsDuration / 1000.0;

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
                .replace("%hero%", "$2").replace("$hero$", "$2")
                .replace("%target%", "$1").replace("$target$", "$1");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.DAMAGE.node(), 35.0);
        config.set("damage-radius", 3.0);
        config.set("bonus-damage-per-level", 0.5);
        config.set("speed-boost-per-level", 0.00125);
        config.set("speed-mult", 1.0);
        config.set("transform-jaws-period", 200);
        config.set("transform-jaws-duration", 2500);
        config.set("transform-jaws-pull-power-reduction", 6.0);
        config.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% has clenched %target% in their jaws!");
        return config;
    }

    @Override
    public SkillResult use(final Hero hero, final String[] args) {
        final Player player = hero.getPlayer();

        PerformLungingBite(hero, player);

        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

    private void PerformLungingBite(final Hero hero, final Player player) {
        final double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 35.0, true);
        final double bonusDmg = SkillConfigManager.getUseSetting(hero, this, "bonus-damage-per-level", 5.0, true);

        final double speedMult = SkillConfigManager.getUseSetting(hero, this, "speed-mult", 1.0, true);
        final double boost = hero.getHeroLevel() * SkillConfigManager.getUseSetting(hero, this, "speed-boost-per-level", 0.000125, true);
        final double radius = SkillConfigManager.getUseSetting(hero, this, "damage-radius", 3.0, true);

        final int stuckInJawsPeriod = SkillConfigManager.getUseSetting(hero, this, "transform-jaws-period", 200, false);
        final int stuckInJawsDuration = SkillConfigManager.getUseSetting(hero, this, "transform-jaws-duration", 2500, false);
        final double stuckInJawsPullPowerReduction = SkillConfigManager.getUseSetting(hero, this, "transform-jaws-pull-power-reduction", 6.0, false);

        final Vector velocity = player.getLocation().getDirection().clone().setY(0.0D).multiply(speedMult + boost);
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 25, 0, 0.1, 0, 0.5);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0F, 1.0F);

        final ArrayList<LivingEntity> lungeEntitiesToHit = new ArrayList<>();
        final Skill skill = this;
        new BukkitRunnable() {
            final int maxTicks = 5;
            int ticks = 0;

            @Override
            public void run() {
                if (ticks == maxTicks) {
                    player.setFallDistance(-3f);
                    cancel();
                    return;
                }
                player.setVelocity(velocity);

                for (final Entity ent : player.getNearbyEntities(radius, radius, radius)) {
                    if (!(ent instanceof LivingEntity)) {
                        continue;
                    }
                    final LivingEntity target = (LivingEntity) ent;
                    if (!damageCheck(player, target)) {
                        continue;
                    }
                    if (lungeEntitiesToHit.contains(target)) {
                        continue;
                    }

                    addSpellTarget(target, hero);
                    lungeEntitiesToHit.add(target);

                    damageEntity(target, player, damage, EntityDamageEvent.DamageCause.ENTITY_ATTACK, false);
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PHANTOM_BITE, 1.0F, 1.0F);

                    if (target.isDead()) {
                        hero.heal(damage / 2);
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_DRINK, 0.5F, 0.5F);
                    } else if (hero.hasEffect("EnderBeastTransformed")) {
                        final CharacterTemplate targetCharacter = plugin.getCharacterManager().getCharacter(target);
                        final StuckInJawsEffect effect = new StuckInJawsEffect(skill, player, stuckInJawsPeriod, stuckInJawsDuration, stuckInJawsPullPowerReduction);
                        targetCharacter.addEffect(effect);
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    public class StuckInJawsEffect extends PeriodicExpirableEffect {

        private final double pullPowerReduction;

        StuckInJawsEffect(final Skill skill, final Player applier, final long period, final long duration, final double pullPowerReduction) {
            super(skill, "StuckInJaws", applier, period, duration, applyText, null);

            this.pullPowerReduction = pullPowerReduction;

            types.add(EffectType.HARMFUL);
            types.add(EffectType.PHYSICAL);
        }

        @Override
        public void tickHero(final Hero hero) {
            final Player player = hero.getPlayer();
            final Location applierLocation = applier.getLocation();
            moveVictimToApplier(player, applierLocation);
        }

        @Override
        public void tickMonster(final Monster monster) {
            final LivingEntity victim = monster.getEntity();
            final Location applierLocation = applier.getLocation();
            moveVictimToApplier(victim, applierLocation);
        }

        private void moveVictimToApplier(final LivingEntity victim, final Location applierLocation) {
            final Location victimLocation = victim.getLocation();
            victim.setVelocity(new Vector(0, 0, 0));
            final double xDir = (applierLocation.getX() - victimLocation.getX()) / pullPowerReduction;
            final double zDir = (applierLocation.getZ() - victimLocation.getZ()) / pullPowerReduction;
            final Vector v = new Vector(xDir, 0, zDir);
            victim.setVelocity(victim.getVelocity().add(v));
        }
    }
}
