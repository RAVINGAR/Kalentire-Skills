package com.herocraftonline.heroes.characters.skill.pack8;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.InvisibleEffect;
import com.herocraftonline.heroes.characters.party.HeroParty;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.Sound;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class SkillFade extends ActiveSkill {
    private static String effectName = "Faded";

    private String applyText;
    private String expireText;
    private String failText;
    private FadeMoveChecker moveChecker;

    public SkillFade(Heroes plugin) {
        super(plugin, "Fade");
        setDescription("You fade into the shadows, hiding you from view. Any unstealthy movements will cause you to reappear.");
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
        return getDescription();
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
        config.set("max-move-distance", 1.5);
        return node;
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

        double requiredLightLevel = SkillConfigManager.getUseSetting(hero, this, "max-light-level", 8, false);
        if (requiredLightLevel > -1) {
            if (loc.getBlock().getLightLevel() > requiredLightLevel) {
                player.sendMessage(failText);
                return SkillResult.FAIL;
            }
        }

        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 30000, false);

        player.getWorld().playEffect(loc, org.bukkit.Effect.EXTINGUISH, 0, 10);
        hero.addEffect(new InvisibleEffect(this, player, effectName, duration, applyText, expireText));

        moveChecker.addHero(hero);
        return SkillResult.NORMAL;
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
                Player player = hero.getPlayer();
                Hero hero = entry.getKey();
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

                double requiredLightLevel = SkillConfigManager.getUseSetting(hero, this, "max-light-level", 8, false);
                if (requiredLightLevel > -1) {
                    if (newLoc.getBlock().getLightLevel() > SkillConfigManager.getUseSetting(hero, skill, "max-light-level", 8, false)) {
                        hero.removeEffect(hero.getEffect(effectName));
                        heroes.remove();
                        continue;
                    }
                }

                double detectRange = SkillConfigManager.getUseSetting(hero, skill, "detection-range", 0.0, false);
                for (Entity entity : player.getNearbyEntities(detectRange, detectRange, detectRange)) {
                    if (!(entity instanceof LivingEntity)) {
                        continue;

                    if (hero.isAlliedTo((LivingEntity) entity));
                        continue;

                    hero.removeEffect(hero.getEffect(effectName));
                    heroes.remove();
                }
            }
        }

        public void addHero(Hero hero) {
            if (!hero.hasEffect(effectName))
                return;

            oldLocations.put(hero, hero.getPlayer().getLocation());
        }
    }
}
