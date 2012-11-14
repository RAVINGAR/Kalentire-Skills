package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.util.Setting;

import static org.bukkit.ChatColor.*;
import static com.herocraftonline.heroes.Heroes.econ;
import static com.herocraftonline.heroes.characters.skill.SkillConfigManager.getUseSetting;
import static com.herocraftonline.heroes.characters.skill.SkillType.*;

public class SkillMonetize extends ActiveSkill{
	private static final String base="base-coin-per-ingot",gain="coin-gain-per-level";
	private static final int def_base = 5;
	private static final float def_gain=0.1f;
	
	public SkillMonetize(Heroes plugin) {
		super(plugin, "Monetize");
		setDescription("You turn all the gold ingots in your inventory into $1 "+econ.currencyNamePlural());
		setUsage("/skill Monetize");
		setArgumentRange(0,0);
		setIdentifiers("skill Monetize");
		setTypes(KNOWLEDGE,PHYSICAL,ITEM,UNBINDABLE);
	}

	@Override
	public SkillResult use(Hero hero, String[] args) {
		final Player player = hero.getPlayer();
		final PlayerInventory inv = player.getInventory();
		
		int count=0;
		for(ItemStack stack:inv.getContents()){
			if(stack==null)continue;
			if(stack.getTypeId()==266){
				count+=stack.getAmount();
			}
		}
		if(count>0){
			inv.remove(266);
			final double amount =calculateCoins(hero)*count;
			econ.depositPlayer(player.getName(),amount);
			broadcastExecuteText(hero);
			player.sendMessage(GRAY+"You have turned "+boldGold(count+" ingot"+((count>1)?"s":""))+" into "+boldGold(econ.format(amount))+"!");
			return SkillResult.NORMAL;
		}else{
			player.sendMessage(GRAY+"You do not have any gold ingots in your Inventory!");
			return SkillResult.FAIL;
		}
	}
	
	private static String boldGold(String string){
		return BOLD+""+GOLD+string+RESET+GRAY;
	}

	@Override
	public String getDescription(Hero hero) {
		return getDescription().replace("$1", calculateCoins(hero).toString());
	}
	
	/**
	 * This may cause issues if the player doesn't have a second class
	 */
	private Double calculateCoins(Hero hero){
		return getUseSetting(hero, this, base, def_base, false)
					+getUseSetting(hero,this,gain,def_gain,false)*hero.getLevel(hero.getSecondClass());
	}
	
	@Override
	public final ConfigurationSection getDefaultConfig(){
		ConfigurationSection config = super.getDefaultConfig();
		config.set(Setting.MANA.node(), 10);
		config.set(Setting.NO_COMBAT_USE.node(), true);
		config.set(base, 5);
		config.set(gain, 0.1);//max possible price per ingot is 11c at level 60, using defaults
		return config;
	}
}
