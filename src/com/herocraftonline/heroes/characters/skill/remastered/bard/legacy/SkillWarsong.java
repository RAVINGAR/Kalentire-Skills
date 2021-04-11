package com.herocraftonline.heroes.characters.skill.remastered.bard.legacy;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.effects.common.SoundEffect;
import com.herocraftonline.heroes.characters.effects.common.SoundEffect.Note;
import com.herocraftonline.heroes.characters.effects.common.SoundEffect.Song;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.scheduler.BukkitRunnable;

public class SkillWarsong extends ActiveSkill {

    private Song skillSong;

    private String applyText;
    private String expireText;

    public SkillWarsong(Heroes plugin) {
        super(plugin, "Warsong");
        setDescription("Increase the melee and bow damage of party members within $1 blocks by $2% for $3 second(s).");
        setArgumentRange(0, 0);
        setUsage("/skill warsong");
        setIdentifiers("skill warsong");
        setTypes(SkillType.BUFFING, SkillType.AREA_OF_EFFECT, SkillType.ABILITY_PROPERTY_SONG);

        skillSong = new Song(
                new Note(Sound.BLOCK_NOTE_BASEDRUM, 0.8F, 2.0F, 0),
                new Note(Sound.BLOCK_NOTE_PLING, 0.8F, 2.0F, 1),
                new Note(Sound.BLOCK_NOTE_SNARE, 0.8F, 1.0F, 2),
                new Note(Sound.BLOCK_NOTE_BASS, 0.8F, 1.0F, 3)
        );

        Bukkit.getServer().getPluginManager().registerEvents(new SkillHeroListener(), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 6.0, false);
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 3000, false);

        double damageModifier = SkillConfigManager.getScaledUseSettingDouble(hero, this, "damage-bonus", 1.1, false);

        String formattedDuration = Util.decFormat.format(duration / 1000.0);
        String formattedDamageModifier = Util.decFormat.format((damageModifier - 1.0) * 100.0);

        return getDescription()
                .replace("$1", radius + "")
                .replace("$2", formattedDamageModifier)
                .replace("$3", formattedDuration);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.RADIUS.node(), 8.0);
        config.set("damage-bonus", 1.1);
        config.set("damage-bonus-increase-per-charisma", 0.0);
        config.set(SkillSetting.DURATION.node(), 3000);
        config.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "Your muscles bulge with power!");
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

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 3000, false);
        double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 6.0, false);
        double damageModifier = SkillConfigManager.getScaledUseSettingDouble(hero, this, "damage-bonus", 1.1, false);
        WarsongEffect mEffect = new WarsongEffect(this, player, duration, damageModifier);

        broadcastExecuteText(hero);

        if (!hero.hasParty()) {
            if (hero.hasEffect("Warsong")) {
                if (((WarsongEffect) hero.getEffect("Warsong")).getDamageBonus() > mEffect.getDamageBonus()) {
                    player.sendMessage("You have a more powerful effect already!");
                    return SkillResult.CANCELLED;
                }
            }

            hero.addEffect(new SoundEffect(this, "WarsongSong", 100, skillSong));
            hero.addEffect(mEffect);
        } else {
            hero.addEffect(new SoundEffect(this, "WarsongSong", 100, skillSong));

            double radiusSquared = radius * radius;
            Location loc = player.getLocation();
            for (Hero pHero : hero.getParty().getMembers()) {
                Player pPlayer = pHero.getPlayer();
                if (!pPlayer.getWorld().equals(player.getWorld()))
                    continue;
                if (pPlayer.getLocation().distanceSquared(loc) > radiusSquared)
                    continue;

                if (pHero.hasEffect("Warsong")) {
                    if (((WarsongEffect) pHero.getEffect("Warsong")).getDamageBonus() > mEffect.getDamageBonus()) {
                        continue;
                    }
                }

                pHero.addEffect(mEffect);
            }
        }

        //1.13 can't deal with these
        player.getWorld().playEffect(player.getLocation().add(0, 2.5, 0), Effect.NOTE, 3);
        player.getWorld().playEffect(player.getLocation().add(0, 2.5, 0), Effect.NOTE, 3);
        player.getWorld().playEffect(player.getLocation().add(0, 2.5, 0), Effect.NOTE, 3);

        return SkillResult.NORMAL;
    }

    public class SkillHeroListener implements Listener {

        @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
        public void onWeaponDamage(WeaponDamageEvent event) {
            if (event.getCause() != DamageCause.ENTITY_ATTACK) {
                return;
            }

            CharacterTemplate character = event.getDamager();
            if (character.hasEffect("Warsong")) {
                double damageBonus = ((WarsongEffect) character.getEffect("Warsong")).damageBonus;
                event.setDamage((event.getDamage() * damageBonus));
            }
        }
    }

    public class WarsongEffect extends ExpirableEffect {
        private final double damageBonus;

        public WarsongEffect(Skill skill, Player applier, long duration, double damageBonus) {
            super(skill, "Warsong", applier, duration, applyText, expireText);

            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.MAGIC);
            types.add(EffectType.SILENT_ACTIONS);

            this.damageBonus = damageBonus;
        }

        public double getDamageBonus() {
            return damageBonus;
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);

            Player player = hero.getPlayer();
            final Player p = player;

            if (player != this.getApplier())
                return;

            new BukkitRunnable() {
                private double time = 0;

                @Override
                public void run() {
                    Location location = p.getLocation();
                    if (time < 0.75) {
                        p.getWorld().spigot().playEffect(location, Effect.NOTE, 0, 0, 6.3F, 1.0F, 6.3F, 0.0F, 1, 16);
                        //p.getWorld().spawnParticle(Particle.NOTE, location, 1, 6.3, 1, 6.3, 0); 1.13
                    } else {
                        cancel();
                    }
                    time += 0.01;
                }
            }.runTaskTimer(plugin, 1, 4);
        }
    }
}