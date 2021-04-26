package com.herocraftonline.heroes.characters.skill.public1;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CustomNameManager;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SkillWeb extends TargettedSkill {

    private String applyText;
    private static Set<Location> changedBlocks = new HashSet<Location>();

    public SkillWeb(Heroes plugin) {
        super(plugin, "Web");
        this.setDescription("You conjure a web around your target.");
        this.setUsage("/skill web <target>");
        this.setArgumentRange(0, 1);
        this.setIdentifiers("skill web");
        this.setTypes(SkillType.ABILITY_PROPERTY_EARTH, SkillType.SILENCEABLE, SkillType.AGGRESSIVE);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillBlockListener(), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DURATION.node(), 5000); // in milliseconds
        node.set(SkillSetting.APPLY_TEXT.node(), "%hero% conjured a web at %target%'s feet!");
        return node;
    }

    @Override
    public void init() {
        super.init();
        this.applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, "%hero% conjured a web at %target%'s feet!").replace("%target%", "$1").replace("%hero%", "$2");
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        final Player player = hero.getPlayer();

        String name = "";
        if (target instanceof Player) {
            name = ((Player) target).getDisplayName();
        } else {
            name = CustomNameManager.getName(target).toLowerCase();
        }

        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
        final WebEffect wEffect = new WebEffect(this, player, duration, target.getLocation().getBlock().getLocation());
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

        private final List<Location> locations = new ArrayList<Location>();
        private final Location loc;

        public WebEffect(Skill skill, Player applier, long duration, Location location) {
            super(skill, "Web", applier, duration, applyText, null); //TODO Implicit broadcast() call - may need changes?
            this.loc = location;
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            this.changeBlock(this.loc, hero);
            final Block block = this.loc.getBlock();
            this.changeBlock(block.getRelative(BlockFace.DOWN).getLocation(), hero);
            for (final BlockFace face : BlockFace.values()) {
                if (face.toString().contains("_") || (face == BlockFace.UP) || (face == BlockFace.DOWN)) {
                    continue;
                }
                Location blockLoc = block.getRelative(face).getLocation();
                this.changeBlock(blockLoc, hero);
                blockLoc = block.getRelative(this.getClockwise(face)).getLocation();
                this.changeBlock(blockLoc, hero);
                blockLoc = block.getRelative(face, 2).getLocation();
                this.changeBlock(blockLoc, hero);
            }
        }

        public Location getLocation() {
            return this.loc;
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            for (final Location location : this.locations) {
                location.getBlock().setType(Material.AIR);
                changedBlocks.remove(location);
            }
            this.locations.clear();
        }

        private void changeBlock(Location location, Hero hero) {
            final Block block = location.getBlock();
            switch (block.getType()) {
                case WATER:
                case LAVA:
                case SNOW:
                case AIR:
                    changedBlocks.add(location);
                    this.locations.add(location);
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
        return this.getDescription();
    }
}
