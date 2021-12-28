package com.herocraftonline.heroes.characters.skill.remastered;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.SkillUseEvent;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.characters.skill.tools.BasicDamageMissile;
import com.herocraftonline.heroes.characters.skill.tools.BasicMissile;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.heroes.util.Util;
import de.slikey.effectlib.Effect;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.EffectType;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.Vector;

import java.util.List;

public class SkillManaMissile extends PassiveSkill {
    private static String cooldownEffectName = "ManaMissile-Cooldown";

    public SkillManaMissile(Heroes plugin) {
        super(plugin, "ManaMissile");
        setDescription("When you attack with your Staff you fire a missile of mana toward your foe. " +
                "Missiles deal $1 held weapon's damage upon impact.$2");
        setUsage("/skill manamissile");
        setIdentifiers("skill manamissile");
        setArgumentRange(0, 0);
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.DAMAGING, SkillType.AGGRESSIVE);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillHeroListener(this), plugin);
    }

    public String getDescription(Hero hero) {
        String weaponText = SkillConfigManager.getUseSetting(hero, this, "weapon-type-text", "staff");
        boolean isMeleeDisabled = SkillConfigManager.getUseSetting(hero, this, "disable-melee-attack", false);

        double damageMultiplier = SkillConfigManager.getUseSetting(hero, this, "damage-multiplier", 1.0, false);
        String damageMutliplierText = "your ";
        if (damageMultiplier != 1.0) {
            damageMutliplierText = Util.decFormat.format(damageMultiplier * 100) + "% of your ";
        }

        String disableText = "";
        if (isMeleeDisabled)
            disableText = " You are also no longer able to melee with your primary weapon type.";

        return getDescription()
                .replace("$1", damageMutliplierText)
                .replace("$2", disableText);
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set("catalysts", Util.hoes);
        config.set("weapon-type-text", "staff");
        config.set("disable-melee-attack", false);
        config.set("damage-multiplier", 1.0);
        config.set(BasicMissile.PROJECTILE_SIZE_NODE, 0.3);
        config.set(BasicMissile.PROJECTILE_VELOCITY_NODE, 75.0);
        config.set(BasicMissile.PROJECTILE_DURATION_TICKS_NODE, 2);
        config.set(BasicMissile.PROJECTILE_GRAVITY_NODE, 0.0);
        config.set(BasicDamageMissile.PROJECTILE_PIERCES_ON_HIT_NODE, true);
        config.set(BasicDamageMissile.PROJECTILE_KNOCKS_BACK_ON_HIT_NODE, false);
        config.set(SkillSetting.COOLDOWN.node(), 500);
        config.set(SkillSetting.APPLY_TEXT.node(), "");
        config.set(SkillSetting.UNAPPLY_TEXT.node(), "");
        return config;
    }

    public class SkillHeroListener implements Listener {
        private Skill skill;

        public SkillHeroListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.MONITOR) // Don't ignore cancelled. Normal left clicks are cancelled by default.
        public void onLeftClick(PlayerInteractEvent event) {
            if (event.getAction() != Action.LEFT_CLICK_AIR && event.getAction() != Action.LEFT_CLICK_BLOCK)
                return;

            Player player = event.getPlayer();
            if (player == null || player.isDead() || player.getHealth() < 0 || player.getGameMode() == GameMode.SPECTATOR || player.getGameMode() == GameMode.CREATIVE)
                return;

            Hero hero = plugin.getCharacterManager().getHero(player);
            if (!hero.canUseSkill(skill))
                return;

            PlayerInventory playerInv = player.getInventory();
            ItemStack mainHand = NMSHandler.getInterface().getItemInMainHand(playerInv);

            if (!isValidCatalyst(hero, mainHand))
                return;
            if (!isAbleToCastRightNow(hero, true))
                return;

            Double damage = plugin.getDamageManager().getDefaultClassDamage(hero, mainHand.getType());
            if (damage == null) {
                player.sendMessage("    " + ChatComponents.GENERIC_SKILL + "You don't deal any damage with that weapon!");
                return;
            }

            damage *= SkillConfigManager.getUseSetting(hero, skill, "damage-multiplier", 1.0, false);
            fireProjectile(player, hero, damage);
            event.setUseItemInHand(Event.Result.DENY);
        }

        @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
        public void onWeaponDamage(WeaponDamageEvent event) {
            if (event.isProjectile() || !(event.getDamager() instanceof Hero))
                return;

            Hero hero = ((Hero) event.getDamager());
            Player player = hero.getPlayer();

            if (player.getGameMode() == GameMode.SPECTATOR || player.getGameMode() == GameMode.CREATIVE)
                return;
            if (!hero.canUseSkill(skill))
                return;

            PlayerInventory playerInv = player.getInventory();
            ItemStack mainHand = NMSHandler.getInterface().getItemInMainHand(playerInv);
            if (!isValidCatalyst(hero, mainHand))
                return;

            if (isAbleToCastRightNow(hero, true)) {
                Double damage = plugin.getDamageManager().getDefaultClassDamage(hero, mainHand.getType());
                if (damage == 0.0D) {
                    player.sendMessage("    " + ChatComponents.GENERIC_SKILL + "You don't deal any damage with that weapon!");
                } else {
                    damage *= SkillConfigManager.getUseSetting(hero, skill, "damage-multiplier", 1.0, false);
                    fireProjectile(hero.getPlayer(), hero, damage);
                }
            }

            boolean meleeDamageDisabled = SkillConfigManager.getUseSetting(hero, skill, "disable-melee-attack", false);
            if (!meleeDamageDisabled)
                return;

            event.setDamage(0.0);
            event.setCancelled(true);
        }

        private void fireProjectile(Player player, Hero hero, double damage) {
            ManaProjectile missile = new ManaProjectile(plugin, skill, hero, damage);
            missile.fireMissile();
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_VEX_HURT, 2F, 1F);

            int cooldown = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.COOLDOWN, 2000, false);
            hero.addEffect(new CooldownEffect(skill, player, cooldown));
        }

        private boolean isValidCatalyst(Hero hero, ItemStack mainHand) {
            List<String> allowedCatalysts = SkillConfigManager.getUseSetting(hero, skill, "catalysts", Util.hoes);
            if (mainHand == null || !allowedCatalysts.contains(mainHand.getType().name()))
                return false;
            return true;
        }

        private boolean isAbleToCastRightNow(Hero hero, boolean applyCosts) {
            Player player = hero.getPlayer();

            if (hero.hasEffect(cooldownEffectName)) {
                double remainingTime = ((CooldownEffect) hero.getEffect(cooldownEffectName)).getRemainingTime() / 1000.0;
                if (remainingTime > 0.0) {    // Sometimes we are below zero with this thing. Kinda weird.
                    String formattedRemainingTime = Util.decFormatCDs.format(remainingTime);
                    ActiveSkill.sendResultMessage(hero, skill, new SkillResult(SkillResult.ResultType.ON_COOLDOWN, true, skill.getName(), formattedRemainingTime));
                    return false;
                }
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

            if (!applyCosts)
                return true;

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

    private class CooldownEffect extends ExpirableEffect {
        CooldownEffect(Skill skill, Player applier, long duration) {
            super(skill, cooldownEffectName, applier, duration);
        }
    }

    private class ManaProjectile extends BasicDamageMissile {
        ManaProjectile(Heroes plugin, Skill skill, Hero hero, double damage) {
            super(plugin, skill, hero);

            this.damage = damage;
            this.visualEffect = new ManaMissileVisualEffect(this.effectManager);
        }

        @Override
        protected void onTick() {
            // Update visuals every tick
            this.visualEffect.setLocation(getLocation());
        }
    }

    private class ManaMissileVisualEffect extends Effect {
        private static final double halfPi = Math.PI / 2.0;

        private double step;
        public Particle particle = Particle.REDSTONE;
        public Color color = BasicMissile.DEFAULT_COLOR;
        private final double sizeMultiplier = 0.5;
        private double rotationSpeed = 4.0D;

        ManaMissileVisualEffect(EffectManager effectManager) {
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
            Location loc = getLocation();
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

        private Vector rotate(Vector v, Location loc) {
            double yaw = loc.getYaw() / 180.0F * Math.PI;
            double pitch = loc.getPitch() / 180.0F * Math.PI;
            v = rotAxisX(v, pitch);
            v = rotAxisY(v, -yaw);
            return v;
        }

        public Vector rotAxisX(Vector v, double a) {
            double y = v.getY() * Math.cos(a) - v.getZ() * Math.sin(a);
            double z = v.getY() * Math.sin(a) + v.getZ() * Math.cos(a);
            return v.setY(y).setZ(z);
        }

        public Vector rotAxisY(Vector v, double b) {
            double x = v.getX() * Math.cos(b) + v.getZ() * Math.sin(b);
            double z = v.getX() * -Math.sin(b) + v.getZ() * Math.cos(b);
            return v.setX(x).setZ(z);
        }

        public Vector rotAxisZ(Vector v, double c) {
            double x = v.getX() * Math.cos(c) - v.getY() * Math.sin(c);
            double y = v.getX() * Math.sin(c) + v.getY() * Math.cos(c);
            return v.setX(x).setY(y);
        }
    }
}
