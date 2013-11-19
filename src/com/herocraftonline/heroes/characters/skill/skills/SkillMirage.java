package com.herocraftonline.heroes.characters.skill.skills;

import java.util.ArrayList;
import org.bukkit.Location;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;

public class SkillMirage extends ActiveSkill {
    
    ArrayList<HumanEntity> clones;

    public SkillMirage(Heroes plugin) {
        super(plugin, "Mirage");
        setIdentifiers("skill mirage");
        setDescription("Summons a controllable clone that explodes on death or after 30 seconds that will travel in a straight line for 5 seconds after summoning by default. Also renders the user invisible for 2 seconds");
        this.clones = new ArrayList<HumanEntity>();
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player p = hero.getPlayer();
        String displayName = p.getName();
        Location spawnLoc = hero.getPlayer().getLocation();
        
        //Create a duplicate entity identical in appearance to the player
        final HumanEntity hE = spawnLoc.getWorld().spawn(spawnLoc, HumanEntity.class);
        hE.setCustomName(displayName);
        hE.setItemInHand(hero.getPlayer().getItemInHand().clone());
        hE.getEquipment().setArmorContents(p.getInventory().getArmorContents().clone());
        hE.setCanPickupItems(false);
        hE.setVelocity(p.getVelocity());
        
        //Add the HumanEntity in question to a hashmap of clones
        clones.add(hE);
        plugin.getServer().getScheduler().runTaskLater(plugin, new BukkitRunnable() {

            @Override
            public void run() {
                if(clones.contains(hE)) {
                    if(!hE.isValid()) {
                        clones.remove(hE);
                        return;
                    }
                }
            }
            
        }, 600);
        
        //Hide the player in question
        
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(Hero hero) {
        // TODO Auto-generated method stub
        return null;
    }

}
