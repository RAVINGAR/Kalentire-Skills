package com.herocraftonline.heroes.characters.skill.reborn;

import java.util.ArrayList;
import java.util.List;

import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import de.slikey.effectlib.Effect;
import de.slikey.effectlib.EffectManager;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;


import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import org.jetbrains.annotations.NotNull;

public class SkillTransform extends ActiveSkill {

    public SkillTransform(Heroes plugin) {
        super(plugin, "Transform");
        setDescription("Take on your true form, causing a small explosion around you and empowering you for a short time");
        setUsage("/skill transform");
        setArgumentRange(0, 0);
        setIdentifiers("skill transform");
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.FORCE, SkillType.DAMAGING, SkillType.AGGRESSIVE, SkillType.AREA_OF_EFFECT);
    }

    @Override
    public String getDescription(Hero hero) {
        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5, false);
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 50, false);
//        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 1.6, false);
//        damage += (int) (damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH));

        return getDescription().replace("$1", radius + "").replace("$2", damage + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DAMAGE.node(), 50);
//        node.set(SkillSetting.DAMAGE_INCREASE_PER_STRENGTH.node(), 1.125);
        node.set(SkillSetting.RADIUS.node(), 5);
        node.set("horizontal-knockback-power", 0.0);
        node.set("vertical-knockback-power", 0.4);
        //node.set("ncp-exemption-duration", 1500);
        node.set(SkillSetting.DELAY.node(), 250);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5, false);

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 50, false);
//        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 1.125, false);
//        damage += damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH);

        double hPower = SkillConfigManager.getUseSetting(hero, this, "horizontal-knockback-power", 2.8, false);
        double vPower = SkillConfigManager.getUseSetting(hero, this, "vertical-knockback-power", 0.5, false);

        broadcastExecuteText(hero);

        List<Entity> entities = hero.getPlayer().getNearbyEntities(radius, radius, radius);
        for (Entity entity : entities) {
            if (!(entity instanceof LivingEntity))
                continue;

            final LivingEntity target = (LivingEntity) entity;
            if (!damageCheck(player, target))
                continue;

            addSpellTarget(target, hero);
            damageEntity(target, player, damage, DamageCause.MAGIC, false);

            Location playerLoc = player.getLocation();
            Location targetLoc = target.getLocation();

            double xDir = targetLoc.getX() - playerLoc.getX();
            double zDir = targetLoc.getZ() - playerLoc.getZ();
            double magnitude = Math.sqrt(xDir * xDir + zDir * zDir);

            xDir = xDir / magnitude * hPower;
            zDir = zDir / magnitude * hPower;

            final Vector velocity = new Vector(xDir, vPower, zDir);
            target.setVelocity(velocity);
        }

        PlayEffects(player, radius);

        return SkillResult.NORMAL;
    }

    private void PlayEffects(Player player, int radius) {
        player.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, player.getLocation(), 3, 0, 0, 0, 1);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0f, 0.533f);

        EffectManager em = new EffectManager(plugin);
        Effect e = CreateParticleEffect(radius, em);
        e.setLocation(player.getLocation().clone());
        e.asynchronous = true;
        e.iterations = 1;
        e.type = de.slikey.effectlib.EffectType.INSTANT;
        e.color = Color.PURPLE;
        e.start();
    }

    @NotNull
    private Effect CreateParticleEffect(int radius, EffectManager em) {
        return new Effect(em) {
            Particle particle = Particle.REDSTONE;
            Color color = Color.PURPLE;

            @Override
            public void onRun() {
                for (Location location : getLocationsInRadius(getLocation(), 72, radius / 2d))
                    display(particle, location);
            }

            private ArrayList<Location> getLocationsInRadius(Location centerPoint, int particleAmount, double circleRadius)
            {
                World world = centerPoint.getWorld();
                double increment = (2 * Math.PI) / particleAmount;

                ArrayList<Location> locations = new ArrayList<Location>();
                for (int i = 0; i < particleAmount; i++)
                {
                    double angle = i * increment;
                    double x = centerPoint.getX() + (circleRadius * Math.cos(angle));
                    double z = centerPoint.getZ() + (circleRadius * Math.sin(angle));
                    locations.add(new Location(world, x, centerPoint.getY(), z));
                }
                return locations;
            }
        };
    }

    public class TransformedEffect extends ExpirableEffect {
        private final ItemStack dragonHead = new ItemStack(Material.DRAGON_HEAD, 1);

        public TransformedEffect(Skill skill, Player applier, long duration) {
            super(skill, "Transformed", applier, duration);

            setTypes(SkillType.FORM_ALTERING);
            setTypes(SkillType.ABILITY_PROPERTY_MAGICAL);
            setTypes(SkillType.BUFFING);

            addPotionEffect(new PotionEffect(PotionEffectType.JUMP, (int) (duration / 1000 * 20), 2));
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);

            PlayerInventory inventory = hero.getPlayer().getInventory();
            inventory.addItem(dragonHead);
            inventory.setHelmet(dragonHead);
            hero.getPlayer().updateInventory();
        }

        public void removeFromHero(Hero hero) {
            super.applyToHero(hero);
            PlayerInventory inventory = hero.getPlayer().getInventory();
            ItemStack emptySlot = new ItemStack(Material.AIR, 0);
            hero.getPlayer().getInventory().setHelmet(emptySlot);
            inventory.remove(dragonHead);
            hero.getPlayer().updateInventory();
        }
    }
}
