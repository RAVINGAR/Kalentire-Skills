package com.herocraftonline.heroes.characters.skill.unfinishedskills;

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

import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.hooks.NCPExemptionManager;

public class SkillDropTheBass extends ActiveSkill {

    private Song skillSong;

    private boolean ncpEnabled = false;

    public SkillDropTheBass(Heroes plugin) {
        super(plugin, "DropTheBass");
        setDescription("Stops your close group members from taking fall damage for $1 seconds.");
        setUsage("/skill dropthebass");
        setArgumentRange(0, 0);
        setIdentifiers("skill dropthebass");
        setTypes(SkillType.MOVEMENT, SkillType.BUFF, SkillType.SILENCABLE);

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

        try {
            if (Bukkit.getServer().getPluginManager().getPlugin("NoCheatPlus") != null) {
                ncpEnabled = true;
            }
        }
        catch (Exception e) {}
    }

    @Override
    public String getDescription(Hero hero) {
        String description = getDescription();
        //DURATION
        int duration = (SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 10000, false)
                + SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE.node(), 0, false) * hero.getSkillLevel(this)) / 1000;
        if (duration > 0) {
            description += " D:" + duration + "s";
        }

        //RADIUS
        int radius = (int) ((SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS.node(), 15.0, false)
                + SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS_INCREASE.node(), 0, false) * hero.getSkillLevel(this)));
        if (duration > 0) {
            description += " R:" + radius + "s";
        }

        //COOLDOWN
        int cooldown = (SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN.node(), 0, false)
                - SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN_REDUCE.node(), 0, false) * hero.getSkillLevel(this)) / 1000;
        if (cooldown > 0) {
            description += " CD:" + cooldown + "s";
        }

        //MANA
        int mana = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MANA.node(), 0, false)
                - (SkillConfigManager.getUseSetting(hero, this, SkillSetting.MANA_REDUCE.node(), 0, false) * hero.getSkillLevel(this));
        if (mana > 0) {
            description += " M:" + mana;
        }

        //HEALTH_COST
        int healthCost = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALTH_COST, 0, false) -
                (SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALTH_COST_REDUCE, mana, true) * hero.getSkillLevel(this));
        if (healthCost > 0) {
            description += " HP:" + healthCost;
        }

        //STAMINA
        int staminaCost = SkillConfigManager.getUseSetting(hero, this, SkillSetting.STAMINA.node(), 0, false)
                - (SkillConfigManager.getUseSetting(hero, this, SkillSetting.STAMINA_REDUCE.node(), 0, false) * hero.getSkillLevel(this));
        if (staminaCost > 0) {
            description += " FP:" + staminaCost;
        }

        //DELAY
        int delay = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DELAY.node(), 0, false) / 1000;
        if (delay > 0) {
            description += " W:" + delay + "s";
        }

        //EXP
        int exp = SkillConfigManager.getUseSetting(hero, this, SkillSetting.EXP.node(), 0, false);
        if (exp > 0) {
            description += " XP:" + exp;
        }
        return description.replace("$1", duration / 1000 + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.APPLY_TEXT.node(), "%hero%'s party celebrates bass-drops!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "%hero%'s party no longer is dropping bass!");
        node.set(SkillSetting.DURATION.node(), 10000);
        node.set(SkillSetting.DURATION_INCREASE.node(), 0);
        node.set(SkillSetting.RADIUS.node(), 15.0);
        node.set(SkillSetting.RADIUS_INCREASE.node(), 0.0);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {

        Player player = hero.getPlayer();

        int duration = (SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 10000, false)
                + SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE.node(), 0, false) * hero.getSkillLevel(this));
        int radius = (int) ((SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS.node(), 15.0, false)
                + SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS_INCREASE.node(), 0, false) * hero.getSkillLevel(this)));

        double radiusSquared = Math.pow(radius, 2);

        if (hero.hasParty()) {
            for (Hero member : hero.getParty().getMembers()) {
                Player memberPlayer = member.getPlayer();
                if (memberPlayer.getWorld() != player.getWorld())
                    continue;

                if (memberPlayer.getLocation().distanceSquared(player.getLocation()) <= radiusSquared) {
                    member.addEffect(new NCPCompatSafeFallEffect(this, duration));
                }
            }
        }
        else {
            hero.addEffect(new NCPCompatSafeFallEffect(this, duration));
        }

        broadcastExecuteText(hero);

        hero.addEffect(new SoundEffect(this, "DropTheBassSong", 100, skillSong));

        return SkillResult.NORMAL;
    }

    private class NCPCompatSafeFallEffect extends SafeFallEffect {

        public NCPCompatSafeFallEffect(Skill skill, long duration) {
            super(skill, duration);
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
