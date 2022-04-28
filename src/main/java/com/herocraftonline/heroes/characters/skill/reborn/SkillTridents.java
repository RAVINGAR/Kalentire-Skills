package com.herocraftonline.heroes.characters.skill.reborn;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRiptideEvent;

//import net.minecraft.server.v1_13_R2.*;
//import org.bukkit.craftbukkit.v1_13_R2.*;
//import org.bukkit.craftbukkit.v1_13_R2.entity.CraftPlayer;
//import org.bukkit.craftbukkit.v1_13_R2.inventory.CraftItemStack;

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
