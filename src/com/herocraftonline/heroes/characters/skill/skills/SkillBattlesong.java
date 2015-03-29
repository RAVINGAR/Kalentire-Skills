package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainStaminaEvent;
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

public class SkillBattlesong extends ActiveSkill {

    private Song skillSong;

    private String applyText;
    private String expireText;

    public SkillBattlesong(Heroes plugin) {
        super(plugin, "Battlesong");
        setDescription("Play a song of battle for $1 seconds. While active, you regenerate $2 stamina for party members within $3 blocks every $4 seconds.");
        setArgumentRange(0, 0);
        setUsage("/skill battlesong");
        setIdentifiers("skill battlesong");
        setTypes(SkillType.STAMINA_INCREASING, SkillType.BUFFING, SkillType.AREA_OF_EFFECT, SkillType.ABILITY_PROPERTY_SONG);

        skillSong = new Song(
                new Note(Sound.NOTE_BASS, 0.8F, 1.0F, 0),
                new Note(Sound.NOTE_BASS, 1.0F, 0.7F, 1),
                new Note(Sound.NOTE_BASS, 1.2F, 0.4F, 2),
                new Note(Sound.NOTE_BASS, 0.8F, 0.2F, 3)
        );
    }

    @Override
    public String getDescription(Hero hero) {
        int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 1500, false);
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 3000, false);
        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 6, false);

        int staminaRestoreTick = SkillConfigManager.getUseSetting(hero, this, "stamina-restore-tick", 12, false);
        double staminaRestoreTickIncrease = SkillConfigManager.getUseSetting(hero, this, "stamina-restore-tick-increase-per-charisma", 0.15, false);
        staminaRestoreTick += (int) (staminaRestoreTickIncrease * hero.getAttributeValue(AttributeType.CHARISMA));

        String formattedPeriod = Util.decFormat.format(period / 1000.0);
        String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription().replace("$1", formattedDuration).replace("$2", staminaRestoreTick + "").replace("$3", radius + "").replace("$4", formattedPeriod);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.RADIUS.node(), 12);
        node.set("stamina-restore-tick", 7);
        node.set("stamina-restore-tick-increase-per-charisma", 0.275);
        node.set(SkillSetting.PERIOD.node(), 1500);
        node.set(SkillSetting.DURATION.node(), 3000);
        node.set(SkillSetting.APPLY_TEXT.node(), Messaging.getSkillDenoter() + "You are filled with ");
        node.set(SkillSetting.EXPIRE_TEXT.node(), Messaging.getSkillDenoter() + "You feel strength leave your body!");
        node.set(SkillSetting.DELAY.node(), 1000);

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, Messaging.getSkillDenoter() + "Your muscles bulge with power!").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, Messaging.getSkillDenoter() + "You feel strength leave your body!").replace("%hero%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {

        Player player = hero.getPlayer();

        hero.addEffect(new SoundEffect(this, "BattlesongSong", 100, skillSong));

        int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 1500, false);
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 3000, false);
        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 6, false);

        int staminaRestoreTick = SkillConfigManager.getUseSetting(hero, this, "stamina-restore-tick", 12, false);
        double staminaRestoreTickIncrease = SkillConfigManager.getUseSetting(hero, this, "stamina-restore-tick-increase-per-charisma", 0.15, false);
        staminaRestoreTick += (int) Math.floor((staminaRestoreTickIncrease * hero.getAttributeValue(AttributeType.CHARISMA)));

        BattlesongEffect mEffect = new BattlesongEffect(this, hero.getPlayer(), period, duration, radius, staminaRestoreTick);
        hero.addEffect(mEffect);

        broadcastExecuteText(hero);

        player.getWorld().playEffect(player.getLocation().add(0, 2.5, 0), org.bukkit.Effect.NOTE, 3);
        player.getWorld().playEffect(player.getLocation().add(0, 2.5, 0), org.bukkit.Effect.NOTE, 3);
        player.getWorld().playEffect(player.getLocation().add(0, 2.5, 0), org.bukkit.Effect.NOTE, 3);

        return SkillResult.NORMAL;
    }

    public class BattlesongEffect extends PeriodicExpirableEffect {

        private final int radius;
        private final int staminaRestore;

        public BattlesongEffect(Skill skill, Player applier, int period, int duration, int radius, int staminaRestore) {
            super(skill, "Battlesong", applier, period, duration, null, null);

            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.MAGIC);
            types.add(EffectType.STAMINA_INCREASING);

            this.radius = radius;
            this.staminaRestore = staminaRestore;
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);

            Player player = hero.getPlayer();
            final Player p = player;

            if (player == this.getApplier())
            {
                new BukkitRunnable() {

                    private double time = 0;

                    @SuppressWarnings("deprecation")
                    @Override
                    public void run()
                    {
                        Location location = p.getLocation();
                        if (time < 0.8)
                        {
                            p.getWorld().spigot().playEffect(location, Effect.NOTE, 0, 0, 6.3F, 1.0F, 6.3F, 0.0F, 1, 16);
                        }
                        else
                        {
                            cancel();
                        }
                        time += 0.01;
                    }
                }.runTaskTimer(plugin, 1, 4);
            }

            Messaging.send(player, "    " + applyText, player.getName());
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);

            Player player = hero.getPlayer();

            Messaging.send(player, "    " + expireText, player.getName());
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
                            HeroRegainStaminaEvent hrsEvent = new HeroRegainStaminaEvent(member, staminaRestore, skill);
                            plugin.getServer().getPluginManager().callEvent(hrsEvent);
                            if (!hrsEvent.isCancelled()) {
                                member.getPlayer().getWorld().spigot().playEffect(member.getPlayer().getLocation(), org.bukkit.Effect.VILLAGER_THUNDERCLOUD, 0, 0, 0.5F, 1.0F, 0.5F, 0.3F, 10, 16);
                                member.setStamina(hrsEvent.getAmount() + member.getStamina());
                            }
                        }
                    }
                }
            }
            else {
                HeroRegainStaminaEvent hrsEvent = new HeroRegainStaminaEvent(hero, staminaRestore, skill);
                plugin.getServer().getPluginManager().callEvent(hrsEvent);
                if (!hrsEvent.isCancelled()) {
                    hero.getPlayer().getWorld().spigot().playEffect(hero.getPlayer().getLocation(), org.bukkit.Effect.VILLAGER_THUNDERCLOUD, 0, 0, 0.5F, 1.0F, 0.5F, 0.3F, 10, 16);

                    hero.setStamina(hrsEvent.getAmount() + hero.getStamina());
                }
            }
        }

        @Override
        public void tickMonster(Monster monster) {}
    }
}