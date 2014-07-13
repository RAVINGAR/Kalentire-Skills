package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;
import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.hooks.NCPExemptionManager;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.util.List;

public class SkillWhirlwind extends ActiveSkill {
    private String applyText;
    private String expireText;

    private boolean ncpEnabled = false;

    public SkillWhirlwind(Heroes plugin) {
        super(plugin, "Whirlwind");
        setDescription("Unleash a furious Whirlwind for $1 seconds. While active, you strike all enemies within $2 blocks every $3 seconds for $4 physical damage. You are slowed during the effect.");
        setUsage("/skill whirlwind");
        setArgumentRange(0, 0);
        setIdentifiers("skill whirlwind");
        setTypes(SkillType.DAMAGING, SkillType.AREA_OF_EFFECT, SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.AGGRESSIVE);

        if (Bukkit.getServer().getPluginManager().getPlugin("NoCheatPlus") != null) {
            ncpEnabled = true;
        }
    }

    @Override
    public String getDescription(Hero hero) {

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 3000, false);
        int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 1500, false);

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 60, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 1.0, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH);

        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5, false);

        String formattedDuration = Util.decFormat.format(duration / 1000.0);
        String formattedPeriod = Util.decFormat.format(period / 1000.0);
        String formattedDamage = Util.decFormat.format(damage);

        return getDescription().replace("$1", formattedDuration).replace("$2", radius + "").replace("$3", formattedPeriod).replace("$4", formattedDamage);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.RADIUS.node(), 5);
        node.set(SkillSetting.DAMAGE.node(), 60);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_STRENGTH.node(), 1.0);
        node.set(SkillSetting.PERIOD.node(), 500);
        node.set(SkillSetting.DURATION.node(), 5000);
        node.set("slow-amplifier", 1);
        node.set(SkillSetting.APPLY_TEXT.node(), Messaging.getSkillDenoter() + "%hero% is unleashing a powerful whirlwind!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), Messaging.getSkillDenoter() + "%target% is no longer whirlwinding!");

        return node;
    }

    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, Messaging.getSkillDenoter() + "%hero% is unleashing a powerful whirlwind!").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, Messaging.getSkillDenoter() + "%hero% is no longer whirlwinding!").replace("%hero%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero);

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 3000, false);
        int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 1500, false);
        int slowAmplifier = SkillConfigManager.getUseSetting(hero, this, "slow-amplifier", Integer.valueOf(1), false);
        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5, false);

        WhirlwindEffect effect = new WhirlwindEffect(this, player, period, duration, slowAmplifier, radius);

        hero.addEffect(effect);

        return SkillResult.NORMAL;
    }

    public class WhirlwindEffect extends PeriodicExpirableEffect {
        private int radius;

        public WhirlwindEffect(Skill skill, Player applier, long period, long duration, int slowAmplifier, int radius) {
            super(skill, "Whirlwind", applier, period, duration, applyText, expireText);

            types.add(EffectType.PHYSICAL);
            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.SLOW);

            this.setRadius(radius);

            int tickDuration = (int) ((duration / 1000) * 20);
            addMobEffect(2, tickDuration, slowAmplifier, false);
            //addMobEffect(8, tickDuration, 254, false);
        }

        public int getRadius() {
            return radius;
        }

        public void setRadius(int radius) {
            this.radius = radius;
        }

        @Override
        public void tickHero(Hero hero) {
            Player player = hero.getPlayer();


            if (ncpEnabled) {
                if (!player.isOp()) {
                    NCPExemptionEffect ncpExemptEffect = new NCPExemptionEffect(this.skill);
                    hero.addEffect(ncpExemptEffect);
                }
            }

            for (Effect effect : hero.getEffects()) {
                if (effect.isType(EffectType.STUN) || effect.isType(EffectType.DISABLE)) {
                    hero.removeEffect(this);
                    return;
                }
            }

            player.getWorld().playSound(player.getLocation(), Sound.BAT_LOOP, 0.6F, 0.6F);

            double damage = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE, Integer.valueOf(60), false);
            double damageIncrease = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 1.0, false);
            damage += damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH);

            boolean hitTarget = false;
            List<Entity> entities = hero.getPlayer().getNearbyEntities(radius, radius, radius);
            for (Entity entity : entities) {
                if (!(entity instanceof LivingEntity)) {
                    continue;
                }

                LivingEntity target = (LivingEntity) entity;
                if (!damageCheck(player, target))
                    continue;

                addSpellTarget(target, hero);
                damageEntity(target, player, damage, DamageCause.ENTITY_ATTACK, false);

                hitTarget = true;
            }

            if (hitTarget)
                player.getWorld().playSound(player.getLocation(), Sound.ANVIL_LAND, 0.3F, 1.6F);

            if (ncpEnabled) {
                if (!player.isOp()) {
                    if (hero.hasEffect("NCPExemptionEffect_FIGHT"))
                        hero.removeEffect(hero.getEffect("NCPExemptionEffect_FIGHT"));
                }
            }
        }

        @Override
        public void tickMonster(Monster monster) {}
    }

    private class NCPExemptionEffect extends Effect {

        public NCPExemptionEffect(Skill skill) {
            super(skill, "NCPExemptionEffect_FIGHT");
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            final Player player = hero.getPlayer();

            NCPExemptionManager.exemptPermanently(player, CheckType.FIGHT);
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            final Player player = hero.getPlayer();

            NCPExemptionManager.unexempt(player, CheckType.FIGHT);
        }
    }
}
