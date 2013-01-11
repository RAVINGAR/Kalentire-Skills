package com.herocraftonline.heroes.characters.skill.skills;

import java.util.Random;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.util.Setting;

public class SkillFireworks extends ActiveSkill {

	public SkillFireworks(Heroes plugin) {
		super(plugin, "Fireworks");
		setDescription("Fires $1 random fireworks into the sky!");
		setUsage("/skill fireworks");
		setIdentifiers("skill fireworks");
		setArgumentRange(0,0);
	}

	@Override
	public SkillResult use(Hero h, String[] args) {
		Random randGen = new Random();
		Location loc = h.getPlayer().getLocation();
		int n = SkillConfigManager.getUseSetting(h, this, "numberFireworks", 5, false); 
		for(int i = 0;i<n;i++) {
			//If you only want a single firework just copy paste whats inside the for loop
			Firework firework = (Firework) h.getPlayer().getWorld().spawnEntity(loc, EntityType.FIREWORK);
			FireworkMeta meta = firework.getFireworkMeta();
			int rand1 =  randGen.nextInt(5);
			Type typeSelect = Type.BALL;
			switch (rand1) {
			case 0: typeSelect = Type.BALL; break;
			case 1: typeSelect = Type.BALL_LARGE; break;
			case 2: typeSelect = Type.BURST; break;
			case 3: typeSelect = Type.CREEPER; break;
			case 4: typeSelect = Type.STAR; break;
			}
			int rand2 = randGen.nextInt(17);
			Color colorSelect = Color.WHITE;
			switch (rand2) {
			case 0: colorSelect = Color.AQUA; break;
			case 1: colorSelect = Color.BLACK; break;
			case 2: colorSelect = Color.BLUE; break;
			case 3: colorSelect = Color.FUCHSIA; break;
			case 4: colorSelect = Color.GRAY; break;
			case 5: colorSelect = Color.GREEN; break;
			case 6: colorSelect = Color.LIME; break;
			case 7: colorSelect = Color.MAROON; break;
			case 8: colorSelect = Color.NAVY; break;
			case 9: colorSelect = Color.OLIVE; break;
			case 10: colorSelect = Color.ORANGE; break;
			case 11: colorSelect = Color.PURPLE; break;
			case 12: colorSelect = Color.RED; break;
			case 13: colorSelect = Color.SILVER; break;
			case 14: colorSelect = Color.TEAL; break;
			case 15: colorSelect = Color.WHITE; break;
			case 16: colorSelect = Color.YELLOW; break;
			}
			int rand3 = randGen.nextInt(17);
			Color fadeSelect = Color.WHITE;
			switch (rand3) {
			case 0: fadeSelect = Color.AQUA; break;
			case 1: fadeSelect = Color.BLACK; break;
			case 2: fadeSelect = Color.BLUE; break;
			case 3: fadeSelect = Color.FUCHSIA; break;
			case 4: fadeSelect = Color.GRAY; break;
			case 5: fadeSelect = Color.GREEN; break;
			case 6: fadeSelect = Color.LIME; break;
			case 7: fadeSelect = Color.MAROON; break;
			case 8: fadeSelect = Color.NAVY; break;
			case 9: fadeSelect = Color.OLIVE; break;
			case 10: fadeSelect = Color.ORANGE; break;
			case 11: fadeSelect = Color.PURPLE; break;
			case 12: fadeSelect = Color.RED; break;
			case 13: fadeSelect = Color.SILVER; break;
			case 14: fadeSelect = Color.TEAL; break;
			case 15: fadeSelect = Color.WHITE; break;
			case 16: fadeSelect = Color.YELLOW; break;
			}
			meta.addEffect(FireworkEffect.builder().flicker(false).trail(true).with(typeSelect).withColor(colorSelect).withFade(fadeSelect).build());
			meta.setPower(randGen.nextInt(30));
			firework.setFireworkMeta(meta);
		}
		return SkillResult.NORMAL;
	}

	@Override
	public String getDescription(Hero h) {
		int numberFireworks = SkillConfigManager.getUseSetting(h, this, "numberFireworks", 5, false);
		return getDescription().replace("$1", numberFireworks + "");
	}
	
	@Override
	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection node = super.getDefaultConfig();
		node.set("numberFireworks", Integer.valueOf(5));
		node.set(Setting.COOLDOWN.node(), Integer.valueOf(180000));
		return node;
	}

}
