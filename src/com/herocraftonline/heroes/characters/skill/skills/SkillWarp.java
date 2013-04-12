package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.onarandombox.MultiverseCore.MultiverseCore;

public class SkillWarp extends ActiveSkill
{
  private MultiverseCore mv;
  
  public SkillWarp(Heroes plugin)
  {
    super(plugin, "Warp");
    setDescription("Teleports you to $1");
    setUsage("/skill warp");
    setArgumentRange(0, 0);
    setIdentifiers(new String[] { "skill warp" });
    mv = (MultiverseCore) plugin.getServer().getPluginManager().getPlugin("Multiverse-Core");

    setTypes(new SkillType[] { SkillType.TELEPORT, SkillType.SILENCABLE });
  }

  public String getDescription(Hero hero)
  {
    String description1 = SkillConfigManager.getUseSetting(hero, this, "description", "a set location");
    String description = getDescription().replace("$1", description1 + "");

    int cooldown = (SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN.node(), 0, false) - SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN_REDUCE.node(), 0, false) * hero.getSkillLevel(this)) / 1000;

    if (cooldown > 0) {
      description = description + " CD:" + cooldown + "s";
    }

    int mana = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MANA.node(), 10, false) - SkillConfigManager.getUseSetting(hero, this, SkillSetting.MANA_REDUCE.node(), 0, false) * hero.getSkillLevel(this);

    if (mana > 0) {
      description = description + " M:" + mana;
    }

    int healthCost = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALTH_COST, 0, false) - SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALTH_COST_REDUCE, mana, true) * hero.getSkillLevel(this);

    if (healthCost > 0) {
      description = description + " HP:" + healthCost;
    }

    int staminaCost = SkillConfigManager.getUseSetting(hero, this, SkillSetting.STAMINA.node(), 0, false) - SkillConfigManager.getUseSetting(hero, this, SkillSetting.STAMINA_REDUCE.node(), 0, false) * hero.getSkillLevel(this);

    if (staminaCost > 0) {
      description = description + " FP:" + staminaCost;
    }

    int delay = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DELAY.node(), 0, false) / 1000;
    if (delay > 0) {
      description = description + " W:" + delay + "s";
    }

    int exp = SkillConfigManager.getUseSetting(hero, this, SkillSetting.EXP.node(), 0, false);
    if (exp > 0) {
      description = description + " XP:" + exp;
    }
    return description;
  }

  public ConfigurationSection getDefaultConfig()
  {
    ConfigurationSection node = super.getDefaultConfig();
    node.set("default-destination", "world");
    node.set("description", "a set location");
    node.set("destinations","world,world_nether,world_the_end");
    return node;
  }

  public SkillResult use(Hero hero, String[] args)
  {
    Player player = hero.getPlayer();

    String possibleDestinationsString = SkillConfigManager.getUseSetting(hero, this, "destinations", "world,world_nether,world_the_end");
    String defaultDestinationString = SkillConfigManager.getUseSetting(hero, this, "default-destination", "world");
    String[] dArgs = possibleDestinationsString.split(",");
    Location destination = null;
    World world = player.getWorld();
    for (String arg : dArgs) {
        if(world.getName().equalsIgnoreCase(arg))
            destination = mv.getMVWorldManager().getMVWorld(arg).getSpawnLocation();
        else
            destination = null;
    }
    if(destination == null) {
        destination = mv.getMVWorldManager().getMVWorld(defaultDestinationString).getSpawnLocation();
    }
    try {
      player.teleport(destination);
    } catch (Exception e) {
      player.sendMessage(ChatColor.GRAY + "SkillWarp has an invalid config.");
      return SkillResult.INVALID_TARGET_NO_MSG;
    }
    hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.WITHER_DEATH , 0.5F, 1.0F); 
    broadcastExecuteText(hero);

    return SkillResult.NORMAL;
  }
}