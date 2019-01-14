package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

public class SkillCripple extends TargettedSkill
{
  private String applyText;
  private String removeText;
  
  public SkillCripple(Heroes plugin)
  {
    super(plugin, "Cripple");
    setDescription("Наносит чистый урон противнику ( урон стакается с бафами )");
    setUsage("/skill cripple [target]");
    setArgumentRange(0, 1);
    setIdentifiers(new String[] { "skill cripple" });
    setTypes(new SkillType[] { SkillType.DEBUFF, SkillType.DAMAGING, SkillType.PHYSICAL });
  }
  
  public String getDescription(Hero hero)
  {
    long duration = (SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 10000, false) + SkillConfigManager.getUseSetting(hero, this, "duration-increase", 0.0D, false) * hero.getSkillLevel(this)) / 1000L;
    
    duration = duration > 0L ? duration : 0L;
    int damage = (int)(SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE.node(), 5, false) + SkillConfigManager.getUseSetting(hero, this, "damage-increase", 0.0D, false) * hero.getSkillLevel(this));
    
    damage = damage > 0 ? damage : 0;
    int tickDamage = (int)(SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK.node(), 2, false) + SkillConfigManager.getUseSetting(hero, this, "tick-damage-increase", 0.0D, false) * hero.getSkillLevel(this));
    
    tickDamage = tickDamage > 0 ? tickDamage : 0;
    int maxDistance = (int)(SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE.node(), 2, false) + SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE_INCREASE.node(), 0.0D, false) * hero.getSkillLevel(this));
    
    maxDistance = maxDistance > 0 ? maxDistance : 0;
    String description = getDescription().replace("$1", duration + "").replace("$2", damage + "").replace("$3", tickDamage + "");
    

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
    node.set(SkillSetting.DURATION.node(), Integer.valueOf(10000));
    node.set("duration-increase", Integer.valueOf(0));
    node.set(SkillSetting.PERIOD.node(), Integer.valueOf(1000));
    node.set(SkillSetting.DAMAGE_TICK.node(), Integer.valueOf(2));
    node.set("tick-damage-increase", Integer.valueOf(0));
    node.set(SkillSetting.DAMAGE.node(), Integer.valueOf(5));
    node.set("damage-incrase", Integer.valueOf(0));
    node.set(SkillSetting.MAX_DISTANCE.node(), Integer.valueOf(15));
    node.set(SkillSetting.MAX_DISTANCE_INCREASE.node(), Integer.valueOf(0));
    node.set(SkillSetting.APPLY_TEXT.node(), "%target% has been Crippled by %hero%!");
    node.set("remove-text", "%target% has recovered from %hero%s Crippling blow!");
    return node;
  }
  
  public void init()
  {
    super.init();
    applyText = SkillConfigManager.getUseSetting(null, this, SkillSetting.APPLY_TEXT.node(), "%target% has been Crippled by %hero%!").replace("%target%", "$1").replace("%hero%", "$2");
    removeText = SkillConfigManager.getUseSetting(null, this, "remove-text", "%target% has recovered from %hero%s Crippling blow!").replace("%target%", "$1").replace("%hero%", "$2");
  }
  
  public SkillResult use(Hero hero, org.bukkit.entity.LivingEntity le, String[] strings)
  {
    Player player = hero.getPlayer();
    if ((!le.equals(player)) && ((le instanceof Player))) {
      Hero tHero = plugin.getCharacterManager().getHero((Player)le);
      if (((hero.getParty() == null) || (!hero.getParty().getMembers().contains(tHero))) && 
        (damageCheck(player, tHero.getPlayer()))) {
        broadcastExecuteText(hero, le);
        int damage = (int)(SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE.node(), 5, false) + SkillConfigManager.getUseSetting(hero, this, "damage-increase", 0.0D, false) * hero.getSkillLevel(this));
        
        damage = damage > 0 ? damage : 0;
        damageEntity(tHero.getPlayer(), player, damage, EntityDamageEvent.DamageCause.ENTITY_ATTACK);
        
        long duration = (SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 10000, false) + SkillConfigManager.getUseSetting(hero, this, "duration-increase", 0.0D, false) * hero.getSkillLevel(this));
        
        duration = duration > 0L ? duration : 0L;
        long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD.node(), 1000, false);
        int tickDamage = (int)(SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK.node(), 2, false) + SkillConfigManager.getUseSetting(hero, this, "tick-damage-increase", 0.0D, false) * hero.getSkillLevel(this));
        
        tickDamage = tickDamage > 0 ? tickDamage : 0;
        CrippleEffect cEffect = new CrippleEffect(this, period, duration, tickDamage, player);
        tHero.addEffect(cEffect);
        return SkillResult.NORMAL;
      }
    }
    
    return SkillResult.INVALID_TARGET;
  }
  
  public class CrippleEffect extends PeriodicExpirableEffect {
    private Player caster;
    private Location prevLocation;
    private final int damageTick;
    
    public CrippleEffect(Skill skill, long period, long duration, int damageTick, Player caster) { super("Cripple", period, duration);
      this.caster = caster;
      this.damageTick = damageTick;
      types.add(EffectType.BLEED);
      types.add(EffectType.DISPELLABLE);
      types.add(EffectType.PHYSICAL);
    }
    
    public void tickHero(Hero hero)
    {
      if ((prevLocation != null) && (Math.abs(hero.getPlayer().getLocation().getX() - prevLocation.getX()) >= 1.0D) && (Math.abs(hero.getPlayer().getLocation().getZ() - prevLocation.getZ()) >= 1.0D))
      {

        Skill.damageEntity(hero.getPlayer(), caster, damageTick, EntityDamageEvent.DamageCause.ENTITY_ATTACK);
      }
      
      prevLocation = hero.getPlayer().getLocation();
    }
    
    public void applyToHero(Hero hero)
    {
      super.applyToHero(hero);
      broadcast(hero.getPlayer().getLocation(), applyText, new Object[] { hero.getPlayer().getDisplayName(), caster.getDisplayName() });
      prevLocation = hero.getPlayer().getLocation();
    }
    
    public void removeFromHero(Hero hero)
    {
      super.removeFromHero(hero);
      broadcast(hero.getPlayer().getLocation(), removeText, new Object[] { hero.getPlayer().getDisplayName(), caster.getDisplayName() });
    }
    
    public void tickMonster(com.herocraftonline.heroes.characters.Monster mnstr)
    {
      super.tick(mnstr);
      Skill.damageEntity(mnstr.getEntity(), caster, damageTick, EntityDamageEvent.DamageCause.ENTITY_ATTACK);
    }
  }
}
