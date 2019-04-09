package com.herocraftonline.heroes.characters.skill.reborn.chronomancer;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainHealthEvent;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.Stacking;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.util.Util;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.SphereEffect;
import de.slikey.effectlib.util.DynamicLocation;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.jetbrains.annotations.NotNull;

public class SkillTimeDifferential extends TargettedSkill {

    public SkillTimeDifferential(Heroes plugin) {
        super(plugin, "TimeDifferential");
        setDescription("Restore your target's time to normal, dispelling any temporal buffs or debuffs, and absorbing their power to achieve an effect. " +
                "If used on an ally, they will be healed for $1 health and an additional $2 health per temporal buff. " +
                "If used on an enemy, they will take $3 damage and take an additional $4 damage per temporal debuff. ");
        setUsage("/skill timedifferential");
        setArgumentRange(0, 0);
        setIdentifiers("skill timedifferential");
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.SILENCEABLE, SkillType.DAMAGING, SkillType.MULTI_GRESSIVE, SkillType.NO_SELF_TARGETTING);
    }

    @Override
    public String getDescription(Hero hero) {
        double healing = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING, 40.0, false);
        double healingPerStack = SkillConfigManager.getUseSetting(hero, this, "healing-per-temporal-effect", 10.0, false);
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 40.0, false);
        double damagePerStack = SkillConfigManager.getUseSetting(hero, this, "damage-per-temporal-effect", 10.0, false);

        return getDescription()
                .replace("$1", Util.decFormat.format(healing))
                .replace("$2", Util.decFormat.format(healingPerStack))
                .replace("$3", Util.decFormat.format(damage))
                .replace("$4", Util.decFormat.format(damagePerStack));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.MAX_DISTANCE.node(), 12);
        config.set(SkillSetting.DAMAGE.node(), 20.0);
        config.set("damage-per-temporal-effect", 10.0);
        config.set(SkillSetting.HEALING.node(), 35.0);
        config.set("healing-per-temporal-effect", 15.0);
        config.set("healing-delay", 1500);
        config.set("damage-delay", 1000);
        return config;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {

        broadcastExecuteText(hero, target);

        CharacterTemplate ctTarget = plugin.getCharacterManager().getCharacter(target);
        if (hero.isAlliedTo(target)) {
            healTarget(hero, ctTarget);
        } else {
            damageTarget(hero, ctTarget, target);
        }

        return SkillResult.NORMAL;
    }
    
    private void healTarget(Hero hero, CharacterTemplate targetCT) {
        final Player player = hero.getPlayer();
        double baseHealing = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING, 40.0, false);
        final double healingPerStack = SkillConfigManager.getUseSetting(hero, this, "healing-per-temporal-effect", 10.0, false);

        double healing = baseHealing;
        for (Effect effect : targetCT.getEffects()) {
            if (!effect.isType(EffectType.TEMPORAL))
                continue;

            if (effect instanceof Stacking) {
                Stacking stack = (Stacking) effect;
                healing += (healingPerStack * stack.getStackCount());
            } else {
                healing += healingPerStack;
            }
            targetCT.removeEffect(effect);
        }

        final Skill skill = this;
        final LivingEntity target = targetCT.getEntity();
        final World world = target.getWorld();
        final Location loc = target.getLocation();
        final double finalHealing = getScaledHealing(hero, healing);
        final int delaySeconds = SkillConfigManager.getUseSetting(hero, this, "healing-delay", 1000, false);

        EffectManager em = new EffectManager(plugin);
        SphereEffect visualEffect = buildBaseVisualEffect(em, target, delaySeconds);
        visualEffect.color = Color.GREEN;

        visualEffect.callback = new Runnable() {
            @Override
            public void run() {
                if (target.getHealth() < 0  || target.isDead())
                    return;

                if (targetCT instanceof Hero) {
                    if (!tryHealHero()) {
                        return;
                    }
                } else {
                    targetCT.heal(finalHealing);
                }
                world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 1.0f, 1.0F);
            }

            private boolean tryHealHero() {
                Hero targetHero = (Hero) targetCT;
                HeroRegainHealthEvent hrhEvent = new HeroRegainHealthEvent(targetHero, finalHealing, skill, hero);
                plugin.getServer().getPluginManager().callEvent(hrhEvent);
                if (hrhEvent.isCancelled()) {
                    player.sendMessage("Your target had their healing prevented!");
                    return false;
                }
                targetHero.heal(hrhEvent.getDelta());
                return true;
            }
        };

        em.start(visualEffect);
        em.disposeOnTermination();
    }

    @NotNull
    private SphereEffect buildBaseVisualEffect(EffectManager em, LivingEntity target, int delaySeconds) {
        SphereEffect visualEffect = new SphereEffect(em);

        DynamicLocation dynamicLoc = new DynamicLocation(target);
        visualEffect.setDynamicOrigin(dynamicLoc);
        visualEffect.disappearWithOriginEntity = true;

        final int delayTicks = delaySeconds / 50;
        final int displayPeriod = 3;
        visualEffect.radius = 5;
        visualEffect.radiusIncrease = -0.5;
        visualEffect.period = displayPeriod;
        visualEffect.iterations = delayTicks / displayPeriod;

        visualEffect.particle = Particle.REDSTONE;
        visualEffect.particleCount = 3;
        return visualEffect;
    }

    private void damageTarget(Hero hero, CharacterTemplate ctTarget, LivingEntity target) {
        final Player player = hero.getPlayer();
        double baseDamage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 40.0, false);
        final double damagePerStack = SkillConfigManager.getUseSetting(hero, this, "damage-per-temporal-effect", 10.0, false);

        double damage = baseDamage;
        for (Effect effect : ctTarget.getEffects()) {
            if (!effect.isType(EffectType.TEMPORAL))
                continue;

            if (effect instanceof Stacking) {
                Stacking stack = (Stacking) effect;
                damage += (damagePerStack * stack.getStackCount());
            } else {
                damage += damagePerStack;
            }
            ctTarget.removeEffect(effect);
        }

        final Skill skill = this;
        final World world = target.getWorld();
        final Location loc = target.getLocation();
        final double finalDamage = damage;
        final int delaySeconds = SkillConfigManager.getUseSetting(hero, this, "damage-delay", 1000, false);

        EffectManager em = new EffectManager(plugin);
        SphereEffect visualEffect = buildBaseVisualEffect(em, target, delaySeconds);
        visualEffect.color = Color.ORANGE;

        visualEffect.callback = new Runnable() {
            @Override
            public void run() {
                if (target.getHealth() < 0  || target.isDead())
                    return;

                plugin.getDamageManager().addSpellTarget(target, hero, skill);
                damageEntity(target, player, finalDamage, DamageCause.MAGIC, false);
                world.playSound(loc, Sound.BLOCK_GLASS_BREAK, 1.0f, 1.0F);
            }
        };

        em.start(visualEffect);
        em.disposeOnTermination();
    }
}
