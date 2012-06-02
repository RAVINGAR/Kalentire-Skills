package com.herocraftonline.heroes.characters.skill.skills;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

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

	public SkillFade(Heroes plugin) {
		super(plugin, "Fade");
		setDescription("You fade into the shadows, hiding you from view");
		setUsage("/skill fade");
		setArgumentRange(0, 0);
		setIdentifiers("skill fade");
		setNotes("Note: Taking damage, moving, or causing damage removes the effect");
		setTypes(SkillType.ILLUSION, SkillType.BUFF, SkillType.COUNTER, SkillType.STEALTHY);
		
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
		hero.addEffect(new InvisibleEffect(this, duration, applyText, expireText));
		
		moveChecker.addHero(hero);
		return SkillResult.NORMAL;
	}
	
	public class FadeMoveChecker implements Runnable{
		
		private Map<Hero, Location> oldLocations = new HashMap<Hero, Location>();
        private Skill skill;

        FadeMoveChecker(Skill skill) {
            this.skill = skill;
        }

        @Override
        public void run() {	
        	Iterator<Entry<Hero, Location>> heroes = oldLocations.entrySet().iterator();
            for(Entry<Hero, Location> entry = null; heroes.hasNext(); entry = heroes.next()) {
            	Hero hero = entry.getKey();
            	Location oldLoc = entry.getValue();
                if(!hero.hasEffect("Invisible")) {
                    heroes.remove();
                    continue;
                }
                Location newLoc = hero.getPlayer().getLocation();
                if(newLoc.distance(oldLoc) > 1) {
                    hero.removeEffect(hero.getEffect("Invisible"));
                    heroes.remove();
                    continue;
                }
                
                if(newLoc.getBlock().getLightLevel() > SkillConfigManager.getUseSetting(hero, skill, "max-light-level", 8, false)) {
                	hero.removeEffect(hero.getEffect("Invisible"));
                    heroes.remove();
                    continue;
                }
                double detectRange = SkillConfigManager.getUseSetting(hero, skill, "detection-range", 1D, false);
                List<Entity> nearEntities = hero.getPlayer().getNearbyEntities(detectRange, detectRange, detectRange);
                for(Entity entity : nearEntities) {
                    if(entity instanceof Player) {
                        hero.removeEffect(hero.getEffect("Invisible"));
                        heroes.remove();
                        break;
                    }
                }
            }
        }

        public void addHero(Hero hero) {
            if(!hero.hasEffect("Invisible"))
                return;
            oldLocations.put(hero, hero.getPlayer().getLocation());
        }
	}

	@Override
	public String getDescription(Hero hero) {
		return getDescription();
	}

}
