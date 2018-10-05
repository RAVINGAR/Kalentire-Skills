package com.herocraftonline.heroes.characters.skill.pack3;

//src=http://pastie.org/private/oeherulcmebfy0lerywsw
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.SafeFallEffect;
import com.herocraftonline.heroes.characters.effects.common.SoundEffect;
import com.herocraftonline.heroes.characters.effects.common.SoundEffect.Note;
import com.herocraftonline.heroes.characters.effects.common.SoundEffect.Song;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

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
                new Note(Sound.BLOCK_NOTE_BLOCK_HARP, 0.8F, 6.0F, 0),
                new Note(Sound.BLOCK_NOTE_BLOCK_HARP, 0.8F, 5.0F, 1),
                new Note(Sound.BLOCK_NOTE_BLOCK_HARP, 0.8F, 4.0F, 2),
                new Note(Sound.BLOCK_NOTE_BLOCK_HARP, 0.8F, 3.0F, 3),
                new Note(Sound.BLOCK_NOTE_BLOCK_HARP, 0.8F, 2.0F, 4),
                new Note(Sound.BLOCK_NOTE_BLOCK_BASS, 0.8F, 1.0F, 5),
                new Note(Sound.BLOCK_NOTE_BLOCK_HARP, 0.0F, 1.0F, 6),
                new Note(Sound.BLOCK_NOTE_BLOCK_BASS, 0.8F, 0.0F, 7),
                new Note(Sound.BLOCK_NOTE_BLOCK_HARP, 0.0F, 1.0F, 8),
                new Note(Sound.BLOCK_NOTE_BLOCK_HARP, 0.8F, 3.0F, 9)
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
        node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero%'s party celebrates bass-drops!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero%'s party no longer is dropping bass!");

        return node;
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
    public SkillResult use(final Hero hero, String[] args) {

        final Skill theSkill = this;
        final Player player = hero.getPlayer();

        broadcastExecuteText(hero);

        hero.addEffect(new SoundEffect(this, "DropTheBassSong", 100, skillSong));

        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 10000, false);
        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS.node(), 15, false);

        List<Location> circle = circle(player.getLocation(), 72, radius);
        for (int i = 0; i < circle.size(); i++)
        {
            //player.getWorld().spigot().playEffect(circle(player.getLocation(), 72, radius).get(i), org.bukkit.Effect.NOTE, 0, 0, 0, 0.2F, 0, 1, 1, 20);
            player.getWorld().spawnParticle(Particle.NOTE, circle.get(i), 1, 0, 0.2, 0, 1);
        }

        double radiusSquared = Math.pow(radius, 2);

        if (hero.hasParty()) {
            for (final Hero member : hero.getParty().getMembers()) {
                Player memberPlayer = member.getPlayer();
                if (memberPlayer.getWorld() != player.getWorld())
                    continue;

                if (memberPlayer.getLocation().distanceSquared(player.getLocation()) <= radiusSquared) {
                    member.addEffect(new SafeFallEffect(theSkill, player, duration));
                }

                //member.getPlayer().getWorld().spigot().playEffect(member.getPlayer().getLocation(), Effect.CLOUD, 0, 0, 0, 0, 0, 1, 16, 16);
                member.getPlayer().getWorld().spawnParticle(Particle.CLOUD, member.getPlayer().getLocation(), 16, 0, 0, 0, 1);
            }
        }
        else {
            hero.addEffect(new SafeFallEffect(theSkill, player, duration));
        }

        //FIXME Is it a particle or a sound
//        player.getWorld().playEffect(player.getLocation().add(0, 2.5, 0), org.bukkit.Effect.NOTE, 3);
//        player.getWorld().playEffect(player.getLocation().add(0, 2.5, 0), org.bukkit.Effect.NOTE, 3);
//        player.getWorld().playEffect(player.getLocation().add(0, 2.5, 0), org.bukkit.Effect.NOTE, 3);

        return SkillResult.NORMAL;
    }
}