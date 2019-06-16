package com.herocraftonline.heroes.characters.skill.reborn.bard;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.WaterBreathingEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class SkillSongOfRespiration extends ActiveSkill {
    private String applyText;
    private String expireText;

    public SkillSongOfRespiration(Heroes plugin) {
        super(plugin, "SongOfRespiration");
        setDescription("You sing a song of respiration, granting water breathing to all party members within $1 blocks for $2 second(s).");
        setUsage("/skill songofrespiration");
        setIdentifiers("skill songofrespiration");
        setArgumentRange(0, 0);

        setTypes(SkillType.AREA_OF_EFFECT, SkillType.BUFFING, SkillType.ABILITY_PROPERTY_AIR, SkillType.ABILITY_PROPERTY_SONG);
    }

    public String getDescription(Hero hero) {
        double radius = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.RADIUS, 15.0, false);
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 2500, false);

        String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription()
                .replace("$1", Util.decFormat.format(radius))
                .replace("$2", formattedDuration);
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.RADIUS.node(), 12.0);
        config.set(SkillSetting.DURATION.node(), 6000);
        config.set(SkillSetting.DELAY.node(), 1000);
        config.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "You are filled with increased respiration!");
        config.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "Your increased respiration has faded.");
        return config;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "You are filled with increased respiration!");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "Your increased respiration has faded.");
    }

    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 2500, false);

        //hero.addEffect(new SoundEffect(this, "SongofRespirationSong", 100, skillSong));
        broadcastExecuteText(hero);

        SongOfRespirationWaterBreathingEffect wbEffect = new SongOfRespirationWaterBreathingEffect(this, player, duration);

        if (!hero.hasParty()) {
            hero.addEffect(wbEffect);
        } else {
            double radius = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.RADIUS, false);
            double radiusSquared = radius * radius;

            Location loc = player.getLocation();

            for (Hero pHero : hero.getParty().getMembers()) {
                Player pPlayer = pHero.getPlayer();
                if (!pPlayer.getWorld().equals(player.getWorld())) {
                    continue;
                }
                if (pPlayer.getLocation().distanceSquared(loc) > radiusSquared) {
                    continue;
                }

                pHero.addEffect(wbEffect);
            }
        }

        //1.13 no put this there
        player.getWorld().playEffect(player.getLocation().add(0, 2.5, 0), org.bukkit.Effect.NOTE, 3);
        player.getWorld().playEffect(player.getLocation().add(0, 2.5, 0), org.bukkit.Effect.NOTE, 3);
        player.getWorld().playEffect(player.getLocation().add(0, 2.5, 0), org.bukkit.Effect.NOTE, 3);

        return SkillResult.NORMAL;
    }

    private class SongOfRespirationWaterBreathingEffect extends WaterBreathingEffect {

        public SongOfRespirationWaterBreathingEffect(Skill skill, Player applier, long duration) {
            super(skill, applier, duration, applyText, expireText);
        }
    }
}