package com.herocraftonline.heroes.characters.skill.reborn.pyromancer;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.SkillUseEvent;
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
import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.Disguise;
import me.libraryaddict.disguise.disguisetypes.DisguiseType;
import me.libraryaddict.disguise.disguisetypes.MobDisguise;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
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

    private boolean disguiseApiLoaded;
    private String effectName = "WitherForm";
    private String applyText;
    private String expireText;

    public SkillTheWither(Heroes plugin) {
        super(plugin, "TheWither");
        setDescription("Channel the power of the Wither himself. " +
                "Altering your appearance and granting you fire immunity for $1 second(s). " +
                "While active, you are only able to use physical, dark, or wither based abilities, but your melee attacks inflict a stacking wither effect that lasts $2 second(s).");
        setUsage("/skill thewither");
        setArgumentRange(0, 0);
        setIdentifiers("skill thewither", "skill witherform");
        setTypes(SkillType.ABILITY_PROPERTY_WITHER, SkillType.ABILITY_PROPERTY_DARK, SkillType.FORM_ALTERING, SkillType.SILENCEABLE, SkillType.BUFFING);

        if (Bukkit.getServer().getPluginManager().getPlugin("LibsDisguises") != null) {
            disguiseApiLoaded = true;
        }

        Bukkit.getServer().getPluginManager().registerEvents(new SkillEffectListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);

        return getDescription()
                .replace("$1", Util.decFormat.format(duration / 1000));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.DURATION.node(), 10000);
        config.set("on-hit-wither-duration", 6000);
        config.set("wither-amplifier-per-stack", 3);
        config.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% has assumed a wither form!");
        config.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% returns to their human form.");
        return config;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%hero% has assumed a wither form!").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%hero% returns to their human form.").replace("%hero%", "$1");
        setUseText(null);
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
        hero.addEffect(new WitherformEffect(this, player, duration));
        Location location = player.getLocation();
        location.getWorld().playSound(location, Sound.ENTITY_WITHER_SKELETON_STEP, 0.5F, 0.5f);
        location.getWorld().playSound(location, Sound.ENTITY_PARROT_IMITATE_WITHER_SKELETON, 1F, 1f);
        broadcastExecuteText(hero);

        return SkillResult.NORMAL;
    }

    public class WitherformEffect extends PeriodicExpirableEffect {

        WitherformEffect(Skill skill, Player applier, long duration) {
            super(skill, effectName, applier, 250, duration, applyText, expireText);

            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.RESIST_FIRE);
            types.add(EffectType.RESIST_WITHER);
            types.add(EffectType.WITHER);
            types.add(EffectType.FORM);

            addPotionEffect(new PotionEffect(PotionEffectType.WITHER, (int) (duration / 50), 1));
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);

            Player player = hero.getPlayer();
            addFireTicks(player);

            if (disguiseApiLoaded) {
                disguiseAsWitherSkelly(player);
            } else {
                addWitherSkull(hero, player);
            }
        }

        @Override
        public void tickMonster(Monster monster) { }

        @Override
        public void tickHero(Hero hero) {
            addFireTicks(hero.getEntity());
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);

            Player player = hero.getPlayer();
            if (disguiseApiLoaded) {
                removeDisguise(player);
            } else {
                removeWitherSkull(player);
            }
        }

        private void disguiseAsWitherSkelly(Player player) {
            if (DisguiseAPI.isDisguised(player)) {
                removeDisguise(player);
            }

            MobDisguise disguise = new MobDisguise(DisguiseType.getType(EntityType.WITHER_SKELETON), true);
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
            if (DisguiseAPI.isDisguised(player)) {
                Disguise disguise = DisguiseAPI.getDisguise(player);
                disguise.stopDisguise();
                disguise.removeDisguise();
            }
        }

        private void addWitherSkull(Hero hero, Player player) {
            PlayerInventory inventory = player.getInventory();

            ItemStack transformedHead = new ItemStack(Material.WITHER_SKELETON_SKULL);
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

        private void addFireTicks(LivingEntity entity) {
            entity.setFireTicks(entity.getFireTicks() + (int) (getPeriod() / 50));
        }
    }

    public class WitherStackEffect extends StackingEffect implements HealthRegainReduction, Stacking {

        private final int witherAmplifier;
        private double healhReductionPerStack;

        public WitherStackEffect(Skill skill, Player applier, int witherAmplifier, double healhReductionPerStack, int maxStacks) {
            super(skill, applier.getName() + "-WitherAttacked", applier, maxStacks);
            this.witherAmplifier = witherAmplifier;
            this.healhReductionPerStack = healhReductionPerStack;

            addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 99999, witherAmplifier));
        }

        @Override
        public Double getDelta() {
            return healhReductionPerStack * effectStack.count();
        }

        @Override
        public void setDelta(Double value) {
            this.healhReductionPerStack = value / effectStack.count();
        }
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
            if (!hero.hasEffect(effectName))
                return;

            Player player = hero.getPlayer();
            int duration = SkillConfigManager.getUseSetting(hero, skill, "on-hit-wither-duration", 2000, false);
            double healingReduction = SkillConfigManager.getUseSetting(hero, skill, "on-hit-healing-reduction-per-stack", 0.075, false);
            int witherAmplifier = SkillConfigManager.getUseSetting(hero, skill, "on-hit-wither-amplifier", 3, false);
            int maxStacks = SkillConfigManager.getUseSetting(hero, skill, "on-hit-max-stacks", 6, false);

            CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter((LivingEntity)event.getEntity());
            targetCT.addEffectStack(player.getName() + "-WitherAttacked", skill, player, duration,
                    () -> new WitherStackEffect(skill, player, witherAmplifier, healingReduction, maxStacks));
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onSkillUse(SkillUseEvent event) {
            if (!event.getHero().hasEffect(effectName))
                return;

            boolean isValidType = false;
            for (SkillType type : event.getSkill().getTypes()) {
                if (type == SkillType.ABILITY_PROPERTY_DARK || type == SkillType.ABILITY_PROPERTY_PHYSICAL || type == SkillType.ABILITY_PROPERTY_WITHER) {
                    isValidType = true;
                }
            }

            if (!isValidType) {
                event.setCancelled(true);
                event.getHero().getPlayer().sendMessage("    " + ChatComponents.GENERIC_SKILL + "You cannot use that skill in this form!");
            }
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onEquipmentChanged(EquipmentChangedEvent event) {
            if (event.getType() != EquipmentType.HELMET || event.getOldArmorPiece() == null
                    || event.getOldArmorPiece().getType() != Material.WITHER_SKELETON_SKULL
                    || event.getMethod() == EquipMethod.EXPIRING_SKILL_EFFECT) {
                return;
            }

            final Hero hero = plugin.getCharacterManager().getHero(event.getPlayer());
            if (hero.hasEffect(effectName))
                event.setCancelled(true);
        }
    }
}
