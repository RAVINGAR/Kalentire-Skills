package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Horse.Variant;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
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

    public SkillDreadSteed(Heroes plugin) {
        super(plugin, "DreadSteed");
        setDescription("Summons a ridable undead steed for $1 seconds");
        setIdentifiers("skill dreadsteed");
        setUsage("/skill dreadsteed");
        setArgumentRange(0,0);
        setTypes(SkillType.SUMMONING, SkillType.SILENCABLE);
        new DreadSteedListener(plugin);
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Location loc = hero.getPlayer().getLocation();
        Horse horse = loc.getWorld().spawn(loc, Horse.class);
        Monster m = plugin.getCharacterManager().getMonster(horse);
        m.setMaxHealth(1000D);
        m.getEntity().setCustomName(hero.getName() + "'s DreadSteed");
        // m.addEffect(new DreadSteedEffect(this.plugin));
        HorseInventory hInv = horse.getInventory();
        hInv.setSaddle(new ItemStack(Material.SADDLE));
        horse.setTamed(true);
        horse.setVariant(Variant.UNDEAD_HORSE);
        horse.setPassenger(hero.getPlayer());

        int summonDuration = SkillConfigManager.getUseSetting(hero, this, "summon-duration", Integer.valueOf(60),false);
        m.addEffect(new ExpirableEffect(this, plugin, "HorseExpiry", hero.getPlayer(), summonDuration*1000) {
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

        @EventHandler
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