package com.herocraftonline.heroes.characters.skill.reborn.necromancer;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainManaEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicEffect;
import com.herocraftonline.heroes.characters.equipment.EquipMethod;
import com.herocraftonline.heroes.characters.equipment.EquipmentChangedEvent;
import com.herocraftonline.heroes.characters.equipment.EquipmentType;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.Disguise;
import me.libraryaddict.disguise.disguisetypes.DisguiseType;
import me.libraryaddict.disguise.disguisetypes.MobDisguise;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.logging.Level;

public class SkillLichForm extends ActiveSkill {
    private static String toggleableEffectName = "LichForm";
    private static final Material noDisguiseLibTransformMaterial = Material.SKULL;

    private boolean disguiseApiLoaded = false;
    private String applyText;
    private String expireText;

    public SkillLichForm(Heroes plugin) {
        super(plugin, "LichForm");
        setDescription("Transform into a Lich. While in this form, you transfer $1 health into $2 mana every $3 second(s).");
        setUsage("/skill lichform");
        setArgumentRange(0, 0);
        setIdentifiers("skill lichform");
        setTypes(SkillType.SILENCEABLE, SkillType.BUFFING, SkillType.ABILITY_PROPERTY_DARK, SkillType.FORM_ALTERING);

        setToggleableEffectName(toggleableEffectName);
        if (Bukkit.getServer().getPluginManager().getPlugin("LibsDisguises") != null) {
            disguiseApiLoaded = true;
        }
    }

    @Override
    public String getDescription(Hero hero) {
        long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 1000, false);
        double healthCost = SkillConfigManager.getUseSetting(hero, this, "health-drain-per-tick", 10.0, false);
        int manaPerTick = SkillConfigManager.getUseSetting(hero, this, "mana-per-tick", 15, false);

        return getDescription()
                .replace("$1", Util.decFormat.format(healthCost))
                .replace("$2", Util.decFormat.format(manaPerTick))
                .replace("$3", Util.decFormat.format(period / 1000.0));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.USE_TEXT.node(), "");
        config.set("health-drain-per-tick", 10.0);
        config.set("mana-per-tick", 15);
        config.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% has become a Lich!");
        config.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% is human once more.");
        return config;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%hero% has become a Lich!").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%hero% is human once more.").replace("%hero%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero);

        long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 1000, false);
        hero.addEffect(new LichFormEffect(this, player, period));

        return SkillResult.NORMAL;
    }

    public class LichFormEffect extends PeriodicEffect {

        private double healthCost;
        private int manaGain;

        public LichFormEffect(Skill skill, Player applier, long period) {
            super(skill, toggleableEffectName, applier, period, applyText, expireText);

            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.DARK);
            types.add(EffectType.FORM);
            types.add(EffectType.WATER_BREATHING);

            addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 72000, 0));  // 1 Hour. We'll see how that goes...
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            Player player = hero.getPlayer();

            this.healthCost = SkillConfigManager.getUseSetting(hero, skill, "health-drain-per-tick", 10.0, false);
            this.manaGain = SkillConfigManager.getUseSetting(hero, skill, "mana-per-tick", 15, false);

            if (disguiseApiLoaded) {
                disguiseAsSkelly(player);
            } else {
                addSkellySkull(hero, player);
            }
        }
        @Override
        public void tickHero(Hero hero) {
            super.tickHero(hero);

            Player player = hero.getPlayer();
            double newHealth = player.getHealth() - this.healthCost;
            if (newHealth < 1) {
                hero.removeEffect(this);
                return;
            }

            player.setHealth(newHealth);
            if (hero.getMana() < hero.getMaxMana()) {
                HeroRegainManaEvent manaEvent = new HeroRegainManaEvent(hero, manaGain, skill);
                plugin.getServer().getPluginManager().callEvent(manaEvent);
                if (!manaEvent.isCancelled()) {
                    hero.setMana(manaEvent.getDelta() + hero.getMana());

                    if (hero.isVerboseMana())
                        hero.getPlayer().sendMessage(ChatComponents.Bars.mana(hero.getMana(), hero.getMaxMana(), true));
                }
            }
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);

            Player player = hero.getPlayer();
            if (disguiseApiLoaded) {
                removeDisguise(player);
            } else {
                removeSkellySkull(player);
            }
        }

        private void disguiseAsSkelly(Player player) {
            if (DisguiseAPI.isDisguised(player)) {
                removeDisguise(player);
            }

            MobDisguise disguise = new MobDisguise(DisguiseType.getType(EntityType.SKELETON), true);
            disguise.setKeepDisguiseOnPlayerDeath(false);
            disguise.setEntity(player);
            disguise.setShowName(true);
            disguise.setModifyBoundingBox(false);
            disguise.setReplaceSounds(true);
            disguise.setHearSelfDisguise(true);
            disguise.setHideHeldItemFromSelf(true);
            disguise.setHideArmorFromSelf(true);
            disguise.startDisguise();
        }

        private void removeDisguise(Player player) {
            if (!DisguiseAPI.isDisguised(player))
                return;

            Disguise disguise = DisguiseAPI.getDisguise(player);
            disguise.stopDisguise();
            disguise.removeDisguise();
        }

        private void addSkellySkull(Hero hero, Player player) {
            PlayerInventory inventory = player.getInventory();

            ItemStack transformedHead = new ItemStack(noDisguiseLibTransformMaterial);
            ItemMeta itemmeta = transformedHead.getItemMeta();
            itemmeta.setDisplayName("Lich Form");
            itemmeta.setUnbreakable(true);
            transformedHead.setItemMeta(itemmeta);

            EquipmentChangedEvent replaceEvent = new EquipmentChangedEvent(player, EquipMethod.APPLYING_SKILL_EFFECT, EquipmentType.HELMET, inventory.getHelmet(), transformedHead);
            Bukkit.getServer().getPluginManager().callEvent(replaceEvent);
            if (replaceEvent.isCancelled()) {
                Heroes.log(Level.WARNING, "SkillLichForm: Somebody tried to cancel a EquipmentChangedEvent, and we are ignoring the cancellation.");
            }

            Util.moveItem(hero, -1, inventory.getHelmet());
            inventory.setHelmet(transformedHead);

            Util.syncInventory(player, plugin);
        }

        private void removeSkellySkull(Player player) {
            PlayerInventory inventory = player.getInventory();

            ItemStack transformedHead = inventory.getHelmet();
            ItemStack emptyHelmet = new ItemStack(Material.AIR, 0);

            EquipmentChangedEvent replaceEvent = new EquipmentChangedEvent(player, EquipMethod.EXPIRING_SKILL_EFFECT, EquipmentType.HELMET, transformedHead, emptyHelmet);
            Bukkit.getServer().getPluginManager().callEvent(replaceEvent);
            if (replaceEvent.isCancelled()) {
                Heroes.log(Level.WARNING, "SkillLichForm: Somebody tried to cancel a EquipmentChangedEvent, and we are ignoring the cancellation.");
            }

            inventory.remove(transformedHead);
            inventory.setHelmet(emptyHelmet);
            Util.syncInventory(player, plugin);
        }
    }

    public class SkillEffectListener implements Listener {
        private final Skill skill;

        SkillEffectListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onEquipmentChanged(EquipmentChangedEvent event) {
            if (event.getType() != EquipmentType.HELMET || event.getOldArmorPiece() == null
                    || event.getOldArmorPiece().getType() != noDisguiseLibTransformMaterial
                    || event.getMethod() == EquipMethod.EXPIRING_SKILL_EFFECT) {
                return;
            }

            final Hero hero = plugin.getCharacterManager().getHero(event.getPlayer());
            if (hero.hasEffect(toggleableEffectName))
                event.setCancelled(true);
        }
    }
}
