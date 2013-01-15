package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Setting;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.meta.FireworkMeta;
import java.lang.reflect.Method;
import org.bukkit.FireworkEffect;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.Location;
import org.bukkit.World;

public class SkillArcaneblast extends TargettedSkill
{
  public SkillArcaneblast(Heroes plugin)
  {
    super(plugin, "Arcaneblast");
    setDescription("You arcaneblast the target for $1 light damage.");
    setUsage("/skill arcaneblast");
    setArgumentRange(0, 0);
    setIdentifiers(new String[] { "skill arcaneblast" });
    setTypes(new SkillType[] { SkillType.DAMAGING, SkillType.LIGHT, SkillType.SILENCABLE, SkillType.HARMFUL });
  }

  public ConfigurationSection getDefaultConfig()
  {
    ConfigurationSection node = super.getDefaultConfig();
    node.set(Setting.DAMAGE.node(), Integer.valueOf(10));
    return node;
  }

  public SkillResult use(Hero hero, LivingEntity target, String[] args)
  {
    Player player = hero.getPlayer();

    int damage = SkillConfigManager.getUseSetting(hero, this, Setting.DAMAGE, 10, false);
    addSpellTarget(target, hero);
    int n = 5;

    damageEntity(target, player, damage, DamageCause.MAGIC);
    broadcastExecuteText(hero, target);
    Firework firework = (Firework)target.getWorld().spawnEntity(target.getLocation(), EntityType.FIREWORK);
    FireworkMeta meta = firework.getFireworkMeta();
    meta.addEffect(FireworkEffect.builder().flicker(false).trail(true).with(FireworkEffect.Type.BALL_LARGE).withColor(Color.BLUE).withFade(Color.WHITE).build());
    meta.setPower(0);
    firework.setFireworkMeta(meta);
    return SkillResult.NORMAL;
  }

  public String getDescription(Hero hero)
  {
    int damage = SkillConfigManager.getUseSetting(hero, this, Setting.DAMAGE, 10, false);
    return getDescription().replace("$1", damage + "");
  }

  /**
   * FireworkEffectPlayer v1.0
   * 
   * FireworkEffectPlayer provides a thread-safe and (reasonably) version independant way to instantly explode a FireworkEffect at a given location.
   * You are welcome to use, redistribute, modify and destroy your own copies of this source with the following conditions:
   * 
   * 1. No warranty is given or implied.
   * 2. All damage is your own responsibility.
   * 3. You provide credit publicly to the original source should you release the plugin.
   * 
   * @author codename_B
   */
  public static class FireworkEffectPlayer {
      
      /*
       * Example use:
       * 
       * public class FireWorkPlugin implements Listener {
       * 
       * FireworkEffectPlayer fplayer = new FireworkEffectPlayer();
       * 
       @EventHandler
       public void onPlayerLogin(PlayerLoginEvent event) {
       *   fplayer.playFirework(event.getPlayer().getWorld(), event.getPlayer.getLocation(), Util.getRandomFireworkEffect());
       * }
       * 
       * }
       */
      
      // internal references, performance improvements
      private Method world_getHandle = null;
      private Method nms_world_broadcastEntityEffect = null;
      private Method firework_getHandle = null;
      
      /**
       * Play a pretty firework at the location with the FireworkEffect when called
       * @param world
       * @param loc
       * @param fe
       * @throws Exception
       */
      public void playFirework(World world, Location loc, FireworkEffect fe) throws Exception {
          // Bukkity load (CraftFirework)
          Firework fw = (Firework) world.spawn(loc, Firework.class);
          // the net.minecraft.server.World
          Object nms_world = null;
          Object nms_firework = null;
          /*
           * The reflection part, this gives us access to funky ways of messing around with things
           */
          if(world_getHandle == null) {
              // get the methods of the craftbukkit objects
              world_getHandle = getMethod(world.getClass(), "getHandle");
              firework_getHandle = getMethod(fw.getClass(), "getHandle");
          }
          // invoke with no arguments
          nms_world = world_getHandle.invoke(world, (Object[]) null);
          nms_firework = firework_getHandle.invoke(fw, (Object[]) null);
          // null checks are fast, so having this seperate is ok
          if(nms_world_broadcastEntityEffect == null) {
              // get the method of the nms_world
              nms_world_broadcastEntityEffect = getMethod(nms_world.getClass(), "broadcastEntityEffect");
          }
          /*
           * Now we mess with the metadata, allowing nice clean spawning of a pretty firework (look, pretty lights!)
           */
          // metadata load
          FireworkMeta data = (FireworkMeta) fw.getFireworkMeta();
          // clear existing
          data.clearEffects();
          // power of one
          data.setPower(1);
          // add the effect
          data.addEffect(fe);
          // set the meta
          fw.setFireworkMeta(data);
          /*
           * Finally, we broadcast the entity effect then kill our fireworks object
           */
          // invoke with arguments
          nms_world_broadcastEntityEffect.invoke(nms_world, new Object[] {nms_firework, (byte) 17});
          // remove from the game
          fw.remove();
      }
      
      /**
       * Internal method, used as shorthand to grab our method in a nice friendly manner
       * @param cl
       * @param method
       * @return Method (or null)
       */
      //      private static Method getMethod(Class<?> cl, String method) {
      private static Method getMethod(Class<?> cl, String method) {
          for(Method m : cl.getMethods()) {
              if(m.getName().equals(method)) {
                  return m;
              }
          }
          return null;
      }

  }
}