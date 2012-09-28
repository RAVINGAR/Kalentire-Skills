package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.ExperienceChangeEvent;
import com.herocraftonline.heroes.api.events.HeroKillCharacterEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.classes.HeroClass.ExperienceType;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Setting;
import com.herocraftonline.heroes.util.Util;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Slime;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class SkillSlimify extends TargettedSkill
  implements Listener
{
  public final Set<Slime> skillSlimes = new HashSet<Slime>();
  private long skillSlimeKillTick;

  public SkillSlimify(Heroes plugin)
  {
    super(plugin, "Slimify");
    setDescription("Your target is surrounded $1 by tiny slimes. $2% chance to spawn big and $3% to spawn small slime instead of every tiny. Slimes despawns after $4s.");

    setUsage("/skill slimify <target>");
    setArgumentRange(0, 1);
    setIdentifiers(new String[] { "skill slime", "skill slimeattack" });
    setTypes(new SkillType[] { SkillType.SUMMON, SkillType.SILENCABLE, SkillType.KNOWLEDGE, SkillType.HARMFUL });

    Bukkit.getPluginManager().registerEvents(this, this.plugin);
  }

  public String getDescription(Hero hero) {
    int chanceBig = Math.min((int)(100.0D * getChanceFor(hero, "big")), 100);
    int chanceSmall = Math.min((int)(100.0D * getChanceFor(hero, "small")), 100);

    StringBuilder descr = new StringBuilder(getDescription().replace("$1", String.valueOf(getAmountFor(hero))).replace("$2", String.valueOf(chanceBig)).replace("$3", String.valueOf(chanceSmall)).replace("$4", Util.stringDouble(getDespawnDelayFor(hero) / 1000.0D)));

    double cdSec = SkillConfigManager.getUseSetting(hero, this, Setting.COOLDOWN, 30000, false) / 1000.0D;
    if (cdSec > 0.0D) {
      descr.append(" CD:");
      descr.append(Util.formatDouble(cdSec));
      descr.append("s");
    }

    int mana = SkillConfigManager.getUseSetting(hero, this, Setting.MANA, 30, false);
    if (mana > 0) {
      descr.append(" M:");
      descr.append(mana);
    }

    double distance = SkillConfigManager.getUseSetting(hero, this, Setting.MAX_DISTANCE.node(), 10, false) + SkillConfigManager.getUseSetting(hero, this, Setting.MAX_DISTANCE_INCREASE.node(), 0.1D, false) * hero.getSkillLevel(this);

    if (distance > 0.0D) {
      descr.append(" Dist:");
      descr.append(Util.formatDouble(distance));
    }

    return descr.toString();
  }

  public ConfigurationSection getDefaultConfig() {
    ConfigurationSection defaultConfig = super.getDefaultConfig();
    defaultConfig.set(Setting.REAGENT.node(), Integer.valueOf(341));
    defaultConfig.set(Setting.REAGENT_COST.node(), Integer.valueOf(5));
    defaultConfig.set(Setting.MAX_DISTANCE.node(), Integer.valueOf(25));
    defaultConfig.set(Setting.MAX_DISTANCE_INCREASE.node(), Double.valueOf(0.35D));
    defaultConfig.set(Setting.COOLDOWN.node(), Integer.valueOf(30000));
    defaultConfig.set(Setting.MANA.node(), Integer.valueOf(30));
    defaultConfig.set("base-chance-big", Double.valueOf(0.1D));
    defaultConfig.set("chance-per-level-big", Double.valueOf(0.002D));
    defaultConfig.set("base-chance-small", Double.valueOf(0.2D));
    defaultConfig.set("chance-per-level-small", Double.valueOf(0.004D));
    defaultConfig.set("slime-amount", Integer.valueOf(4));
    defaultConfig.set("base-despawn-delay", Integer.valueOf(5000));
    defaultConfig.set("per-level-despawn-delay", Integer.valueOf(100));
    return defaultConfig;
  }

  public double getChanceFor(Hero hero, String key) {
    return SkillConfigManager.getUseSetting(hero, this, new StringBuilder().append("base-chance-").append(key).toString(), 0.1D, false) + SkillConfigManager.getUseSetting(hero, this, new StringBuilder().append("chance-per-level-").append(key).toString(), 0.002D, false) * hero.getSkillLevel(this);
  }

  public int getAmountFor(Hero hero)
  {
    return SkillConfigManager.getUseSetting(hero, this, "slime-amount", 4, false);
  }

  public int getDespawnDelayFor(Hero hero) {
    return SkillConfigManager.getUseSetting(hero, this, "base-despawn-delay", 5000, false) + SkillConfigManager.getUseSetting(hero, this, "per-level-despawn-delay", 100, false) * hero.getSkillLevel(this);
  }

  public SkillResult use(Hero hero, LivingEntity target, String[] args)
  {
    if (hero.getPlayer() == target) {
      return SkillResult.INVALID_TARGET_NO_MSG;
    }

    int amount = getAmountFor(hero);
    List<Slime> spawnedSlimes = new ArrayList<Slime>();

    Location targetLoc = target.getLocation();
    for (int i = 0; i < amount; i++)
    {
      double roll = Util.nextRand();
      int size;
      if (roll < getChanceFor(hero, "big")) {
        size = 4;
      }
      else
      {
        if (roll < getChanceFor(hero, "small"))
          size = 2;
        else {
          size = 1;
        }
      }
      double r = 0.5D * size;
      Location curSpawnLoc = targetLoc.clone().add(r * Math.cos(6.283185307179586D / amount * i), 0.0D, r * Math.sin(6.283185307179586D / amount * i));

      Slime slime = (Slime)curSpawnLoc.getWorld().spawn(curSpawnLoc, Slime.class);
      slime.setSize(size);

      spawnedSlimes.add(slime);
    }

    this.skillSlimes.addAll(spawnedSlimes);
    int despawnDelayTicks = getDespawnDelayFor(hero) / 50;
    Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, new DespawnSlimesTask(spawnedSlimes), despawnDelayTicks);

    broadcastExecuteText(hero, target);
    return SkillResult.NORMAL;
  }

  @EventHandler(priority=EventPriority.NORMAL)
  public void onEntityDeath(EntityDeathEvent event) {
    LivingEntity living = event.getEntity();
    if ((living instanceof Slime)) {
      Slime slime = (Slime)living;
      if (this.skillSlimes.contains(slime)) {
        event.setDroppedExp(0);
        event.getDrops().clear();
        slime.remove();
      }
    }
  }

  @EventHandler(priority=EventPriority.MONITOR)
  public void onHeroKillCharacter(HeroKillCharacterEvent event) {
    LivingEntity living = event.getDefender().getEntity();
    if ((living instanceof Slime)) {
      Slime slime = (Slime)living;
      if (this.skillSlimes.contains(slime))
        this.skillSlimeKillTick = getFirstWorldTime();
    }
  }

  @EventHandler(priority=EventPriority.NORMAL)
  public void onExperienceChange(ExperienceChangeEvent event)
  {
    if ((event.getSource() == ExperienceType.KILLING) && (this.skillSlimeKillTick == getFirstWorldTime()))
      event.setCancelled(true);
  }

  private long getFirstWorldTime()
  {
    return ((World)Bukkit.getWorlds().get(0)).getFullTime();
  }

  private class DespawnSlimesTask implements Runnable
  {
    private final List<Slime> slimes;

    public DespawnSlimesTask(List<Slime> slimes) {
      this.slimes = slimes;
    }

    public void run()
    {
      SkillSlimify.this.skillSlimes.removeAll(this.slimes);
      for (Slime slime : this.slimes)
        slime.remove();
    }
  }
}