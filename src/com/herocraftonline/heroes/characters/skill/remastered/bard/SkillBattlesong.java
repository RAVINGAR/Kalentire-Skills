package com.herocraftonline.heroes.characters.skill.remastered.bard;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainManaEvent;
import com.herocraftonline.heroes.api.events.HeroRegainStaminaEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.effects.common.SoundEffect;
import com.herocraftonline.heroes.characters.effects.common.SoundEffect.Note;
import com.herocraftonline.heroes.characters.effects.common.SoundEffect.Song;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class SkillBattlesong extends ActiveSkill {

    private final Song skillSong;

    private String applyText;
    private String expireText;

    public SkillBattlesong(Heroes plugin) {
        super(plugin, "Battlesong");
        setDescription("Play a song of battle for $1 second(s). While active, you regenerate $2 stamina and $3 for " +
                "party members within $4 blocks every $5 second(s).");
        setArgumentRange(0, 0);
        setUsage("/skill battlesong");
        setIdentifiers("skill battlesong");
        setTypes(SkillType.STAMINA_INCREASING, SkillType.MANA_INCREASING, SkillType.BUFFING, SkillType.AREA_OF_EFFECT, SkillType.ABILITY_PROPERTY_SONG);

        skillSong = new Song(
                new Note(Sound.BLOCK_NOTE_BLOCK_BASS, 0.8F, 1.0F, 0),
                new Note(Sound.BLOCK_NOTE_BLOCK_BASS, 1.0F, 0.7F, 1),
                new Note(Sound.BLOCK_NOTE_BLOCK_BASS, 1.2F, 0.4F, 2),
                new Note(Sound.BLOCK_NOTE_BLOCK_BASS, 0.8F, 0.2F, 3)
        );
    }

    @Override
    public String getDescription(Hero hero) {
        int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 1500, false);
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 3000, false);
        int radius = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.RADIUS, false);

        int staminaRestoreTick = SkillConfigManager.getScaledUseSettingInt(hero, this, "stamina-restore-tick",  false);
        int manaRestoreTick = SkillConfigManager.getScaledUseSettingInt(hero, this, "mana-restore-tick", false);

        return getDescription()
                .replace("$1", Util.decFormat.format(duration / 1000.0))
                .replace("$2", staminaRestoreTick + "")
                .replace("$3", manaRestoreTick + "")
                .replace("$4", radius + "")
                .replace("$5", Util.decFormat.format(period / 1000.0));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.RADIUS.node(), 12);
        config.set("stamina-restore-tick", 7);
        config.set("mana-restore-tick", 7);
        config.set("stamina-restore-tick-increase-per-charisma", 0.275);
        config.set("mana-restore-tick-increase-per-charisma", 0.275);
        config.set(SkillSetting.PERIOD.node(), 1500);
        config.set(SkillSetting.DURATION.node(), 3000);
        config.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "You are filled with ");
        config.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "You feel strength leave your body!");
        config.set(SkillSetting.DELAY.node(), 1000);
        return config;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "Your muscles bulge with power!");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "You feel strength leave your body!");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        hero.addEffect(new SoundEffect(this, "BattlesongSong", 100, skillSong));

        int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 1500, false);
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 3000, false);
        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 6, false);

        int staminaRestoreTick = SkillConfigManager.getScaledUseSettingInt(hero, this, "stamina-restore-tick", false);
        int manaRestoreTick = SkillConfigManager.getScaledUseSettingInt(hero, this, "mana-restore-tick",  false);

        BattlesongEffect mEffect = new BattlesongEffect(this, player, period, duration, radius, staminaRestoreTick, manaRestoreTick);
        hero.addEffect(mEffect);

        broadcastExecuteText(hero);

        //FIXME Is it a particle or a sound
//        player.getWorld().playEffect(player.getLocation().add(0, 2.5, 0), org.bukkit.Effect.NOTE, 3);
//        player.getWorld().playEffect(player.getLocation().add(0, 2.5, 0), org.bukkit.Effect.NOTE, 3);
//        player.getWorld().playEffect(player.getLocation().add(0, 2.5, 0), org.bukkit.Effect.NOTE, 3);

        return SkillResult.NORMAL;
    }

    public class BattlesongEffect extends PeriodicExpirableEffect {
        private final int radius;
        private final int staminaRestore;
        private final int manaRestore;

        public BattlesongEffect(Skill skill, Player applier, int period, int duration, int radius,
                                int staminaRestore, int manaRestore) {
            super(skill, "Battlesong", applier, period, duration, null, null);
            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.MAGIC);
            types.add(EffectType.STAMINA_INCREASING);
            types.add(EffectType.MANA_INCREASING);

            this.radius = radius;
            this.staminaRestore = staminaRestore;
            this.manaRestore = manaRestore;
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);

            Player player = hero.getPlayer();
            final Player p = player;

            if (player == this.getApplier()) {
                new BukkitRunnable() {
                    private double time = 0;

                    @Override
                    public void run() {
                        Location location = p.getLocation();
                        if (time < 0.8) {
                            //p.getWorld().spigot().playEffect(location, Effect.NOTE, 0, 0, 6.3F, 1.0F, 6.3F, 0.0F, 1, 16);
                            p.getWorld().spawnParticle(Particle.NOTE, location, 1, 6.3, 1, 6.3, 0);
                        }
                        else {
                            cancel();
                        }
                        time += 0.01;
                    }
                }.runTaskTimer(plugin, 1, 4);
            }

            player.sendMessage("    " + applyText.replace("%hero%", player.getName()));
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);

            Player player = hero.getPlayer();
            player.sendMessage("    " + expireText.replace("%hero%", player.getName()));
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
                    if (memberLocation.getWorld().equals(playerLocation.getWorld())
                            && memberLocation.distanceSquared(playerLocation) <= radiusSquared) {
                        if (hero.getStamina() < hero.getMaxStamina()) {
                            tryRegainStamina(member);
                        }
                        if (hero.getMana() < hero.getMaxMana()) {
                            tryRegainMana(member);
                        }
                    }
                }
            }
            else {
                if (hero.getStamina() < hero.getMaxStamina()) {
                    tryRegainStamina(hero);
                }
                if (hero.getMana() < hero.getMaxMana()) {
                    tryRegainMana(hero);
                }
            }
        }

        public void tryRegainStamina(Hero hero) {
            HeroRegainStaminaEvent hrsEvent = new HeroRegainStaminaEvent(hero, staminaRestore, skill);
            plugin.getServer().getPluginManager().callEvent(hrsEvent);
            if (hrsEvent.isCancelled())
                return;

            final Player player = hero.getPlayer();
            hero.setStamina(hrsEvent.getDelta() + hero.getStamina());
            //player.getWorld().spigot().playEffect(player.getLocation(), org.bukkit.Effect.VILLAGER_THUNDERCLOUD, 0, 0, 0.5F, 1.0F, 0.5F, 0.3F, 10, 16);
            player.getWorld().spawnParticle(Particle.VILLAGER_ANGRY, player.getLocation(), 10, 0.5, 1, 0.5, 0.3);

            if (hero.isVerboseStamina())
                player.sendMessage(ChatComponents.Bars.stamina(hero.getStamina(), hero.getMaxStamina(), false));
        }

        public void tryRegainMana(Hero hero) {
            HeroRegainManaEvent hrmEvent = new HeroRegainManaEvent(hero, manaRestore, skill);
            plugin.getServer().getPluginManager().callEvent(hrmEvent);
            if (hrmEvent.isCancelled())
                return;

            final Player player = hero.getPlayer();
            hero.setMana(hrmEvent.getDelta() + hero.getMana());
            //player.getWorld().spigot().playEffect(player.getLocation(), Effect.SPLASH, 0, 0, 0.5F, 0.5F, 0.5F, 0, 20, 16);
            player.getWorld().spawnParticle(Particle.WATER_SPLASH, player.getLocation(), 20, 0.5, 0.5, 0.5, 0);

            if (hero.isVerboseMana())
                player.sendMessage(ChatComponents.Bars.mana(hero.getMana(), hero.getMaxMana(), false));
        }

        @Override
        public void tickMonster(Monster monster) {}
    }
}