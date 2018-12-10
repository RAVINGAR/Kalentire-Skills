package com.herocraftonline.heroes.characters.skill.pack3;

// src http://pastie.org/private/syyyftinqa5r1uv4ixka
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.SkillDamageEvent;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.common.SoundEffect;
import com.herocraftonline.heroes.characters.effects.common.SoundEffect.Note;
import com.herocraftonline.heroes.characters.effects.common.SoundEffect.Song;
import com.herocraftonline.heroes.characters.effects.common.SpeedEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

//import com.herocraftonline.heroes.characters.skill.animations.AreaOfEffectAnimation;

//import de.slikey.effectlib.EffectManager;

public class SkillAccelerando extends ActiveSkill {

    private String applyText;
    private String expireText;

    private Song skillSong;

    public SkillAccelerando(Heroes plugin) {
        super(plugin, "Accelerando");
        setDescription("Your song boons movement speed boost to all nearby party members for $1 seconds. The effect is removed on damage.");
        setUsage("/skill accelerando");
        setArgumentRange(0, 0);
        setIdentifiers("skill accelerando");
        setTypes(SkillType.BUFFING, SkillType.ABILITY_PROPERTY_SONG, SkillType.MOVEMENT_INCREASING, SkillType.AREA_OF_EFFECT);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(), plugin);

        skillSong = new Song(
                new Note(Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 0.9F, 0.2F, 0),
                new Note(Sound.BLOCK_NOTE_BLOCK_BASS, 0.9F, 0.5F, 1),
                new Note(Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 0.9F, 0.9F, 2),
                new Note(Sound.BLOCK_NOTE_BLOCK_BASS, 0.9F, 0.2F, 3),
                new Note(Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 0.9F, 0.5F, 4),
                new Note(Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 0.9F, 0.9F, 5),
                new Note(Sound.BLOCK_NOTE_BLOCK_BASS, 0.9F, 0.2F, 6),
                new Note(Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 0.9F, 0.5F, 7),
                new Note(Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 0.9F, 0.9F, 8),
                new Note(Sound.BLOCK_NOTE_BLOCK_BASS, 0.9F, 0.2F, 9),
                new Note(Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 0.9F, 0.5F, 10),
                new Note(Sound.BLOCK_NOTE_BLOCK_BASS, 0.9F, 0.9F, 11)
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

        node.set(SkillSetting.RADIUS.node(), 12);
        node.set("speed-multiplier", 2);
        node.set(SkillSetting.DURATION.node(), 3000);
        node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% gained a burst of speed!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% returned to normal speed!");
        node.set(SkillSetting.DELAY.node(), 1000);
        node.set(SkillSetting.COOLDOWN.node(), 1000);

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%hero% gained a burst of speed!");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%hero% returned to normal speed!");
    }

    public ArrayList<Location> circle(Location centerPoint, int particleAmount, double circleRadius)
    {
        World world = centerPoint.getWorld();

        double increment = (2 * Math.PI) / particleAmount;

        ArrayList<Location> locations = new ArrayList<Location>();

        for (int i = 0; i < particleAmount; i++)
        {
            double angle = i * increment;
            double x = centerPoint.getX() + (circleRadius * Math.cos(angle));
            double z = centerPoint.getZ() + (circleRadius * Math.sin(angle));
            locations.add(new Location(world, x, centerPoint.getY(), z));
        }
        return locations;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {

        Player player = hero.getPlayer();

        broadcastExecuteText(hero);

        hero.addEffect(new SoundEffect(this, "AccelarandoSong", 100, skillSong));

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 3000, false);
        int multiplier = SkillConfigManager.getUseSetting(hero, this, "speed-multiplier", 2, false);

        AccelerandoEffect accelEffect = new AccelerandoEffect(this, player, duration, multiplier);

        if (!hero.hasParty()) {
            hero.addEffect(accelEffect);
            return SkillResult.NORMAL;
        }

        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 15, false);
        int rSquared = radius * radius;

        Location playerLoc = player.getLocation();

        List<Location> circle = circle(playerLoc, 72, radius);
        for (int i = 0; i < circle.size(); i++)
        {
            //player.getWorld().spigot().playEffect(circle(player.getLocation(), 72, radius).get(i), org.bukkit.Effect.NOTE, 0, 0, 0, 0.2F, 0, 1, 1, 20);
            player.getWorld().spawnParticle(Particle.NOTE, circle.get(i), 1, 0, 0.2, 0, 1);
        }

        //Apply the effect to all party members
        for (Hero tHero : hero.getParty().getMembers()) {
            Player tPlayer = tHero.getPlayer();
            if (!player.getWorld().equals(tPlayer.getWorld()))
                continue;

            if (playerLoc.distanceSquared(tPlayer.getLocation()) > rSquared)
                continue;

            tHero.addEffect(accelEffect);
            //tHero.getPlayer().getWorld().spigot().playEffect(tHero.getPlayer().getLocation(), Effect.CLOUD, 0, 0, 0, 0, 0, 1, 16, 16);
            tHero.getPlayer().getWorld().spawnParticle(Particle.CLOUD, tHero.getPlayer().getLocation(), 16, 0, 0, 0, 1);
        }

        //FIXME Is it a particle or a sound
//        player.getWorld().playEffect(player.getLocation().add(0, 2.5, 0), org.bukkit.Effect.NOTE, 3);
//        player.getWorld().playEffect(player.getLocation().add(0, 2.5, 0), org.bukkit.Effect.NOTE, 3);
//        player.getWorld().playEffect(player.getLocation().add(0, 2.5, 0), org.bukkit.Effect.NOTE, 3);

        return SkillResult.NORMAL;
    }

    public class SkillEntityListener implements Listener {

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onSkillDamage(SkillDamageEvent event) {
            if (event.getDamage() == 0 || !(event.getEntity() instanceof Player))
                return;

            Player player = (Player) event.getEntity();
            final Hero hero = plugin.getCharacterManager().getHero(player);
            if (hero.hasEffect("Accelerando"))
                hero.removeEffect(hero.getEffect("Accelerando"));
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onWeaponDamage(WeaponDamageEvent event) {
            if (event.getDamage() == 0 || !(event.getEntity() instanceof Player))
                return;

            Player player = (Player) event.getEntity();
            final Hero hero = plugin.getCharacterManager().getHero(player);
            if (hero.hasEffect("Accelerando"))
                hero.removeEffect(hero.getEffect("Accelerando"));
        }
    }

    public class AccelerandoEffect extends SpeedEffect {

        public AccelerandoEffect(Skill skill, Player applier, int duration, int multiplier) {
            super(skill, "Accelerando", applier, duration, multiplier, null, null);

            types.add(EffectType.DISPELLABLE);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);

            Player player = hero.getPlayer();
            if (applyText != null && applyText.length() > 0) {
                player.sendMessage("    " + applyText.replace("%hero%",  player.getName()));
            }
        }

        @Override
        public void removeFromHero(final Hero hero) {
            Player player = hero.getPlayer();

            if (player.hasPotionEffect(PotionEffectType.POISON) || player.hasPotionEffect(PotionEffectType.WITHER)
                    || player.hasPotionEffect(PotionEffectType.HARM)) {
                // If they have a harmful effect present when removing the ability, delay effect removal by a bit.
                Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                    @Override
                    public void run() {
                        AccelerandoEffect.super.removeFromHero(hero);
                    }
                }, 2L);
            }
            else {
                super.removeFromHero(hero);
            }

            if (expireText != null && expireText.length() > 0) {
                player.sendMessage("    " + expireText.replace("%hero%", player.getName()));
            }
        }
    }
}