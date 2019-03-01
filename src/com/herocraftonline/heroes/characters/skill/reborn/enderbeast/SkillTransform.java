package com.herocraftonline.heroes.characters.skill.reborn.enderbeast;

import java.util.ArrayList;
import java.util.List;

import com.herocraftonline.heroes.characters.effects.PeriodicEffect;
import com.herocraftonline.heroes.characters.effects.common.WalkSpeedIncreaseEffect;
import com.herocraftonline.heroes.characters.equipment.*;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import com.herocraftonline.heroes.characters.equipment.EquipmentChangedEvent;
import de.slikey.effectlib.Effect;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.EffectType;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;

public class SkillTransform extends ActiveSkill {

    private String toggleableEffectName = "EnderBeastTransformed";
    private String applyText;
    private String expireText;

    public SkillTransform(Heroes plugin) {
        super(plugin, "Transform");
        setDescription("Take on your true form, granting new powers to all of your other abilities. "
                + "Your lose $1 health per second while in this state.");
        setUsage("/skill transform");
        setArgumentRange(0, 0);
        setIdentifiers("skill transform");
        setToggleableEffectName(toggleableEffectName);
        setTypes(SkillType.ABILITY_PROPERTY_ENDER, SkillType.FORM_ALTERING);

        Bukkit.getServer().getPluginManager().registerEvents(new HelmetListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {

        int healthDrainTick = SkillConfigManager.getUseSetting(hero, this, "health-drain-tick", 20, false);
        int healthDrainPeriod = SkillConfigManager.getUseSetting(hero, this, "health-drain-period", 500, false);

        double perSecondMultiplier = 1000d / healthDrainPeriod;
        double healthPerSecond = healthDrainTick * perSecondMultiplier;

        return getDescription()
                .replace("$1", Util.decFormat.format(healthPerSecond));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.DELAY.node(), 250);
        config.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% has transformed!");
        config.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% returns to their human form.");
        config.set("health-drain-tick", 20.0D);
        config.set("health-drain-period", 500);
        return config;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%hero% has transformed!").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%hero% returns to their human form.").replace("%hero%", "$1");
        setUseText(null);
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        double healthDrainTick = SkillConfigManager.getUseSetting(hero, this, "health-drain-tick", 20.0D, false);
        int healthDrainPeriod = SkillConfigManager.getUseSetting(hero, this, "health-drain-period", 500, false);

        if (player.getHealth() <= healthDrainTick) {
            player.sendMessage("You do not have enough health to sustain an transformation right now!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        //jumpUpwards(hero, player);
        //performKnockback(hero, player, radius, damage);

        //playEffects(player, radius);
        hero.addEffect(new TransformedEffect(this, player, healthDrainTick, healthDrainPeriod));
        Location location = player.getLocation();
        location.getWorld().playSound(location, Sound.ENTITY_ZOMBIE_AMBIENT, 1F, 0.6f);
        broadcastExecuteText(hero);

        return SkillResult.NORMAL;
    }

    private void jumpUpwards(Hero hero, Player player) {
        double vPower = SkillConfigManager.getUseSetting(hero, this, "upwards-velocity", 0.5, false);
        Location location = player.getLocation();
        player.setVelocity(player.getVelocity().add(new Vector(0, vPower, 0)));
        player.playSound(location, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.5f, 0.5f);
        player.playSound(location, Sound.ENTITY_PHANTOM_FLAP, 0.5f, 0.5f);
        player.getWorld().spawnParticle(Particle.CLOUD, location, 1, 0, 0, 0, 1);
    }

    private void performKnockback(Hero hero, Player player, int radius, double damage) {
        double hPower = SkillConfigManager.getUseSetting(hero, this, "horizontal-knockback-power", 1.4, false);
        double vPower = SkillConfigManager.getUseSetting(hero, this, "vertical-knockback-power", 0.5, false);

        List<Entity> entities = hero.getPlayer().getNearbyEntities(radius, radius, radius);
        for (Entity entity : entities) {
            if (!(entity instanceof LivingEntity))
                continue;

            final LivingEntity target = (LivingEntity) entity;
            if (!damageCheck(player, target))
                continue;

            addSpellTarget(target, hero);
            damageEntity(target, player, damage / 2, DamageCause.MAGIC, false);
            damageEntity(target, player, damage / 2, DamageCause.FIRE, false);

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
    }

    private void playEffects(Player player, int radius) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.5f, 0.533f);

        EffectManager em = new EffectManager(plugin);
        Effect e = createParticleEffect(radius, em);
        e.setLocation(player.getLocation().clone());
        e.period = 10;
        e.iterations = 5;
        e.type = EffectType.REPEATING;
        e.color = Color.PURPLE;
        e.start();
    }

    private Effect createParticleEffect(int radius, EffectManager em) {
        return new Effect(em) {
            Particle particle = Particle.REDSTONE;
            Color color = Color.PURPLE;

            List<Location> locations = null;

            @Override
            public void onRun() {
                if (locations == null)
                    locations = getParticleLocations(getLocation(), 72, radius, false);

                for (Location location : locations)
                    display(particle, location);
            }

            private ArrayList<Location> getParticleLocations(Location centerLocation, int particleAmount, int circleRadius, boolean hollow) {
                World world = centerLocation.getWorld();
                double increment = (2 * Math.PI) / particleAmount;

                ArrayList<Location> locations = new ArrayList<Location>();

                double currentRadius = hollow ? circleRadius : 1d;
                do {
                    for (double i = 0; i < particleAmount; i += 0.5) {
                        double angle = i * increment;
                        double x = centerLocation.getX() + (currentRadius * Math.cos(angle));
                        double z = centerLocation.getZ() + (currentRadius * Math.sin(angle));
                        locations.add(new Location(world, x, centerLocation.getY(), z));
                    }
                    currentRadius += 0.2;
                } while (currentRadius <= circleRadius);
                locations.add(centerLocation);
                return locations;
            }
        };
    }

    public class TransformedEffect extends PeriodicEffect {

        private final double healthDrainTick;

        TransformedEffect(Skill skill, Player applier, double healthDrainTick, long period) {
            super(skill, toggleableEffectName, applier, period, applyText, expireText);
            this.healthDrainTick = healthDrainTick;

            setTypes(SkillType.FORM_ALTERING);
            setTypes(SkillType.ABILITY_PROPERTY_MAGICAL);
            setTypes(SkillType.BUFFING);
        }

        @Override
        public void tickHero(Hero hero) {
            Player player = hero.getPlayer();
            double newHealth = player.getHealth() - healthDrainTick;
            if (newHealth < 1)
                hero.removeEffect(this);
            else
                player.setHealth(newHealth);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);

            PlayerInventory inventory = hero.getPlayer().getInventory();
            Util.moveItem(hero, -1, inventory.getHelmet());

            ItemStack dragonHead = new ItemStack(Material.DRAGON_HEAD);
            ItemMeta itemmeta = dragonHead.getItemMeta();
            itemmeta.setDisplayName("True Form");
            itemmeta.setUnbreakable(true);
//            ArrayList<String> lore = new ArrayList<String>();
//            lore.add("LORE");
//            itemmeta.setLore(lore);
            dragonHead.setItemMeta(itemmeta);
            inventory.setHelmet(dragonHead);
            hero.getPlayer().updateInventory();
        }

        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);

            Player player = hero.getPlayer();
            PlayerInventory inventory = player.getInventory();
            ItemStack dragonHead = inventory.getHelmet();
            inventory.remove(dragonHead);
            inventory.setHelmet(new ItemStack(Material.AIR, 0));
            player.updateInventory();
        }
    }

    public class HelmetListener implements Listener {
        private final Skill skill;

        HelmetListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onPlayerDisconnect(PlayerQuitEvent event) {

        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onEquipmentChanged(EquipmentChangedEvent event) {
            if (event.getType() != EquipmentType.HELMET || event.getOldArmorPiece() == null || event.getOldArmorPiece().getType() != Material.DRAGON_HEAD)
                return;

            final Hero hero = plugin.getCharacterManager().getHero(event.getPlayer());
            if (hero.hasEffect(toggleableEffectName))
                event.setCancelled(true);
        }
    }
}
