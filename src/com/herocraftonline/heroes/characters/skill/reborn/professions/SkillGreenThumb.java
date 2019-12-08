package com.herocraftonline.heroes.characters.skill.reborn.professions;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import com.herocraftonline.townships.users.TownshipsUser;
import com.herocraftonline.townships.users.UserManager;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.RegionContainer;
import com.sk89q.worldguard.bukkit.RegionQuery;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.logging.Level;

public class SkillGreenThumb extends ActiveSkill {

    private boolean townships = false;
    private boolean worldguard = false;
    private WorldGuardPlugin wgp;
    private String failedUseText;

    public SkillGreenThumb(Heroes plugin) {
        super(plugin, "GreenThumb");
        setDescription("You have a $1% chance of growing grass on dirt.");
        setUsage("/skill greenthumb");
        setArgumentRange(0, 0);
        setIdentifiers("skill greenthumb");
        setTypes(SkillType.BLOCK_MODIFYING);

        try {
            if (Bukkit.getServer().getPluginManager().getPlugin("WorldGuard") != null) {
                worldguard = true;
                wgp = (WorldGuardPlugin) this.plugin.getServer().getPluginManager().getPlugin("WorldGuard");
            }
            if (Bukkit.getServer().getPluginManager().getPlugin("Townships") != null) {
                townships = true;
            }
        } catch (Exception e) {
            Heroes.log(Level.SEVERE, "SkillGreenThumb: Could not get WorldGuard or Townships plugins! Region checking may not work!");
        }
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("ignore-region-plugins", false);
        node.set(SkillSetting.CHANCE.node(), 0.3);
        node.set(SkillSetting.CHANCE_PER_LEVEL.node(), 0.035); // e.g. if lvl20 starting, 100% chance at level 40
        node.set("chance-per-level-beyond-skill-level", true);
        node.set(SkillSetting.COOLDOWN.node(), 300);
        node.set(SkillSetting.MAX_DISTANCE.node(), 15);
        node.set(SkillSetting.REAGENT.node(), "SEEDS");
        node.set(SkillSetting.REAGENT_COST.node(), 1);
        node.set("failed-use-text", ChatComponents.GENERIC_SKILL + "%hero% failed to encourage growth of grass.");
        return node;
    }

    @Override
    public void init() {
        super.init();
        failedUseText = SkillConfigManager.getRaw(this, "failed-use-text",
                ChatComponents.GENERIC_SKILL + "%hero% failed to encourage growth of grass.");
    }

    @Override
    public SkillResult use(Hero hero, String[] strings) {
        Player player = hero.getPlayer();

        // Cancel if not a dirt block
        int distance = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 15, false);
        final Block targetBlock = player.getTargetBlock((HashSet<Material>)null, distance);
        final Location blockLocation = targetBlock.getLocation();
        if (targetBlock.getType() != Material.DIRT) {
            player.sendMessage(ChatColor.RED + "That is not dirt!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        boolean ignoreRegionPlugins = SkillConfigManager.getUseSettingBool(hero,this, "ignore-region-plugins");
        if (!ignoreRegionPlugins) {
            // Validate Townships permission
            if (townships) {
                TownshipsUser user = UserManager.fromOfflinePlayer(player);
                if (!user.canBuild(blockLocation)) {
                    player.sendMessage(ChatColor.RED + "You cannot grow in a region you have no access to!");
                    return SkillResult.FAIL;
                }
            }

            // Validate WorldGuard permission
            if (worldguard) {
                LocalPlayer wgPlayer = wgp.wrapPlayer(player);
                RegionContainer container = WorldGuardPlugin.inst().getRegionContainer();
                RegionQuery query = container.createQuery();
                if (!query.testState(blockLocation, wgPlayer, DefaultFlag.BUILD) && !WorldGuardPlugin.inst().getSessionManager().hasBypass(player, player.getWorld())) {
                    player.sendMessage(ChatColor.RED + "You cannot grow in a region you have no access to!");
                    return SkillResult.FAIL;
                }
            }
        }

//        broadcastExecuteText(hero);

        double chance = SkillConfigManager.getUseSetting(hero, this, SkillSetting.CHANCE, 0.3, false);
        double chancePerLevel = SkillConfigManager.getUseSetting(hero, this, SkillSetting.CHANCE_PER_LEVEL, 0D, false);
        boolean chancePerLevelBeyondSkillLevel = SkillConfigManager.getUseSetting(hero,this, "chance-per-level-beyond-skill-level", true);
        int chanceLevels = hero.getHeroLevel(this);
        if (chancePerLevelBeyondSkillLevel){
            chanceLevels -= hero.getHeroSkillLevel(this).orElse(0);
        }
        chance += (chanceLevels * chancePerLevel);

        if (chance >= 1.0 || Util.nextRand() < chance){
            //TODO spawn particle coming up from dirt
            //        world.spigot().playEffect(target.getLocation(), // location
//                org.bukkit.Effect.HAPPY_VILLAGER, // effect
//                0, // id
//                0, // data
//                1, 1, 1, // offset
//                1.0f, // speed
//                25, // particle count
//                1); // radius
            blockLocation.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, blockLocation.add(0,0.1,0),
                    10, // particle count
                    0.1, 0.1, 0.1, // x,y,z offset (stretch of particles)
                    0); // extra data

            //set block to grass
            targetBlock.setType(Material.GRASS);
        } else if (!failedUseText.isEmpty()) {
            player.sendMessage("    " + failedUseText.replace("%hero%", player.getName()));
            return SkillResult.FAIL;
        }

        return SkillResult.NORMAL;
    }
}
