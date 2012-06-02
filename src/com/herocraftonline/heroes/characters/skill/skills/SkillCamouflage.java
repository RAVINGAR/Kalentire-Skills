package com.herocraftonline.heroes.characters.skill.skills;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.InvisibleEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Setting;

public class SkillCamouflage extends ActiveSkill {
	
	private String applyText;
	private String expireText;
	private String failText;
	private CamoMoveChecker moveChecker;
	private Collection<Material> allowedMaterials = new java.util.ArrayList<Material>();

	public SkillCamouflage(Heroes plugin) {
		super(plugin, "Camouflage");
		setDescription("You attempt to hide in the surrounding terrain");
		setUsage("/skill camouflage");
		setArgumentRange(0, 0);
		setIdentifiers("skill camouflage", "skill camo");
		setNotes("Note: Taking damage, moving, or causing damage removes the effect");
		setTypes(SkillType.ILLUSION, SkillType.BUFF, SkillType.COUNTER, SkillType.STEALTHY);
		
		Material[] allowedMaterialList = new Material[]{
				Material.DIRT, Material.GRASS, Material.GRAVEL,
				Material.LOG, Material.LEAVES, Material.MYCEL,
				Material.MELON_BLOCK, Material.PUMPKIN, Material.SAND,
				Material.SANDSTONE, Material.SNOW, Material.SNOW_BLOCK};
		for(int i = 0; i < allowedMaterialList.length; i++)
			this.allowedMaterials.add(allowedMaterialList[i]);
		
		Bukkit.getServer().getPluginManager().registerEvents(new SkillEventListener(), plugin);
		moveChecker = new CamoMoveChecker(this);
		Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, moveChecker, 1, 1);
	}
	
	@Override
	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection node = super.getDefaultConfig();
		node.set(Setting.DURATION.node(), 30000);
		node.set(Setting.APPLY_TEXT.node(), "You blend into the terrain");
		node.set(Setting.EXPIRE_TEXT.node(), "You come back into view");
		node.set("fail-text", "The surrounding terrain isn't natural enough");
		node.set("detection-range", 1D);
		return node;
	}

	@Override
	public void init() {
		super.init();
		applyText = SkillConfigManager.getRaw(this, Setting.APPLY_TEXT, "You blend into the terrain");
		expireText = SkillConfigManager.getRaw(this, Setting.EXPIRE_TEXT, "You come back into view");
		failText = SkillConfigManager.getRaw(this, "fail-text", "The surrounding terrain isn't natural enough");
	}
	
	

	@Override
	public SkillResult use(Hero hero, String[] args) {
		Player player = hero.getPlayer();
		Location loc = player.getLocation();
		
		Location blockCheckLoc = loc.clone();
		blockCheckLoc.subtract(1, 0, 1);
		boolean inSnow = true; {
			for(int x = 0; x < 3; x++) {
				for(int z = 0; z < 3; z++) {
					if(!blockCheckLoc.getBlock().getType().equals(Material.SNOW)) {
						inSnow = false;
					}
					blockCheckLoc.add(0, 0, 1);
				}
				blockCheckLoc.add(1, 0, -3);
			}
		}
		
		blockCheckLoc = loc.clone();
		blockCheckLoc.subtract(1, 1, 1);
		boolean inNature = true;
		for(int x = 0; x < 3; x++) {
			for(int z = 0; z < 3; z++) {
				if(!allowedMaterials.contains(blockCheckLoc.getBlock().getType())) {
					inNature = false;
				}
				blockCheckLoc.add(0, 0, 1);
			}
			blockCheckLoc.add(1, 0, -3);
		}
		
		if(!inSnow && !inNature) {
			Messaging.send(player, failText);
			return SkillResult.FAIL;
		}
		
		long duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 30000, false);
		player.getWorld().playEffect(player.getLocation(), org.bukkit.Effect.SMOKE, 4);
		hero.addEffect(new CamoEffect(this, duration, applyText, expireText));
		moveChecker.addHero(hero);
		return SkillResult.NORMAL;
	}
	
	public class CamoEffect extends InvisibleEffect {
		
		private final String applyText, expireText;
	    
	    public CamoEffect(Skill skill, long duration, String applyText, String expireText) {
	        super(skill, duration, "", "");
	        this.applyText = applyText;
	        this.expireText = expireText;
	    }

	    @Override
	    public void applyToHero(Hero hero) {
	        super.applyToHero(hero);
	        Player player = hero.getPlayer();

	        Messaging.send(player, applyText);
	    }

	    @Override
	    public void removeFromHero(Hero hero) {
	        super.removeFromHero(hero);
	        Player player = hero.getPlayer();

	        Messaging.send(player, expireText);
	    }
		
	}
	
	public class CamoMoveChecker implements Runnable{
		
		private List<Hero> heroes = new java.util.ArrayList<Hero>();
		private Map<Hero, Location> oldLocations = new java.util.HashMap<Hero, Location>();
		private Skill skill;
		
		CamoMoveChecker(Skill skill) {
			this.skill = skill;
		}

		@Override
		public void run() {	
			if(heroes.size() == 0)
				return;
			List<Hero> toCheck = new java.util.ArrayList<Hero>();
			toCheck.addAll(heroes);
			
			for(Hero hero : toCheck) {
				if(!hero.hasEffect("Invisible")) {
					heroes.remove(hero);
					oldLocations.remove(hero);
					continue;
				}
				Location newLoc = hero.getPlayer().getLocation();
				Location oldLoc = oldLocations.get(hero);
				
				if(newLoc.distance(oldLoc) > 1) {
					hero.removeEffect(hero.getEffect("Invisible"));
					heroes.remove(hero);
					oldLocations.remove(hero);
					continue;
				}
				double detectRange = SkillConfigManager.getUseSetting(hero, skill, "detection-range", 1D, false);
				List<Entity> nearEntities = hero.getPlayer().getNearbyEntities(detectRange, detectRange, detectRange);
				for(Entity entity : nearEntities) {
					if(entity instanceof Player) {
						hero.removeEffect(hero.getEffect("Invisible"));
						heroes.remove(hero);
						oldLocations.remove(hero);
						break;
					}
				}
			}
		}
		
		public void addHero(Hero hero) {
			if(!hero.hasEffect("Invisible"))
				return;
			heroes.add(hero);
			oldLocations.put(hero, hero.getPlayer().getLocation());
		}
	}
	
	public class SkillEventListener implements Listener {
		@EventHandler
		public void onDamage(EntityDamageEvent event) {
			if (event.isCancelled() || event.getDamage() == 0) {
                return;
            }
			Player player = null;
			if (event.getEntity() instanceof Player) {
                player = (Player) event.getEntity();
                Hero hero = plugin.getCharacterManager().getHero(player);
                if (hero.hasEffect("Invisible")) {
                    hero.removeEffect(hero.getEffect("Invisible"));
                }
            } 
            player = null;
            if (event instanceof EntityDamageByEntityEvent) {
                EntityDamageByEntityEvent subEvent = (EntityDamageByEntityEvent) event;
                if (subEvent.getDamager() instanceof Player) {
                    player = (Player) subEvent.getDamager();
                } else if (subEvent.getDamager() instanceof Projectile) {
                    if (((Projectile) subEvent.getDamager()).getShooter() instanceof Player) {
                        player = (Player) ((Projectile) subEvent.getDamager()).getShooter();
                    }
                }
                if (player != null) {
                    Hero hero = plugin.getCharacterManager().getHero(player);
                    if (hero.hasEffect("Invisible")) {
                        hero.removeEffect(hero.getEffect("Invisible"));
                    }
                }
            }
		}
	}

	@Override
	public String getDescription(Hero hero) {
		return getDescription();
	}

}
