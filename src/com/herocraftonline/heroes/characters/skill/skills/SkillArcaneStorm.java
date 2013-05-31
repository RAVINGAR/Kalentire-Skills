package com.herocraftonline.heroes.characters.skill.skills;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.heroes.characters.skill.VisualEffect;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.RootEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillType;

public class SkillArcaneStorm extends ActiveSkill  {
	
    public VisualEffect fplayer = new VisualEffect();
	public SkillArcaneStorm(Heroes plugin) {
		super(plugin, "ArcaneStorm");
		setIdentifiers("skill ArcaneStorm");
        setTypes(SkillType.DAMAGING, SkillType.LIGHT, SkillType.SILENCABLE, SkillType.HARMFUL);
		setUsage("/skill ArcaneStorm");
		setArgumentRange(0,0);
		setDescription("On use, user is rooted into place for 5 seconds. " +
				"After the 5 seconds, the user unleashes a hail of devastating magical artillery in the surrounding area");
	}

	@Override
	public SkillResult use(final Hero h, String[] arg1) {
		h.addEffect(new RootEffect(this, 5000L) {
			@Override
			public void applyToHero(Hero h) {
				super.applyToHero(h);
			    final Player p = h.getPlayer();
			    broadcast(h.getEntity().getLocation(), "§7[§2Skill§7] $1 has begun channeling an arcane storm!", new Object[] {h.getPlayer().getName()});
			    List<Location> fireworkLocations = circle(h.getPlayer(),h.getPlayer().getLocation(),10,1,true,false,15);
			    long ticksPerFirework = (int) (100.00/((double)fireworkLocations.size()));
			    final VisualEffect fplayer = new VisualEffect();
			    for(int i = 0; i < fireworkLocations.size(); i++) {
			    	final Location fLoc = fireworkLocations.get(i);
			    	Bukkit.getScheduler().runTaskLater(this.plugin, new Runnable() {
						@Override
						public void run() {
							try {
								fplayer.playFirework(fLoc.getWorld(), fLoc, 
										FireworkEffect.builder()
					            		.flicker(false).trail(false)
					            		.with(FireworkEffect.Type.BURST)
					            		.withColor(Color.AQUA)
					            		.withFade(Color.AQUA)
					            		.build());
							} catch (IllegalArgumentException e) {
								e.printStackTrace();
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
			    		
			    	}, ticksPerFirework*i);
			    	Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, new Runnable() {

						@Override
						public void run() {
							Iterator<Entity> nearby = p.getNearbyEntities(16, 2, 16).iterator();
							while(nearby.hasNext()) {
								Entity e = nearby.next();
								if(!(e instanceof LivingEntity)) {
									continue;
								}
								if(!Skill.damageCheck(p, (LivingEntity) e)) {
									continue;
								}
								Skill.damageEntity((LivingEntity)e, p, 50, DamageCause.MAGIC);
								p.getWorld().strikeLightningEffect(e.getLocation());
							}
						}
			    		
			    	}, 100);
			    }
			}
			@Override
			public void removeFromHero(Hero hero) {
			    broadcast(hero.getPlayer().getLocation(), "§7[§2Skill§7] Arcane Storm Unleashed!", new Object[] {});
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
