package com.herocraftonline.heroes.characters.skill.skills;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import net.minecraft.server.v1_6_R2.EntityCreature;
import net.minecraft.server.v1_6_R2.EntityPlayer;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.craftbukkit.v1_6_R2.entity.CraftCreature;
import org.bukkit.craftbukkit.v1_6_R2.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetEvent;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.SkillUseEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;

public class SkillFade extends ActiveSkill {

    private String applyText;
    private String expireText;
    private String failText;
    private FadeMoveChecker moveChecker;

    public SkillFade(Heroes plugin) {
        super(plugin, "Fade");
        setDescription("You fade into the shadows, hiding you from view. Any unstealthy movements will cause you to reappear.");
        setUsage("/skill fade");
        setArgumentRange(0, 0);
        setIdentifiers("skill fade");
        setNotes("Note: Taking damage, moving, or causing damage removes the effect");
        setTypes(SkillType.ILLUSION, SkillType.BUFF, SkillType.COUNTER, SkillType.STEALTHY);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(), plugin);

        moveChecker = new FadeMoveChecker(this);
        Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, moveChecker, 1, 1);
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DURATION.node(), 30000);
        node.set(SkillSetting.APPLY_TEXT.node(), "You fade into the shadows");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "You come back into view");
        node.set("fail-text", "It's too bright to fade");
        node.set("detection-range", 1D);
        node.set("max-light-level", 8);
        node.set("max-move-distance", 1D);
        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, "You fade into the shadows");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "You come back into view");
        failText = SkillConfigManager.getRaw(this, "fail-text", "It's too bright to fade");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        Location loc = player.getLocation();
        if (loc.getBlock().getLightLevel() > SkillConfigManager.getUseSetting(hero, this, "max-light-level", 8, false)) {
            Messaging.send(player, failText);
            return SkillResult.FAIL;
        }
        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 30000, false);

        player.getWorld().playEffect(loc, org.bukkit.Effect.EXTINGUISH, 0, 10);
        hero.addEffect(new FadeEffect(this, duration));

        // If any nearby monsters are targeting the player, force them to change their target.
        for (Entity entity : player.getNearbyEntities(50, 50, 50)) {
            if (!(entity instanceof CraftCreature))
                continue;

            EntityCreature notchMob = (EntityCreature) ((CraftCreature) entity).getHandle();
            if (notchMob.target == null)
                continue;

            EntityPlayer notchPlayer = (EntityPlayer) ((CraftPlayer) player).getHandle();
            if (notchMob.target.equals(notchPlayer))
                notchMob.setGoalTarget(null);
        }

        moveChecker.addHero(hero);
        return SkillResult.NORMAL;
    }

    public class SkillEntityListener implements Listener {

        public SkillEntityListener() {
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onEntityTarget(EntityTargetEvent event) {
            if (!(event.getTarget() instanceof Player) || event.getTarget() == null) {
                return;
            }

            Player player = (Player) event.getTarget();
            Hero hero = plugin.getCharacterManager().getHero(player);
            if (!(hero.hasEffect("FadeEffect")))
                return;

            event.setCancelled(true);
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onSkillUse(SkillUseEvent event) {
            Hero hero = event.getHero();

            if (hero.hasEffect("FadeEffect")) {
                if (!event.getSkill().getTypes().contains(SkillType.STEALTHY))
                    hero.removeEffect(hero.getEffect("FadeEffect"));
            }
        }
    }

    public class FadeMoveChecker implements Runnable {

        private Map<Hero, Location> oldLocations = new HashMap<Hero, Location>();
        private Skill skill;

        FadeMoveChecker(Skill skill) {
            this.skill = skill;
        }

        @Override
        public void run() {
            Iterator<Entry<Hero, Location>> heroes = oldLocations.entrySet().iterator();
            while (heroes.hasNext()) {
                Entry<Hero, Location> entry = heroes.next();
                Hero hero = entry.getKey();
                Location oldLoc = entry.getValue();
                if (!hero.hasEffect("FadeEffect")) {
                    heroes.remove();
                    continue;
                }
                Location newLoc = hero.getPlayer().getLocation();
                if (newLoc.getWorld() != oldLoc.getWorld() || newLoc.distance(oldLoc) > SkillConfigManager.getUseSetting(hero, skill, "max-move-distance", 1D, false)) {
                    hero.removeEffect(hero.getEffect("FadeEffect"));
                    heroes.remove();
                    continue;
                }

                if (newLoc.getBlock().getLightLevel() > SkillConfigManager.getUseSetting(hero, skill, "max-light-level", 8, false)) {
                    hero.removeEffect(hero.getEffect("FadeEffect"));
                    heroes.remove();
                    continue;
                }
                double detectRange = SkillConfigManager.getUseSetting(hero, skill, "detection-range", 1D, false);
                for (Entity entity : hero.getPlayer().getNearbyEntities(detectRange, detectRange, detectRange)) {
                    if (entity instanceof Player) {
                        hero.removeEffect(hero.getEffect("FadeEffect"));
                        heroes.remove();
                        break;
                    }
                }
            }
        }

        public void addHero(Hero hero) {
            if (!hero.hasEffect("FadeEffect"))
                return;
            oldLocations.put(hero, hero.getPlayer().getLocation());
        }
    }

    public class FadeEffect extends ExpirableEffect {

        public FadeEffect(Skill skill, long duration) {
            super(skill, "FadeEffect", duration);

            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.INVIS);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);

            Player player = hero.getPlayer();

            for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
                if (onlinePlayer.equals(player) || onlinePlayer.hasPermission("heroes.admin.seeinvis")) {
                    continue;
                }
                onlinePlayer.hidePlayer(player);
            }

            if (applyText != null && applyText.length() > 0)
                Messaging.send(player, applyText);
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);

            Player player = hero.getPlayer();
            for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
                if (onlinePlayer.equals(player)) {
                    continue;
                }
                onlinePlayer.showPlayer(player);
            }

            if (expireText != null && expireText.length() > 0)
                Messaging.send(player, expireText);
        }
    }
}
