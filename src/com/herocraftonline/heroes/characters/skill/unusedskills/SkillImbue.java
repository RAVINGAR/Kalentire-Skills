package com.herocraftonline.heroes.characters.skill.unusedskills;
/*package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Setting;
import net.legendarysoftware.arrows.ArrowType;
import net.legendarysoftware.arrows.HeroArrows;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class SkillImbue extends ActiveSkill
{
  public SkillImbue(Heroes plugin)
  {
    super(plugin, "imbue");
    setDescription("You can imbue powers into your arrows");
    setUsage("/skill imbue <type>");
    setIdentifiers(new String[] { "skill imbue" });
    setArgumentRange(0, 1);
    setTypes(new SkillType[] { SkillType.FIRE, SkillType.ICE, SkillType.SILENCABLE, SkillType.ITEM, SkillType.UNBINDABLE });
  }

  public SkillResult use(Hero hero, String[] args)
  {
    Player player = hero.getPlayer();
    if (args.length != 1) {
      player.sendMessage("/skill imbue <type>");
      return SkillResult.FAIL;
    }

    String name = args[0].toLowerCase();

    if (name.equals("info")) {
      player.sendMessage(getDescription());
      return SkillResult.FAIL;
    }

    ArrowType type = HeroArrows.getImbueable(name);

    if (type == null) {
      player.sendMessage(ChatColor.GRAY + "Invalid arrow type!");
      return SkillResult.FAIL;
    }

    if (SkillConfigManager.getUseSetting(hero, this, "available", false)) {
      player.sendMessage("You do not have the ability to imbue " + type.getName() + "s" + ChatColor.GRAY + "!");
      return SkillResult.FAIL;
    }

    int level = SkillConfigManager.getUseSetting(hero, this, Setting.LEVEL, 20, false);
    if (hero.getLevel(hero.getHeroClass()) < level) {
      player.sendMessage(ChatColor.GRAY + "You must be level " + ChatColor.YELLOW + level + " to imbue " + type.getName() + "s" + ChatColor.GRAY + "!");
      return SkillResult.FAIL;
    }

    player.getWorld().dropItem(player.getLocation(), 
      type.makeSpecial(
      new ItemStack(Material.ARROW, SkillConfigManager.getUseSetting(hero, this, Setting.AMOUNT, 10, false))));

    player.sendMessage(ChatColor.GRAY + "You imbue your arrows into " + type.getName() + "s" + ChatColor.GRAY + "!");
    return SkillResult.NORMAL;
  }

  public String getDescription(Hero arg0)
  {
    return getDescription();
  }

  public ConfigurationSection getDefaultConfig()
  {
    ConfigurationSection node = super.getDefaultConfig();
    node.set(Setting.AMOUNT.node(), Integer.valueOf(10));
    for (String type : HeroArrows.imbueables()) {
      node.set(type + "." + "level", Integer.valueOf(20));
      node.set(type + "." + "available", Boolean.valueOf(false));
    }
    return node;
  }
}*/