package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Setting;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class SkillMonetize extends ActiveSkill
{
  private static final String base = "base-coin-per-ingot";
  private static final String gain = "coin-gain-per-level";
  private static final int def_base = 5;
  private static final float def_gain = 0.1F;

  public SkillMonetize(Heroes plugin)
  {
    super(plugin, "Monetize");
    setDescription("You turn all the gold ingots in your inventory into $1 " + Heroes.econ.currencyNamePlural());
    setUsage("/skill Monetize");
    setArgumentRange(0, 0);
    setIdentifiers(new String[] { "skill Monetize" });
    setTypes(new SkillType[] { SkillType.KNOWLEDGE, SkillType.PHYSICAL, SkillType.ITEM, SkillType.UNBINDABLE });
  }

  public SkillResult use(Hero hero, String[] args)
  {
    Player player = hero.getPlayer();
    PlayerInventory inv = player.getInventory();

    int count = 0;
    for (ItemStack stack : inv.getContents()) {
      if ((stack != null) && 
        (stack.getTypeId() == 266)) {
        count += stack.getAmount();
      }
    }
    if (count > 0) {
      inv.remove(266);
      double amount = calculateCoins(hero).doubleValue() * count;
      Heroes.econ.depositPlayer(player.getName(), amount);
      broadcastExecuteText(hero);
      player.sendMessage(ChatColor.GRAY + "You have turned " + boldGold(new StringBuilder(String.valueOf(count)).append(" ingot").append(count > 1 ? "s" : "").toString()) + " into " + boldGold(Heroes.econ.format(amount)) + "!");
      return SkillResult.NORMAL;
    }
    player.sendMessage(ChatColor.GRAY + "You do not have any gold ingots in your Inventory!");
    return SkillResult.FAIL;
  }

  private static String boldGold(String string)
  {
    return ChatColor.BOLD + ChatColor.GOLD + string + ChatColor.RESET + ChatColor.GRAY;
  }

  public String getDescription(Hero hero)
  {
    return getDescription().replace("$1", calculateCoins(hero).toString());
  }

  private Double calculateCoins(Hero hero)
  {
    return Double.valueOf(SkillConfigManager.getUseSetting(hero, this, "base-coin-per-ingot", 5, false) + 
      SkillConfigManager.getUseSetting(hero, this, "coin-gain-per-level", 0.1000000014901161D, false) * hero.getLevel(hero.getSecondClass()));
  }

  public final ConfigurationSection getDefaultConfig()
  {
    ConfigurationSection config = super.getDefaultConfig();
    config.set(Setting.MANA.node(), Integer.valueOf(10));
    config.set(Setting.NO_COMBAT_USE.node(), Boolean.valueOf(true));
    config.set("base-coin-per-ingot", Integer.valueOf(5));
    config.set("coin-gain-per-level", Double.valueOf(0.1D));
    return config;
  }
}