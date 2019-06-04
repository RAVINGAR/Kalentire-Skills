package com.herocraftonline.heroes.characters.skill.reborn.necromancer;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainHealthEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.GeometryUtil;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.List;

public class SkillSacrifice extends ActiveSkill {

    public SkillSacrifice(Heroes plugin) {
        super(plugin, "Sacrifice");
        setDescription("You summon all of your minions and clear their current targets. " +
                "Upon being summoned, you will buff each of them with Speed $1 for $2 seconds.");
        setUsage("/skill sacrifice");
        setIdentifiers("skill sacrifice");
        setArgumentRange(0, 0);
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.SILENCEABLE);
    }

    @Override
    public String getDescription(Hero hero) {
        double manaGainPer = SkillConfigManager.getUseSetting(hero, this, "mana-gain-per", 100.0, false);
        double healthGainPer = SkillConfigManager.getUseSetting(hero, this, "health-gain-per", 25.0, false);

        return getDescription()
                .replace("$1", Util.decFormat.format(healthGainPer))
                .replace("$2", Util.decFormat.format(manaGainPer));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set("mana-gain-per", 100.0);
        config.set("health-gain-per", 25.0);
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

        double manaGainPer = SkillConfigManager.getUseSetting(hero, this, "mana-gain-per", 100.0, false);
        double healthGainPer = SkillConfigManager.getUseSetting(hero, this, "health-gain-per", 25.0, false);

        double manaToRestore = 0;
        double healthToRestore = 0.0;

        for (Monster summon : hero.getSummons()) {
            manaToRestore += manaGainPer;
            healthToRestore += healthGainPer;

            world.playSound(summon.getEntity().getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5F, 2.0F);

            for (double r = 1.0; r < 3.0 * 2.0; r++) {
                List<Location> particleLocations = GeometryUtil.circle(summon.getEntity().getLocation(), 36, r / 2);
                for (Location particleLocation : particleLocations) {
                    player.getWorld().spigot().playEffect(particleLocation, Effect.WITCH_MAGIC, 0, 0, 0, 0.1F, 0, 0.0F, 1, 16);
                    player.getWorld().spigot().playEffect(particleLocation, Effect.COLOURED_DUST, 0, 0, 0, 0.1F, 0, 0.0F, 1, 16);
//                player.getWorld().spawnParticle(Particle.BLAZE_SHOOT, particleLocation, 1, 0, 0.1, 0, 0, null, true);
                }
            }
        }

        hero.clearSummons();

        hero.tryHeal(null, this, healthToRestore);
        hero.tryRestoreMana(hero, this, (int) manaToRestore);

        return SkillResult.NORMAL;
    }
}