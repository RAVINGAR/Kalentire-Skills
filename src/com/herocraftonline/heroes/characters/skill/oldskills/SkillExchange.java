package com.herocraftonline.heroes.characters.skill.oldskills;

import static com.herocraftonline.heroes.Heroes.econ;
import static com.herocraftonline.heroes.characters.skill.SkillConfigManager.getUseSetting;
import static org.bukkit.ChatColor.BOLD;
import static org.bukkit.ChatColor.GOLD;
import static org.bukkit.ChatColor.GRAY;
import static org.bukkit.ChatColor.RESET;

import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;

public class SkillExchange extends ActiveSkill{
	private static final String base="base-coin-per-ingot",loss="coin-loss-per-level";
	
	public SkillExchange(Heroes plugin) {
		super(plugin, "Exchange");
		setDescription("You can turn $1 "+" into a gold ingot.  You can buy up to a stack at a time");
		setUsage("/skill exchange [amount]");
		setIdentifiers("skill exchange");
		setArgumentRange(0, 1);
        setTypes(SkillType.UNBINDABLE);
	}
	
	private static String boldGold(String string){
		return BOLD+""+GOLD+string+RESET+GRAY;
	}
	
	private double calculateCoins(Hero hero){
		return getUseSetting(hero, this, base, 16, false)
					-getUseSetting(hero,this,loss,0.05f,false)*hero.getLevel(hero.getSecondClass());
	}
	
	@Override
	public String getDescription(Hero hero) {
		return getDescription().replace("$1", econ.format(calculateCoins(hero)));
	}

	@Override
	public SkillResult use(Hero hero, String[] args) {
		final Player player = hero.getPlayer();
		final int amount;
		if(args.length>0){
			try{
				amount = Integer.parseInt(args[0]);
				if(amount<1||amount>64)throw new NumberFormatException();
			}catch(NumberFormatException ex){
				player.sendMessage(ChatColor.GRAY+"If you provide an argument, it must be a postive integer less than 65");
				return SkillResult.FAIL;
			}
		}else{
			amount=1;
		}
		final double cost = calculateCoins(hero)*((double)amount);
		final String cost_string = econ.format(cost);
		if(econ.has(player.getName(),cost)&&econ.withdrawPlayer(player.getName(), cost).transactionSuccess()){
			player.sendMessage(ChatColor.GRAY+"You bought "+boldGold(amount+" ingots")+" for "+boldGold(cost_string)+"!");
			player.getWorld().dropItem(player.getLocation(), new ItemStack(Material.GOLD_INGOT, amount));
	        player.getWorld().playEffect(player.getLocation(), Effect.ENDER_SIGNAL, 3);
	        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.ORB_PICKUP , 0.8F, 1.0F);
			broadcastExecuteText(hero);
			return SkillResult.NORMAL;
		}else{
			player.sendMessage(ChatColor.GRAY+"You do not have the necessary "+boldGold(cost_string)+" to buy "+boldGold(amount+" ingots")+"!");
			return SkillResult.FAIL;
		}
	}
	
	@Override
	public final ConfigurationSection getDefaultConfig(){
		ConfigurationSection config = super.getDefaultConfig();
		config.set(SkillSetting.MANA.node(), 10);
		config.set(SkillSetting.NO_COMBAT_USE.node(), true);
		config.set(base, 16);
		config.set(loss,  0.05f);//max possible price per ingot is 11c at level 60, using defaults
		return config;
	}
}
