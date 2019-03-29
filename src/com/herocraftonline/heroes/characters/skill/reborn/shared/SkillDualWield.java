package com.herocraftonline.heroes.characters.skill.reborn.shared;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.heroes.util.Util;
import net.minecraft.server.v1_13_R2.Entity;
import net.minecraft.server.v1_13_R2.EntityPlayer;
import net.minecraft.server.v1_13_R2.PacketPlayOutAnimation;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftPlayer;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

public class SkillDualWield extends PassiveSkill {

    private NMSHandler nmsHandler = NMSHandler.getInterface();

    public SkillDualWield(Heroes plugin) {
        super(plugin, "DualWield");
        setDescription("You are able to wield two weapons! Whenever you attack an enemy, "
                + "you will follow it up with an additional attack from your offhand weapon, dealing $1% of its normal damage");
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.DAMAGING);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillHeroListener(this), plugin);
    }

    public String getDescription(Hero hero) {
        double damageEffectiveness = SkillConfigManager.getUseSetting(hero, this, "damage-effectiveness", 1.0, false);
        return getDescription().replace("$1", Util.decFormat.format(damageEffectiveness * 100));
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set("damage-effectiveness", 1.0);
        config.set("attack-delay-ticks", 5);
        return config;
    }

    public class SkillHeroListener implements Listener {
        private Skill skill;
        private String metaDataName = "SwungOffhandLast";

        public SkillHeroListener(Skill skill) {
            this.skill = skill;
        }

//        @EventHandler(priority = EventPriority.LOWEST)
//        public void onLeftClick(PlayerInteractEvent event) {
//            if (event.getAction() != Action.LEFT_CLICK_AIR && event.getAction() != Action.LEFT_CLICK_BLOCK)
//                return;
//
//            Player player = event.getPlayer();
//            PlayerInventory playerInv = player.getInventory();
//            ItemStack offHand = NMSHandler.getInterface().getItemInOffHand(playerInv);
//            if (offHand == null || !Util.weapons.contains(offHand.getType().name()))
//                return;
//
//            Hero hero = plugin.getCharacterManager().getHero(player);
//            if (!hero.canUseSkill(skill))
//                return;
//
//            int delayTicks = SkillConfigManager.getUseSetting(hero, skill, "attack-delay-ticks", 5, false);
//            new BukkitRunnable() {
//                @Override
//                public void run() {
//                    nmsHandler.sendPlayerAnimationPacket(player, 3);
//                }
//            }.runTaskLaterAsynchronously(plugin, delayTicks);
//
////            event.setUseItemInHand(Event.Result.DENY);
////            event.setCancelled(true);
////            player.removeMetadata(metaDataName, plugin);
//        }

        @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
        public void onWeaponDamage(WeaponDamageEvent event) {
            if (!(event.getDamager() instanceof Hero) || !(event.getEntity() instanceof LivingEntity))
                return;

            Hero hero = (Hero) event.getDamager();
            if (!hero.canUseSkill(skill))
                return;

            Player player = hero.getPlayer();
            PlayerInventory playerInv = player.getInventory();
            ItemStack mainHand = NMSHandler.getInterface().getItemInMainHand(playerInv);
            ItemStack offHand = NMSHandler.getInterface().getItemInOffHand(playerInv);
            if (mainHand == null || !Util.weapons.contains(mainHand.getType().name()) || offHand == null || !Util.weapons.contains(offHand.getType().name()))
                return;

            double damageEffectiveness = SkillConfigManager.getUseSetting(hero, skill, "damage-effectiveness", 1.0, false);
            double damage = plugin.getDamageManager().getHighestItemDamage(hero, offHand.getType()) * damageEffectiveness;
            if (damage <= 0)
                return;

            int delayTicks = SkillConfigManager.getUseSetting(hero, skill, "attack-delay-ticks", 5, false);
            LivingEntity targetLE = (LivingEntity) event.getEntity();
            new BukkitRunnable() {

                @Override
                public void run() {
                    if (!damageCheck(player, targetLE))
                        return;

                    addSpellTarget(targetLE, hero);
                    damageEntity(targetLE, player, damage, EntityDamageEvent.DamageCause.ENTITY_ATTACK, false);

                    nmsHandler.sendPlayerAnimationPacket(player, 3);
                    targetLE.getWorld().playSound(targetLE.getLocation(), Sound.ENTITY_GENERIC_HURT, 0.8F, 1.0F);
                }
            }.runTaskLaterAsynchronously(plugin, delayTicks);
        }
    }
}
