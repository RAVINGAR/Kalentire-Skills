package com.herocraftonline.heroes.characters.skill.skills;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import mc.alk.tracker.controllers.MessageController;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.inventory.ItemStack;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.InvisibleEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

public class SkillFeignDeath extends ActiveSkill {
    private String applyText;
    private String expireText;

    private boolean battleTrackerEnabled = false;

    private FeignMoveChecker moveChecker;

    public SkillFeignDeath(Heroes plugin) {
        super(plugin, "FeignDeath");
        setDescription("You feign your death, displaying a deceptive message of death to nearby players. After feigning, you are invisible for $1 seconds. Moving will break the effect however.");
        setUsage("/skill feigndeath");
        setArgumentRange(0, 0);
        setIdentifiers("skill feigndeath");
        setTypes(SkillType.ABILITY_PROPERTY_ILLUSION, SkillType.BLINDING, SkillType.SILENCABLE, SkillType.STEALTHY, SkillType.BUFFING);

        moveChecker = new FeignMoveChecker(this);

        if (Bukkit.getServer().getPluginManager().getPlugin("BattleTracker") != null) {
            battleTrackerEnabled = true;
        }
    }

    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 60000, false);

        String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription().replace("$1", formattedDuration);
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DURATION.node(), Integer.valueOf(60000));
        node.set("detection-range", Double.valueOf(1.0));
        node.set("max-move-distance", Double.valueOf(1.0));

        return node;
    }

    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, Messaging.getSkillDenoter() + "You feign death!");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, Messaging.getSkillDenoter() + "You appear to be living!");
    }

    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 60000, false);

        String playerName = player.getName();

        boolean isPVE = true;
        LivingEntity lastCombatTarget = hero.getCombatEffect().getLastCombatant();
        if (lastCombatTarget == null) {

            String deathMessage = "";

            if (battleTrackerEnabled) {
                deathMessage = MessageController.getPvEMessage(false, null, playerName, null);
            }
            else
                deathMessage = "[" + ChatColor.AQUA + "PVE" + ChatColor.DARK_GRAY + "]" + ChatColor.DARK_AQUA + playerName + ChatColor.DARK_GRAY + " choked on a pretzel.";

            broadcast(player.getLocation(), deathMessage);
        }
        else if (lastCombatTarget instanceof Player) {
            String targetName = ((Player) lastCombatTarget).getName();

            String deathMessage = "";

            if (battleTrackerEnabled) {
                boolean melee = true;
                ItemStack lastEnemyHeldItem = ((Player) lastCombatTarget).getItemInHand();
                if (lastEnemyHeldItem.getType() == Material.BOW)
                    melee = false;

                deathMessage = MessageController.getPvPMessage(melee, targetName, playerName, lastEnemyHeldItem);
            }
            else
                deathMessage = "[" + ChatColor.GREEN + "PVP" + ChatColor.DARK_GRAY + "]" + ChatColor.DARK_AQUA + playerName + ChatColor.DARK_GRAY + " was dominated by " + ChatColor.BLUE + targetName + ChatColor.DARK_GRAY + "!";

            isPVE = false;
            broadcast(player.getLocation(), deathMessage);
        }
        else {
            String targetName = Messaging.getLivingEntityName(lastCombatTarget);

            String deathMessage = "";

            if (battleTrackerEnabled) {
            boolean melee = true;
            if (lastCombatTarget instanceof Skeleton)
                melee = false;

                deathMessage = MessageController.getPvEMessage(melee, targetName, playerName, null);
            }
            else
                deathMessage = "[" + ChatColor.AQUA + "PVE" + ChatColor.DARK_GRAY + "]" + ChatColor.DARK_AQUA + playerName + ChatColor.DARK_GRAY + " was dominated by " + ChatColor.BLUE + targetName + ChatColor.DARK_GRAY + "!";

            broadcast(player.getLocation(), deathMessage);
        }


        // Feign Death
        hero.addEffect(new FeignDeathEffect(this, player, duration, isPVE));

        moveChecker.addHero(hero);

        return SkillResult.NORMAL;
    }
    
    public class FeignMoveChecker implements Runnable {

        private Map<Hero, Location> oldLocations = new HashMap<Hero, Location>();
        private Skill skill;

        FeignMoveChecker(Skill skill) {
            this.skill = skill;
        }

        @Override
        public void run() {
            Iterator<Entry<Hero, Location>> heroes = oldLocations.entrySet().iterator();
            while (heroes.hasNext()) {
                Entry<Hero, Location> entry = heroes.next();
                Hero hero = entry.getKey();
                Location oldLoc = entry.getValue();
                if (!hero.hasEffect("FeignDeathed")) {
                    heroes.remove();
                    continue;
                }

                Location newLoc = hero.getPlayer().getLocation();
                if (newLoc.distance(oldLoc) > SkillConfigManager.getUseSetting(hero, skill, "max-move-distance", Double.valueOf(1.0), false)) {
                    hero.removeEffect(hero.getEffect("Invisible"));
                    heroes.remove();
                    continue;
                }
                double detectRange = SkillConfigManager.getUseSetting(hero, skill, "detection-range", Double.valueOf(1.0), false);
                List<Entity> nearEntities = hero.getPlayer().getNearbyEntities(detectRange, detectRange, detectRange);
                for (Entity entity : nearEntities) {
                    if (entity instanceof Player) {
                        hero.removeEffect(hero.getEffect("Invisible"));
                        heroes.remove();
                        break;
                    }
                }
            }
        }

        public void addHero(Hero hero) {
            if (!hero.hasEffect("Invisible"))
                return;

            oldLocations.put(hero, hero.getPlayer().getLocation());
        }
    }

    // Buff effect used to keep track of warmup time
    public class FeignDeathEffect extends InvisibleEffect {

        private boolean isPVE;

        public FeignDeathEffect(Skill skill, Player applier, long duration, boolean isPVE) {
            super(skill, applier, duration, applyText, expireText);

            this.isPVE = isPVE;

            addMobEffect(15, (int) ((duration / 1000) * 20), 1, false);             // Blind
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);

            Player player = hero.getPlayer();
            String playerName = player.getName();

            String feignDeathExpireText = "";
            if (isPVE)
                feignDeathExpireText = "[" + ChatColor.AQUA + "PVE" + ChatColor.DARK_GRAY + "]";
            else
                feignDeathExpireText = "[" + ChatColor.GREEN + "PVP" + ChatColor.DARK_GRAY + "]";

            feignDeathExpireText += ChatColor.DARK_AQUA + playerName + ChatColor.DARK_GRAY + " was faking death!";

            broadcast(player.getLocation(), feignDeathExpireText);
        }
    }
}
