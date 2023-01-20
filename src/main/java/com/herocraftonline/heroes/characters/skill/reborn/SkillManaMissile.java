package com.herocraftonline.heroes.characters.skill.reborn;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.SkillUseEvent;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.tools.BasicDamageMissile;
import com.herocraftonline.heroes.characters.skill.tools.BasicMissile;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.libs.slikey.effectlib.Effect;
import com.herocraftonline.heroes.libs.slikey.effectlib.EffectManager;
import com.herocraftonline.heroes.libs.slikey.effectlib.EffectType;
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class SkillManaMissile extends PassiveSkill {

    private static final Color blueViolet = Color.fromRGB(138, 43, 226);
    private static final String cooldownEffectName = "ManaMissile-Cooldown";

    public SkillManaMissile(final Heroes plugin) {
        super(plugin, "ManaMissile");
        setDescription("When you attack when your wand, you also conjure a missile of pure mana that fires towards your enemies. " +
                "The missile deals $1 damage on hit.");
        setUsage("/skill manamissile");
        setIdentifiers("skill manamissile");
        setArgumentRange(0, 0);
        setTypes(SkillType.DAMAGING, SkillType.SILENCEABLE, SkillType.ABILITY_PROPERTY_MAGICAL);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillHeroListener(this), plugin);
    }

    @Override
    public String getDescription(final Hero hero) {
        final double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 50.0, false);

        return getDescription()
                .replace("$1", Util.decFormat.format(damage));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.DAMAGE.node(), 40.0);
        config.set("catalysts", Util.hoes);
        config.set("projectile-size", 0.3);
        config.set("projectile-velocity", 75.0);
        config.set("projectile-ticks-lives", 30);
        config.set("projectile-gravity", 0.0);
        config.set("projectile-pierces-on-hit", true);
        config.set("knockback-on-hit", false);
        config.set(SkillSetting.COOLDOWN.node(), 500);
        config.set(SkillSetting.APPLY_TEXT.node(), "");
        config.set(SkillSetting.UNAPPLY_TEXT.node(), "");
        return config;
    }

    private static class CooldownEffect extends ExpirableEffect {
        public CooldownEffect(final Skill skill, final Player applier, final long duration) {
            super(skill, cooldownEffectName, applier, duration);
        }
    }

    private static class ManaMissileVisualEffect extends Effect {
        private static final double halfPi = Math.PI / 2.0;
        public final Particle particle = Particle.REDSTONE;
        public final Color color = BasicMissile.DEFAULT_COLOR;
        private final double sizeMultiplier = 0.5;
        private final double rotationSpeed = 4.0D;
        private double step;

        ManaMissileVisualEffect(final EffectManager effectManager) {
            super(effectManager);

            this.type = EffectType.REPEATING;
            this.period = 1;
            this.iterations = 6000; // Arbitrary 5 minute duration. We will either dispose it early, or the extender will modify the effect themselves.
        }

        @Override
        public void onRun() {
            step += 1.0D;
            if (step > 20.0D) {
                step = 1.0D;
            }
            final Location loc = getLocation();
            for (int j = 0; j < 2; j++) {
                for (double i = -Math.PI; i < Math.PI; i += halfPi) {
                    Vector v = new Vector(Math.cos(i + step / rotationSpeed), Math.sin(i + step / rotationSpeed), 0.0D);
                    v = rotate(v, loc).multiply(sizeMultiplier);

                    display(particle, loc.add(v), color, 0.8F, 0);        // For some reason effect lib looks like shit with the exact same parameters. Makes no sense to me.

                    // Doesn't work super great with redstone. You want FireworkSpark instead if you're gonna use this.
//					Particle.DustOptions data = new Particle.DustOptions(color, 1.0F);
//					loc.getWorld().spawnParticle(particle, loc, 0, v.getX(), v.getY(), v.getZ(), 0.08D, data, false);

//					this.particleOffsetX = (float) v.getX();
//					this.particleOffsetY = (float) v.getY();
//					this.particleOffsetZ = (float) v.getZ();
//					display(particle, loc, color, 0.8F, 0);		// For some reason effect lib looks like shit with the exact same parameters. Makes no sense to me.
                }
            }
        }

        private Vector rotate(Vector v, final Location loc) {
            final double yaw = loc.getYaw() / 180.0F * Math.PI;
            final double pitch = loc.getPitch() / 180.0F * Math.PI;
            v = rotAxisX(v, pitch);
            v = rotAxisY(v, -yaw);
            return v;
        }

        public Vector rotAxisX(final Vector v, final double a) {
            final double y = v.getY() * Math.cos(a) - v.getZ() * Math.sin(a);
            final double z = v.getY() * Math.sin(a) + v.getZ() * Math.cos(a);
            return v.setY(y).setZ(z);
        }

        public Vector rotAxisY(final Vector v, final double b) {
            final double x = v.getX() * Math.cos(b) + v.getZ() * Math.sin(b);
            final double z = v.getX() * -Math.sin(b) + v.getZ() * Math.cos(b);
            return v.setX(x).setZ(z);
        }

        public Vector rotAxisZ(final Vector v, final double c) {
            final double x = v.getX() * Math.cos(c) - v.getY() * Math.sin(c);
            final double y = v.getX() * Math.sin(c) + v.getY() * Math.cos(c);
            return v.setX(x).setY(y);
        }
    }

    public class SkillHeroListener implements Listener {
        private final Skill skill;

        public SkillHeroListener(final Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onLeftClick(final PlayerInteractEvent event) {
            if (event.getAction() != Action.LEFT_CLICK_AIR && event.getAction() != Action.LEFT_CLICK_BLOCK) {
                return;
            }

            final Player player = event.getPlayer();
            final Hero hero = plugin.getCharacterManager().getHero(player);
            if (!validateCanCast(hero)) {
                return;
            }

            fireProjectile(player, hero);
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onWeaponDamage(final WeaponDamageEvent event) {
            if (event.isProjectile() || !(event.getDamager() instanceof Hero)) {
                return;
            }

            final Hero hero = ((Hero) event.getDamager());
            if (!validateCanCast(hero)) {
                return;
            }

            fireProjectile(hero.getPlayer(), hero);
        }

        private void fireProjectile(final Player player, final Hero hero) {
            final double projSize = SkillConfigManager.getUseSetting(hero, skill, "projectile-size", 0.25, false);
            final double projVelocity = SkillConfigManager.getUseSetting(hero, skill, "projectile-velocity", 20.0, false);
            final ManaProjectile missile = new ManaProjectile(plugin, skill, hero, projSize, projVelocity);
            missile.fireMissile();
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_VEX_HURT, 2F, 1F);

            final int cooldown = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.COOLDOWN, 2000, false);
            hero.addEffect(new CooldownEffect(skill, player, cooldown));
        }

        private boolean validateCanCast(final Hero hero) {
            if (!hero.canUseSkill(skill)) {
                return false;
            }

            if (hero.hasEffect(cooldownEffectName)) {
                final double remainingTime = ((CooldownEffect) hero.getEffect(cooldownEffectName)).getRemainingTime() / 1000.0;
                if (remainingTime > 0.0) {    // Sometimes we are below zero with this thing. Kinda weird.
                    final String formattedRemainingTime = Util.decFormatCDs.format(remainingTime);
                    ActiveSkill.sendResultMessage(hero, skill, new SkillResult(SkillResult.ResultType.ON_COOLDOWN, true, skill.getName(), formattedRemainingTime));
                    return false;
                }
            }

            final Player player = hero.getPlayer();
            final PlayerInventory playerInv = player.getInventory();
            final ItemStack mainHand = NMSHandler.getInterface().getItemInMainHand(playerInv);

            final List<String> allowedCatalysts = SkillConfigManager.getUseSetting(hero, skill, "catalysts", Util.hoes);
            if (mainHand == null || !allowedCatalysts.contains(mainHand.getType().name())) {
                return false;
            }

            double healthCost = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.HEALTH_COST, 0.0, false);
            int stamCost = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.STAMINA, 0, false);
            int manaCost = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.MANA, 0, false);

            final SkillUseEvent skillEvent = new SkillUseEvent(skill, player, hero, manaCost, healthCost, stamCost, null, null);
            plugin.getServer().getPluginManager().callEvent(skillEvent);
            if (skillEvent.isCancelled()) {
                ActiveSkill.sendResultMessage(hero, skill, SkillResult.CANCELLED);
                return false;
            }

            // Update manaCost with result of SkillUseEvent
            manaCost = skillEvent.getManaCost();
            if (manaCost > hero.getMana()) {
                ActiveSkill.sendResultMessage(hero, skill, SkillResult.LOW_MANA);
                return false;
            }

            // Update healthCost with results of SkillUseEvent
            healthCost = skillEvent.getHealthCost();
            if (healthCost > 0 && (hero.getPlayer().getHealth() <= healthCost)) {
                ActiveSkill.sendResultMessage(hero, skill, SkillResult.LOW_HEALTH);
                return false;
            }

            //Update staminaCost with results of SkilluseEvent
            stamCost = skillEvent.getStaminaCost();
            if (stamCost > 0 && (hero.getStamina() < stamCost)) {
                ActiveSkill.sendResultMessage(hero, skill, SkillResult.LOW_STAMINA);
                return false;
            }

            // Deduct health
            if (healthCost > 0) {
                player.setHealth(player.getHealth() - healthCost);
            }

            // Deduct mana
            if (manaCost > 0) {
                hero.setMana(hero.getMana() - manaCost);
                if (hero.isVerboseMana()) {
                    hero.getPlayer().sendMessage(ChatComponents.Bars.mana(hero.getMana(), hero.getMaxMana(), true));
                }
            }

            // Deduct stamina
            if (stamCost > 0) {
                hero.setStamina(hero.getStamina() - stamCost);
                if (hero.isVerboseStamina()) {
                    hero.getPlayer().sendMessage(ChatComponents.Bars.stamina(hero.getStamina(), hero.getMaxStamina(), true));
                }
            }

            return true;
        }
    }

    private class ManaProjectile extends BasicDamageMissile {
        private final boolean knockBackOnHit;
        private final boolean shouldPierce;
        private final List<LivingEntity> hitTargets = new ArrayList<>();

        public ManaProjectile(final Heroes plugin, final Skill skill, final Hero hero, final double projectileSize, final double projVelocity) {
            super(plugin, skill, hero, Particle.DRIP_WATER, Color.TEAL, EntityDamageEvent.DamageCause.MAGIC);

            setRemainingLife(SkillConfigManager.getUseSetting(hero, skill, "projectile-max-ticks-lived", 30, false));
            setGravity(SkillConfigManager.getUseSetting(hero, skill, "projectile-gravity", 0.0, false));
            this.knockBackOnHit = SkillConfigManager.getUseSetting(hero, skill, "knockback-on-hit", false);
            this.shouldPierce = SkillConfigManager.getUseSetting(hero, skill, "projectile-pierces-on-hit", false);
            this.damage = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE, 50.0, false);
            this.visualEffect = new ManaMissileVisualEffect(plugin.getEffectLibManager());
        }

        @Override
        protected void onTick() {
            final Location loc = getLocation();
            this.visualEffect.setLocation(loc);
//			if (this.getTicksLived() % 2 == 0) {
//				loc.getWorld().playSound(loc, Sound.ENTITY_VEX_HURT, 2F, 1F);
//			}
        }

        @Override
        protected boolean onCollideWithEntity(final Entity entity) {
            if (shouldPierce) {
                return false;
            }
            return entity instanceof LivingEntity;
        }

        @Override
        protected void onEntityPassed(final Entity entity, final Vector passOrigin, final Vector passForce) {
            if (!(entity instanceof LivingEntity) || hitTargets.contains(entity)) {
                return;
            }

            final LivingEntity target = (LivingEntity) entity;
            if (!Skill.damageCheck(player, target)) {
                return;
            }

            addSpellTarget(target, hero);
            damageEntity(target, player, damage, EntityDamageEvent.DamageCause.MAGIC, knockBackOnHit);
            hitTargets.add(target);

            if (!shouldPierce) {
                this.kill();
            }
        }
    }
}
