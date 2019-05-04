package com.herocraftonline.heroes.characters.skill.reborn.hellspawn;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.*;
import com.herocraftonline.heroes.characters.effects.common.interfaces.HealthRegainReduction;
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
import org.bukkit.entity.LivingEntity;
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

public class SkillTheWither extends ActiveSkill {

    private String toggleableEffectName = "WitherForm";
    private String applyText;
    private String expireText;

    public SkillTheWither(Heroes plugin) {
        super(plugin, "TheWither");
        setDescription("Channel the power of the Wither himself, altering your appearance and granting you passive wither damage immunity. " +
                "Sustaining this form drains $2 mana from you every $3 second(s). " +
                "While active, your melee attacks inflict a stacking Wither-Decay effect that lowers incoming healing on the target by $4% for $5 second(s). " +
                "The maximum amount of Wither-Decay stacks a target can have is $6.");
        setUsage("/skill thewither");
        setIdentifiers("skill thewither", "skill witherform");
        setArgumentRange(0, 0);
        setToggleableEffectName(toggleableEffectName);
        setTypes(SkillType.ABILITY_PROPERTY_WITHER, SkillType.ABILITY_PROPERTY_DARK, SkillType.FORM_ALTERING, SkillType.SILENCEABLE, SkillType.BUFFING);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillEffectListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        int drainPeriod = SkillConfigManager.getUseSetting(hero, this, "mana-drain-period", 1000, false);
        double manaDrain = SkillConfigManager.getUseSetting(hero, this, "mana-drain-per-tick", 0.075, false);

        int witherAmplifier = SkillConfigManager.getUseSetting(hero, this, "on-hit-wither-amplifier", 3, false);
        int witherDuration = SkillConfigManager.getUseSetting(hero, this, "on-hit-wither-duration", 2000, false);
        double healingReduction = SkillConfigManager.getUseSetting(hero, this, "on-hit-healing-reduction-per-stack", 0.075, false);
        int maxStacks = SkillConfigManager.getUseSetting(hero, this, "on-hit-max-stacks", 6, false);

        return getDescription()
                .replace("$1", Util.decFormat.format(witherDuration / 1000.0))
                .replace("$2", Util.decFormat.format(manaDrain))
                .replace("$3", Util.decFormat.format(drainPeriod / 1000.0))
                .replace("$4", Util.decFormat.format(healingReduction * 100))
                .replace("$5", Util.decFormat.format(witherDuration / 1000.0))
                .replace("$6", maxStacks + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set("mana-drain-period", 1000);
        config.set("mana-drain-per-tick", 25);
        config.set("on-hit-wither-amplifier", 3);
        config.set("on-hit-wither-duration", 2000);
        config.set("on-hit-healing-reduction-per-stack", 0.075);
        config.set("on-hit-max-stacks", 6);
        config.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% has assumed a wither form!");
        config.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% returns to their human form.");
        return config;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT,
                ChatComponents.GENERIC_SKILL + "%hero% has assumed a wither form!")
                .replace("%hero%", "$1");

        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT,
                ChatComponents.GENERIC_SKILL + "%hero% returns to their human form.")
                .replace("%hero%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero);

        long period = SkillConfigManager.getUseSetting(hero, this, "mana-drain-period", 1000, false);
        int manaDrain = SkillConfigManager.getUseSetting(hero, this, "mana-drain-per-tick", 500, false);

        hero.addEffect(new WitherformEffect(this, player, period, manaDrain));

        Location location = player.getLocation();
        location.getWorld().playSound(location, Sound.ENTITY_WITHER_SKELETON_STEP, 0.5F, 0.5f);
        location.getWorld().playSound(location, Sound.ENTITY_PARROT_IMITATE_WITHER_SKELETON, 1F, 1f);

        return SkillResult.NORMAL;
    }

    public class WitherformEffect extends PeriodicEffect {
        private final int manaDrainTick;

        WitherformEffect(Skill skill, Player applier, long period, int manaDrainTick) {
            super(skill, toggleableEffectName, applier, period, applyText, expireText);
            this.manaDrainTick = manaDrainTick;

            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.RESIST_WITHER);
            types.add(EffectType.WITHER);
            types.add(EffectType.FORM);

            addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 999999999, 1));
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            Player player = hero.getPlayer();
            addWitherSkull(hero, player);
        }

        @Override
        public void tickHero(Hero hero) {
            int newMana = hero.getMana() - manaDrainTick;
            if (newMana < 1) {
                hero.removeEffect(this);
            } else {
                hero.setMana(newMana);
            }
        }

        @Override
        public void tickMonster(Monster monster) { }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);

            Player player = hero.getPlayer();
            removeWitherSkull(player);
        }

        private void addWitherSkull(Hero hero, Player player) {
            PlayerInventory inventory = player.getInventory();

            ItemStack transformedHead = new ItemStack(Material.SKULL_ITEM, 1, (short) 1);
            ItemMeta itemMeta = transformedHead.getItemMeta();
            itemMeta.setDisplayName("Wither Form");
            itemMeta.setUnbreakable(true);
            transformedHead.setItemMeta(itemMeta);

            EquipmentChangedEvent replaceEvent = new EquipmentChangedEvent(player, EquipMethod.APPLYING_SKILL_EFFECT, EquipmentType.HELMET, inventory.getHelmet(), transformedHead);
            Bukkit.getServer().getPluginManager().callEvent(replaceEvent);
            if (replaceEvent.isCancelled()) {
                Heroes.log(Level.WARNING, "SkillTheWither: Somebody tried to cancel a EquipmentChangedEvent, and we are ignoring the cancellation.");
            }

            Util.moveItem(hero, -1, inventory.getHelmet());
            inventory.setHelmet(transformedHead);

            Util.syncInventory(player, plugin);
        }

        private void removeWitherSkull(Player player) {
            PlayerInventory inventory = player.getInventory();

            ItemStack transformedHead = inventory.getHelmet();
            ItemStack emptyHelmet = new ItemStack(Material.AIR, 0);

            EquipmentChangedEvent replaceEvent = new EquipmentChangedEvent(player, EquipMethod.EXPIRING_SKILL_EFFECT, EquipmentType.HELMET, transformedHead, emptyHelmet);
            Bukkit.getServer().getPluginManager().callEvent(replaceEvent);
            if (replaceEvent.isCancelled()) {
                Heroes.log(Level.WARNING, "SkillTheWither: Somebody tried to cancel a EquipmentChangedEvent, and we are ignoring the cancellation.");
            }

            inventory.remove(transformedHead);
            inventory.setHelmet(emptyHelmet);
            Util.syncInventory(player, plugin);
        }
    }

    public static class WitherDecayEffect extends StackingEffect implements HealthRegainReduction {

        private final int witherAmplifier;
        private double healingReductionPerStack;

        WitherDecayEffect(Skill skill, Player applier, int witherAmplifier, double healhReductionPerStack, int maxStacks) {
            super(skill, getWitherDecayEffectName(applier), applier, maxStacks);

            this.types.add(EffectType.DISPELLABLE);
            this.types.add(EffectType.HARMFUL);
            this.types.add(EffectType.HEALING_REDUCTION);
            this.types.add(EffectType.WITHER);

            this.witherAmplifier = witherAmplifier;
            this.healingReductionPerStack = healhReductionPerStack;

            addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 99999, witherAmplifier));
        }

        @Override
        public Double getDelta() {
            return healingReductionPerStack * effectStack.count();
        }

        @Override
        public void setDelta(Double value) {
            this.healingReductionPerStack = value / effectStack.count();
        }
    }

    private static String getWitherDecayEffectName(Player player) {
        return player.getName() + "-WitherDecayed";
    }

    public class SkillEffectListener implements Listener {
        private final Skill skill;

        SkillEffectListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onWeaponDamageEvent(WeaponDamageEvent event) {
            if (!(event.getDamager() instanceof Hero) || !(event.getEntity() instanceof LivingEntity))
                return;

            Hero hero = (Hero) event.getDamager();
            if (!hero.hasEffect(toggleableEffectName))
                return;

            Player player = hero.getPlayer();
            int duration = SkillConfigManager.getUseSetting(hero, skill, "on-hit-wither-duration", 2000, false);
            double healingReduction = SkillConfigManager.getUseSetting(hero, skill, "on-hit-healing-reduction-per-stack", 0.075, false);
            int witherAmplifier = SkillConfigManager.getUseSetting(hero, skill, "on-hit-wither-amplifier", 3, false);
            int maxStacks = SkillConfigManager.getUseSetting(hero, skill, "on-hit-max-stacks", 6, false);

            CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter((LivingEntity) event.getEntity());
            targetCT.addEffectStack(getWitherDecayEffectName(player), skill, player, duration,
                    () -> new WitherDecayEffect(skill, player, witherAmplifier, healingReduction, maxStacks));
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onEquipmentChanged(EquipmentChangedEvent event) {
            if (event.getMethod() == EquipMethod.EXPIRING_SKILL_EFFECT
                    || event.getType() != EquipmentType.HELMET
                    || event.getOldArmorPiece() == null
                    || event.getOldArmorPiece().getType() != Material.SKULL_ITEM
                    || event.getOldArmorPiece().getData() == null
                    || event.getOldArmorPiece().getData().getData() != (byte) 1) {
                return;
            }

            final Hero hero = plugin.getCharacterManager().getHero(event.getPlayer());
            if (hero.hasEffect(toggleableEffectName))
                event.setCancelled(true);
        }
    }
}
