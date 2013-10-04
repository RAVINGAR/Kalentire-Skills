package com.herocraftonline.heroes.characters.skill.skills;

//src=http://pastie.org/private/oeherulcmebfy0lerywsw
import org.bukkit.Bukkit;
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

import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.hooks.NCPExemptionManager;

public class SkillDropTheBass extends ActiveSkill {

    private Song skillSong;

    private boolean ncpEnabled = false;

    public SkillDropTheBass(Heroes plugin) {
        super(plugin, "DropTheBass");
        setDescription("Apply a m your close group members from taking fall damage for $1 seconds.");
        setUsage("/skill dropthebass");
        setArgumentRange(0, 0);
        setIdentifiers("skill dropthebass");
        setTypes(SkillType.ABILITY_PROPERTY_SONG, SkillType.BUFFING, SkillType.AREA_OF_EFFECT, SkillType.SILENCABLE);

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

            if (Bukkit.getServer().getPluginManager().getPlugin("NoCheatPlus") != null) {
                ncpEnabled = true;
            }
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

        node.set(SkillSetting.RADIUS.node(), Integer.valueOf(15));
        node.set(SkillSetting.DURATION.node(), Integer.valueOf(10000));
        node.set(SkillSetting.APPLY_TEXT.node(), Messaging.getSkillDenoter() + "%hero%'s party celebrates bass-drops!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), Messaging.getSkillDenoter() + "%hero%'s party no longer is dropping bass!");

        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {

        Player player = hero.getPlayer();

        broadcastExecuteText(hero);

        hero.addEffect(new SoundEffect(this, "DropTheBassSong", 100, skillSong));

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 10000, false);
        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS.node(), 15, false);

        double radiusSquared = Math.pow(radius, 2);

        if (hero.hasParty()) {
            for (Hero member : hero.getParty().getMembers()) {
                Player memberPlayer = member.getPlayer();
                if (memberPlayer.getWorld() != player.getWorld())
                    continue;

                if (memberPlayer.getLocation().distanceSquared(player.getLocation()) <= radiusSquared) {
                    member.addEffect(new NCPCompatSafeFallEffect(this, player, duration));
                }
            }
        }
        else
            hero.addEffect(new NCPCompatSafeFallEffect(this, player, duration));

        return SkillResult.NORMAL;
    }

    private class NCPCompatSafeFallEffect extends SafeFallEffect {

        public NCPCompatSafeFallEffect(Skill skill, Player applier, long duration) {
            super(skill, applier, duration);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            final Player player = hero.getPlayer();

            if (ncpEnabled)
                NCPExemptionManager.exemptPermanently(player, CheckType.MOVING_NOFALL);
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            final Player player = hero.getPlayer();

            if (ncpEnabled)
                NCPExemptionManager.unexempt(player, CheckType.MOVING_NOFALL);
        }
    }
}
