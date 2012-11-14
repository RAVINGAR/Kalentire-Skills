package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Setting;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class SkillExchange extends ActiveSkill
{
  private static final String base = "base-coin-per-ingot";
  private static final String loss = "coin-loss-per-level";

  public SkillExchange(Heroes plugin)
  {
    super(plugin, "Exchange");
    setDescription("You can turn $1  into a gold ingot.  You can buy up to a stack at a time");
    setUsage("/skill exchange [amount]");
    setIdentifiers(new String[] { "skill exchange" });
    setArgumentRange(0, 1);
    setTypes(new SkillType[] { SkillType.HEAL, SkillType.LIGHT, SkillType.MANA, SkillType.SILENCABLE, SkillType.ITEM });
  }

  private static String boldGold(String string) {
    StringBuffer sb = new StringBuffer().append(ChatColor.BOLD).append(ChatColor.GOLD).append(string).append(ChatColor.RESET).append(ChatColor.GRAY);
    return sb.toString();
  }

  private double calculateCoins(Hero hero) {
    return SkillConfigManager.getUseSetting(hero, this, "base-coin-per-ingot", 16, false) - 
      SkillConfigManager.getUseSetting(hero, this, "coin-loss-per-level", 0.0500000007450581D, false) * hero.getLevel(hero.getSecondClass());
  }

  public String getDescription(Hero hero)
  {
    return getDescription().replace("$1", Heroes.econ.format(calculateCoins(hero)));
  }

  public SkillResult use(Hero hero, String[] args)
  {
    Player player = hero.getPlayer();
    int amount;
    if (args.length > 0)
      try {
        amount = Integer.parseInt(args[0]);
        if ((amount < 1) || (amount > 64)) throw new NumberFormatException();
      } catch (NumberFormatException ex) {
        player.sendMessage(ChatColor.GRAY + "If you provide an argument, it must be a postive integer less than 65");
        return SkillResult.FAIL;
      }
    else {
      amount = 1;
    }
    double cost = calculateCoins(hero) * amount;
    String cost_string = Heroes.econ.format(cost);
    if ((Heroes.econ.has(player.getName(), cost)) && (Heroes.econ.withdrawPlayer(player.getName(), cost).transactionSuccess())) {
      player.sendMessage(ChatColor.GRAY + "You bought " + boldGold(new StringBuilder(String.valueOf(amount)).append(" ingots").toString()) + " for " + boldGold(cost_string) + "!");
      player.getWorld().dropItem(player.getLocation(), new ItemStack(Material.GOLD_INGOT, amount));
      broadcastExecuteText(hero);
      return SkillResult.NORMAL;
    }
    player.sendMessage(ChatColor.GRAY + "You do not have the necessary " + boldGold(cost_string) + " to buy " + boldGold(new StringBuilder(String.valueOf(amount)).append(" ingots").toString()) + "!");
    return SkillResult.FAIL;
  }

  public final ConfigurationSection getDefaultConfig()
  {
    ConfigurationSection config = super.getDefaultConfig();
    config.set(Setting.MANA.node(), Integer.valueOf(10));
    config.set(Setting.NO_COMBAT_USE.node(), Boolean.valueOf(true));
    config.set("base-coin-per-ingot", Integer.valueOf(16));
    config.set("coin-loss-per-level", Float.valueOf(0.05F));
    return config;
  }
}