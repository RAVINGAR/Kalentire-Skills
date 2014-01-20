package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Horse.Color;
import org.bukkit.entity.Horse.Variant;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.inventory.HorseInventory;
import org.bukkit.inventory.ItemStack;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;

public class SkillDreadSteed extends ActiveSkill {

    // BlockFace[] faces = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.NORTH_EAST, BlockFace.NORTH_WEST, BlockFace.SOUTH_EAST, BlockFace.SOUTH_WEST};
    
    public SkillDreadSteed(Heroes plugin) {
        super(plugin, "DreadSteed");
        setDescription("Summons a dread steed for $1");
        setIdentifiers("skill dreadsteed");
        setUsage("/skill dreadsteed");
        setArgumentRange(0,0);
        new DreadSteedListener(plugin);
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player heroP = hero.getPlayer();
        if(heroP.isInsideVehicle()) {
            heroP.sendMessage(ChatColor.RED + "Cannot use while mounted!");
            return SkillResult.FAIL;
        }
        Location loc = heroP.getLocation();
        // Basic no spawn check, denies if there's a block in a 2 radius horizontally. Inconvenient to players, but failsafe in case suffocation listener fails
        /*for(BlockFace face: faces) {
            if(loc.getBlock().getRelative(face).getType() != Material.AIR || 
                    loc.getBlock().getRelative(face).getRelative(BlockFace.UP).getType() != Material.AIR ||
                    loc.getBlock().getRelative(face, 2).getType() != Material.AIR ||
                    loc.getBlock().getRelative(face, 2).getRelative(BlockFace.UP).getType() != Material.AIR) {
                heroP.sendMessage(ChatColor.RED + "A steed needs breathing room!");
                return SkillResult.FAIL;
            }
        }*/
        Horse horse = loc.getWorld().spawn(loc, Horse.class);
        Monster m = plugin.getCharacterManager().getMonster(horse);
        m.setMaxHealth(1000D);
        m.getEntity().setCustomName(hero.getName() + "'s DreadSteed");
        // m.addEffect(new DreadSteedEffect(this.plugin));
        HorseInventory hInv = horse.getInventory();
        hInv.setSaddle(new ItemStack(Material.SADDLE));
        horse.setTamed(true);
        horse.setVariant(Variant.HORSE);
        horse.setColor(Color.BLACK);
        horse.setPassenger(heroP);

        int summonDuration = SkillConfigManager.getUseSetting(hero, this, "summon-duration", Integer.valueOf(60),false);
        m.addEffect(new ExpirableEffect(this, plugin, "HorseExpiry", heroP, summonDuration*1000) {
            @Override
            public void removeFromMonster(Monster m) {
                m.getEntity().remove();
            }
        });
        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

    private class DreadSteedListener implements Listener {
        public DreadSteedListener(Heroes plugin) {
            plugin.getServer().getPluginManager().registerEvents(this, plugin);
        }

        // Lowest priority so it's before HeroFeatures
        @EventHandler(priority=EventPriority.LOWEST, ignoreCancelled=true)
        public void onVehicleExit(VehicleExitEvent event) {
            if(event.getVehicle().getType() == EntityType.HORSE) {
                Monster m = plugin.getCharacterManager().getMonster((Horse) event.getVehicle());
                if(m.hasEffect("HorseExpiry")) {
                    // Remove horse on unmount
                    Effect e = m.getEffect("HorseExpiry");
                    if(e instanceof ExpirableEffect) {
                        ((ExpirableEffect) e).expire();
                    }
                }
            }
        }
        
        // Removes horse if it suffocates, probably because it was summoned next to a wall.
        @EventHandler
        public void onEntityDamage(EntityDamageEvent event) {
            if(event.getEntityType() == EntityType.HORSE && event.getCause() == DamageCause.SUFFOCATION) {
                Monster m = plugin.getCharacterManager().getMonster((Horse) event.getEntity());
                if(m.hasEffect("HorseExpiry")) {
                    Effect e = m.getEffect("HorseExpiry");
                    if(e instanceof ExpirableEffect) {
                        Entity passenger = m.getEntity().getPassenger();
                        if(passenger instanceof Player) {
                            ((Player) passenger).sendMessage(ChatColor.RED + "A steed needs breathing room!");
                        }
                        ((ExpirableEffect) e).expire();
                    }
                }
            }
        }
        
        @EventHandler
        public void onPlayerQuit(PlayerQuitEvent event) {
            Player player = event.getPlayer();
            if(player.isInsideVehicle() || player.getVehicle().getType() != EntityType.HORSE) {
                Monster m = plugin.getCharacterManager().getMonster((Horse) player.getVehicle());
                if(m.hasEffect("HorseExpiry")) {
                    Effect e = m.getEffect("HorseExpiry");
                    if(e instanceof ExpirableEffect) {
                        m.getEntity().eject();
                    }
                }
            }
        }
    }

    /*private class DreadSteedEffect extends PeriodicEffect {

        public DreadSteedEffect(Heroes plugin) {
            super(plugin, "DreadSteedEffect", 1000);
        }

        @Override//Damage portion.
        public void tickMonster(Monster m) {
            Entity passengerEntity = m.getEntity().getPassenger();
            if(passengerEntity == null) {
                return;
            }
            if(!(passengerEntity instanceof Player)) {
                return;
            }
            Player passenger = (Player) m.getEntity().getPassenger();
            for(Entity e: m.getEntity().getNearbyEntities(2, 2, 2)) {
                if(!(e instanceof LivingEntity)) {
                    continue;
                }
                LivingEntity lE = (LivingEntity)e;
                if(Skill.damageCheck(passenger, lE)) {
                    addSpellTarget(lE,plugin.getCharacterManager().getHero(passenger));
                    Skill.damageEntity(lE, passenger, 5D, DamageCause.ENTITY_ATTACK, false);
                }
            }
        }
    }*/
    @Override
    public String getDescription(Hero hero) {
        int summonDuration = SkillConfigManager.getUseSetting(hero, this, "summon-duration", Integer.valueOf(60),false);
        return getDescription().replace("$1", summonDuration + "");
    }


}