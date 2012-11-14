package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Setting;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.server.EntityPotion;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.entity.CraftThrownPotion;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PotionSplashEvent;

public class SkillAmpul extends ActiveSkill
{
  private Map<ThrownPotion, Long> potions = new LinkedHashMap<ThrownPotion, Long>(89)
  {
    private static final long serialVersionUID = -8018803104297802046L;

    @Override
    protected boolean removeEldestEntry(Map.Entry<ThrownPotion, Long> eldest) {
      return (size() > 50) || (((Long)eldest.getValue()).longValue() + 10000L <= System.currentTimeMillis());
    }
  };

  public SkillAmpul(Heroes plugin)
  {
    super(plugin, "Ampul");
    setDescription("You throw a health vial that heals nearby party members for a maximum of $1.  Can also be used on a single player by providing a name");
    setUsage("/skill ampul [player name]");
    setIdentifiers(new String[] { "skill ampul" });
    setArgumentRange(0, 1);
    setTypes(new SkillType[] { SkillType.HEAL, SkillType.LIGHT, SkillType.MANA, SkillType.SILENCABLE, SkillType.ITEM });
    plugin.getServer().getPluginManager().registerEvents(new PotionListener(), plugin);
  }

  public String getDescription(Hero hero)
  {
    return getDescription().replaceAll("$1", getMaxHeal(hero) + " hp");
  }

  private int getMaxHeal(Hero hero) {
    return SkillConfigManager.getUseSetting(hero, this, Setting.AMOUNT, 150, false);
  }

  public SkillResult use(Hero casterHero, String[] args)
  {
    Player casterPlayer = casterHero.getPlayer();

    if (args.length > 0) {
      Player targetPlayer = Bukkit.getPlayer(args[0]);

      if ((targetPlayer == null) || (!targetPlayer.isOnline())) return SkillResult.INVALID_TARGET;

      Location targetLocation = targetPlayer.getLocation();

      if ((casterPlayer.getLocation().getWorld() != targetLocation.getWorld()) || 
        (casterPlayer.getLocation().distance(targetLocation) > SkillConfigManager.getUseSetting(casterHero, this, Setting.MAX_DISTANCE, 10, false))) {
        casterHero.getPlayer().sendMessage("Target is out of range!");
        return SkillResult.FAIL;
      }

      targetLocation.getWorld().playEffect(targetLocation, Effect.POTION_BREAK, 8197);

      broadcast(casterPlayer.getLocation(), "$1 used an ampul on $2", new Object[] { casterPlayer.getDisplayName(), targetPlayer.getDisplayName() });
      Hero targetHero = this.plugin.getCharacterManager().getHero(targetPlayer);
      targetHero.setHealth(targetHero.getHealth() + getMaxHeal(casterHero));
      targetHero.syncHealth();
      return SkillResult.NORMAL;
    }

    broadcastExecuteText(casterHero);

    net.minecraft.server.World world = ((CraftWorld)casterPlayer.getWorld()).getHandle();
    EntityPotion entityPotion = new EntityPotion(world, ((CraftLivingEntity)casterPlayer).getHandle(), 8197);
    world.addEntity(entityPotion);

    ThrownPotion thrownPotion = new CraftThrownPotion(world.getServer(), entityPotion);
    thrownPotion.setVelocity(thrownPotion.getVelocity().multiply(2));

    this.potions.put(thrownPotion, Long.valueOf(System.currentTimeMillis()));
    return SkillResult.NORMAL;
  }

  public ConfigurationSection getDefaultConfig()
  {
    ConfigurationSection node = super.getDefaultConfig();
    node.set(Setting.HEALTH.node(), Integer.valueOf(250));
    node.set(Setting.MAX_DISTANCE.node(), Integer.valueOf(10));
    return node;
  }
  public class PotionListener implements Listener {
    public PotionListener() {
    }

    @EventHandler(priority=EventPriority.LOWEST, ignoreCancelled=false)
    public void onPotionSplash(PotionSplashEvent event) {
      if (SkillAmpul.this.potions.remove(event.getPotion()) != null) return;
      LivingEntity shooter = event.getPotion().getShooter();
      Hero casterHero;
      if ((shooter != null) && ((shooter instanceof Player)))
        casterHero = SkillAmpul.this.plugin.getCharacterManager().getHero((Player)shooter);
      else
        return;
      for (LivingEntity affected : event.getAffectedEntities()) {
        if ((affected instanceof Player)) {
          Hero affectedHero = SkillAmpul.this.plugin.getCharacterManager().getHero((Player)affected);

          if (isFriendlyPlayer(casterHero, affectedHero)) {
            affectedHero.setHealth(affectedHero.getHealth() + (int)(SkillAmpul.this.getMaxHeal(casterHero) * event.getIntensity(affected)));
            affectedHero.syncHealth();
          }

        }

        event.setIntensity(affected, 0.0D);
      }
      event.setCancelled(true);
    }

    private boolean isFriendlyPlayer(Hero hero, Hero affected)
    {
      if (hero.getPlayer() == affected.getPlayer()) return true;
      return (hero.getParty() != null) && (hero.getParty() == affected.getParty());
    }
  }
}