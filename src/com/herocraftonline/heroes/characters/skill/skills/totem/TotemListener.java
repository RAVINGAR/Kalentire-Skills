package com.herocraftonline.heroes.characters.skill.skills.totem;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import org.bukkit.block.Block;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.server.PluginDisableEvent;

import java.util.Iterator;

public class TotemListener implements Listener {

    private Heroes plugin;

    public TotemListener(Heroes plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPluginDisable(PluginDisableEvent e) {

        if(e.getPlugin() != plugin) {
            return;
        }

        Iterator<Totem> iter = SkillBaseTotem.totems.iterator();
        while(iter.hasNext()) {
            Totem totem = iter.next();
            totem.destroyTotem();
            // Probably doesn't do anything but rather be safe than sorry.
            totem.getEffect().expire();
            iter.remove();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent e) {
        Hero hero = plugin.getCharacterManager().getHero(e.getEntity());

        if(!hero.hasEffect("TotemEffect")) {
            return;
        }
        
        Effect effect = hero.getEffect("TotemEffect");
        
        if(effect instanceof TotemEffect) {
            ((TotemEffect) effect).expire();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent e) {
        if(SkillBaseTotem.isTotemBlock(e.getBlock())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageEvent e) {
        if((e.getEntity() instanceof EnderCrystal) && SkillBaseTotem.isTotemCrystal((EnderCrystal) e.getEntity())) {
            e.setCancelled(true);
        }
        if((e.getEntity() instanceof LivingEntity) && e.getCause() == DamageCause.SUFFOCATION && SkillBaseTotem.isTotemBlock(((LivingEntity) e.getEntity()).getEyeLocation().getBlock())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityExplode(EntityExplodeEvent e) {
        //if(!((e.getEntity() instanceof EnderCrystal) && SkillBaseTotem.isTotemCrystal((EnderCrystal) e.getEntity()))) {
        if(!(e.getEntity() instanceof EnderCrystal)) {
            return;
        }
        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        //if(!((e.getDamager() instanceof EnderCrystal) && SkillBaseTotem.isTotemCrystal((EnderCrystal) e.getDamager()))) {
        if(!(e.getDamager() instanceof EnderCrystal)) {
            return;
        }
        e.setCancelled(true);
    }

    // This should prevent Piston movement of Totem blocks as listed in Exploit Reports.
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPistonExtend(BlockPistonExtendEvent e) {
        for(Block block : e.getBlocks()) {
            if(SkillBaseTotem.isTotemBlock(block)) {
                e.setCancelled(true);
                return; // Since loop could continue if not returned
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPistonRetract(BlockPistonRetractEvent e) {
        for(Block block : e.getBlocks()) { // This is Spigot exclusive, bukkit doesn't have getBlocks() on Retract.
            if(SkillBaseTotem.isTotemBlock(block)) {
                e.setCancelled(true);
                return; // Since loop could continue if not returned
            }
        }
    }

}