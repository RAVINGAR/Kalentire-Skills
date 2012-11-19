package com.herocraftonline.heroes.characters.skill.skills;

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

    public final Set<Slime> skillSlimes = new HashSet<Slime>();

    private long skillSlimeKillTick;

    public SkillBatfury(Heroes plugin) {
        super(plugin, "Batfury");
        setDescription("Your target is surrounded $1 by tiny slimes. $2% chance to spawn big and $3% to spawn small slime instead of every tiny. " +
                "Slimes despawns after $4s.");
        setUsage("/skill slime <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill slime", "skill batfury");
        setTypes(SkillType.SUMMON, SkillType.SILENCABLE, SkillType.KNOWLEDGE, SkillType.HARMFUL);

        Bukkit.getPluginManager().registerEvents(this, this.plugin);
    }

    public String getDescription(Hero hero) {
        int chanceBig = Math.min((int) (100.0 * getChanceFor(hero, "big")), 100);
        int chanceSmall = Math.min((int) (100.0 * getChanceFor(hero, "small")), 100);

        StringBuilder descr = new StringBuilder(getDescription().replace("$1", String.valueOf(getAmountFor(hero)))
                .replace("$2", String.valueOf(chanceBig))
                .replace("$3", String.valueOf(chanceSmall))
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
        defaultConfig.set(Setting.REAGENT.node(), 341);
        defaultConfig.set(Setting.REAGENT_COST.node(), 5);
        defaultConfig.set(Setting.MAX_DISTANCE.node(), 25);
        defaultConfig.set(Setting.MAX_DISTANCE_INCREASE.node(), 0.35);
        defaultConfig.set(Setting.COOLDOWN.node(), 30000);
        defaultConfig.set(Setting.MANA.node(), 30);
        defaultConfig.set("bat-amount", 4);
        defaultConfig.set("base-despawn-delay", 5000);
        defaultConfig.set("per-level-despawn-delay", 100);
        return defaultConfig;
    }

    public double getChanceFor(Hero hero, String key) {
        return SkillConfigManager.getUseSetting(hero, this, "base-chance-" + key, 0.1, false) +
                SkillConfigManager.getUseSetting(hero, this, "chance-per-level-" + key, 0.002, false) * hero.getSkillLevel(this);
    }

    public int getAmountFor(Hero hero) {
        return SkillConfigManager.getUseSetting(hero, this, "slime-amount", 4, false);
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
        List<Slime> spawnedSlimes = new ArrayList<Slime>();

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
            Slime slime = curSpawnLoc.getWorld().spawn(curSpawnLoc, Slime.class);
            slime.setSize(size);

            spawnedSlimes.add(slime);
        }

        skillSlimes.addAll(spawnedSlimes);
        int despawnDelayTicks = getDespawnDelayFor(hero) / 50;
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new DespawnSlimesTask(spawnedSlimes), despawnDelayTicks);

        broadcastExecuteText(hero, target);
        return SkillResult.NORMAL;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity living = event.getEntity();
        if (living instanceof Slime) {
            Slime slime = (Slime) living;
            if (skillSlimes.contains(slime)) {
                event.setDroppedExp(0);
                event.getDrops().clear();
                slime.remove();
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onHeroKillCharacter(HeroKillCharacterEvent event) {
        LivingEntity living = event.getDefender().getEntity();
        if (living instanceof Slime) {
            Slime slime = (Slime) living;
            if (skillSlimes.contains(slime)) {
                skillSlimeKillTick = getFirstWorldTime();
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onExperienceChange(ExperienceChangeEvent event) {
        if (event.getSource() == HeroClass.ExperienceType.KILLING && skillSlimeKillTick == getFirstWorldTime()) {
            event.setCancelled(true);
        }
    }

    private long getFirstWorldTime() {
        return Bukkit.getWorlds().get(0).getFullTime();
    }

    private class DespawnSlimesTask implements Runnable {

        private final List<Slime> slimes;

        public DespawnSlimesTask(List<Slime> slimes) {
            this.slimes = slimes;
        }

        @Override
        public void run() {
            skillSlimes.removeAll(slimes);
            for (Slime slime : slimes) {
                slime.remove();
            }
        }
    }
}
