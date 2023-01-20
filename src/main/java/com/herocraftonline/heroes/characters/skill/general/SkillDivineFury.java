package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.skills.SkillBaseBeam;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

public class SkillDivineFury extends SkillBaseBeam {

    public SkillDivineFury(final Heroes plugin) {
        super(plugin, "DivineFury");
        setDescription("Unleash Divine Fury at your target dealing $1 damage in a line.");
        setIdentifiers("skill divinefury");
        setUsage("/skill divinefury");
        setArgumentRange(0, 0);
        setTypes(SkillType.AGGRESSIVE, SkillType.SILENCEABLE);
    }

    @Override
    public String getDescription(final Hero hero) {
        return super.getDescription()
                .replace("$1", "" + SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE, 15, false));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DAMAGE.node(), 15);
        node.set(SkillSetting.MAX_DISTANCE.node(), 10);
        node.set(SkillSetting.RADIUS.node(), 0.5);

        return node;
    }

    @Override
    public SkillResult use(final Hero hero, final String[] strings) {

        final double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE, 15, false);
        final double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 0.5, false);
        final int distance = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 10, false);

        broadcastExecuteText(hero);

        final Player player = hero.getPlayer();
        final Beam beam = createObstructedBeam(player.getEyeLocation(), distance, radius);
        final Location location = player.getLocation();
        final World world = location.getWorld();
        world.playSound(location, Sound.ENTITY_BLAZE_SHOOT, 0.9F, 0.9F);
        world.playSound(location, Sound.ENTITY_BAT_DEATH, 0.8F, 0.4F);

        castBeam(hero, beam, (hero1, target, pointData) -> {
            if (damageCheck(hero1.getPlayer(), target)) {
                addSpellTarget(target, hero1);
                damageEntity(target, hero1.getPlayer(), damage, EntityDamageEvent.DamageCause.MAGIC, 0.3f);
                final Location location1 = target.getLocation();
                final World world1 = location1.getWorld();
                world1.spawnParticle(Particle.END_ROD, location1, 15, 0.05, 0.05, 0.05, 0);
            }
        });

        renderEyeBeam(player, beam, Particle.VILLAGER_ANGRY, 40, 10, 40, 0.125, 0);
        renderEyeBeam(player, beam, Particle.ELECTRIC_SPARK, 20, 10, 40, 0.125, 0);

        return SkillResult.NORMAL;
    }
}
