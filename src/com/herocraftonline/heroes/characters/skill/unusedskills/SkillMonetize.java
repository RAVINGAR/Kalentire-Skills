package com.herocraftonline.heroes.characters.skill.unusedskills;

import static com.herocraftonline.heroes.Heroes.econ;
import static com.herocraftonline.heroes.characters.skill.SkillConfigManager.getUseSetting;
import static org.bukkit.ChatColor.BOLD;
import static org.bukkit.ChatColor.GOLD;
import static org.bukkit.ChatColor.GRAY;
import static org.bukkit.ChatColor.RESET;

import org.bukkit.Effect;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;

public class SkillMonetize extends ActiveSkill{
	private static final String base="base-coin-per-ingot",gain="coin-gain-per-level";
	private static final int def_base = 5;
	private static final float def_gain=0.1f;
	
	public SkillMonetize(Heroes plugin) {
		super(plugin, "Monetize");
		setDescription("You turn all the gold ingots in your inventory into $1 coins");
		setUsage("/skill Monetize");
		setArgumentRange(0,0);
		setIdentifiers("skill Monetize");
		setTypes(SkillType.KNOWLEDGE, SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.ITEM_MODIFYING, SkillType.UNBINDABLE);
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
	        player.getWorld().playEffect(player.getLocation(), Effect.ENDER_SIGNAL, 3);
	        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP , 0.8F, 1.0F);
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
	
	// This may cause issues if the player doesn't have a second class
	private Double calculateCoins(Hero hero){
		return getUseSetting(hero, this, base, def_base, false)
					+getUseSetting(hero,this,gain,def_gain,false)*hero.getLevel(hero.getSecondClass());
	}
	
	@Override
	public final ConfigurationSection getDefaultConfig(){
		ConfigurationSection config = super.getDefaultConfig();
		config.set(SkillSetting.MANA.node(), 10);
		config.set(SkillSetting.NO_COMBAT_USE.node(), true);
		config.set(base, 5);
		config.set(gain, 0.1);//max possible price per ingot is 11c at level 60, using defaults
		return config;
	}
}
