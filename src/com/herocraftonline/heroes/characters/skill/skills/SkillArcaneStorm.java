package com.herocraftonline.heroes.characters.skill.skills;
//http://pastie.org/private/i04dtc6t4oannstqls6sq

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.RootEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.VisualEffect;

public class SkillArcaneStorm extends ActiveSkill  {

	public SkillArcaneStorm(Heroes plugin) {
		super(plugin, "ArcaneStorm");
		setIdentifiers("skill ArcaneStorm");
		setUsage("/skill ArcaneStorm");
		setArgumentRange(0,0);
        setTypes(SkillType.DAMAGING, SkillType.LIGHT, SkillType.SILENCABLE, SkillType.HARMFUL);
		setDescription("On use, user is rooted into place for 5 seconds. " +
				"After the 5 seconds, the user unleashes a hail of devastating magical artillery in the surrounding area");
	}

	@Override
	public SkillResult use(final Hero hero, String[] arg1) {
		hero.addEffect(new RootEffect(this, 5000L) {
			@Override
			public void applyToHero(Hero hero) {
				super.applyToHero(hero);
			    final Player player = hero.getPlayer();
			    broadcast(hero.getEntity().getLocation(), ChatColor.GRAY + "["+ChatColor.DARK_GREEN+"Skill"+ ChatColor.GRAY+ "] $1 has begun channeling an arcane storm!", new Object[] 
			    		{hero.getPlayer().getName()});
			    List<Location> fireworkLocations = circle(hero.getPlayer(),hero.getPlayer().getLocation(),10,1,true,false,15);
			    long ticksPerFirework = (int) (100.00/((double)fireworkLocations.size()));
			    final VisualEffect fplayer = new VisualEffect();
			    for(int i = 0; i < fireworkLocations.size(); i++) {
			    	final Location fLoc = fireworkLocations.get(i);
			    	Bukkit.getScheduler().runTaskLater(this.plugin, new Runnable() {
						@Override
						public void run() {
							try {
								fplayer.playFirework(fLoc.getWorld(), fLoc, FireworkEffect
										.builder()
										.withColor(Color.AQUA)
										.with(Type.BURST)
										.build());
							} catch (IllegalArgumentException e) {
								e.printStackTrace();
							} catch (Exception e) {
								e.printStackTrace();
							}
						}

			    	}, ticksPerFirework*i);	    	
			    }
			    Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, new Runnable() {

					@Override
					public void run() {
						Iterator<Entity> nearby = player.getNearbyEntities(16, 5, 16).iterator();
						while(nearby.hasNext()) {
							Entity entity = nearby.next();
							if(!(entity instanceof LivingEntity)) {
								continue;
							}
							if(!Skill.damageCheck(player, (LivingEntity) entity)) {
								continue;
							}
							Skill.damageEntity((LivingEntity)entity, player, 50D, DamageCause.MAGIC);
							player.getWorld().strikeLightningEffect(entity.getLocation());
						}
					}

		    	}, 100);
			}
			@Override
			public void removeFromHero(Hero hero) {
				super.removeFromHero(hero);
			    broadcast(hero.getPlayer().getLocation(), ChatColor.GRAY + "["+ChatColor.DARK_GREEN+"Skill"+ ChatColor.GRAY+ "] Arcane Storm Unleashed!", new Object[] {});
			}

		});
		return SkillResult.NORMAL;
	}
	protected List<Location> circle(Player player, Location loc, Integer r, Integer h, boolean hollow, boolean sphere, int plus_y) {
		List<Location> circleblocks = new ArrayList<Location>();
        int cx = loc.getBlockX();
        int cy = loc.getBlockY();
        int cz = loc.getBlockZ();
        for (int x = cx - r; x <= cx +r; x++)
            for (int z = cz - r; z <= cz +r; z++)
                for (int y = (sphere ? cy - r : cy); y < (sphere ? cy + r : cy + h); y++) {
                    double dist = (cx - x) * (cx - x) + (cz - z) * (cz - z) + (sphere ? (cy - y) * (cy - y) : 0);
                    if (dist < r*r && !(hollow && dist < (r-1)*(r-1))) {
                        Location l = new Location(loc.getWorld(), x, y + plus_y, z);
                        circleblocks.add(l);
                        }
                    }
     
        return circleblocks;
    }
	@Override
	public String getDescription(Hero arg0) {
		return getDescription();
	}


}
