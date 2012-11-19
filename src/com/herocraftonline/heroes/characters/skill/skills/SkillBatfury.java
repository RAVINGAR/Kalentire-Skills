package com.herocraftonline.heroes.characters.skill.skills;
//TODO: add in following AI so the skill is more stressful to the victim 
//https://github.com/DMarby/Pets/blob/master/src/main/java/se/DMarby/Pets/EntityBatPet.java
//possible AI source from Pets
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.ExperienceChangeEvent;
import com.herocraftonline.heroes.api.events.HeroKillCharacterEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.classes.HeroClass;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Setting;
import com.herocraftonline.heroes.util.Util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SkillBatfury extends TargettedSkill implements Listener {

    public final Set<Bat> skillBats = new HashSet<Bat>();

    private long skillBatKillTick;

    public SkillBatfury(Heroes plugin) {
        super(plugin, "Batfury");
        setDescription("Your target is surrounded $1 by bats. " +
                "Bats despawn after $4s.");
        setUsage("/skill batfury <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill bats", "skill batfury");
        setTypes(SkillType.SUMMON, SkillType.SILENCABLE, SkillType.KNOWLEDGE, SkillType.HARMFUL);

        Bukkit.getPluginManager().registerEvents(this, this.plugin);
    }

    public String getDescription(Hero hero) {
        StringBuilder descr = new StringBuilder(getDescription().replace("$1", String.valueOf(getAmountFor(hero)))
                .replace("$4", Util.stringDouble(getDespawnDelayFor(hero) / 1000.0)));

        double cdSec = SkillConfigManager.getUseSetting(hero, this, Setting.COOLDOWN, 30000, false) / 1000.0;
        if (cdSec > 0) {
            descr.append(" CD:");
            descr.append(Util.formatDouble(cdSec));
            descr.append("s");
        }

        int mana = SkillConfigManager.getUseSetting(hero, this, Setting.MANA, 30, false);
        if (mana > 0) {
            descr.append(" M:");
            descr.append(mana);
        }

        double distance = SkillConfigManager.getUseSetting(hero, this, Setting.MAX_DISTANCE.node(), 10, false) +
                SkillConfigManager.getUseSetting(hero, this, Setting.MAX_DISTANCE_INCREASE.node(), 0.1, false) * hero.getSkillLevel(this);
        if (distance > 0) {
            descr.append(" Dist:");
            descr.append(Util.formatDouble(distance));
        }

        return descr.toString();
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection defaultConfig = super.getDefaultConfig();
        defaultConfig.set(Setting.MAX_DISTANCE.node(), 25);
        defaultConfig.set(Setting.COOLDOWN.node(), 10000);
        defaultConfig.set(Setting.MANA.node(), 30);
        defaultConfig.set("bat-amount", 10);
        defaultConfig.set("base-despawn-delay", 5000);
        defaultConfig.set("per-level-despawn-delay", 50);
        return defaultConfig;
    }

    public double getChanceFor(Hero hero, String key) {
        return SkillConfigManager.getUseSetting(hero, this, "base-chance-" + key, 0.1, false) +
                SkillConfigManager.getUseSetting(hero, this, "chance-per-level-" + key, 0.002, false) * hero.getSkillLevel(this);
    }

    public int getAmountFor(Hero hero) {
        return SkillConfigManager.getUseSetting(hero, this, "bat-amount", 4, false);
    }

    public int getDespawnDelayFor(Hero hero) {
        return SkillConfigManager.getUseSetting(hero, this, "base-despawn-delay", 5000, false) +
                SkillConfigManager.getUseSetting(hero, this, "per-level-despawn-delay", 100, false) * hero.getSkillLevel(this);
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        if (hero.getPlayer() == target) {
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        int amount = getAmountFor(hero);
        List<Bat> spawnedBats = new ArrayList<Bat>();

        Location targetLoc = target.getLocation();
        for (int i = 0; i < amount; i++) {
            int size;
            double roll = Util.nextRand();
            if (roll < getChanceFor(hero, "big")) {
                size = 4;
            } else if (roll < getChanceFor(hero, "small")) {
                size = 2;
            } else {
                size = 1;
            }

            double r = 0.5 * size;
            Location curSpawnLoc = targetLoc.clone().add(r * Math.cos(2 * Math.PI / (double) amount * i), 0,
                    r * Math.sin(2 * Math.PI / (double) amount * i));
            Bat bat = curSpawnLoc.getWorld().spawn(curSpawnLoc, Bat.class);

            spawnedBats.add(bat);
        }

        skillBats.addAll(spawnedBats);
        int despawnDelayTicks = getDespawnDelayFor(hero) / 50;
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new DespawnBatsTask(spawnedBats), despawnDelayTicks);

        broadcastExecuteText(hero, target);
        return SkillResult.NORMAL;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity living = event.getEntity();
        if (living instanceof Bat) {
            Bat bat = (Bat) living;
            if (skillBats.contains(bat)) {
                event.setDroppedExp(0);
                event.getDrops().clear();
                bat.remove();
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onHeroKillCharacter(HeroKillCharacterEvent event) {
        LivingEntity living = event.getDefender().getEntity();
        if (living instanceof Bat) {
            Bat bat = (Bat) living;
            if (skillBats.contains(bat)) {
                skillBatKillTick = getFirstWorldTime();
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onExperienceChange(ExperienceChangeEvent event) {
        if (event.getSource() == HeroClass.ExperienceType.KILLING && skillBatKillTick == getFirstWorldTime()) {
            event.setCancelled(true);
        }
    }

    private long getFirstWorldTime() {
        return Bukkit.getWorlds().get(0).getFullTime();
    }

    private class DespawnBatsTask implements Runnable {

        private final List<Bat> bats;

        public DespawnBatsTask(List<Bat> bats) {
            this.bats = bats;
        }

        @Override
        public void run() {
            skillBats.removeAll(bats);
            for (Bat bat : bats) {
                bat.remove();
            }
        }
    }
}
