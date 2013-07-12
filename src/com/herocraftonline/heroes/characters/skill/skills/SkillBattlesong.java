package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.VisualEffect;

public class SkillBattlesong extends ActiveSkill {
    public VisualEffect fplayer = new VisualEffect();
	public SkillBattlesong(Heroes plugin) {
		super(plugin, "Battlesong");
		setDescription("Party members within $1 blocks regain $2 stamina.");
		setUsage("/skill battlesong");
		setArgumentRange(0, 0);
		setIdentifiers("skill battlesong");
        setTypes(SkillType.BUFF, SkillType.EARTH, SkillType.SILENCABLE);
	}
	
	@Override
	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection conf = super.getDefaultConfig();
		conf.set(SkillSetting.COOLDOWN.node(), 10000);
		conf.set(SkillSetting.RADIUS.node(), 15);
		conf.set(SkillSetting.MANA.node(), 20);
		conf.set(SkillSetting.USE_TEXT.node(), ChatColor.GRAY.toString() + ChatColor.GRAY + "["+ChatColor.DARK_GREEN+"Skill"+ ChatColor.GRAY+ "] %hero% screams a battle cry!");
		conf.set("stamina-restored", 5);
		return conf;
	}
	
	@Override
	public String getDescription(Hero hero) {
		Integer range = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 15, false);
		Integer stamina = SkillConfigManager.getUseSetting(hero, this, "stamina-restored", 5, true);
		return getDescription().replace("$1", range.toString()).replace("$2", stamina.toString());
	}
	
	@Override
	public SkillResult use(Hero hero, String[] args) {
		int foodRestore = SkillConfigManager.getUseSetting(hero, this, "stamina-restored", 5, true);
		Player player = hero.getPlayer();
		if(hero.hasParty()) {
			int rangeSquared = (int) Math.pow(SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 15, false), 2);
			
			for(Hero member : hero.getParty().getMembers()) {
				Player mPlayer = member.getPlayer();
				
				if(!mPlayer.getWorld().equals(player.getWorld())) continue;
				if(mPlayer.getLocation().distanceSquared(player.getLocation()) > rangeSquared) continue;
				
				mPlayer.setFoodLevel(Math.min(player.getFoodLevel() + foodRestore, 20));
			}
		}
		else {
			player.setFoodLevel(Math.min(player.getFoodLevel() + foodRestore, 20));
		}
        // this is our fireworks shit
        try {
            fplayer.playFirework(player.getWorld(), 
            		player.getLocation().add(0,2,0), 
            		FireworkEffect.builder()
            		.flicker(false).trail(false)
            		.with(FireworkEffect.Type.STAR)
            		.withColor(Color.WHITE)
            		.withFade(Color.YELLOW)
            		.build());
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
		broadcastExecuteText(hero);
		return SkillResult.NORMAL;
	}


}
