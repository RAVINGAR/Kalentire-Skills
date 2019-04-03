package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.GeometryUtil;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SkillRadiance extends ActiveSkill
{
    public SkillRadiance(Heroes plugin)
    {
        super(plugin, "Radiance");
        setDescription("You radiate holy energy, steadily healing all allies within $1 meters for a total of $2 health over $3 second(s).");
        setUsage("/skill radiance");
        setIdentifiers("skill radiance");
        setArgumentRange(0, 0);
        setTypes(SkillType.HEALING, SkillType.AREA_OF_EFFECT);
    }

    public String getDescription(Hero ardorPlayer)
    {
        double radius = SkillConfigManager.getUseSetting(ardorPlayer, this, SkillSetting.RADIUS, 8, true);
        double healing = SkillConfigManager.getUseSetting(ardorPlayer, this, SkillSetting.HEALING, 100, true);
        healing = getScaledHealing(ardorPlayer, healing);
        healing += (SkillConfigManager.getUseSetting(ardorPlayer, this, SkillSetting.HEALING_INCREASE_PER_WISDOM, 3, true)
                * ardorPlayer.getAttributeValue(AttributeType.WISDOM));
        long duration = SkillConfigManager.getUseSetting(ardorPlayer, this, SkillSetting.DURATION, 8000, true);
        String formattedDuration = String.valueOf((double) duration / 1000);

        return getDescription().replace("$1", radius + "")
                .replace("$2", ((int)healing) + "")
                .replace("$3", formattedDuration);
    }

    public ConfigurationSection getDefaultConfig()
    {
        ConfigurationSection cs = super.getDefaultConfig();

        cs.set(SkillSetting.RADIUS.node(), 8);
        cs.set(SkillSetting.HEALING.node(), 100);
        cs.set(SkillSetting.HEALING_INCREASE_PER_WISDOM.node(), 3);
        cs.set(SkillSetting.DURATION.node(), 8000);

        return cs;
    }

    public SkillResult use(final Hero hero, String[] args)
    {
        final Player player = hero.getPlayer();

        double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 8, true);
        double healing = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING, 100, true);
        healing = getScaledHealing(hero, healing);
        healing += (SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING_INCREASE_PER_WISDOM, 3, true)
                * hero.getAttributeValue(AttributeType.WISDOM));
        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 8000, true);
        final double healingPerTick = (healing / (duration / 50));

        hero.addEffect(new RadianceEffect(this, player, duration));

        new BukkitRunnable() {
            int index = 0;
            Random rand = new Random();
            public void run() {
                if (!hero.hasEffect("Radiance")) cancel();
                List<Location> circle = GeometryUtil.circle(player.getLocation().clone().add(0, 1, 0), 56, radius);
//                player.getWorld().spigot().playEffect(player.getLocation().clone().add(0, 0.3, 0), Effect.INSTANT_SPELL, 0, 0,
//                        3.0F, 0.1F, 3.0F, 0.0F, 15, 128);
                player.getWorld().spawnParticle(Particle.SPELL_INSTANT, player.getLocation().add(0, 0.3, 0), 15, 3, 0.1, 3, 0, true);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.2f, rand.nextFloat() + 1.0f);
                for (int i = 0; i < 4; i++)
                {
                    Location l = circle.get(index);
//                    l.getWorld().spigot().playEffect(l, Effect.COLOURED_DUST, 0, 0,
//                            1.0F, 1.0F, 0.0F, 1.0F, 0, 128);
                    l.getWorld().spawnParticle(Particle.REDSTONE, l, 0, 1, 1, 0, 1, new Particle.DustOptions(Color.YELLOW, 1), true);
                    index++;
                    if (index == circle.size() - 1) index = 0;
                }
                if (hero.hasParty()) {
                    for (Hero h : hero.getParty().getMembers()) {
                        Player p = h.getPlayer();
                        if (p.getLocation().distance(player.getLocation()) < radius)
                            h.heal(healingPerTick); // if this plays hearts, i will cry.
                    }
                } else hero.heal(healingPerTick);
            }
        }.runTaskTimer(plugin, 0, 1);

        broadcastExecuteText(hero);

        return SkillResult.NORMAL;
    }

    public class RadianceEffect extends ExpirableEffect
    {
        public RadianceEffect(Skill skill, Player applier, long duration)
        {
            super(skill, "Radiance", applier, duration);
        }

        public void applyToHero(Hero ap)
        {
            super.applyToHero(ap);
            broadcast(ap.getPlayer().getLocation(), ChatComponents.GENERIC_SKILL + ap.getName() + " radiates restorative energy!");
        }

        public void removeFromHero(Hero ap)
        {
            super.removeFromHero(ap);
            broadcast(ap.getPlayer().getLocation(), ChatComponents.GENERIC_SKILL + ap.getName() + " no longer radiates energy.");
        }
    }
}
