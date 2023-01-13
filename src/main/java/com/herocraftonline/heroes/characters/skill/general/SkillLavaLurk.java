package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterManager;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.effects.common.InvisibleEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Listenable;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class SkillLavaLurk extends ActiveSkill implements Listenable {

    private static String effectName = "LavaLurking";
    private String applyText;
    private String expireText;
    private String failText;
    private LavaLurkMoveChecker moveChecker;
    private Listener listener;

    public SkillLavaLurk(Heroes plugin) {
        super(plugin, "LavaLurk");
        setDescription("You become one with lava, preventing all incoming fire damage and hiding your presence for $1 seconds. Every $2 seconds a spout of flames will erupt from your location");
        setUsage("/skill lavaLurk");
        setArgumentRange(0, 0);
        setIdentifiers("skill lavaLurk");
        setTypes(SkillType.BUFFING, SkillType.STEALTHY, SkillType.ABILITY_PROPERTY_FIRE);

        moveChecker = new LavaLurkMoveChecker(this);
        listener = new LavaLurkListener(plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        return super.getDescription()
                .replace("$1", Util.decFormat.format(SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 30000, false) / 1000))
                .replace("$2", Util.decFormat.format(Double.parseDouble(SkillConfigManager.getRaw(this, "check-interval", "2000")) / 1000));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.DURATION.node(), 30000);
        config.set("on-break-delayed-fire-resist-duration", 1500);
        config.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "You have become one with the lava.");
        config.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "Your body has returned to normal.");
        config.set("check-interval", 2000);
        config.set("fail-text", ChatComponents.GENERIC_SKILL + "Unable to Lava Lurk. You are not submerged in lava.");
        return config;
    }

    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "You have become one with the lava.");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "Your body has returned to normal.");
        failText = SkillConfigManager.getRaw(this, "fail-text", ChatComponents.GENERIC_SKILL + "Unable to Lava Lurk. You are not submerged in lava.");

        double interval = Double.parseDouble(SkillConfigManager.getRaw(this, "check-interval", "2000")) / 1000 * 20;
        Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, moveChecker, 10, (long) interval);
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        Location loc = player.getLocation();

        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 30000, false);
        long onBreakDuration = SkillConfigManager.getUseSetting(hero, this, "on-break-delayed-fire-resist-duration", 1500, false);

        player.getWorld().playEffect(loc, org.bukkit.Effect.EXTINGUISH, 0, 10);
        LavaLurkEffect lurkEffect = new LavaLurkEffect(this, player, duration, onBreakDuration);
        hero.addEffect(lurkEffect);

        moveChecker.addHero(hero);
        return SkillResult.NORMAL;
    }

    @NotNull
    @Override
    public Listener getListener() {
        return listener;
    }

    class LavaLurkEffect extends InvisibleEffect {

        private final long onBreakFireResistDuration;

        public LavaLurkEffect(Skill skill, Player applier, long duration, long onBreakFireResistDuration) {
            super(skill, effectName, applier, duration, applyText, expireText);
            this.onBreakFireResistDuration = onBreakFireResistDuration;

            types.add(EffectType.RESIST_FIRE);
            types.add(EffectType.INVISIBILITY);
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);

            ExpirableEffect temporaryResistEffect = new ExpirableEffect(skill, "LavaLurkDelayedFireResist", applier, onBreakFireResistDuration);
            temporaryResistEffect.types.add(EffectType.RESIST_FIRE);
            hero.addEffect(temporaryResistEffect);
        }
    }

    public class LavaLurkListener implements Listener {
        private CharacterManager manager;
        public LavaLurkListener(Heroes heroes) {
            manager = heroes.getCharacterManager();
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onDamage(EntityDamageEvent event) {
            if(event.isCancelled()) {
                return;
            }
            if(event.getEntity() instanceof Player player) {
                Hero hero = manager.getHero(player);
                if(!hero.hasEffect(effectName)) {
                    return;
                }
                World world = player.getWorld();
                world.playSound(player.getLocation(), Sound.ENTITY_BLAZE_DEATH, 0.7F, 0.8F);
                world.spawnParticle(Particle.SMALL_FLAME, player.getLocation(), 24, 0.15, 1, 0.15);
            }
        }
    }

    public class LavaLurkMoveChecker implements Runnable {

        private Map<Hero, Location> oldLocations = new HashMap<>();
        private Skill skill;

        LavaLurkMoveChecker(Skill skill) {
            this.skill = skill;
        }

        @Override
        public void run() {
            Iterator<Entry<Hero, Location>> heroes = oldLocations.entrySet().iterator();
            while (heroes.hasNext()) {
                Entry<Hero, Location> entry = heroes.next();
                Hero hero = entry.getKey();
                Location oldLoc = entry.getValue();

                if (!hero.hasEffect(effectName)) {
                    heroes.remove();
                    continue;
                }
                Location newLoc = hero.getPlayer().getLocation();
                World world = newLoc.getWorld();
                //todo mayb do thing where like there are two particles for eyes
                world.spawnParticle(Particle.FLAME, oldLoc, 24, 0.15, 1, 0.15);
                if (newLoc.getBlock().getType() == Material.WATER) {
                    hero.removeEffect(hero.getEffect(effectName));
                    heroes.remove();
                }


                /*
                if (newLoc.getBlock().getType() != Material.LAVA) {
                    hero.removeEffect(hero.getEffect(effectName));
                    heroes.remove();
                    continue;
                }*/
            }
        }

        void addHero(Hero hero) {
            if (!hero.hasEffect(effectName))
                return;
            oldLocations.put(hero, hero.getPlayer().getLocation());
        }
    }
}
