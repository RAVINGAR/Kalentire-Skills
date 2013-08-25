package com.herocraftonline.heroes.characters.skill.unfinishedskills;
//originalsrc-http://pastie.org/private/udeqd6iuoirm3wraci4ga
import java.lang.reflect.Method;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Firework;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.meta.FireworkMeta;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;

public class SkillFireblast extends TargettedSkill
{
  // silly kainzo, tricks are for kids.
  public FireworkEffectPlayer fplayer = new FireworkEffectPlayer();

  public SkillFireblast(Heroes plugin)
  {
    super(plugin, "Fireblast");
    setDescription("You fireblast the target for $1 light damage.");
    setUsage("/skill fireblast");
    setArgumentRange(0, 0);
    setIdentifiers(new String[] { "skill fireblast" });
    setTypes(new SkillType[] { SkillType.DAMAGING, SkillType.FIRE, SkillType.SILENCABLE, SkillType.HARMFUL });
  }

  public ConfigurationSection getDefaultConfig()
  {
    ConfigurationSection node = super.getDefaultConfig();
    node.set(SkillSetting.DAMAGE.node(), Integer.valueOf(10));
    return node;
  }

  public SkillResult use(Hero hero, LivingEntity target, String[] args)
  {
    Player player = hero.getPlayer();

    double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 10, false);
    addSpellTarget(target, hero);

    damageEntity(target, player, damage, DamageCause.MAGIC);
    broadcastExecuteText(hero, target);
    
    // this is our funky shit
    try {
		fplayer.playFirework(player.getWorld(), target.getLocation(), 
				FireworkEffect.builder().flicker(false).trail(true)
				.with(FireworkEffect.Type.BURST)
				.withColor(Color.RED)
				.withFade(Color.ORANGE)
				.build());
	} catch (IllegalArgumentException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
    return SkillResult.NORMAL;
  }

  public String getDescription(Hero hero)
  {
    int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 10, false);
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
              // get the methods of the craftbukkit tables
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