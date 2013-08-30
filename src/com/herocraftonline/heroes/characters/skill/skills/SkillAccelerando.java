package com.herocraftonline.heroes.characters.skill.skills;

// src http://pastie.org/private/syyyftinqa5r1uv4ixka
import net.minecraft.server.v1_6_R2.EntityLiving;
import net.minecraft.server.v1_6_R2.MobEffectList;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.craftbukkit.v1_6_R2.entity.CraftLivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.common.SoundEffect;
import com.herocraftonline.heroes.characters.effects.common.SoundEffect.Note;
import com.herocraftonline.heroes.characters.effects.common.SoundEffect.Song;
import com.herocraftonline.heroes.characters.effects.common.SpeedEffect;
import com.herocraftonline.heroes.characters.effects.common.StunEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

public class SkillAccelerando extends ActiveSkill {

    private String applyText;
    private String expireText;
    private Song skillSong;

    public SkillAccelerando(Heroes plugin) {
        super(plugin, "Accelerando");
        setDescription("You song boons movement speed boost to all nearby party members for $1 seconds. Players under the effects of Accelerando that are damaged are stunned for $2 seconds.");
        setUsage("/skill accelerando");
        setArgumentRange(0, 0);
        setIdentifiers("skill accelerando");
        setTypes(SkillType.BUFFING, SkillType.ABILITY_PROPERTY_SONG, SkillType.MOVEMENT_INCREASING, SkillType.AREA_OF_EFFECT, SkillType.UNINTERRUPTIBLE);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(this), plugin);

        skillSong = new Song(
                             new Note(Sound.NOTE_BASS_DRUM, 0.9F, 0.2F, 0),
                             new Note(Sound.NOTE_BASS, 0.9F, 0.5F, 1),
                             new Note(Sound.NOTE_BASS_DRUM, 0.9F, 0.9F, 2),
                             new Note(Sound.NOTE_BASS, 0.9F, 0.2F, 3),
                             new Note(Sound.NOTE_BASS_DRUM, 0.9F, 0.5F, 4),
                             new Note(Sound.NOTE_BASS_DRUM, 0.9F, 0.9F, 5),
                             new Note(Sound.NOTE_BASS, 0.9F, 0.2F, 6),
                             new Note(Sound.NOTE_BASS_DRUM, 0.9F, 0.5F, 7),
                             new Note(Sound.NOTE_BASS_DRUM, 0.9F, 0.9F, 8),
                             new Note(Sound.NOTE_BASS, 0.9F, 0.2F, 9),
                             new Note(Sound.NOTE_BASS_DRUM, 0.9F, 0.5F, 10),
                             new Note(Sound.NOTE_BASS, 0.9F, 0.9F, 11)
                );
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 3000, false);
        int stunDuration = SkillConfigManager.getUseSetting(hero, this, "stun-duration", 1500, false);

        String formattedDuration = Util.decFormat.format(duration / 1000.0);
        String formattedStunDuration = Util.decFormat.format(stunDuration / 1000.0);

        return getDescription().replace("$1", formattedDuration).replace("$2", formattedStunDuration);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set("speed-multiplier", 2);
        node.set(SkillSetting.DURATION.node(), 3000);
        node.set(SkillSetting.DELAY.node(), 1000);
        node.set(SkillSetting.RADIUS.node(), 12);
        node.set("apply-text", Messaging.getSkillDenoter() + "%hero% gained a burst of speed!");
        node.set("expire-text", Messaging.getSkillDenoter() + "%hero% returned to normal speed!");

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, Messaging.getSkillDenoter() + "%hero% gained a burst of speed!").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, Messaging.getSkillDenoter() + "%hero% returned to normal speed!").replace("%hero%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        broadcastExecuteText(hero);

        Player player = hero.getPlayer();

        hero.addEffect(new SoundEffect(this, "AccelarandoSong", 100, skillSong));

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 3000, false);
        int multiplier = SkillConfigManager.getUseSetting(hero, this, "speed-multiplier", 2, false);
        if (multiplier > 20) {
            multiplier = 20;
        }

        AccelerandoEffect accelEffect = new AccelerandoEffect(this, player, duration, multiplier, applyText, expireText);

        if (!hero.hasParty()) {
            hero.addEffect(accelEffect);
            return SkillResult.NORMAL;
        }

        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 15, false);
        int rSquared = radius * radius;

        Location loc = player.getLocation();
        //Apply the effect to all party members
        for (Hero tHero : hero.getParty().getMembers()) {
            if (!tHero.getPlayer().getWorld().equals(player.getWorld())) {
                continue;
            }

            if (loc.distanceSquared(tHero.getPlayer().getLocation()) > rSquared) {
                continue;
            }

            tHero.addEffect(accelEffect);
        }
        return SkillResult.NORMAL;
    }

    //    //Added this
    //    public class SkillSoundPlayer implements Runnable {
    //        private final Player player;
    //
    //        public SkillSoundPlayer(Hero hero) {
    //            this.player = hero.getPlayer();
    //        }
    //
    //        public void run() {
    //            World world = player.getWorld();
    //            Location loc = player.getLocation();
    //            world.playSound(loc, Sound.NOTE_BASS_DRUM, 0.9F, 0.2F);
    //            world.playSound(loc, Sound.NOTE_BASS, 0.9F, 0.5F);
    //            world.playSound(loc, Sound.NOTE_BASS_DRUM, 0.9F, 0.9F);
    //            world.playSound(loc, Sound.NOTE_BASS, 0.9F, 0.2F);
    //            world.playSound(loc, Sound.NOTE_BASS_DRUM, 0.9F, 0.5F);
    //            world.playSound(loc, Sound.NOTE_BASS_DRUM, 0.9F, 0.9F);
    //            world.playSound(loc, Sound.NOTE_BASS, 0.9F, 0.2F);
    //            world.playSound(loc, Sound.NOTE_BASS_DRUM, 0.9F, 0.5F);
    //            world.playSound(loc, Sound.NOTE_BASS_DRUM, 0.9F, 0.9F);
    //            world.playSound(loc, Sound.NOTE_BASS, 0.9F, 0.2F);
    //            world.playSound(loc, Sound.NOTE_BASS_DRUM, 0.9F, 0.5F);
    //            world.playSound(loc, Sound.NOTE_BASS, 0.9F, 0.9F);
    //        }
    //    }

    public class SkillEntityListener implements Listener {
        
        private Skill skill;
        
        public SkillEntityListener(Skill skill) {
            this.skill = skill;
        }
        
        @EventHandler(priority = EventPriority.MONITOR)
        public void onWeaponDamage(WeaponDamageEvent event) {
            if (event.isCancelled() || event.getDamage() == 0 || !(event.getEntity() instanceof Player)) {
                return;
            }

            final Hero hero = plugin.getCharacterManager().getHero((Player) event.getEntity());
            if (hero.hasEffect("Accelerando")) {
                int duration = SkillConfigManager.getUseSetting(hero, skill, "stun-duration", 1500, false);
                hero.addEffect(new StunEffect(skill, hero.getPlayer(), duration));
                hero.removeEffect(hero.getEffect("Accelerando"));
            }
        }
    }

    public class AccelerandoEffect extends SpeedEffect {

        public AccelerandoEffect(Skill skill, Player applier, int duration, int multiplier, String applyText, String expireText) {
            super(skill, "Accelerando", applier, duration, multiplier, applyText, expireText);

            types.add(EffectType.DISPELLABLE);
        }

        @Override
        public void applyToMonster(Monster monster) {
            super.applyToMonster(monster);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
        }

        @Override
        public void removeFromHero(final Hero hero) {
            Player player = hero.getPlayer();
            EntityLiving el = ((CraftLivingEntity) player).getHandle();

            if (el.hasEffect(MobEffectList.POISON) || el.hasEffect(MobEffectList.WITHER) || el.hasEffect(MobEffectList.HARM)) {
                // If they have a harmful effect present when removing the ability, delay effect removal by a bit.
                Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                    @Override
                    public void run() {
                        AccelerandoEffect.super.removeFromHero(hero);
                    }
                }, (long) (0.2 * 20));
            }
            else
                super.removeFromHero(hero);
        }

        @Override
        public void removeFromMonster(Monster monster) {
            super.removeFromMonster(monster);
        }
    }
}