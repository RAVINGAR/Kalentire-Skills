package com.herocraftonline.heroes.characters.skill.reborn;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.effects.common.InvisibleEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class SkillLavaLurk extends ActiveSkill {

    private static String effectName = "LavaLurking";
    private String applyText;
    private String expireText;
    private String failText;
    private LavaLurkMoveChecker moveChecker;

    public SkillLavaLurk(Heroes plugin) {
        super(plugin, "LavaLurk");
        setDescription("You become one with lava, preventing all incoming fire damage and hiding your presence. Any unstealthy movements will cause you to reappear.");
        setUsage("/skill lavaLurk");
        setArgumentRange(0, 0);
        setIdentifiers("skill lavaLurk");
        setNotes("Note: Taking damage, moving, or causing damage removes the effect");
        setTypes(SkillType.BUFFING, SkillType.STEALTHY, SkillType.ABILITY_PROPERTY_FIRE);

        moveChecker = new LavaLurkMoveChecker(this);
        Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, moveChecker, 1, 1);
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.DURATION.node(), 30000);
        config.set("on-break-delayed-fire-resist-duration", 1500);
        config.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "You have become one with the lava.");
        config.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "Your body has returned to normal.");
        config.set("max-move-distance", 3);
        config.set("fail-text", ChatComponents.GENERIC_SKILL + "Unable to Lava Lurk. You are not submerged in lava.");
        return config;
    }

    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "You have become one with the lava.");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "Your body has returned to normal.");
        failText = SkillConfigManager.getRaw(this, "fail-text", ChatComponents.GENERIC_SKILL + "Unable to Lava Lurk. You are not submerged in lava.");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        Location loc = player.getLocation();
        if (loc.getBlock().getType() != Material.LAVA) {
            player.sendMessage(failText);
            return SkillResult.FAIL;
        }

        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 30000, false);
        long onBreakDuration = SkillConfigManager.getUseSetting(hero, this, "on-break-delayed-fire-resist-duration", 1500, false);

        player.getWorld().playEffect(loc, org.bukkit.Effect.EXTINGUISH, 0, 10);
        LavaLurkEffect lurkEffect = new LavaLurkEffect(this, player, duration, onBreakDuration);
        hero.addEffect(lurkEffect);

        moveChecker.addHero(hero);
        return SkillResult.NORMAL;
    }

    class LavaLurkEffect extends InvisibleEffect {

        private final long onBreakFireResistDuration;

        public LavaLurkEffect(Skill skill, Player applier, long duration, long onBreakFireResistDuration) {
            super(skill, effectName, applier, duration, applyText, expireText);
            this.onBreakFireResistDuration = onBreakFireResistDuration;

            types.add(EffectType.RESIST_FIRE);
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);

            ExpirableEffect temporaryResistEffect = new ExpirableEffect(skill, "LavaLurkDelayedFireResist", applier, onBreakFireResistDuration);
            temporaryResistEffect.types.add(EffectType.RESIST_FIRE);
            hero.addEffect(temporaryResistEffect);
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
                if (newLoc.getWorld() != oldLoc.getWorld() || newLoc.distance(oldLoc) > SkillConfigManager.getUseSetting(hero, skill, "max-move-distance", 3.0, false)) {
                    hero.removeEffect(hero.getEffect(effectName));
                    heroes.remove();
                    continue;
                }

                if (newLoc.getBlock().getType() != Material.LAVA) {
                    hero.removeEffect(hero.getEffect(effectName));
                    heroes.remove();
                    continue;
                }
            }
        }

        void addHero(Hero hero) {
            if (!hero.hasEffect(effectName))
                return;
            oldLocations.put(hero, hero.getPlayer().getLocation());
        }
    }
}
