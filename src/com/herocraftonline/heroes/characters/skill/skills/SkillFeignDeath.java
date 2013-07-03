package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.InvisibleEffect;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SkillFeignDeath extends TargettedSkill
{
  private String applyText;
  private String expireText;

  public SkillFeignDeath(Heroes plugin)
  {
    super(plugin, "FeignDeath");
    setDescription("You feign your death, displaying a message of death but instead go invisible for $2s.");
    setUsage("/skill FeignDeath");
    setArgumentRange(0, 0);
    setIdentifiers(new String[] { "skill feigndeath" });
    setTypes(new SkillType[] { SkillType.ILLUSION, SkillType.SILENCABLE, SkillType.BUFF });
  }

  public String getDescription(Hero hero)
  {
    int duration = SkillConfigManager.getUseSetting(hero, this, "smoke-duration", 6000, false) / 1000;
    String description = getDescription().replace("$2", duration + "");
    return description;
  }

  public ConfigurationSection getDefaultConfig()
  {
    ConfigurationSection node = super.getDefaultConfig();
    
    node.set("smoke-duration", 6000);
    
    return node;
  }

  public void init()
  {
    this.applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, "You feign death!");
    this.expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "You appear to be living!");
    super.init();
  }

  public SkillResult use(Hero hero, LivingEntity target, String[] args)
  {
    Player player = hero.getPlayer();
    if (!(target instanceof Player)) {
      return SkillResult.INVALID_TARGET;
    }
    if (((Player)target).equals(player)) {
      return SkillResult.INVALID_TARGET;
    }
    Player tPlayer = (Player)target;
    if (!damageCheck(player, tPlayer)) {
      return SkillResult.INVALID_TARGET;
    }
    Hero tHero = this.plugin.getCharacterManager().getHero(tPlayer);
    String tn = tHero.getPlayer().getDisplayName();
    String pn = player.getDisplayName();
    long duration = SkillConfigManager.getUseSetting(hero, this, "smoke-duration", 6000, false);
    hero.addEffect(new InvisibleEffect(this, duration, applyText, expireText));
    //hero.addEffect(new InvisibleEffect(this, duration, this.applyText, this.expireText));
    String playername = player.getName();
    broadcast(player.getLocation(), ChatColor.WHITE + pn + " was slain by " + tn, new Object[0]);
    return SkillResult.NORMAL;
  }
}

