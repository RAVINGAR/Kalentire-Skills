package com.herocraftonline.heroes.characters.skill.skills;
//http://pastie.org/private/i04dtc6t4oannstqls6sq

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.RootEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.VisualEffect;
import org.bukkit.*;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.util.ArrayList;
import java.util.List;

public class SkillArcaneStorm extends ActiveSkill  {

	public SkillArcaneStorm(Heroes plugin) {
		super(plugin, "ArcaneStorm");
		setIdentifiers("skill ArcaneStorm");
		setUsage("/skill ArcaneStorm");
		setArgumentRange(0,0);
        setTypes(SkillType.DAMAGING, SkillType.ABILITY_PROPERTY_LIGHT, SkillType.SILENCEABLE, SkillType.AGGRESSIVE);
		setDescription("On use, user is rooted into place for 5 seconds. " +
				"After the 5 seconds, the user unleashes a hail of devastating magical artillery in the surrounding area");
	}

	@Override
	public SkillResult use(final Hero hero, String[] arg1) {
		hero.addEffect(new RootEffect(this, hero.getPlayer(), 1000, 2000L) {
			@Override
			public void applyToHero(Hero hero) {
				super.applyToHero(hero);
			    final Player player = hero.getPlayer();
			    broadcast(hero.getEntity().getLocation(), ChatColor.GRAY + "["+ChatColor.DARK_GREEN+"Skill"+ ChatColor.GRAY+ "] $1 has begun channeling an arcane storm!",
						hero.getPlayer().getName());
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
							} catch (Exception e) {
								e.printStackTrace();
							}
						}

			    	}, ticksPerFirework*i);	    	
			    }
			    Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, new Runnable() {

					@Override
					public void run() {
						for (Entity entity : player.getNearbyEntities(16, 5, 16)) {
							if (!(entity instanceof LivingEntity)) {
								continue;
							}
							if (!Skill.damageCheck(player, (LivingEntity) entity)) {
								continue;
							}
							Skill.damageEntity((LivingEntity) entity, player, 200D, DamageCause.MAGIC);
							player.getWorld().strikeLightningEffect(entity.getLocation());
						}
					}

		    	}, 100);
			}
			@Override
			public void removeFromHero(Hero hero) {
				super.removeFromHero(hero);
			    broadcast(hero.getPlayer().getLocation(), ChatColor.GRAY + "["+ChatColor.DARK_GREEN+"Skill"+ ChatColor.GRAY+ "] Arcane Storm Unleashed!");
			}

		});
		return SkillResult.NORMAL;
	}
	protected List<Location> circle(Player player, Location loc, Integer r, Integer h, boolean hollow, boolean sphere, int plus_y) {
		List<Location> circleBlocks = new ArrayList<Location>();
        int cx = loc.getBlockX();
        int cy = loc.getBlockY();
        int cz = loc.getBlockZ();
        for (int x = cx - r; x <= cx +r; x++)
            for (int z = cz - r; z <= cz +r; z++)
                for (int y = (sphere ? cy - r : cy); y < (sphere ? cy + r : cy + h); y++) {
                    double dist = (cx - x) * (cx - x) + (cz - z) * (cz - z) + (sphere ? (cy - y) * (cy - y) : 0);
                    if (dist < r*r && !(hollow && dist < (r-1)*(r-1))) {
                        Location l = new Location(loc.getWorld(), x, y + plus_y, z);
                        circleBlocks.add(l);
                        }
                    }
     
        return circleBlocks;
    }
	@Override
	public String getDescription(Hero arg0) {
		return getDescription();
	}


}
