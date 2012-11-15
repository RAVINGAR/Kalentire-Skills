package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.util.Setting;

import static com.herocraftonline.heroes.characters.skill.SkillConfigManager.getUseSetting;
import static com.herocraftonline.heroes.characters.skill.SkillType.*;

public class SkillSmeltIron extends ActiveSkill{
	private static final String base="base-ingot-chance",gain="chance-gain-per-level";
	
	public SkillSmeltIron(Heroes plugin) {
		super(plugin, "SmeltIron");
		setDescription("You can turn iron ore into an iron ingot with a $1 percent chance of getting an extra ingot");
		setUsage("/skill smeltiron");
		setIdentifiers("skill smeltiron");
		setArgumentRange(0, 0);
		setTypes(KNOWLEDGE,PHYSICAL,ITEM,UNBINDABLE);
	}
	
	private double calculateChance(Hero hero){
		return getUseSetting(hero, this, base, 10, false)
					+getUseSetting(hero,this,gain,0.2,false)*hero.getLevel(hero.getSecondClass());
	}
	
	@Override
	public String getDescription(Hero hero) {
		return getDescription().replace("$1", calculateChance(hero)+"");
	}

	@Override
	public SkillResult use(Hero hero, String[] args) {
		final Player player = hero.getPlayer();
		boolean present=false;
		ItemStack[] contents = player.getInventory().getContents();
		ItemStack stack;
		for(int i=0;i<contents.length;i++){
			stack=contents[i];
			if(stack!=null&&stack.getType()==Material.IRON_ORE){
				final int cur_amount = stack.getAmount();
				if(cur_amount==1){
					player.getInventory().setItem(i, null);
				}else{
					stack.setAmount(cur_amount-1);
				}
				present=true;
				break;
			}
		}
		if(present){
			int amount=1;
			broadcastExecuteText(hero);
			if(calculateChance(hero)>(Math.random()*100)){
				amount++;
				player.sendMessage(ChatColor.GRAY+"You got an extra ingot from the smelting process!");
			}
			player.getWorld().dropItem(player.getLocation(), new ItemStack(Material.IRON_INGOT,amount));
			return SkillResult.NORMAL;
		}else{
			player.sendMessage(ChatColor.GRAY+"You do not have any iron ore to smelt!");
			return SkillResult.FAIL;
		}
	}
	
	@Override
	public final ConfigurationSection getDefaultConfig(){
		ConfigurationSection config = super.getDefaultConfig();
		config.set(Setting.NO_COMBAT_USE.node(), true);
		config.set(base, 10);
		config.set(gain,  0.2f);//max possible price per ingot is 11c at level 60, using defaults
		return config;
	}
}
