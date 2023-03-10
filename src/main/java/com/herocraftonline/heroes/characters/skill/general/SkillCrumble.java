package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.StunEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

import static com.herocraftonline.heroes.util.GeometryUtil.circle;

public class SkillCrumble extends TargettedSkill {
    private String applyText;
    private String expireText;

    public SkillCrumble(final Heroes plugin) {
        super(plugin, "Crumble");
        setDescription("You encumber your enemy with sharp stones for six second(s). For every two blocks they move, they are stunned for 0.5 second(s).");
        setUsage("/skill crumble");
        setArgumentRange(0, 0);
        setIdentifiers("skill crumble");
        setTypes(SkillType.DEBUFFING, SkillType.SILENCEABLE, SkillType.ABILITY_PROPERTY_EARTH, SkillType.DAMAGING,
                SkillType.NO_SELF_TARGETTING);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 7);
        node.set(SkillSetting.DURATION.node(), 500);
        node.set(SkillSetting.APPLY_TEXT.node(), "The ground around %target%'s feet begins to crumble!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "The ground under %target%'s feet has stabilised.");

        return node;
    }


    @Override
    public String getDescription(final Hero arg0) {
        // What?
        return "You encumber your enemy with sharp stones for six second(s). For every two blocks they move, they are stunned for 0.5 second(s).";
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getUseSetting(null, this, SkillSetting.APPLY_TEXT, "The ground around %target%'s feet begins to crumble!").replace("%target%", "$1").replace("$target$", "$1");
        expireText = SkillConfigManager.getUseSetting(null, this, SkillSetting.EXPIRE_TEXT, "The ground under %target%'s feet has stabilised.").replace("%target%", "$1").replace("$target$", "$1");

    }

    @Override
    public SkillResult use(final Hero hero, final LivingEntity target, final String[] args) {
        final Player player = hero.getPlayer();
        final LivingEntity t = target;

        if (!damageCheck(t, player)) {
            return SkillResult.INVALID_TARGET;
        }

        new BukkitRunnable() // This is the visual effect, should iterate though all points in a circle. Should, I say.
        {
            final int maxTicks = 30; // every 4 ticks means this runs 5 times a second, a total of 5 times a second for 6 seconds = 30 ticks
            int point = 0;
            int ticks = 0;

            @Override
            public void run() {
                final List<Location> surrounding = circle(t.getLocation(), 24, 1); // This is down here to make sure it updates
                // next point
                if (point < surrounding.size()) // making sure we're staying within index boundaries
                {
                    //t.getWorld().spigot().playEffect(surrounding.get(point), Effect.TILE_BREAK, Material.STONE.getId(), 0, 0.2F, 0.2F, 0.2F, 1.0F, 10, 16);
                } else {
                    point = 0; // reset the circle
                    //t.getWorld().spigot().playEffect(surrounding.get(point), Effect.TILE_BREAK, Material.STONE.getId(), 0, 0.2F, 0.2F, 0.2F, 1.0F, 10, 16);
                }
                t.getWorld().spawnParticle(Particle.BLOCK_CRACK, surrounding.get(point), 10, 0.2, 0.2, 0.2, 1);
                t.getWorld().playSound(t.getLocation(), Sound.BLOCK_STONE_BREAK, 0.4F, 0.8F);
                point++; // next point
                ticks += 1;
                if (ticks >= maxTicks) // if the effect has played for 6 seconds
                {
                    cancel(); // cancel the visual
                }
            }
        }.runTaskTimer(plugin, 1, 4);

        final Skill skill = this; // for the effect

        broadcast(t.getLocation(), " The ground around " + t.getName() + "'s feet has begun to crumble!");

        new BukkitRunnable() // Bukkit has no entity move event, and this is actually a faster way of doing the effect because an entity move event would lag.
        {
            private final int maxTicks = 120; // running 20 TPS, this is about 6 seconds
            private double blocksMoved = 0.0D; // Total blocks moved, in x, y, and/or z
            private Location l1 = t.getLocation(); // initial location for comparison
            private int ticks = 0; // current ticks into the effect

            @Override
            public void run() {
                final Location l2 = t.getLocation(); // current location
                if (l2.distance(l1) >= 1) {
                    blocksMoved += 1.0D;
                    l1 = l2; // sets the new comparison location to the current location
                    if (blocksMoved == 2.0D) {
                        final CharacterTemplate ct = plugin.getCharacterManager().getCharacter(t);
                        final StunEffect stun = new StunEffect(skill, player, 500); // stuns for half a second
                        ct.addEffect(stun); // adds the stun
                        blocksMoved = 0.0D; // resets block count
                    }
                }
                ticks++; // another tick
                if (ticks == maxTicks) // if we've reached 120 ticks (6 seconds)
                {
                    broadcast(t.getLocation(), " The ground around " + t.getName() + "'s feet has stabilised.");
                    cancel(); // stop the effect
                }
            }
        }.runTaskTimer(plugin, 1, 1);


        return SkillResult.NORMAL;
    }
}