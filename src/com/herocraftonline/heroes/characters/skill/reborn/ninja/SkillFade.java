package com.herocraftonline.heroes.characters.skill.reborn.ninja;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.common.InvisibleEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class SkillFade extends ActiveSkill {
    private static String effectName = "Faded";
    private static int VANILLA_DARKNESS_LEVEL_START = 8;

    private String applyText;
    private String expireText;
    private String failText;
    private FadeMoveChecker moveChecker;

    public SkillFade(Heroes plugin) {
        super(plugin, "Fade");
        setDescription("You fade into the shadows, hiding you from view. Any unstealthy movements will cause you to reappear. " +
                "This ability lasts much longer if used in the darkness. $1");
        setNotes("Note: Taking damage, moving, or causing damage removes the effect");
        setUsage("/skill fade");
        setIdentifiers("skill fade");
        setArgumentRange(0, 0);
        setTypes(SkillType.ABILITY_PROPERTY_ILLUSION, SkillType.BUFFING, SkillType.STEALTHY);

        moveChecker = new FadeMoveChecker(this);
        Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, moveChecker, 1, 1);
    }

    @Override
    public String getDescription(Hero hero) {
        double requiredLightLevel = SkillConfigManager.getUseSetting(hero, this, "max-light-level", 8, false);
        String lightLevelText = "";
        if (requiredLightLevel < 0) {
            lightLevelText = "You can use this ability even in broad daylight!";
        } else {
            lightLevelText = "Requires it to be somewhat dark to use.";
        }

        return getDescription()
                .replace("$1", lightLevelText);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.DURATION.node(), 15000);
        config.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "You fade into the shadows");
        config.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "You come back into view");
        config.set("fail-text", ChatComponents.GENERIC_SKILL + "It's too bright to fade");
        config.set("detection-range", 0.5);
        config.set("max-light-level", -1);
        config.set("extra-duration-per-darkness-level", 2500);
        config.set("max-move-distance", 1.5);
        return config;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "You fade into the shadows");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "You come back into view");
        failText = SkillConfigManager.getRaw(this, "fail-text", ChatComponents.GENERIC_SKILL + "It's too bright to fade");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        Location loc = player.getLocation();

        int lightLevel = loc.getBlock().getLightLevel();
        double requiredLightLevel = SkillConfigManager.getUseSetting(hero, this, "max-light-level", 8, false);
        if (requiredLightLevel > -1) {
            if (lightLevel > requiredLightLevel) {
                player.sendMessage(failText);
                return SkillResult.FAIL;
            }
        }

        long duration = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.DURATION, false);

        double lightLevelDurationIncrease = SkillConfigManager.getUseSetting(hero, this, "extra-duration-per-darkness-level", 2500, false);
        duration+= lightLevelDurationIncrease * VANILLA_DARKNESS_LEVEL_START - lightLevel;

        hero.addEffect(new FadeEffect(this, player, duration));
        moveChecker.addHero(hero);

        player.getWorld().playEffect(loc, org.bukkit.Effect.EXTINGUISH, 0, 10);

        return SkillResult.NORMAL;
    }

    private class FadeEffect extends InvisibleEffect {
        FadeEffect(Skill skill, Player applier, long duration) {
            super(skill, effectName, applier, duration, applyText, expireText);

            types.add(EffectType.NIGHT_VISION);

            addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, (int) duration / 50, 0));
        }
    }

    public class FadeMoveChecker implements Runnable {
        private Map<Hero, Location> oldLocations = new HashMap<>();
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
                Player player = hero.getPlayer();
                Location oldLoc = entry.getValue();
                if (!hero.hasEffect(effectName)) {
                    heroes.remove();
                    continue;
                }
                Location newLoc = player.getLocation();
                double maxMoveDistance = SkillConfigManager.getUseSetting(hero, skill, "max-move-distance", 1.0, false);
                if (newLoc.getWorld() != oldLoc.getWorld() || newLoc.distance(oldLoc) > maxMoveDistance) {
                    hero.removeEffect(hero.getEffect(effectName));
                    heroes.remove();
                    continue;
                }

                int lightLevel = newLoc.getBlock().getLightLevel();
                int requiredLightLevel = SkillConfigManager.getUseSetting(hero, skill, "max-light-level", 8, false);
                if (requiredLightLevel > -1) {
                    if (lightLevel > requiredLightLevel) {
                        hero.removeEffect(hero.getEffect(effectName));
                        heroes.remove();
                        continue;
                    }
                }

                double detectRange = SkillConfigManager.getUseSetting(hero, skill, "detection-range", 0.0, false);
                for (Entity entity : player.getNearbyEntities(detectRange, detectRange, detectRange)) {
                    if (!(entity instanceof LivingEntity))
                        continue;

                    if (hero.isAlliedTo((LivingEntity) entity))
                        continue;

                    hero.removeEffect(hero.getEffect(effectName));
                    heroes.remove();
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