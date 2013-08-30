package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainManaEvent;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
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

public class SkillManasong extends ActiveSkill {

    private Song skillSong;

    public SkillManasong(Heroes plugin) {
        super(plugin, "Manasong");
        setDescription("Play a song of mana for $1 seconds. While active, you regenerate $2 mana for party members within $3 blocks every $4 seconds.");
        setArgumentRange(0, 0);
        setUsage("/skill manasong");
        setIdentifiers("skill manasong");
        setTypes(SkillType.MANA_INCREASING, SkillType.ABILITY_PROPERTY_SONG, SkillType.UNINTERRUPTIBLE);

        skillSong = new Song(
                             new Note(Sound.NOTE_PIANO, 0.8F, 1.0F, 0),
                             new Note(Sound.NOTE_BASS, 0.8F, 1.0F, 1)
                );
    }

    @Override
    public String getDescription(Hero hero) {
        int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, Integer.valueOf(1500), false);
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, Integer.valueOf(3000), false);
        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, Integer.valueOf(6), false);

        int manaRegenTick = SkillConfigManager.getUseSetting(hero, this, "mana-regen-tick", Integer.valueOf(12), false);
        double manaRegenTickIncrease = SkillConfigManager.getUseSetting(hero, this, "mana-regen-tick-increase-per-charisma", Double.valueOf(0.15), false);
        manaRegenTick += (int) (manaRegenTickIncrease * hero.getAttributeValue(AttributeType.CHARISMA));

        String formattedPeriod = Util.decFormat.format(period / 1000.0);
        String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription().replace("$1", formattedDuration).replace("$2", manaRegenTick + "").replace("$3", radius + "").replace("$4", formattedPeriod);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.RADIUS.node(), Integer.valueOf(6));
        node.set("mana-regen-tick", Integer.valueOf(12));
        node.set("mana-regen-tick-increase-per-charisma", Double.valueOf(0.15));
        node.set(SkillSetting.PERIOD.node(), Integer.valueOf(1500));
        node.set(SkillSetting.DURATION.node(), Integer.valueOf(3000));
        node.set(SkillSetting.DELAY.node(), Integer.valueOf(1000));

        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {

        hero.addEffect(new SoundEffect(this, "ManaSong", 100, skillSong));

        int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, Integer.valueOf(1500), false);
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, Integer.valueOf(3000), false);
        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, Integer.valueOf(6), false);

        int manaRegenTick = SkillConfigManager.getUseSetting(hero, this, "mana-regen-tick", Integer.valueOf(12), false);
        double manaRegenTickIncrease = SkillConfigManager.getUseSetting(hero, this, "mana-regen-tick-increase-per-charisma", Double.valueOf(0.15), false);
        manaRegenTick += (int) (manaRegenTickIncrease * hero.getAttributeValue(AttributeType.CHARISMA));

        ManasongEffect mEffect = new ManasongEffect(this, hero.getPlayer(), period, duration, radius, manaRegenTick);
        hero.addEffect(mEffect);

        broadcastExecuteText(hero);

        return SkillResult.NORMAL;
    }


    public class ManasongEffect extends PeriodicExpirableEffect {

        private final int radius;
        private final int manaRegen;

        public ManasongEffect(Skill skill, Player applier, int period, int duration, int radius, int manaRegen) {
            super(skill, "Manasong", applier, period, duration);

            this.radius = radius;
            this.manaRegen = manaRegen;

            this.types.add(EffectType.BENEFICIAL);
            this.types.add(EffectType.MAGIC);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
        }

        @Override
        public void tickHero(Hero hero) {
            Player player = hero.getPlayer();

            if (hero.hasParty()) {
                int radiusSquared = radius * radius;
                Location playerLocation = player.getLocation();
                // Loop through the player's party members and heal as necessary
                for (Hero member : hero.getParty().getMembers()) {
                    Location memberLocation = member.getPlayer().getLocation();

                    // Ensure the party member is close enough
                    if (memberLocation.getWorld().equals(playerLocation.getWorld())) {
                        if (memberLocation.distanceSquared(playerLocation) <= radiusSquared) {
                            HeroRegainManaEvent hrmEvent = new HeroRegainManaEvent(member, manaRegen, skill);
                            plugin.getServer().getPluginManager().callEvent(hrmEvent);
                            if (!hrmEvent.isCancelled()) {
                                member.setMana(hrmEvent.getAmount() + member.getMana());

                                if (member.isVerbose())
                                    Messaging.send(player, Messaging.createManaBar(member.getMana(), member.getMaxMana()));
                            }
                        }
                    }
                }
            }
            else {
                HeroRegainManaEvent hrmEvent = new HeroRegainManaEvent(hero, manaRegen, skill);
                plugin.getServer().getPluginManager().callEvent(hrmEvent);
                if (!hrmEvent.isCancelled()) {
                    hero.setMana(hrmEvent.getAmount() + hero.getMana());

                    if (hero.isVerbose())
                        Messaging.send(player, Messaging.createManaBar(hero.getMana(), hero.getMaxMana()));
                }
            }
        }

        @Override
        public void tickMonster(Monster monster) {}
    }
}
