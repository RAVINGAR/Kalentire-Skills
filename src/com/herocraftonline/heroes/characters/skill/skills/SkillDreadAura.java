package com.herocraftonline.heroes.characters.skill.skills;

import java.util.List;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainHealthEvent;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

public class SkillDreadAura extends ActiveSkill {
    private String applyText;
    private String expireText;

    public SkillDreadAura(Heroes plugin) {
        super(plugin, "DreadAura");
        setDescription("You emit an aura of dread for $1 seconds. While active, you damage all enemies within $2 blocks every $3 seconds for $4 dark damage, and are healed for $5% of damage dealt. You cannot heal more than $6 health from this effect.");
        setUsage("/skill dreadaura");
        setArgumentRange(0, 0);
        setIdentifiers("skill dreadaura");
        setTypes(SkillType.DAMAGING, SkillType.AREA_OF_EFFECT, SkillType.ABILITY_PROPERTY_DARK, SkillType.HEALING, SkillType.BUFFING, SkillType.AGGRESSIVE);
    }

    @Override
    public String getDescription(Hero hero) {

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 3000, false);
        int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 1500, false);

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 60, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.0, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT);

        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5, false);

        double healMult = SkillConfigManager.getUseSetting(hero, this, "heal-mult", Double.valueOf(0.1), false);

        int maxHealing = SkillConfigManager.getUseSetting(hero, this, "maximum-healing", Integer.valueOf(200), false);

        String formattedDuration = Util.decFormat.format(duration / 1000.0);
        String formattedPeriod = Util.decFormat.format(period / 1000.0);
        String formattedDamage = Util.decFormat.format(damage);
        String formattedHealMult = Util.decFormat.format(healMult * 100.0);

        return getDescription().replace("$1", formattedDuration).replace("$2", radius + "").replace("$3", formattedPeriod).replace("$4", formattedDamage).replace("$5", formattedHealMult).replace("$6", maxHealing + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.RADIUS.node(), Integer.valueOf(7));
        node.set(SkillSetting.DAMAGE.node(), Integer.valueOf(28));
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), Double.valueOf(0.05));
        node.set("maximum-healing", Integer.valueOf(125));
        node.set("heal-mult", Double.valueOf(0.2));
        node.set(SkillSetting.PERIOD.node(), Integer.valueOf(1000));
        node.set(SkillSetting.DURATION.node(), Integer.valueOf(10000));
        node.set(SkillSetting.APPLY_TEXT.node(), Messaging.getSkillDenoter() + "%hero% is emitting an aura of dread!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), Messaging.getSkillDenoter() + "%hero% is no longer emitting an aura of dread.");

        return node;
    }

    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, Messaging.getSkillDenoter() + "%hero% is emitting an aura of dread!").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, Messaging.getSkillDenoter() + "%hero% is no longer emitting an aura of dread.").replace("%hero%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero);

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 3000, false);
        int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 1500, false);
        double healMult = SkillConfigManager.getUseSetting(hero, this, "heal-mult", Double.valueOf(0.77), false);
        int maxHealing = SkillConfigManager.getUseSetting(hero, this, "maximum-healing", Integer.valueOf(200), false);
        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5, false);

        hero.addEffect(new DreadAuraEffect(this, player, period, duration, healMult, maxHealing, radius));

        return SkillResult.NORMAL;
    }

    public class DreadAuraEffect extends PeriodicExpirableEffect {
        private double healMult;
        private int radius;
        private double totalHealthHealed = 0.0;
        private int maxHealing;

        public DreadAuraEffect(Skill skill, Player applier, long period, long duration, double healMult, int maxHealing, int radius) {
            super(skill, "DreadAura", applier, period, duration, applyText, expireText);

            types.add(EffectType.MAGIC);
            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.HEALING);
            types.add(EffectType.DARK);

            this.radius = radius;
            this.healMult = healMult;
            this.maxHealing = maxHealing;
        }

        @Override
        public void tickHero(Hero hero) {
            Player player = hero.getPlayer();

            double damage = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE, Integer.valueOf(60), false);
            double damageIncrease = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, Double.valueOf(1.0), false);
            damage += damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT);

            List<Entity> entities = player.getNearbyEntities(radius, radius, radius);
            for (Entity entity : entities) {
                if (!(entity instanceof LivingEntity)) {
                    continue;
                }

                LivingEntity target = (LivingEntity) entity;
                if (!damageCheck(player, target))
                    continue;

                addSpellTarget(target, hero);
                damageEntity(target, player, damage, DamageCause.MAGIC, false);

                double healing = damage * healMult;

                if (totalHealthHealed < maxHealing) {
                    Heroes.log(Level.INFO, "DreadAura Debug: HealthToHeal: " + healing);
                    if (healing + totalHealthHealed > maxHealing) {
                        healing = maxHealing - totalHealthHealed;
                        Heroes.log(Level.INFO, "DreadAura Debug: Hit Cap. New HealthToHeal: " + healing);
                    }

                    HeroRegainHealthEvent healEvent = new HeroRegainHealthEvent(hero, healing, skill);       // Bypass self heal nerf because this cannot be used on others.
                    Bukkit.getPluginManager().callEvent(healEvent);
                    if (!healEvent.isCancelled()) {
                        double finalHealing = healEvent.getAmount();
                        hero.heal(finalHealing);
                    }
                }
            }
        }

        @Override
        public void tickMonster(Monster monster) {}

        public int getRadius() {
            return radius;
        }

        public void setRadius(int radius) {
            this.radius = radius;
        }

        public double getHealMult() {
            return healMult;
        }

        public void setHealMult(double healMult) {
            this.healMult = healMult;
        }

        public int getMaximumHealing() {
            return maxHealing;
        }

        public void setMaximumHealing(int maxHealing) {
            this.maxHealing = maxHealing;
        }

        public double getTotalHealthHealed() {
            return totalHealthHealed;
        }

        public void setTotalHealthHealed(double totalHealthHealed) {
            this.totalHealthHealed = totalHealthHealed;
        }
    }
}
