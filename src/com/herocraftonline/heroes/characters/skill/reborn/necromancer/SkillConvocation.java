package com.herocraftonline.heroes.characters.skill.reborn.necromancer;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.common.SpeedEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.GeometryUtil;
import com.herocraftonline.heroes.util.Util;
import io.lumine.xikage.mythicmobs.MythicMobs;
import io.lumine.xikage.mythicmobs.mobs.ActiveMob;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;

public class SkillConvocation extends ActiveSkill {

    private final MythicMobs mythicMobs;

    public SkillConvocation(Heroes plugin) {
        super(plugin, "Convocation");
        setDescription("You summon all of your minions and clear their current targets. " +
                "Upon being summoned, you will buff each of them with Speed $1 for $2 seconds and heal them for $3 hp.");
        setUsage("/skill convocation");
        setIdentifiers("skill convocation");
        setArgumentRange(0, 0);
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.SILENCEABLE);

        if (Bukkit.getServer().getPluginManager().getPlugin("MythicMobs") != null) {
            this.mythicMobs = MythicMobs.inst();
        } else {
            this.mythicMobs = null;
        }
    }

    @Override
    public String getDescription(Hero hero) {
        long duration = SkillConfigManager.getUseSetting(hero, this, "speed-duration", 3000, false);
        int speedAmplifier = SkillConfigManager.getUseSetting(hero, this, "speed-amplifier", 3, false);
        double heal = SkillConfigManager.getUseSetting(hero, this, "minion-healing", 25.0, false);

        return getDescription()
                .replace("$1", (speedAmplifier + 1) + "")
                .replace("$2", Util.decFormat.format(duration / 1000.0))
                .replace("$3", Util.decFormat.format(heal));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set("speed-duration", 3000);
        config.set("speed-amplifier", 3);
        config.set("minion-healing", 25.0);
        return config;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        World world = player.getWorld();
        Location playerLoc = player.getLocation();

        if (hero.getSummons().isEmpty()) {
            player.sendMessage("    " + ChatComponents.GENERIC_SKILL + "You don't have any active summons!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        broadcastExecuteText(hero);

        long duration = SkillConfigManager.getUseSetting(hero, this, "speed-duration", 3000, false);
        double heal = SkillConfigManager.getUseSetting(hero, this, "minion-healing", 25.0, false);
        int speedAmplifier = SkillConfigManager.getUseSetting(hero, this, "speed-amplifier", 3, false);

        for (Monster summon : hero.getSummons()) {
            summon.getEntity().teleport(playerLoc);

            summon.setTargetIfAble(null, false);
            if (mythicMobs != null) {
                Optional<ActiveMob> summonedMob = mythicMobs.getMobManager().getActiveMob(summon.getEntity().getUniqueId());
                summonedMob.ifPresent(ActiveMob::resetTarget);
            }

            summon.addEffect(new SpeedEffect(this, "ConvocationSpeed", player, duration, speedAmplifier));
            summon.tryHeal(hero, this, heal);

            world.playSound(summon.getEntity().getLocation(), Sound.ENTITY_WITHER_HURT, 0.5F, 2.0F);

            for (double r = 1.0; r < 3.0 * 2.0; r++) {
                List<Location> particleLocations = GeometryUtil.circle(summon.getEntity().getLocation(), 36, r / 2);
                for (Location particleLocation : particleLocations) {
                    player.getWorld().spigot().playEffect(particleLocation, Effect.INSTANT_SPELL, 0, 0, 0, 0.1F, 0, 0.0F, 1, 16);
                    player.getWorld().spigot().playEffect(particleLocation, Effect.VILLAGER_THUNDERCLOUD, 0, 0, 0, 0.1F, 0, 0.0F, 1, 16);
//                player.getWorld().spawnParticle(Particle.INSTANT_SPELL, particleLocation, 1, 0, 0.1, 0, 0, null, true);
//                player.getWorld().spawnParticle(Particle.VILLAGER_THUNDERCLOUD, particleLocation, 1, 0, 0.1, 0, 0, null, true);
                }
            }
        }


        for (double r = 1.0; r < 3.0 * 2.0; r++) {
            List<Location> particleLocations = GeometryUtil.circle(player.getLocation(), 36, r / 2);
            for (Location particleLocation : particleLocations) {
                player.getWorld().spigot().playEffect(particleLocation, Effect.HAPPY_VILLAGER, 0, 0, 0, 0.1F, 0, 0.0F, 1, 16);
//                player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, particleLocation, 1, 0, 0.1, 0, 0, null, true);
            }
        }

        return SkillResult.NORMAL;
    }
}