package com.herocraftonline.heroes.characters.skill.oldskills;
//http://pastie.org/private/0mwuk61iottgjmswmw

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.CombustEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.WitherSkull;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public class SkillDarkbolt extends ActiveSkill
{
  private final Map<WitherSkull, Long> Darkbolts = new LinkedHashMap<WitherSkull, Long>(100) {
    private static final long serialVersionUID = 4329526013158603250L;

    protected boolean removeEldestEntry(Map.Entry<WitherSkull, Long> eldest) {
      return (size() > 60) || (((Long)eldest.getValue()).longValue() + 5000L <= System.currentTimeMillis());
    }
  };

  public SkillDarkbolt(Heroes plugin)
  {
    super(plugin, "Darkbolt");
    setDescription("You shoot a ball of fire that deals $1 damage and lights your target on fire");
    setUsage("/skill darkbolt");
    setArgumentRange(0, 0);
    setIdentifiers(new String[] { "skill darkbolt" });
    setTypes(new SkillType[] { SkillType.FIRE, SkillType.SILENCABLE, SkillType.DAMAGING, SkillType.HARMFUL });
    Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(this), plugin);
  }

  public ConfigurationSection getDefaultConfig()
  {
    ConfigurationSection node = super.getDefaultConfig();
    node.set(SkillSetting.DAMAGE.node(), Integer.valueOf(4));
    node.set(SkillSetting.DAMAGE_INCREASE.node(), Double.valueOf(0.0D));
    node.set("velocity-multiplier", Double.valueOf(1.5D));
    node.set("fire-ticks", Integer.valueOf(100));
    return node;
  }

  public SkillResult use(Hero hero, String[] args)
  {
    Player player = hero.getPlayer();
    WitherSkull Darkbolt = (WitherSkull)player.launchProjectile(WitherSkull.class);
    Darkbolt.setFireTicks(100);
    this.Darkbolts.put(Darkbolt, Long.valueOf(System.currentTimeMillis()));
    double mult = SkillConfigManager.getUseSetting(hero, this, "velocity-multiplier", 1.5D, false);
    Darkbolt.setVelocity(Darkbolt.getVelocity().multiply(mult));
    broadcastExecuteText(hero);
    return SkillResult.NORMAL;
  }

  public String getDescription(Hero hero)
  {
    int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 1, false);
    damage += (int)(SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE, 0.0D, false) * hero.getSkillLevel(this));
    return getDescription().replace("$1", damage + "");
  }

  public class SkillEntityListener
    implements Listener
  {
    private final Skill skill;

    public SkillEntityListener(Skill skill)
    {
      this.skill = skill;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
      if ((event.isCancelled()) || (!(event instanceof EntityDamageByEntityEvent)) || (!(event.getEntity() instanceof LivingEntity))) {
        return;
      }

      EntityDamageByEntityEvent subEvent = (EntityDamageByEntityEvent)event;
      Entity projectile = subEvent.getDamager();
      if ((!(projectile instanceof WitherSkull)) || (!SkillDarkbolt.this.Darkbolts.containsKey(projectile))) {
        return;
      }
      SkillDarkbolt.this.Darkbolts.remove(projectile);
      LivingEntity entity = (LivingEntity)subEvent.getEntity();
      Entity dmger = ((WitherSkull)projectile).getShooter();
      if ((dmger instanceof Player)) {
        Hero hero = SkillDarkbolt.this.plugin.getCharacterManager().getHero((Player)dmger);

        if (!Skill.damageCheck((Player)dmger, entity)) {
          event.setCancelled(true);
          return;
        }

        entity.setFireTicks(SkillConfigManager.getUseSetting(hero, this.skill, "fire-ticks", 100, false));
        SkillDarkbolt.this.plugin.getCharacterManager().getCharacter(entity).addEffect(new CombustEffect(this.skill, (Player)dmger));

        SkillDarkbolt.this.addSpellTarget(entity, hero);
        double damage = SkillConfigManager.getUseSetting(hero, this.skill, SkillSetting.DAMAGE, 4, false);
        damage += (SkillConfigManager.getUseSetting(hero, this.skill, SkillSetting.DAMAGE_INCREASE, 0.0D, false) * hero.getSkillLevel(this.skill));
        Skill.damageEntity(entity, hero.getPlayer(), damage, EntityDamageEvent.DamageCause.MAGIC);
        event.setCancelled(true);
      }
    }
  }
}