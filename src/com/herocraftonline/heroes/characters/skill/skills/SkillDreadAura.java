package com.herocraftonline.heroes.characters.skill.skills;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
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
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicEffect;
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
        setDescription("Emit an aura of Dread. While active, every $1 seconds you damage all enemies within $2 blocks for $3 dark damage, and are healed for $4% of damage dealt. Requires $5 mana per tick to maintain this effect, and you cannot heal more than $6 health in a single instance.");
        setUsage("/skill dreadaura");
        setArgumentRange(0, 0);
        setIdentifiers("skill dreadaura");
        setTypes(SkillType.DAMAGING, SkillType.AREA_OF_EFFECT, SkillType.ABILITY_PROPERTY_DARK, SkillType.HEALING, SkillType.BUFFING);
    }

    @Override
    public String getDescription(Hero hero) {
        int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, Integer.valueOf(1500), false);

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, Integer.valueOf(60), false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, Double.valueOf(1.0), false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT);

        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, Integer.valueOf(5), false);

        double healMult = SkillConfigManager.getUseSetting(hero, this, "heal-mult", Double.valueOf(0.1), false);

        int maxHealing = SkillConfigManager.getUseSetting(hero, this, "maximum-healing-per-tick", Integer.valueOf(200), false);

        int manaTick = SkillConfigManager.getUseSetting(hero, this, "mana-tick", Integer.valueOf(13), false);

        String formattedPeriod = Util.decFormat.format(period / 1000.0);
        String formattedDamage = Util.decFormat.format(damage);
        String formattedHealMult = Util.decFormat.format(healMult * 100.0);

        return getDescription().replace("$1", formattedPeriod).replace("$2", radius + "").replace("$3", formattedDamage).replace("$4", formattedHealMult).replace("$5", manaTick + "").replace("$6", maxHealing + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.RADIUS.node(), Integer.valueOf(7));
        node.set(SkillSetting.DAMAGE.node(), Integer.valueOf(28));
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), Double.valueOf(0.05));
        node.set("maximum-healing-per-tick", Double.valueOf(25));
        node.set("mana-tick", Integer.valueOf(7));
        node.set("heal-mult", Double.valueOf(0.2));
        node.set(SkillSetting.PERIOD.node(), Integer.valueOf(3000));
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
    public SkillResult use(Hero hero, String args[]) {
        if (hero.hasEffect("DreadAura")) {
            hero.removeEffect(hero.getEffect("DreadAura"));
            return SkillResult.REMOVED_EFFECT;
        }

        int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, Integer.valueOf(1500), false);
        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, Integer.valueOf(5), false);
        double healMult = SkillConfigManager.getUseSetting(hero, this, "heal-mult", Double.valueOf(0.1), false);
        int maxHealingPerTick = SkillConfigManager.getUseSetting(hero, this, "maximum-healing-per-tick", Integer.valueOf(200), false);
        int manaTick = SkillConfigManager.getUseSetting(hero, this, "mana-tick", Integer.valueOf(13), false);

        hero.addEffect(new DreadAuraEffect(this, period, manaTick, radius, healMult, maxHealingPerTick));

        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.WITHER_SPAWN, 0.5F, 1.0F);
        return SkillResult.NORMAL;
    }

    public class DreadAuraEffect extends PeriodicEffect {

        private final int manaTick;

        private int radius;
        private double healMult;
        private double maxHealingPerTick;

        public DreadAuraEffect(Skill skill, long period, int manaTick, int radius, double healMult, double maxHealingPerTick) {
            super(skill, "DreadAura", period);

            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.AREA_OF_EFFECT);
            types.add(EffectType.DAMAGING);
            types.add(EffectType.HEALING);
            types.add(EffectType.DARK);

            this.manaTick = manaTick;
            this.healMult = healMult;
            this.maxHealingPerTick = maxHealingPerTick;
            this.radius = radius;
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);

            if (applyText != null && applyText.length() > 0) {
                Player player = hero.getPlayer();
                if (hero.hasEffectType(EffectType.SILENT_ACTIONS))
                    Messaging.send(player, applyText, player.getName());
                else
                    broadcast(player.getLocation(), "    " + applyText, player.getName());
            }
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);

            if (expireText != null && expireText.length() > 0) {
                final Player player = hero.getPlayer();
                if (hero.hasEffectType(EffectType.SILENT_ACTIONS))
                    Messaging.send(player, expireText, player.getName());
                else
                    broadcast(player.getLocation(), "    " + expireText, player.getName());
            }
        }

        @Override
        public void tickHero(Hero hero) {
            super.tickHero(hero);

            // Remove the effect if they don't have enough mana
            if (hero.getMana() < manaTick) {
                hero.removeEffect(this);
                return;
            }
            else {      // They have enough mana--continue
                // Drain the player's mana
                hero.setMana(hero.getMana() - manaTick);
            }
            
            Player player = hero.getPlayer();

            double damage = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE, Integer.valueOf(60), false);
            double damageIncrease = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, Double.valueOf(1.0), false);
            damage += damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT);

            double totalHealthHealed = 0;

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

                if (totalHealthHealed < maxHealingPerTick) {
                    if (healing + totalHealthHealed > maxHealingPerTick) {
                        healing = maxHealingPerTick - totalHealthHealed;
                        // Heroes.log(Level.INFO, "DreadAura Debug: Hit Cap. New HealthToHeal: " + healing);
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

        public double getMaxHealingPerTick() {
            return maxHealingPerTick;
        }

        public void setMaxHealingPerTick(double maxHealingPerTick) {
            this.maxHealingPerTick = maxHealingPerTick;
        }
    }
}
