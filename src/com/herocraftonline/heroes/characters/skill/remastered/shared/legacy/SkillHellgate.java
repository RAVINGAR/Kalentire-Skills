package com.herocraftonline.heroes.characters.skill.remastered.shared.legacy;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.skill.*;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class SkillHellgate extends ActiveSkill {

    public SkillHellgate(Heroes plugin) {
        super(plugin, "Hellgate");
        setDescription("You teleport your party to or from the nether.");
        setUsage("/skill hellgate");
        setIdentifiers("skill hellgate");
        setArgumentRange(0, 0);
        setTypes(SkillType.SILENCEABLE, SkillType.TELEPORTING, SkillType.AREA_OF_EFFECT, SkillType.ABILITY_PROPERTY_DARK, SkillType.ABILITY_PROPERTY_FIRE);
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.RADIUS.node(), 10);
        config.set(SkillSetting.NO_COMBAT_USE.node(), true);
        config.set("teleport-absolute", false);
        config.set("x", 612);
        config.set("y", 124);
        config.set("z", -65);
        config.set("hell-world", "world_nether");
        config.set("default-return", "world");
        return config;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        String defaultWorld = SkillConfigManager.getUseSetting(hero, this, "default-return", "world");
        String hellWorld = SkillConfigManager.getUseSetting(hero, this, "hell-world", "world_nether");
        World world;
        Location teleportLocation = null;
        Location castLocation = player.getLocation().clone();

        // if in nether, set teleport target to spawn
        if (player.getWorld().getEnvironment() == Environment.NETHER) {
            world = plugin.getServer().getWorld(defaultWorld);
            if (world == null) {
                world = plugin.getServer().getWorlds().get(0);
            }
            if (hero.hasEffect("Hellgate")) {
                teleportLocation = ((HellgateEffect) hero.getEffect("Hellgate")).getLocation();
                hero.removeEffect(hero.getEffect("Hellgate"));
            } else {
                teleportLocation = world.getSpawnLocation();
            }
        } else {
            // We are on the main world so lets setup a teleport to nether!
            world = plugin.getServer().getWorld(hellWorld);
            if (world == null) {
                for (World tWorld : plugin.getServer().getWorlds()) {
                    if (tWorld.getEnvironment() == Environment.NETHER) {
                        world = tWorld;
                    }
                }
            }

            // If world is still null then there is no world to teleport to
            if (world == null) {
                player.sendMessage("No world to open a Hellgate into!");
                return SkillResult.FAIL;
            }

            boolean absolute = SkillConfigManager.getUseSetting(hero, this, "teleport-absolute", false);
            if (absolute) {
                Location cur = hero.getPlayer().getLocation();

                //If no config setting, just leave them where they are
                int x = SkillConfigManager.getUseSetting(hero, this, "x", cur.getBlockX(), false);
                int y = SkillConfigManager.getUseSetting(hero, this, "y", cur.getBlockY(), false);
                int z = SkillConfigManager.getUseSetting(hero, this, "z", cur.getBlockZ(), false);

                teleportLocation = new Location(world, x, y, z);
            } else {
                teleportLocation = world.getSpawnLocation();
            }
        }

        if (hero.hasParty()) {
            Location heroLoc = hero.getPlayer().getLocation();
            double rangeSquared = Math.pow(SkillConfigManager.getUseSettingDouble(hero, this, SkillSetting.RADIUS, false), 2);
            for (Hero targetHero : hero.getParty().getMembers()) {
                Player target = targetHero.getPlayer();
                if (target.equals(player)) {
                    continue;
                }
                if (castLocation.getWorld() != target.getWorld()) {
                    target.sendMessage("You're in a different world than the caster!");
                    player.sendMessage("The party member, " + target.getName() + ", is in a different world than you are!");
                } else {
                    if (target.getLocation().distanceSquared(heroLoc) > rangeSquared) {
                        continue;
                    }
                    target.teleport(teleportLocation);
                }
            }
        }

        broadcastExecuteText(hero);

        hero.addEffect(new HellgateEffect(this, player.getLocation()));
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.BLOCK_PORTAL_TRAVEL, 0.5F, 1.0F);
        player.teleport(teleportLocation);
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.BLOCK_PORTAL_TRAVEL, 0.5F, 1.0F);

        return SkillResult.NORMAL;
    }

    // Tracks the players original location for returning
    public class HellgateEffect extends Effect {
        private final Location location;

        HellgateEffect(Skill skill, Location location) {
            super(skill, "Hellgate");

            this.location = location;
        }

        public Location getLocation() {
            return location;
        }
    }
}