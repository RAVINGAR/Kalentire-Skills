package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.*;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class SkillBattleRage
  extends PassiveSkill
{
  public SkillBattleRage(Heroes plugin)
  {
    super(plugin, "BattleRage");
    setDescription("For every $1% of your missing health, you gain a flat increase of $2 basic attack damage.");
    setTypes(SkillType.BUFFING);
    Bukkit.getServer().getPluginManager().registerEvents(new SkillHeroListener(this), plugin);
  }
  
  public String getDescription(Hero hero)
  {
   
	double healthPercentage = SkillConfigManager.getUseSetting(hero, this, "health-percentage", 0.2D, false) + SkillConfigManager.getUseSetting(hero, this, "damage-multiplier-increase", 0.0D, false) * hero.getSkillLevel(this);
    double damageMod = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 0.2D, false) + SkillConfigManager.getUseSetting(hero, this, "damage-multiplier-increase", 0.0D, false) * hero.getSkillLevel(this);
    
    damageMod = damageMod > 0.0D ? damageMod : 0.0D;
    String description = getDescription().replace("$1", healthPercentage + "").replace("$2", damageMod + "");
    return description;
  }
  
  public ConfigurationSection getDefaultConfig()
  {
    ConfigurationSection node = super.getDefaultConfig();
   
    node.set("health-percentage", Double.valueOf(5));
    node.set(SkillSetting.DAMAGE.node(), Double.valueOf(1.5));
    node.set(SkillSetting.DAMAGE_INCREASE.node(), Double.valueOf(0));
    return node;
  }
  
  public class SkillHeroListener
    implements Listener
  {
    private Skill skill;
    
    public SkillHeroListener(Skill skill)
    {
      this.skill = skill;
    }
    
    @EventHandler
    public void onEntityDamage(WeaponDamageEvent event)
    {
      if ((!event.isCancelled()) && ((event.getDamager() instanceof Hero)))
      {
    	
        Hero hero = (Hero)event.getDamager();
        Player p = hero.getPlayer();
        if (hero.hasEffect("BattleRage"))
        {
          double damage = SkillConfigManager.getUseSetting(hero, this.skill, SkillSetting.DAMAGE.node(), 1.5, false) + SkillConfigManager.getUseSetting(hero, this.skill, SkillSetting.DAMAGE_INCREASE.node(), 0, false) * hero.getSkillLevel(this.skill);
          //Convoluted way of adding damage for every 5% of damage
         double missingHealth = p.getMaxHealth()- p.getHealth();
         double mhPercent = (missingHealth/p.getMaxHealth())*100;
         double multiplier = mhPercent/5;
         double damageAdd = damage*multiplier;

            event.setDamage(event.getDamage() + damageAdd);
          }
        }
      }
    
  }
}
