package com.herocraftonline.heroes.characters.skill.skills;
// src http://pastie.org/private/syyyftinqa5r1uv4ixka
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.QuickenEffect;
import com.herocraftonline.heroes.characters.effects.common.SoundEffect;
import com.herocraftonline.heroes.characters.effects.common.SoundEffect.Note;
import com.herocraftonline.heroes.characters.effects.common.SoundEffect.Song;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;

public class SkillAccelerando extends ActiveSkill {

    private String applyText;
    private String expireText;
    private Song skillSong;

    public SkillAccelerando(Heroes plugin) {
        super(plugin, "Accelerando");
        setDescription("You song boons movement speed boost to all nearby party members for $1 seconds.");
        setUsage("/skill accelerando");
        setArgumentRange(0, 0);
        setIdentifiers("skill accelerando", "skill accelerate");
        setTypes(SkillType.BUFF, SkillType.MOVEMENT, SkillType.SILENCABLE);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(), plugin);
        skillSong = new Song(
        		new Note(Sound.NOTE_BASS_DRUM , 0.9F, 0.2F, 0),
                new Note(Sound.NOTE_BASS , 0.9F, 0.5F, 1),
                new Note(Sound.NOTE_BASS_DRUM , 0.9F, 0.9F, 2),
                new Note(Sound.NOTE_BASS , 0.9F, 0.2F, 3),
                new Note(Sound.NOTE_BASS_DRUM , 0.9F, 0.5F, 4),
                new Note(Sound.NOTE_BASS_DRUM , 0.9F, 0.9F, 5),
                new Note(Sound.NOTE_BASS , 0.9F, 0.2F, 6),
                new Note(Sound.NOTE_BASS_DRUM , 0.9F, 0.5F, 7),
                new Note(Sound.NOTE_BASS_DRUM , 0.9F, 0.9F, 8),
                new Note(Sound.NOTE_BASS , 0.9F, 0.2F, 9),
                new Note(Sound.NOTE_BASS_DRUM , 0.9F, 0.5F, 10),
                new Note(Sound.NOTE_BASS , 0.9F, 0.9F, 11)
        );
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("speed-multiplier", 2);
        node.set(SkillSetting.DURATION.node(), 10000);
        node.set(SkillSetting.RADIUS.node(), 15);
        node.set("apply-text", "%hero% gained a burst of speed!");
        node.set("expire-text", "%hero% returned to normal speed!");
        return node;
    }

    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, "%hero% gained a burst of speed!").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "%hero% returned to normal speed!").replace("%hero%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        broadcastExecuteText(hero);
        //Removed sound playing here, see SkilSoundPlayer below
        //Schedule a delayed task using the SkillSoundPlayer class to happen in 100 milliseconds (1 / 10 of a second)
        //Substitute the name of the variable referring to a Heroes object for 'plugin'
//        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new SkillSoundPlayer(hero), 100);
        hero.addEffect(new SoundEffect(this, "AccelarandoSong", 100, skillSong));
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 300000, false);
        int multiplier = SkillConfigManager.getUseSetting(hero, this, "speed-multiplier", 2, false);
        if (multiplier > 20) {
            multiplier = 20;
        }
        QuickenEffect qEffect = new QuickenEffect(this, getName(), duration, multiplier, applyText, expireText);
        if (!hero.hasParty()) {
            hero.addEffect(qEffect);
            return SkillResult.NORMAL;
        }
        Player player = hero.getPlayer();
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
            
            tHero.addEffect(qEffect);
        }
        return SkillResult.NORMAL;
    }
    
    //Added this
    public class SkillSoundPlayer implements Runnable {
        private final Player player;
        public SkillSoundPlayer(Hero hero) {
            this.player = hero.getPlayer();
        }
        public void run() {
            World world = player.getWorld();
            Location loc = player.getLocation();
            world.playSound(loc, Sound.NOTE_BASS_DRUM , 0.9F, 0.2F);
            world.playSound(loc, Sound.NOTE_BASS , 0.9F, 0.5F);
            world.playSound(loc, Sound.NOTE_BASS_DRUM , 0.9F, 0.9F);
            world.playSound(loc, Sound.NOTE_BASS , 0.9F, 0.2F);
            world.playSound(loc, Sound.NOTE_BASS_DRUM , 0.9F, 0.5F);
            world.playSound(loc, Sound.NOTE_BASS_DRUM , 0.9F, 0.9F);
            world.playSound(loc, Sound.NOTE_BASS , 0.9F, 0.2F);
            world.playSound(loc, Sound.NOTE_BASS_DRUM , 0.9F, 0.5F);
            world.playSound(loc, Sound.NOTE_BASS_DRUM , 0.9F, 0.9F);
            world.playSound(loc, Sound.NOTE_BASS , 0.9F, 0.2F);
            world.playSound(loc, Sound.NOTE_BASS_DRUM , 0.9F, 0.5F);
            world.playSound(loc, Sound.NOTE_BASS , 0.9F, 0.9F);
        }
    }
    
    public class SkillEntityListener implements Listener {

        @EventHandler(priority = EventPriority.MONITOR)
        public void onEntityDamage(EntityDamageEvent event) {
            if (event.isCancelled() || event.getDamage() == 0 || !(event.getEntity() instanceof Player)) {
                return;
            }
            
            Hero hero = plugin.getCharacterManager().getHero((Player) event.getEntity());
            if (hero.hasEffect(getName())) {
                hero.removeEffect(hero.getEffect(getName()));
            }
        }
    }
    
    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
        return getDescription().replace("$1", duration / 1000 + "");
    }
}