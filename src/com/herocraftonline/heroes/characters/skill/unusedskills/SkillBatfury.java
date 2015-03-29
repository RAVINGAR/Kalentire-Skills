package com.herocraftonline.heroes.characters.skill.unusedskills;
//TODO: add in following AI so the skill is more stressful to the victim 
//https://github.com/DMarby/Pets/blob/master/src/main/java/se/DMarby/Pets/EntityBatPet.java
//possible AI source from Pets
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.craftbukkit.v1_8_R2.entity.CraftEntity;
import org.bukkit.entity.Bat;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.ExperienceChangeEvent;
import com.herocraftonline.heroes.api.events.HeroKillCharacterEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.classes.HeroClass;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Util;

public class SkillBatfury extends TargettedSkill implements Listener {

    public final Set<Bat> skillBats = new HashSet<Bat>();
	
    private long skillBatKillTick;

    public SkillBatfury(Heroes plugin) {
        super(plugin, "Batfury");
        setDescription("Your target is surrounded by a fury of bats. Bats despawn after $4s.");
        setUsage("/skill batfury ");
        setArgumentRange(0, 0);
        setIdentifiers("skill bats", "skill batfury");
        setTypes(SkillType.SUMMONING, SkillType.SILENCEABLE, SkillType.KNOWLEDGE, SkillType.AGGRESSIVE);

        Bukkit.getPluginManager().registerEvents(this, this.plugin);
    }

    public String getDescription(Hero hero) {
        StringBuilder descr = new StringBuilder(getDescription().replace("$1", String.valueOf(getAmountFor(hero)))
                .replace("$4", Util.stringDouble(getDespawnDelayFor(hero) / 1000.0)));

        double cdSec = SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN, 30000, false) / 1000.0;
        if (cdSec > 0) {
            descr.append(" CD:");
            descr.append(Util.formatDouble(cdSec));
            descr.append("s");
        }

        int mana = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MANA, 30, false);
        if (mana > 0) {
            descr.append(" M:");
            descr.append(mana);
        }

        double distance = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE.node(), 10, false) +
                SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE_INCREASE.node(), 0.1, false) * hero.getSkillLevel(this);
        if (distance > 0) {
            descr.append(" Dist:");
            descr.append(Util.formatDouble(distance));
        }

        return descr.toString();
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection defaultConfig = super.getDefaultConfig();
        defaultConfig.set(SkillSetting.MAX_DISTANCE.node(), 25);
        defaultConfig.set(SkillSetting.COOLDOWN.node(), 10000);
        defaultConfig.set(SkillSetting.MANA.node(), 30);
        defaultConfig.set("bat-amount", 30);
        defaultConfig.set("base-despawn-delay", 5000);
        defaultConfig.set("per-level-despawn-delay", 50);
        return defaultConfig;
    }

    public double getChanceFor(Hero hero, String key) {
        return SkillConfigManager.getUseSetting(hero, this, "base-chance-" + key, 0.1, false) +
                SkillConfigManager.getUseSetting(hero, this, "chance-per-level-" + key, 0.002, false) * hero.getSkillLevel(this);
    }

    public int getAmountFor(Hero hero) {
        return SkillConfigManager.getUseSetting(hero, this, "bat-amount", 4, false);
    }

    public int getDespawnDelayFor(Hero hero) {
        return SkillConfigManager.getUseSetting(hero, this, "base-despawn-delay", 5000, false) +
                SkillConfigManager.getUseSetting(hero, this, "per-level-despawn-delay", 100, false) * hero.getSkillLevel(this);
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        if (hero.getPlayer() == target) {
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        int amount = getAmountFor(hero);
        List<Bat> spawnedBats = new ArrayList<Bat>();

        Location targetLoc = target.getLocation();
        for (int i = 0; i < amount; i++) {
            
            Bat bat = targetLoc.getWorld().spawn(targetLoc, Bat.class);

            spawnedBats.add(bat);
        }
		
        skillBats.addAll(spawnedBats);
        int despawnDelay = getDespawnDelayFor(hero);
		Timer timer = new Timer();
		timer.schedule(new BatFlightTimer(spawnedBats, target,despawnDelay), 0, 125);
        broadcastExecuteText(hero, target);
        return SkillResult.NORMAL;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity living = event.getEntity();
        if (living instanceof Bat) {
            Bat bat = (Bat) living;
            if (skillBats.contains(bat)) {
                event.setDroppedExp(0);
                event.getDrops().clear();
                bat.remove();
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onHeroKillCharacter(HeroKillCharacterEvent event) {
        LivingEntity living = event.getDefender().getEntity();
        if (living instanceof Bat) {
            Bat bat = (Bat) living;
            if (skillBats.contains(bat)) {
                skillBatKillTick = getFirstWorldTime();
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onExperienceChange(ExperienceChangeEvent event) {
        if (event.getSource() == HeroClass.ExperienceType.KILLING && skillBatKillTick == getFirstWorldTime()) {
            event.setCancelled(true);
        }
    }
	
	
	//
	//
	//	Timer for setting bats in front of face.
	//	Note this can be set to swarm target instead.
	//
	//
	public class BatFlightTimer extends TimerTask{
			
			private final List<Bat> bats;
			private final LivingEntity target;
			private int despawnDelay, expires;
			
			double x,y,z;
			double angle = 0;
			
			public BatFlightTimer(List<Bat> bats, LivingEntity target, int despawnDelay) {
	            this.bats = bats;
				this.target = target;
				this.despawnDelay = despawnDelay/125;
	        }
			
			public void run() {
				
				expires++;
				
				if(expires >= despawnDelay){
					for (Bat bat : bats) {
						bat.remove();
					}
					this.cancel();
				}
				
				for (Bat bat : bats) {
					angle = Math.random() * 360;
					
					x = (target.getLocation().getX() + (0.5 * Math.sin(angle)));
					z = (target.getLocation().getZ() + (1.5 * Math.sin(angle)));
					y = (target.getLocation().getY() + (1 * Math.cos(angle)));
					
					// This can be use to swarm around target. Adjust x,z radius accordingly. I use 1.5 for tha radius
					//((CraftEntity) bat).getHandle().setPosition(x, target.getLocation().getY() + (Math.random()*3), z);
					//
					
					// This places bats in front of target's face all the time.
					((CraftEntity) bat).getHandle().setPosition((target.getLocation().getDirection().getX()*1) + x, (target.getLocation().getY()*1)+(Math.random()*3),(target.getLocation().getDirection().getZ()*1)+z);
				}
			}	
		}
		
    private long getFirstWorldTime() {
        return Bukkit.getWorlds().get(0).getFullTime();
    }
}
