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

public class SkillFade extends ActiveSkill {
	
	private String applyText;
	private String expireText;
	private String failText;
	private FadeMoveChecker moveChecker;
	private Collection<Material> allowedMaterials = new java.util.ArrayList<Material>();

	public SkillFade(Heroes plugin) {
		super(plugin, "Fade");
		setDescription("You fade into the shadows, hiding you from view");
		setUsage("/skill fade");
		setArgumentRange(0, 0);
		setIdentifiers("skill fade");
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
		moveChecker = new FadeMoveChecker(this);
		Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, moveChecker, 1, 1);
	}
	
	@Override
	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection node = super.getDefaultConfig();
		node.set(Setting.DURATION.node(), 30000);
		node.set(Setting.APPLY_TEXT.node(), "You fade into the shadows");
		node.set(Setting.EXPIRE_TEXT.node(), "You come back into view");
		node.set("fail-text", "It's too bright to fade");
		node.set("detection-range", 1D);
		node.set("max-light-level", 8);
		return node;
	}

	@Override
	public void init() {
		super.init();
		applyText = SkillConfigManager.getRaw(this, Setting.APPLY_TEXT, "You fade into the shadows");
		expireText = SkillConfigManager.getRaw(this, Setting.EXPIRE_TEXT, "You come back into view");
		failText = SkillConfigManager.getRaw(this, "fail-text", "It's too bright to fade");
	}
	
	

	@Override
	public SkillResult use(Hero hero, String[] args) {
		Player player = hero.getPlayer();
		Location loc = player.getLocation();
		if(loc.getBlock().getLightLevel() > SkillConfigManager.getUseSetting(hero, this, "max-light-level", 8, false)) {
			Messaging.send(player, failText);
			return SkillResult.FAIL;
		}
		long duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 30000, false);
		
		for(int i = 0; i < 3; i++)
			player.getWorld().playEffect(loc, org.bukkit.Effect.EXTINGUISH, 0, 10);
		player.getWorld().playEffect(player.getLocation(), org.bukkit.Effect.SMOKE, 4);
		hero.addEffect(new FadeEffect(this, duration, applyText, expireText));
		
		moveChecker.addHero(hero);
		return SkillResult.NORMAL;
	}
	
	public class FadeEffect extends InvisibleEffect {
		
		private final String applyText, expireText;
	    
	    public FadeEffect(Skill skill, long duration, String applyText, String expireText) {
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
	
	public class FadeMoveChecker implements Runnable{
		
		private List<Hero> heroes = new java.util.ArrayList<Hero>();
		private Map<Hero, Location> oldLocations = new java.util.HashMap<Hero, Location>();
		private Skill skill;
		
		FadeMoveChecker(Skill skill) {
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
				if(newLoc.getBlock().getLightLevel() > SkillConfigManager.getUseSetting(hero, skill, "max-light-level", 8, false)) {
					hero.removeEffect(hero.getEffect("Invisible"));
					heroes.remove(hero);
					oldLocations.remove(hero);
					continue;
				}
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
