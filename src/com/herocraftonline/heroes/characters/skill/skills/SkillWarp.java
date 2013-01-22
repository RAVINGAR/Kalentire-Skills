package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Setting;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class SkillWarp extends ActiveSkill
{
  public SkillWarp(Heroes plugin)
  {
    super(plugin, "Warp");
    setDescription("Teleports you to $1");
    setUsage("/skill warp");
    setArgumentRange(0, 0);
    setIdentifiers(new String[] { "skill warp" });

    setTypes(new SkillType[] { SkillType.TELEPORT, SkillType.SILENCABLE });
  }

  public String getDescription(Hero hero)
  {
    String description1 = SkillConfigManager.getUseSetting(hero, this, "description", "a set location");
    String description = getDescription().replace("$1", description1 + "");

    int cooldown = (SkillConfigManager.getUseSetting(hero, this, Setting.COOLDOWN.node(), 0, false) - SkillConfigManager.getUseSetting(hero, this, Setting.COOLDOWN_REDUCE.node(), 0, false) * hero.getSkillLevel(this)) / 1000;

    if (cooldown > 0) {
      description = description + " CD:" + cooldown + "s";
    }

    int mana = SkillConfigManager.getUseSetting(hero, this, Setting.MANA.node(), 10, false) - SkillConfigManager.getUseSetting(hero, this, Setting.MANA_REDUCE.node(), 0, false) * hero.getSkillLevel(this);

    if (mana > 0) {
      description = description + " M:" + mana;
    }

    int healthCost = SkillConfigManager.getUseSetting(hero, this, Setting.HEALTH_COST, 0, false) - SkillConfigManager.getUseSetting(hero, this, Setting.HEALTH_COST_REDUCE, mana, true) * hero.getSkillLevel(this);

    if (healthCost > 0) {
      description = description + " HP:" + healthCost;
    }

    int staminaCost = SkillConfigManager.getUseSetting(hero, this, Setting.STAMINA.node(), 0, false) - SkillConfigManager.getUseSetting(hero, this, Setting.STAMINA_REDUCE.node(), 0, false) * hero.getSkillLevel(this);

    if (staminaCost > 0) {
      description = description + " FP:" + staminaCost;
    }

    int delay = SkillConfigManager.getUseSetting(hero, this, Setting.DELAY.node(), 0, false) / 1000;
    if (delay > 0) {
      description = description + " W:" + delay + "s";
    }

    int exp = SkillConfigManager.getUseSetting(hero, this, Setting.EXP.node(), 0, false);
    if (exp > 0) {
      description = description + " XP:" + exp;
    }
    return description;
  }

  public ConfigurationSection getDefaultConfig()
  {
    ConfigurationSection node = super.getDefaultConfig();
    node.set("destination", "world,0,64,0");
    node.set("description", "a set location");
    return node;
  }

  public SkillResult use(Hero hero, String[] args)
  {
    Player player = hero.getPlayer();

    String destinationString = SkillConfigManager.getUseSetting(hero, this, "destination", "world,0,64,0");
    String[] dArgs = destinationString.split(",");
    Location destination = null;
    try {
      destination = new Location(Bukkit.getWorld(dArgs[0]), Double.parseDouble(dArgs[1]), Double.parseDouble(dArgs[2]), Double.parseDouble(dArgs[3]));
      player.teleport(destination);
    } catch (Exception e) {
      player.sendMessage(ChatColor.GRAY + "SkillWarp has an invalid config.");
      return SkillResult.INVALID_TARGET_NO_MSG;
    }
    hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.WITHER_DEATH , 10.0F, 1.0F); 
    broadcastExecuteText(hero);

    return SkillResult.NORMAL;
  }
}