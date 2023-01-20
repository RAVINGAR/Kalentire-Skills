package com.herocraftonline.heroes.characters.skill.reborn;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.SlowEffect;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.skills.SkillBaseBlockWave;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class SkillSeismicAssault extends SkillBaseBlockWave {

    public SkillSeismicAssault(final Heroes plugin) {
        super(plugin, "SeismicAssault");
        setDescription("The seismic assault erupts and throws blocks around hitting all targets in a $1 degree arc in front of you, with a radius of $2, height of $3, and depth of $4, " +
                "Expanding at a rate of $5 block(s) per second. Targets can be hit hit a maximum of $6 time(s) each hit dealing $7 damage, slowing them for $8 second(s).");
        setUsage("/skill " + getName().toLowerCase());
        setIdentifiers("skill " + getName().toLowerCase());
        setArgumentRange(0, 0);
        setTypes(SkillType.DAMAGING, SkillType.AREA_OF_EFFECT, SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.AGGRESSIVE);
    }

    @Override
    public String getDescription(final Hero hero) {
        final double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5d, false);
        final double expansionRate = SkillConfigManager.getUseSetting(hero, this, EXPANSION_RATE_NODE, 1d, false);
        final double waveArc = SkillConfigManager.getUseSetting(hero, this, WAVE_ARC_NODE, 360d, false);
        final int hitLimit = SkillConfigManager.getUseSetting(hero, this, HIT_LIMIT_NODE, 1, false);
        final int depth = SkillConfigManager.getUseSetting(hero, this, DEPTH_NODE, 5, false);
        final int height = SkillConfigManager.getUseSetting(hero, this, HEIGHT_NODE, 3, false);


        final double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE, 100d, false);

        final long duration = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.DURATION, 4000, false);

        return getDescription()
                .replace("$1", Util.decFormat.format(waveArc))
                .replace("$2", Util.decFormat.format(radius))
                .replace("$3", "" + depth)
                .replace("$4", "" + height)
                .replace("$5", Util.largeDecFormat.format(expansionRate))
                .replace("$6", "" + hitLimit)
                .replace("$7", Util.decFormat.format(damage))
                .replace("$8", Util.decFormat.format((double) duration / 1000));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.RADIUS.node(), 8);
        config.set(HEIGHT_NODE, 3);
        config.set(DEPTH_NODE, 5);
        config.set(EXPANSION_RATE_NODE, 1);
        config.set(WAVE_ARC_NODE, 60.0);
        config.set(LAUNCH_FORCE_NODE, 0.2);
        config.set(HIT_LIMIT_NODE, 1);
        config.set(SkillSetting.DAMAGE.node(), 100.0);
        config.set(SkillSetting.DURATION.node(), 4000);

        return config;
    }

    @Override
    public SkillResult use(final Hero hero, final String[] strings) {

        final double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE, 100d, false);
        final long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 4000, false);

        castBlockWave(hero, hero.getPlayer().getLocation().getBlock(), (hero1, target, center) -> {
            if (damageCheck(hero1.getPlayer(), target)) {
                damageEntity(target, hero1.getPlayer(), damage, EntityDamageEvent.DamageCause.MAGIC, 0.0f);

                final CharacterTemplate targetCt = plugin.getCharacterManager().getCharacter(target);

                final SlowEffect slow = new SlowEffect(SkillSeismicAssault.this, hero1.getPlayer(), duration, 2);
                targetCt.addEffect(slow);
            }
        });

        final World world = hero.getPlayer().getWorld();
        new BukkitRunnable() {

            float volume = 1;

            @Override
            public void run() {
                world.playSound(hero.getPlayer().getLocation(), Sound.BLOCK_GRASS_HIT, volume, 1f);
                volume -= 0.1;

                if (volume <= 0) {
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0, 1);

        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }
}
