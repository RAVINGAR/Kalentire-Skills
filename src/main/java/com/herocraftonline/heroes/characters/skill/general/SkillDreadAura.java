package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainHealthEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.GeometryUtil;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.util.List;

//TODO make into a passive skill
//import de.slikey.effectlib.EffectManager;

public class SkillDreadAura extends ActiveSkill {

    private String applyText;
    private String expireText;

    public SkillDreadAura(final Heroes plugin) {
        super(plugin, "DreadAura");
        setDescription("Emit an aura of Dread. While active, every $1 seconds you damage all enemies within $2 blocks" +
                " for $3 dark damage, and are healed for $4% of damage dealt. Requires $5 mana to activate, $6 mana " +
                "per tick to maintain this effect, and you cannot heal more than $7 health in a single instance.");
        setUsage("/skill dreadaura");
        setArgumentRange(0, 0);
        setIdentifiers("skill dreadaura");
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.DAMAGING, SkillType.AREA_OF_EFFECT,
                SkillType.ABILITY_PROPERTY_DARK, SkillType.HEALING, SkillType.BUFFING);
    }

    @Override
    public String getDescription(final Hero hero) {
        final int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 1500, false);

        final double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE, 60, false);

        final int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5, false);
        final double healMult = SkillConfigManager.getUseSetting(hero, this, "heal-mult", 0.1, false);
        final int maxHealing = SkillConfigManager.getUseSetting(hero, this, "maximum-healing-per-tick", 200, false);
        final int manaActivate = SkillConfigManager.getUseSetting(hero, this, "mana-activate", 150, false);
        final int manaTick = SkillConfigManager.getUseSetting(hero, this, "mana-tick", 13, false);

        return getDescription().replace("$1", Util.decFormat.format(period / 1000.0))
                .replace("$2", radius + "")
                .replace("$3", Util.decFormat.format(damage))
                .replace("$4", Util.decFormat.format(healMult * 100.0))
                .replace("$5", manaActivate + "")
                .replace("$6", manaTick + "")
                .replace("$7", maxHealing + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.RADIUS.node(), 7);
        node.set(SkillSetting.DAMAGE.node(), 28);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 0.05);
        node.set("maximum-healing-per-tick", (double) 25);
        node.set("mana-activate", 150);
        node.set("mana-tick", 7);
        node.set("heal-mult", 0.2);
        node.set(SkillSetting.PERIOD.node(), 3000);
        node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "$1 is emitting an aura of dread!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "$1 is no longer emitting an aura of dread.");

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT,
                ChatComponents.GENERIC_SKILL + "%hero% is emitting an aura of dread!").replace("%hero%", "$1").replace("$hero$", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT,
                ChatComponents.GENERIC_SKILL + "%hero% is no longer emitting an aura of dread.").replace("%hero%", "$1").replace("$hero$", "$1");
    }

    @Override
    public SkillResult use(final Hero hero, final String[] args) {
        if (hero.hasEffect("DreadAura")) {
            hero.removeEffect(hero.getEffect("DreadAura"));
            return SkillResult.REMOVED_EFFECT;
        }

        final int currentMana = hero.getMana();
        final int manaActivate = SkillConfigManager.getUseSetting(hero, this, "mana-activate", 150, false);

        if (manaActivate > currentMana) {
            return SkillResult.LOW_MANA; // Sends a "Not enough mana!" message on its own.
        }
        hero.setMana(currentMana - manaActivate);

        final int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 1500, false);
        final int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5, false);
        final double healMult = SkillConfigManager.getUseSetting(hero, this, "heal-mult", 0.1, false);
        final int maxHealingPerTick = SkillConfigManager.getUseSetting(hero, this, "maximum-healing-per-tick", 200, false);
        final int manaTick = SkillConfigManager.getUseSetting(hero, this, "mana-tick", 13, false);

        hero.addEffect(new DreadAuraEffect(this, period, manaTick, radius, healMult, maxHealingPerTick));

        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5F, 1.0F);
        return SkillResult.NORMAL;
    }

    public class DreadAuraEffect extends PeriodicEffect {

        private final int manaTick;

        private int radius;
        private double healMult;
        private double maxHealingPerTick;

        public DreadAuraEffect(final Skill skill, final long period, final int manaTick, final int radius, final double healMult, final double maxHealingPerTick) {
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
        public void applyToHero(final Hero hero) {
            super.applyToHero(hero);

            if (applyText != null && applyText.length() > 0) {
                final Player player = hero.getPlayer();
                if (hero.hasEffectType(EffectType.SILENT_ACTIONS)) {
                    player.sendMessage("    " + applyText.replace("$1", player.getName()));
                } else {
                    broadcast(player.getLocation(), "    " + applyText, player.getName());
                }
            }
        }

        @Override
        public void removeFromHero(final Hero hero) {
            super.removeFromHero(hero);

            if (expireText != null && expireText.length() > 0) {
                final Player player = hero.getPlayer();
                if (hero.hasEffectType(EffectType.SILENT_ACTIONS)) {
                    player.sendMessage("    " + expireText.replace("$1", player.getName()));
                } else {
                    broadcast(player.getLocation(), "    " + expireText, player.getName());
                }
            }
        }

        @Override
        public void tickHero(final Hero hero) {
            super.tickHero(hero);

            // Remove the effect if they don't have enough mana
            if (hero.getMana() < manaTick) {
                hero.removeEffect(this);
                return;
            } else {      // They have enough mana--continue
                // Drain the player's mana
                hero.setMana(hero.getMana() - manaTick);
            }
            
            /*AreaOfEffectAnimation aoe = new AreaOfEffectAnimation(new EffectManager(this.plugin), SkillType.ABILITY_PROPERTY_DARK, radius);
            aoe.setEntity(hero.getPlayer());
            aoe.run();*/

            final Player player = hero.getPlayer();

            for (double r = 1; r < radius * 2; r++) {
                final List<Location> particleLocations = GeometryUtil.circle(player.getLocation(), 36, r / 2);
                for (final Location particleLocation : particleLocations) {
                    //player.getWorld().spigot().playEffect(particleLocations.get(i), Effect.WITCH_MAGIC, 0, 0, 0, 0.1F, 0, 0.1F, 1, 16);
                    player.getWorld().spawnParticle(Particle.SPELL_WITCH, particleLocation, 1, 0, 0.1, 0, 0.1);
                }
            }

            final double damage = SkillConfigManager.getScaledUseSettingDouble(hero, skill, SkillSetting.DAMAGE, 60, false);
            final double totalHealthHealed = 0;

            final List<Entity> entities = player.getNearbyEntities(radius, radius, radius);
            for (final Entity entity : entities) {
                if (!(entity instanceof LivingEntity)) {
                    continue;
                }

                final LivingEntity target = (LivingEntity) entity;
                if (!damageCheck(player, target)) {
                    continue;
                }

                addSpellTarget(target, hero);
                damageEntity(target, player, damage, DamageCause.MAGIC, false);

                double healing = damage * healMult;

                if (totalHealthHealed < maxHealingPerTick) {
                    if (healing + totalHealthHealed > maxHealingPerTick) {
                        healing = maxHealingPerTick - totalHealthHealed;
                        // Heroes.log(Level.INFO, "DreadAura Debug: Hit Cap. New HealthToHeal: " + healing);
                    }

                    // Bypass self heal nerf because this cannot be used on others.
                    final HeroRegainHealthEvent healEvent = new HeroRegainHealthEvent(hero, healing, skill);
                    Bukkit.getPluginManager().callEvent(healEvent);
                    if (!healEvent.isCancelled()) {
                        final double finalHealing = healEvent.getDelta();
                        hero.heal(finalHealing);
                    }
                }
            }
        }

        public int getRadius() {
            return radius;
        }

        public void setRadius(final int radius) {
            this.radius = radius;
        }

        public double getHealMult() {
            return healMult;
        }

        public void setHealMult(final double healMult) {
            this.healMult = healMult;
        }

        public double getMaxHealingPerTick() {
            return maxHealingPerTick;
        }

        public void setMaxHealingPerTick(final double maxHealingPerTick) {
            this.maxHealingPerTick = maxHealingPerTick;
        }
    }
}
