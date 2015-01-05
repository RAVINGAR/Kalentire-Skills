package com.herocraftonline.heroes.characters.skill.skills;

//src=http://pastie.org/private/oeherulcmebfy0lerywsw
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.SafeFallEffect;
import com.herocraftonline.heroes.characters.effects.common.SoundEffect;
import com.herocraftonline.heroes.characters.effects.common.SoundEffect.Note;
import com.herocraftonline.heroes.characters.effects.common.SoundEffect.Song;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

public class SkillDropTheBass extends ActiveSkill {

    private Song skillSong;

    public SkillDropTheBass(Heroes plugin) {
        super(plugin, "DropTheBass");
        setDescription("Stops your close group members from taking fall damage for $1 seconds.");
        setUsage("/skill dropthebass");
        setArgumentRange(0, 0);
        setIdentifiers("skill dropthebass");
        setTypes(SkillType.ABILITY_PROPERTY_SONG, SkillType.BUFFING, SkillType.AREA_OF_EFFECT, SkillType.SILENCEABLE);

        skillSong = new Song(
                             new Note(Sound.NOTE_BASS, 0.8F, 1.0F, 0),
                             new Note(Sound.NOTE_BASS, 0.8F, 2.0F, 1),
                             new Note(Sound.NOTE_BASS, 0.8F, 3.0F, 2),
                             new Note(Sound.NOTE_BASS, 0.8F, 4.0F, 3),
                             new Note(Sound.NOTE_BASS, 0.8F, 5.0F, 4),
                             new Note(Sound.NOTE_BASS_GUITAR, 0.8F, 6.0F, 5),
                             new Note(Sound.NOTE_BASS_DRUM, 0.8F, 7.0F, 6),
                             new Note(Sound.NOTE_BASS, 0.8F, 8.0F, 7)
                );
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 10000, false);

        String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription().replace("$1", formattedDuration);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.RADIUS.node(), 15);
        node.set(SkillSetting.DURATION.node(), 10000);
        node.set(SkillSetting.APPLY_TEXT.node(), Messaging.getSkillDenoter() + "%hero%'s party celebrates bass-drops!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), Messaging.getSkillDenoter() + "%hero%'s party no longer is dropping bass!");

        return node;
    }

    @Override
    public SkillResult use(final Hero hero, String[] args) {

        final Skill theSkill = this;
        final Player player = hero.getPlayer();

        broadcastExecuteText(hero);

        hero.addEffect(new SoundEffect(this, "DropTheBassSong", 100, skillSong));

        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 10000, false);
        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS.node(), 15, false);

        double radiusSquared = Math.pow(radius, 2);

        if (hero.hasParty()) {
            for (final Hero member : hero.getParty().getMembers()) {
                Player memberPlayer = member.getPlayer();
                if (memberPlayer.getWorld() != player.getWorld())
                    continue;

                if (memberPlayer.getLocation().distanceSquared(player.getLocation()) <= radiusSquared) {
                    member.addEffect(new SafeFallEffect(theSkill, player, duration));
                }
            }
        }
        else {
            hero.addEffect(new SafeFallEffect(theSkill, player, duration));
        }

        player.getWorld().playEffect(player.getLocation().add(0, 2.5, 0), org.bukkit.Effect.NOTE, 3);
        player.getWorld().playEffect(player.getLocation().add(0, 2.5, 0), org.bukkit.Effect.NOTE, 3);
        player.getWorld().playEffect(player.getLocation().add(0, 2.5, 0), org.bukkit.Effect.NOTE, 3);

        return SkillResult.NORMAL;
    }
}
