package com.herocraftonline.heroes.characters.skill.skills;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Setting;

public class SkillWeb extends TargettedSkill {

    private String applyText;
    private static Set<Location> changedBlocks = new HashSet<Location>();

    public SkillWeb(Heroes plugin) {
        super(plugin, "Web");
        setDescription("You conjure a web around your target.");
        setUsage("/skill web <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill web");
        setTypes(SkillType.EARTH, SkillType.SILENCABLE, SkillType.HARMFUL);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillBlockListener(), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(Setting.DURATION.node(), 5000); // in milliseconds
        node.set(Setting.APPLY_TEXT.node(), "%hero% conjured a web at %target%'s feet!");
        return node;
    }

    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, Setting.APPLY_TEXT, "%hero% conjured a web at %target%'s feet!").replace("%hero%", "$1").replace("%target%", "$2");
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        String name = "";
        if (target instanceof Player) {
            name = ((Player) target).getDisplayName();
        } else {
            name = Messaging.getLivingEntityName(target).toLowerCase();
        }

        broadcast(player.getLocation(), applyText, player.getDisplayName(), name);
        int duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 5000, false);
        WebEffect wEffect = new WebEffect(this, duration, target.getLocation().getBlock().getLocation());
        hero.addEffect(wEffect);
        return SkillResult.NORMAL;
    }

    public class SkillBlockListener implements Listener {

        @EventHandler(priority = EventPriority.HIGHEST)
        public void onBlockBreak(BlockBreakEvent event) {
            if (event.isCancelled()) {
                return;
            }

            // Check out mappings to see if this block was a changed block, if so lets deny breaking it.
            if (changedBlocks.contains(event.getBlock().getLocation())) {
                event.setCancelled(true);
            }
        }
    }

    public class WebEffect extends ExpirableEffect {

        private List<Location> locations = new ArrayList<Location>();
        private Location loc;

        public WebEffect(Skill skill, long duration, Location location) {
            super(skill, "Web", duration);
            this.loc = location;
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            changeBlock(loc, hero);
            Block block = loc.getBlock();
            changeBlock(block.getRelative(BlockFace.DOWN).getLocation(), hero);
            for (BlockFace face : BlockFace.values()) {
                if (face.toString().contains("_") || face == BlockFace.UP || face == BlockFace.DOWN) {
                    continue;
                }
                Location blockLoc = block.getRelative(face).getLocation();
                changeBlock(blockLoc, hero);
                blockLoc = block.getRelative(getClockwise(face)).getLocation();
                changeBlock(blockLoc, hero);
                blockLoc = block.getRelative(face, 2).getLocation();
                changeBlock(blockLoc, hero);
            }
        }

        public Location getLocation() {
            return this.loc;
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            for (Location location : locations) {
                location.getBlock().setType(Material.AIR);
                changedBlocks.remove(location);
            }
            locations.clear();
        }

        private void changeBlock(Location location, Hero hero) {
            Block block = location.getBlock();
            switch (block.getType()) {
            case WATER:
            case LAVA:
            case SNOW:
            case AIR:
                changedBlocks.add(location);
                locations.add(location);
                location.getBlock().setType(Material.WEB);
            default:
            }
        }

        private BlockFace getClockwise(BlockFace face) {
            switch (face) {
            case NORTH:
                return BlockFace.EAST;
            case EAST:
                return BlockFace.SOUTH;
            case SOUTH:
                return BlockFace.WEST;
            case WEST:
            default:
                return BlockFace.SELF;
            }
        }
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }
}
