package com.herocraftonline.heroes.characters.skill.reborn.myrmidon;

import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
//import net.minecraft.server.v1_13_R2.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.WeatherType;
import org.bukkit.configuration.ConfigurationSection;
import com.herocraftonline.heroes.Heroes;
//import org.bukkit.craftbukkit.v1_13_R2.*;
//import org.bukkit.craftbukkit.v1_13_R2.entity.CraftPlayer;
//import org.bukkit.craftbukkit.v1_13_R2.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityToggleSwimEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRiptideEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

public class SkillTridents extends PassiveSkill {

    public SkillTridents(Heroes plugin) {
        super(plugin, "Tridents");
        setDescription("You are able wield Tridents!");
        setArgumentRange(0, 0);
        setEffectTypes(EffectType.BENEFICIAL);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.APPLY_TEXT.node(), "");
        config.set(SkillSetting.UNAPPLY_TEXT.node(), "");
        config.set(SkillSetting.LEVEL.node(), 1);
        return config;
    }

    public class SkillListener implements Listener {

        private final Skill skill;
        public SkillListener(Skill skill) {
            this.skill = skill;
        }

//        @EventHandler
//        public void onProjectileLaunch(ProjectileLaunchEvent event) {
//            if(!(event.getEntity() instanceof Trident))
//                return;
//
//            Trident projectile = (Trident) event.getEntity();
//            Player player = (Player) projectile.getShooter();
//
//            event.setCancelled(true);
//
//            player.setSwimming(true);
//        }

//        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
//        public void onPlayerLaunchProjectileEvent(ProjectileLaunchEvent event) {
//            if (!(event.getEntity() instanceof Trident))
//                return;
//
////            Trident projectile = (Trident) event.getEntity();
//            Player player = (Player) event.getEntity().getShooter();
//
//            ItemStack trident = new ItemStack(Material.TRIDENT, 1);
//            trident.addEnchantment(Enchantment.RIPTIDE, 3);
//
//            ItemStack playerMainHand = player.getInventory().getItemInMainHand();
//            ItemStack tempPlayerMainHand = playerMainHand.clone();
//
//            player.getInventory().remove(playerMainHand);
//
//            player.getInventory().setItemInMainHand(trident);
//
//
//            if(!player.isRiptiding() && player.getInventory().getItemInMainHand().getType() == Material.TRIDENT) {
//                event.setCancelled(true);
//
//                player.setPlayerWeather(WeatherType.DOWNFALL);
////                PlayerRiptideEvent riptideEvent = new PlayerRiptideEvent(player, trident);
//
//                PlayerRiptideEvent playerRiptideEvent = new PlayerRiptideEvent(player, trident);
//
//
//                net.minecraft.server.v1_13_R2.ItemStack tridentNMS = CraftItemStack.asNMSCopy(trident);
//                EntityPlayer ep = ((CraftPlayer)player).getHandle();
//                World world = ((CraftWorld)player.getLocation().getWorld()).getHandle();
//
//                net.minecraft.server.v1_13_R2.PacketPlayInEntityAction packetPlayInEntityAction = new PacketPlayInEntityAction();
//
//                EntityThrownTrident entityThrownTrident = new EntityThrownTrident(world, ep, tridentNMS);
//
//                NBTTagCompound nbtTagCompound = tridentNMS.getTag();
//
//                entityThrownTrident.b(nbtTagCompound);
//
//                Bukkit.getPluginManager().callEvent(playerRiptideEvent);
//
//                player.sendMessage("Riptiding!");
//                player.sendMessage("Event Name: " + playerRiptideEvent.getEventName() + " - Player Name: " + playerRiptideEvent.getPlayer().getDisplayName() + " - Riptide Enchantment Level: " + playerRiptideEvent.getItem().getEnchantmentLevel(Enchantment.RIPTIDE));
//
//                player.getInventory().remove(trident);
//                player.getInventory().setItemInMainHand(tempPlayerMainHand);
//            }
//        }

//        @EventHandler(priority = EventPriority.MONITOR)
//        public void onPlayerSwim(EntityToggleSwimEvent event) {
////            Player player = (Player) event.getEntity();
////
////            if(player.isSwimming()) {
////
////            }
////
////            if(player.getLocation().getBlock().getType() == Material.WATER) {
////                return;
////            }
//            event.setCancelled(true);
//        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onPlayerJoin(PlayerJoinEvent event) {
            Player player = event.getPlayer();
//
//            EntityPlayer ep = ((CraftPlayer)player).getHandle();
//
//            ep.setFlag(3, true);
//            ep.setFlag(4, true);
//
//            ep.setPlayerWeather(WeatherType.DOWNFALL, true);
//
//            PacketPlayOutSpawnEntityWeather packetPlayOutSpawnEntityWeather = new PacketPlayOutSpawnEntityWeather();
//
//            PacketPlayOutEntityMetadata packetPlayOutEntityMetadata = new PacketPlayOutEntityMetadata(ep.getId(), ep.getDataWatcher(), true);
//
//            ep.playerConnection.sendPacket(packetPlayOutEntityMetadata);

            player.setPlayerWeather(WeatherType.DOWNFALL);
        }

//        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
//        public void onPlayerInteract(PlayerInteractEvent event) {
//            Player player = event.getPlayer();
//
//            player.sendMessage("InteractEvent....");
//
//            // Check if player is holding a trident and if it has riptide...
//            if(player.getInventory().getItemInMainHand().getType() == Material.TRIDENT) {
//                player.setSwimming(true);
//            }
//        }

        // called when player launches trident with riptide enchantment
        // not sure if it gets called when they are not in rain
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onPlayerRiptide(PlayerRiptideEvent event) {
            Player player = event.getPlayer();
//            player.setPlayerWeather(WeatherType.CLEAR);
            player.sendMessage("PlayerRiptide!");
        }

    }
}
