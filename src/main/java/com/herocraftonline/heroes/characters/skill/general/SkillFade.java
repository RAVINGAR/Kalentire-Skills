package com.herocraftonline.heroes.characters.skill.general;

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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class SkillFade extends ActiveSkill {

    private String applyText;
    private String expireText;
    private String failText;
    private final FadeMoveChecker moveChecker;

    public SkillFade(Heroes plugin) {
        super(plugin, "Fade");
        setDescription("You fade into the shadows, hiding you from view. Any unstealthy movements will cause you to reappear.");
        setUsage("/skill fade");
        setArgumentRange(0, 0);
        setIdentifiers("skill fade");
        setNotes("Note: Taking damage, moving, or causing damage removes the effect");
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
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DURATION.node(), 30000);
        node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "You fade into the shadows");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "You come back into view");
        node.set("fail-text", ChatComponents.GENERIC_SKILL + "It's too bright to fade");
        node.set("detection-range", 0);
        node.set("max-light-level", 8);
        node.set("max-move-distance", 1.0);

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
        if (loc.getBlock().getLightLevel() > SkillConfigManager.getUseSetting(hero, this, "max-light-level", 8, false)) {
            player.sendMessage(failText);
            return SkillResult.FAIL;
        }

        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 30000, false);

        player.getWorld().playEffect(loc, org.bukkit.Effect.EXTINGUISH, 0, 10);
        hero.addEffect(new InvisibleEffect(this, player, duration, applyText, expireText));

        moveChecker.addHero(hero);
        return SkillResult.NORMAL;
    }

    public class FadeMoveChecker implements Runnable {

        private final Map<Hero, Location> oldLocations = new HashMap<>();
        private final Skill skill;

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
                if (!hero.hasEffect("Invisible")) {
                    heroes.remove();
                    continue;
                }
                Location newLoc = hero.getPlayer().getLocation();
                if (newLoc.getWorld() != oldLoc.getWorld() || newLoc.distance(oldLoc) > SkillConfigManager.getUseSetting(hero, skill, "max-move-distance", 1.0, false)) {
                    hero.removeEffect(hero.getEffect("Invisible"));
                    heroes.remove();
                    continue;
                }

                if (newLoc.getBlock().getLightLevel() > SkillConfigManager.getUseSetting(hero, skill, "max-light-level", 8, false)) {
                    hero.removeEffect(hero.getEffect("Invisible"));
                    heroes.remove();
                    continue;
                }
                double detectRange = SkillConfigManager.getUseSetting(hero, skill, "detection-range", 0.0, false);
                for (Entity entity : hero.getPlayer().getNearbyEntities(detectRange, detectRange, detectRange)) {
                    if (entity instanceof Player) {
                        if (hero.hasParty()) {
                            Hero nearHero = plugin.getCharacterManager().getHero((Player) entity);
                            HeroParty heroParty = hero.getParty();
                            boolean isPartyMember = false;
                            for (Hero partyMember : heroParty.getMembers()) {
                                if (nearHero.equals(partyMember)) {
                                    isPartyMember = true;
                                    break;
                                }
                            }

                            if (isPartyMember)
                                return;
                        }

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
}
