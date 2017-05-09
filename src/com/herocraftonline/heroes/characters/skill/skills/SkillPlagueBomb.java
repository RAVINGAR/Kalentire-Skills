package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterManager;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.CompatSound;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;

public class SkillPlagueBomb
  extends ActiveSkill
{
  private Map<Integer, Player> plagueBombs;
  
  public SkillPlagueBomb(Heroes plugin)
  {
    super(plugin, "PlagueBomb");
    setDescription("Throw out a fucking sheep that explodes!");
    setUsage("/skill plaguebomb");
    setArgumentRange(0, 0);
    setIdentifiers(new String[] { "skill plaguebomb" });
    
    this.plagueBombs = new HashMap();
    setTypes(new SkillType[] { SkillType.DAMAGING, SkillType.ABILITY_PROPERTY_MAGICAL });
    Bukkit.getServer().getPluginManager().registerEvents(new PlagueBombListener(this), plugin);
  }
  
  public ConfigurationSection getDefaultConfig()
  {
    ConfigurationSection node = super.getDefaultConfig();
    node.set(SkillSetting.DAMAGE.node(), Integer.valueOf(250));
    node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), Integer.valueOf(5));
    node.set(SkillSetting.RADIUS.node(), Integer.valueOf(5));
    node.set("sheep-velocity", Integer.valueOf(1));
    node.set("sheep-duration", Integer.valueOf(10000));
    return node;
  }
  
  public String getDescription(Hero hero)
  {
    return getDescription();
  }
  
  public SkillResult use(Hero hero, String[] args)
  {
    Player player = hero.getPlayer();
    
    long sheepMultiplier = SkillConfigManager.getUseSetting(hero, this, "sheep-velocity", 1, false);
    double sheepDuration = SkillConfigManager.getUseSetting(hero, this, "sheep-duration", 10000, false);
    
    final Sheep sheep = (Sheep)player.getWorld().spawn(player.getEyeLocation(), Sheep.class);
    this.plagueBombs.put(Integer.valueOf(sheep.getEntityId()), player);
    sheep.setMaxHealth(1000.0D);
    sheep.setHealth(1000.0D);
    sheep.setCustomName(ChatColor.DARK_RED + "PlagueBomb");
    sheep.setVelocity(player.getLocation().getDirection().normalize().multiply((float)sheepMultiplier));
    
    Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, new Runnable()
    {
      public void run()
      {
        if (!sheep.isDead()) {
          SkillPlagueBomb.this.sheepBomb(sheep);
        }
      }
    }, sheepDuration / 1000L * 20L);
    
    return SkillResult.NORMAL;
  }
  
  public void sheepBomb(Sheep sheep)
  {
    Player player = (Player)this.plagueBombs.get(Integer.valueOf(sheep.getEntityId()));
    Hero hero = this.plugin.getCharacterManager().getHero(player);
    sheep.setColor(DyeColor.BLUE);
    sheep.getWorld().playEffect(sheep.getLocation(), Effect.EXPLOSION_HUGE, 3);
    sheep.getWorld().playSound(sheep.getLocation(), CompatSound.ENTITY_GENERIC_EXPLODE.value(), 0.8F, 1.0F);
    sheep.damage(1000.0D);
    this.plagueBombs.remove(sheep);
    
    int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5, false);
    double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 250, false);
    double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 5, false);
    damage += damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT);
    for (Entity entity : sheep.getNearbyEntities(radius, radius, radius)) {
      if ((entity instanceof LivingEntity))
      {
        LivingEntity target = (LivingEntity)entity;
        if (damageCheck(player, target))
        {
          addSpellTarget(target, hero);
          damageEntity(target, player, damage, EntityDamageEvent.DamageCause.MAGIC);
        }
      }
    }
  }
  
  private class PlagueBombListener
    implements Listener
  {
    private Skill skill;
    
    public PlagueBombListener(Skill skill)
    {
      this.skill = skill;
    }
    
    @EventHandler(priority=EventPriority.MONITOR)
    public void onShear(PlayerShearEntityEvent event)
    {
      event.setCancelled(true);
    }
    
    @EventHandler(priority=EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event)
    {
      if ((event.getEntity() instanceof Sheep))
      {
        Sheep sheep = (Sheep)event.getEntity();
        if (SkillPlagueBomb.this.plagueBombs.containsKey(Integer.valueOf(sheep.getEntityId()))) {
          SkillPlagueBomb.this.sheepBomb(sheep);
        }
      }
    }
    
    @EventHandler(priority=EventPriority.MONITOR)
    public void onEntityDamage(EntityDamageEvent event)
    {
      if ((event.getEntity() instanceof Sheep))
      {
        Sheep sheep = (Sheep)event.getEntity();
        if ((SkillPlagueBomb.this.plagueBombs.containsKey(Integer.valueOf(sheep.getEntityId()))) && 
          (!sheep.isDead())) {
          event.setDamage(1000.0D);
        }
      }
    }
  }
}
