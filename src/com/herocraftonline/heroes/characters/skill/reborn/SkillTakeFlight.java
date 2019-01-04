package com.herocraftonline.heroes.characters.skill.reborn;

import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.common.SafeFallEffect;

public class SkillTakeFlight extends ActiveSkill {

    public SkillTakeFlight(Heroes plugin) {
        super(plugin, "TakeFlight");
        setDescription("You transform and boost into the air, giving you great hight, but also weakening you for a time.");
        setUsage("/skill takeflight");
        setArgumentRange(0, 0);
        setIdentifiers("skill takeflight");
        setTypes(SkillType.VELOCITY_INCREASING, SkillType.MOVEMENT_INCREASING, SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.SILENCING);
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DAMAGE.node(), 100);
        node.set("damage-per-level", 25);
        node.set("exhausted-duration", 3000);
        node.set("vertical-power", 0.15);
        node.set("flight-ticks", 5);
        node.set("submerged-hinder-percent", 0.70);
        //node.set("ncp-exemption-duration", 2000);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        final Player player = hero.getPlayer();

        Material mat = player.getLocation().getBlock().getRelative(BlockFace.DOWN).getType();

        final int flightTicks = SkillConfigManager.getUseSetting(hero, this, "flight-ticks", 5, false);
        final double submergedHinderPercent = SkillConfigManager.getUseSetting(hero, this, "submerged-hinder-percent", 0.70, true);
        double vPower = SkillConfigManager.getUseSetting(hero, this, "vertical-power", 0.15, false);
        switch (mat) {
            case WATER:
            case LAVA:
            case SOUL_SAND:
                vPower *= submergedHinderPercent;
                break;
            default:
                break;
        }

        final Vector velocity = player.getVelocity().setY(vPower);
        Vector directionVector = player.getLocation().getDirection();
        directionVector.setY(0);
        directionVector.normalize();
        velocity.add(directionVector);
        velocity.multiply(new Vector(0, 1, 0));

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5F, 1.0F);
        Skill skill = this;
        new BukkitRunnable() {
            int ticks = 0;

            public void run() {
                if (ticks == flightTicks) {
                    hero.addEffect(new EnderFloatEffect(skill, player, 5000));
                    cancel();
                    return;
                }
                player.setVelocity(velocity);
                if (ticks % 3 == 0)
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0F, 1.0F);
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);

        return SkillResult.NORMAL;
    }

    private class EnderFloatEffect extends ExpirableEffect {
        public EnderFloatEffect(Skill skill, Player applier, long duration) {
            super(skill, "EnderFloat", applier, duration);
            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.AIR);
            types.add(EffectType.FORM);

            addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, (int) duration / 1000 * 20, -10));
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            hero.addEffect(new JumpBoostEffect(skill, hero.getPlayer(), 2000));
            final int exhaustionDuration = SkillConfigManager.getUseSetting(hero, skill, "exhaustion-duration", 5000, false);
            hero.addEffect(new FlightExhaustionEffect(skill, hero.getPlayer(), exhaustionDuration));
        }
    }

    private class FlightExhaustionEffect extends ExpirableEffect {
        public FlightExhaustionEffect(Skill skill, Player applier, long duration) {
            super(skill, "FlightExhaustion", applier, duration);
            types.add(EffectType.HARMFUL);
            types.add(EffectType.HUNGER);
            types.add(EffectType.STAMINA_FREEZING);

            addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, (int) duration / 1000 * 20, 0));
        }
    }

    private class JumpBoostEffect extends ExpirableEffect {
        public JumpBoostEffect(Skill skill, Player applier, long duration) {
            super(skill, "GenericJumpBoost", applier, duration);

            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.PHYSICAL);
            types.add(EffectType.JUMP_BOOST);

            addPotionEffect(new PotionEffect(PotionEffectType.JUMP, (int) (duration / 1000 * 20), 1));
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            applier.setFallDistance(-512f);
        }
    }
}