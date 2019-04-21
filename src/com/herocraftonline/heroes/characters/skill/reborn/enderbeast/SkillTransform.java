package com.herocraftonline.heroes.characters.skill.reborn.enderbeast;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicEffect;
import com.herocraftonline.heroes.characters.equipment.EquipMethod;
import com.herocraftonline.heroes.characters.equipment.EquipmentChangedEvent;
import com.herocraftonline.heroes.characters.equipment.EquipmentType;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.logging.Level;

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
        setTypes(SkillType.ABILITY_PROPERTY_ENDER, SkillType.ABILITY_PROPERTY_DARK, SkillType.FORM_ALTERING, SkillType.SILENCEABLE, SkillType.BUFFING);

        Bukkit.getServer().getPluginManager().registerEvents(new HelmetListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        int healthDrainTick = SkillConfigManager.getUseSetting(hero, this, "health-drain-tick", 20, false);
        int healthDrainPeriod = SkillConfigManager.getUseSetting(hero, this, "health-drain-period", 500, false);

        double perSecondMultiplier = 1000.0 / healthDrainPeriod;
        double healthPerSecond = healthDrainTick * perSecondMultiplier;

        return getDescription()
                .replace("$1", Util.decFormat.format(healthPerSecond));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.DELAY.node(), 500);
        config.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% has transformed!");
        config.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% returns to their human form.");
        config.set("health-drain-tick", 20.0D);
        config.set("health-drain-period", 1000);
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

        hero.addEffect(new TransformedEffect(this, player, healthDrainTick, healthDrainPeriod));
        Location location = player.getLocation();
        location.getWorld().playSound(location, Sound.ENTITY_ZOMBIE_AMBIENT, 1F, 0.6f);
        broadcastExecuteText(hero);

        return SkillResult.NORMAL;
    }

    public class TransformedEffect extends PeriodicEffect {

        private final double healthDrainTick;

        TransformedEffect(Skill skill, Player applier, double healthDrainTick, long period) {
            super(skill, toggleableEffectName, applier, period, applyText, expireText);
            this.healthDrainTick = healthDrainTick;

            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.ENDER);
            types.add(EffectType.FORM);
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

            Player player = hero.getPlayer();
            PlayerInventory inventory = player.getInventory();

            ItemStack transformedHead = new ItemStack(Material.SKULL_ITEM, 1, (short) 5);
            ItemMeta itemmeta = transformedHead.getItemMeta();
            itemmeta.setDisplayName("Transformed");
            itemmeta.setUnbreakable(true);
            transformedHead.setItemMeta(itemmeta);

            EquipmentChangedEvent replaceEvent = new EquipmentChangedEvent(player, EquipMethod.APPLYING_SKILL_EFFECT, EquipmentType.HELMET, inventory.getHelmet(), transformedHead);
            Bukkit.getServer().getPluginManager().callEvent(replaceEvent);
            if (replaceEvent.isCancelled()) {
                Heroes.log(Level.WARNING, "SkillTransform: Somebody tried to cancel a EquipmentChangedEvent, and we are ignoring the cancellation.");
            }

            Util.moveItem(hero, -1, inventory.getHelmet());
            inventory.setHelmet(transformedHead);
            Util.syncInventory(player, plugin);
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);

            Player player = hero.getPlayer();
            PlayerInventory inventory = player.getInventory();

            ItemStack transformedHead = inventory.getHelmet();
            ItemStack emptyHelmet = new ItemStack(Material.AIR, 0);

            EquipmentChangedEvent replaceEvent = new EquipmentChangedEvent(player, EquipMethod.EXPIRING_SKILL_EFFECT, EquipmentType.HELMET, transformedHead, emptyHelmet);
            Bukkit.getServer().getPluginManager().callEvent(replaceEvent);
            if (replaceEvent.isCancelled()) {
                Heroes.log(Level.WARNING, "SkillTransform: Somebody tried to cancel a EquipmentChangedEvent, and we are ignoring the cancellation.");
            }

            inventory.remove(transformedHead);
            inventory.setHelmet(emptyHelmet);
            Util.syncInventory(player, plugin);
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
            if (event.getType() != EquipmentType.HELMET || event.getOldArmorPiece() == null
                    || event.getOldArmorPiece().getType() != Material.SKULL_ITEM
                    || event.getMethod() == EquipMethod.EXPIRING_SKILL_EFFECT) {
                return;
            }

            final Hero hero = plugin.getCharacterManager().getHero(event.getPlayer());
            if (hero.hasEffect(toggleableEffectName))
                event.setCancelled(true);
        }
    }
}
