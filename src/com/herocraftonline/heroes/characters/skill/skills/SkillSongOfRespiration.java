package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.WaterBreathingEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

public class SkillSongOfRespiration extends ActiveSkill {

    private String applyText;
    private String expireText;

    public SkillSongOfRespiration(Heroes plugin) {
        super(plugin, "SongOfRespiration");
        setDescription("You sing a song of respiration, granting water breathing to all party members within $1 blocks for $2 seconds..");
        setUsage("/skill songofrespiration");
        setIdentifiers("skill songofrespiration");
        setTypes(SkillType.AREA_OF_EFFECT, SkillType.BUFFING, SkillType.ABILITY_PROPERTY_AIR, SkillType.ABILITY_PROPERTY_SONG);
        setArgumentRange(0, 0);
    }

    public String getDescription(Hero hero) {
        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, Integer.valueOf(15), false);

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 2500, false);
        int durationIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE_PER_CHARISMA, 38, false);
        duration += hero.getAttributeValue(AttributeType.CHARISMA) * durationIncrease;

        String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription().replace("$1", radius + "").replace("$2", formattedDuration);
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.RADIUS.node(), 7);
        node.set(SkillSetting.DURATION.node(), 6000);
        node.set(SkillSetting.DURATION_INCREASE_PER_CHARISMA.node(), 225);
        node.set(SkillSetting.DELAY.node(), 1000);
        node.set(SkillSetting.APPLY_TEXT.node(), Messaging.getSkillDenoter() + "You are filled with increased respiration!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), Messaging.getSkillDenoter() + "Your increased respiration has faded.");

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, Messaging.getSkillDenoter() + "You are filled with increased respiration!").replace("%hero%", "$2").replace("%target%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, Messaging.getSkillDenoter() + "Your increased respiration has faded.").replace("%hero%", "$2").replace("%target%", "$1");
    }

    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 2500, false);
        int durationIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE_PER_CHARISMA, 38, false);
        duration += hero.getAttributeValue(AttributeType.CHARISMA) * durationIncrease;

        //hero.addEffect(new SoundEffect(this, "SongofRespirationSong", 100, skillSong));
        broadcastExecuteText(hero);

        SongOfRespirationWaterBreathingEffect wbEffect = new SongOfRespirationWaterBreathingEffect(this, player, duration);

        if (!hero.hasParty())
            hero.addEffect(wbEffect);
        else {
            int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, Integer.valueOf(15), false);
            int radiusSquared = radius * radius;

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

        return SkillResult.NORMAL;
    }

    private class SongOfRespirationWaterBreathingEffect extends WaterBreathingEffect {

        public SongOfRespirationWaterBreathingEffect(Skill skill, Player applier, long duration) {
            super(skill, applier, duration, null, null);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);

            Player player = hero.getPlayer();
            if (applyText != null && applyText.length() > 0) {
                Messaging.send(player, "    " + applyText, player.getName(), applier.getName());
            }
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);

            Player player = hero.getPlayer();
            if (expireText != null && expireText.length() > 0) {
                Messaging.send(player, "    " + expireText, player.getName(), applier.getName());
            }
        }
    }
}