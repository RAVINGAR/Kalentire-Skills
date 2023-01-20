package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.effects.StackingEffect;
import com.herocraftonline.heroes.characters.effects.common.interfaces.Burning;
import com.herocraftonline.heroes.characters.effects.common.interfaces.HealthRegainReduction;
import com.herocraftonline.heroes.characters.equipment.EquipMethod;
import com.herocraftonline.heroes.characters.equipment.EquipmentChangedEvent;
import com.herocraftonline.heroes.characters.equipment.EquipmentType;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.Disguise;
import me.libraryaddict.disguise.disguisetypes.DisguiseType;
import me.libraryaddict.disguise.disguisetypes.MobDisguise;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
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

    private final String effectName = "WitherForm";
    private boolean disguiseApiLoaded;
    private String applyText;
    private String expireText;

    public SkillTheWither(final Heroes plugin) {
        super(plugin, "TheWither");
        setDescription("Channel the power of the Wither himself. " +
                "Altering your appearance and granting you fire immunity for $1 second(s). " +
                "While active, you are only able to use physical, dark, or wither based abilities, but your melee attacks inflict a stacking wither effect that lasts $2 second(s).");
        setUsage("/skill thewither");
        setArgumentRange(0, 0);
        setIdentifiers("skill thewither", "skill witherform");
        setTypes(SkillType.ABILITY_PROPERTY_WITHER, SkillType.ABILITY_PROPERTY_DARK, SkillType.FORM_ALTERING, SkillType.SILENCEABLE, SkillType.BUFFING);

        // We don't really want to use disguise API I don't think...
//        if (Bukkit.getServer().getPluginManager().getPlugin("LibsDisguises") != null) {
//            disguiseApiLoaded = true;
//        }

        Bukkit.getServer().getPluginManager().registerEvents(new SkillEffectListener(this), plugin);
    }

    @Override
    public String getDescription(final Hero hero) {
        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);

        return getDescription()
                .replace("$1", Util.decFormat.format(duration / 1000));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection config = super.getDefaultConfig();
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

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%hero% has assumed a wither form!").replace("%hero%", "$1").replace("$hero$", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%hero% returns to their human form.").replace("%hero%", "$1").replace("$hero$", "$1");
        setUseText(null);
    }

    @Override
    public SkillResult use(final Hero hero, final String[] args) {
        final Player player = hero.getPlayer();

        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
        hero.addEffect(new WitherformEffect(this, player, duration));
        final Location location = player.getLocation();
        location.getWorld().playSound(location, Sound.ENTITY_WITHER_SKELETON_STEP, 0.5F, 0.5f);
        location.getWorld().playSound(location, Sound.ENTITY_PARROT_IMITATE_WITHER_SKELETON, 1F, 1f);
        broadcastExecuteText(hero);

        return SkillResult.NORMAL;
    }

    public static class WitherStackEffect extends StackingEffect implements HealthRegainReduction {

        private final int witherAmplifier;
        private double healhReductionPerStack;

        public WitherStackEffect(final Skill skill, final Player applier, final int witherAmplifier, final double healhReductionPerStack, final int maxStacks) {
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
        public void setDelta(final Double value) {
            this.healhReductionPerStack = value / effectStack.count();
        }
    }

    public class WitherformEffect extends PeriodicExpirableEffect {

        WitherformEffect(final Skill skill, final Player applier, final long duration) {
            super(skill, effectName, applier, 500, duration, applyText, expireText);

            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.RESIST_FIRE);
            types.add(EffectType.RESIST_WITHER);
            types.add(EffectType.WITHER);
            types.add(EffectType.FORM);

            addPotionEffect(new PotionEffect(PotionEffectType.WITHER, (int) (duration / 50), 1));
        }

        @Override
        public void applyToHero(final Hero hero) {
            super.applyToHero(hero);

            final Player player = hero.getPlayer();
            forceFireTicks(player);

            if (disguiseApiLoaded) {
                disguiseAsWitherSkelly(player);
            } else {
                addWitherSkull(hero, player);
                player.setFireTicks((int) (getRemainingTime() * 50));
            }
        }

        @Override
        public void tickMonster(final Monster monster) {
        }

        @Override
        public void tickHero(final Hero hero) {
            forceFireTicks(hero.getEntity());
        }

        @Override
        public void removeFromHero(final Hero hero) {
            super.removeFromHero(hero);

            final Player player = hero.getPlayer();
            if (disguiseApiLoaded) {
                removeDisguise(player);
            } else {
                removeWitherSkull(player);
            }

            for (final Effect effect : hero.getEffects()) {
                if (effect instanceof Burning) {
                    effect.removeFromHero(hero);
                }
            }

            player.setFireTicks(-1);
        }

        private void disguiseAsWitherSkelly(final Player player) {
            if (DisguiseAPI.isDisguised(player)) {
                removeDisguise(player);
            }

            final MobDisguise disguise = new MobDisguise(DisguiseType.getType(EntityType.WITHER_SKELETON), true);
            disguise.setKeepDisguiseOnPlayerDeath(false);
            disguise.setEntity(player);
            disguise.setCustomDisguiseName(true); // Is this the same? as disguise.setShowName(true) ?
            disguise.setReplaceSounds(true);
            disguise.setHearSelfDisguise(true);
            disguise.startDisguise();
        }

        private void removeDisguise(final Player player) {
            if (DisguiseAPI.isDisguised(player)) {
                final Disguise disguise = DisguiseAPI.getDisguise(player);
                disguise.stopDisguise();
                disguise.removeDisguise();
            }
        }

        private void addWitherSkull(final Hero hero, final Player player) {
            final PlayerInventory inventory = player.getInventory();

            final ItemStack transformedHead = new ItemStack(Material.WITHER_SKELETON_SKULL);
            final ItemMeta itemMeta = transformedHead.getItemMeta();
            itemMeta.setDisplayName("Wither Form");
            itemMeta.setUnbreakable(true);
            transformedHead.setItemMeta(itemMeta);

            final EquipmentChangedEvent replaceEvent = new EquipmentChangedEvent(player, EquipMethod.APPLYING_SKILL_EFFECT, EquipmentType.HELMET, inventory.getHelmet(), transformedHead);
            Bukkit.getServer().getPluginManager().callEvent(replaceEvent);
            if (replaceEvent.isCancelled()) {
                Heroes.log(Level.WARNING, "SkillTheWither: Somebody tried to cancel a EquipmentChangedEvent, and we are ignoring the cancellation.");
            }

            Util.moveItem(hero, -1, inventory.getHelmet());
            inventory.setHelmet(transformedHead);

            Util.syncInventory(player, plugin);
        }

        private void removeWitherSkull(final Player player) {
            final PlayerInventory inventory = player.getInventory();

            final ItemStack transformedHead = inventory.getHelmet();
            final ItemStack emptyHelmet = new ItemStack(Material.AIR, 0);

            final EquipmentChangedEvent replaceEvent = new EquipmentChangedEvent(player, EquipMethod.EXPIRING_SKILL_EFFECT, EquipmentType.HELMET, transformedHead, emptyHelmet);
            Bukkit.getServer().getPluginManager().callEvent(replaceEvent);
            if (replaceEvent.isCancelled()) {
                Heroes.log(Level.WARNING, "SkillTheWither: Somebody tried to cancel a EquipmentChangedEvent, and we are ignoring the cancellation.");
            }

            inventory.remove(transformedHead);
            inventory.setHelmet(emptyHelmet);
            Util.syncInventory(player, plugin);
        }

        private void forceFireTicks(final LivingEntity entity) {
            entity.setFireTicks((int) (getPeriod() / 50) + 1);
        }
    }

    public class SkillEffectListener implements Listener {
        private final Skill skill;

        SkillEffectListener(final Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onWeaponDamageEvent(final WeaponDamageEvent event) {
            if (!(event.getDamager() instanceof Hero) || !(event.getEntity() instanceof LivingEntity)) {
                return;
            }

            final Hero hero = (Hero) event.getDamager();
            if (!hero.hasEffect(effectName)) {
                return;
            }

            final Player player = hero.getPlayer();
            final int duration = SkillConfigManager.getUseSetting(hero, skill, "on-hit-wither-duration", 2000, false);
            final double healingReduction = SkillConfigManager.getUseSetting(hero, skill, "on-hit-healing-reduction-per-stack", 0.075, false);
            final int witherAmplifier = SkillConfigManager.getUseSetting(hero, skill, "on-hit-wither-amplifier", 3, false);
            final int maxStacks = SkillConfigManager.getUseSetting(hero, skill, "on-hit-max-stacks", 6, false);

            final CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter((LivingEntity) event.getEntity());
            targetCT.addEffectStack(player.getName() + "-WitherAttacked", skill, player, duration,
                    () -> new WitherStackEffect(skill, player, witherAmplifier, healingReduction, maxStacks));
        }

//        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
//        public void onSkillUse(SkillUseEvent event) {
//            if (!event.getHero().hasEffect(effectName))
//                return;
//
//            boolean isValidType = false;
//            for (SkillType type : event.getSkill().getTypes()) {
//                if (type == SkillType.ABILITY_PROPERTY_DARK || type == SkillType.ABILITY_PROPERTY_PHYSICAL || type == SkillType.ABILITY_PROPERTY_WITHER) {
//                    isValidType = true;
//                }
//            }
//
//            if (!isValidType) {
//                event.setCancelled(true);
//                event.getHero().getPlayer().sendMessage("    " + ChatComponents.GENERIC_SKILL + "You cannot use that skill in this form!");
//            }
//        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onEquipmentChanged(final EquipmentChangedEvent event) {
            if (event.getType() != EquipmentType.HELMET || event.getOldArmorPiece() == null
                    || event.getOldArmorPiece().getType() != Material.WITHER_SKELETON_SKULL
                    || event.getMethod() == EquipMethod.EXPIRING_SKILL_EFFECT) {
                return;
            }

            final Hero hero = plugin.getCharacterManager().getHero(event.getPlayer());
            if (hero.hasEffect(effectName)) {
                event.setCancelled(true);
            }
        }
    }
}
