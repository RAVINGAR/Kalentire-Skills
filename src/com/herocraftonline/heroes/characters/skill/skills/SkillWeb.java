package com.herocraftonline.heroes.characters.skill.skills;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
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
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Messaging;

public class SkillWeb extends TargettedSkill {

    private String applyText;
    private static Set<Location> changedBlocks = new HashSet<Location>();

    public SkillWeb(Heroes plugin) {
        super(plugin, "Web");
        setDescription("You conjure a web around your target.");
        setUsage("/skill web");
        setArgumentRange(0, 0);
        setIdentifiers("skill web");
        setTypes(SkillType.ABILITY_PROPERTY_DARK, SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.SILENCABLE, SkillType.AGGRESSIVE);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillBlockListener(), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), Integer.valueOf(8));
        node.set(SkillSetting.MAX_DISTANCE_INCREASE_PER_INTELLECT.node(), Double.valueOf(0.15));
        node.set(SkillSetting.DURATION.node(), Integer.valueOf(2000));
        node.set(SkillSetting.DURATION_INCREASE_PER_CHARISMA.node(), Integer.valueOf(75));
        node.set("root-duration", Integer.valueOf(500));
        node.set(SkillSetting.APPLY_TEXT.node(), Messaging.getSkillDenoter() + "%hero% conjured a web at %target%'s feet!");

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, Messaging.getSkillDenoter() + "%hero% conjured a web at %target%'s feet!").replace("%hero%", "$1").replace("%target%", "$2");
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 4000, false);
        int durationIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE_PER_CHARISMA, 50, false);
        duration += hero.getAttributeValue(AttributeType.CHARISMA) * durationIncrease;

        long rootDuration = SkillConfigManager.getUseSetting(hero, this, "root-duration", 500, false);
        WebEffect wEffect = new WebEffect(this, player, duration, rootDuration);

        CharacterTemplate targCT = plugin.getCharacterManager().getCharacter((LivingEntity) target);
        targCT.addEffect(wEffect);

        player.getWorld().playEffect(player.getLocation(), Effect.POTION_BREAK, 3);
        player.getWorld().playSound(player.getLocation(), Sound.SPIDER_IDLE, 0.8F, 1.0F);

        return SkillResult.NORMAL;
    }

    public class SkillBlockListener implements Listener {

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onBlockBreak(BlockBreakEvent event) {
            if (changedBlocks.contains(event.getBlock().getLocation())) {
                event.setCancelled(true);
            }
        }
    }

    private class WebEffect extends ExpirableEffect {

        private List<Location> locations = new ArrayList<Location>();
        private Location loc;

        public WebEffect(Skill skill, Player applier, long webDuration, long rootDuration) {
            super(skill, "Web", applier, webDuration);

            types.add(EffectType.MAGIC);
            types.add(EffectType.HARMFUL);

            this.applier = applier;

            addMobEffect(2, (int) ((rootDuration / 1000.0) * 20), 127, false);      // Max slowness is 127
            addMobEffect(8, (int) ((rootDuration / 1000.0) * 20), 128, false);      // Max negative jump boost
        }

        @Override
        public void applyToMonster(Monster monster) {
            super.applyToMonster(monster);

            loc = monster.getEntity().getLocation();

            broadcast(loc, applyText, applier.getDisplayName(), Messaging.getLivingEntityName(monster));

            createWeb();
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);

            Player player = hero.getPlayer();
            loc = player.getLocation();

            broadcast(loc, applyText, applier.getDisplayName(), player.getDisplayName());

            createWeb();
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);

            revertBlocks();
        }

        @Override
        public void removeFromMonster(Monster monster) {
            super.removeFromMonster(monster);

            revertBlocks();
        }

        private void createWeb() {
            attemptToChangeBlock(loc);
            Block block = loc.getBlock();
            attemptToChangeBlock(block.getRelative(BlockFace.DOWN).getLocation());
            for (BlockFace face : BlockFace.values()) {
                if (face.toString().contains("_") || face == BlockFace.UP || face == BlockFace.DOWN) {
                    continue;
                }
                Location blockLoc = block.getRelative(face).getLocation();
                attemptToChangeBlock(blockLoc);
                blockLoc = block.getRelative(getClockwise(face)).getLocation();
                attemptToChangeBlock(blockLoc);
                blockLoc = block.getRelative(face, 2).getLocation();
                attemptToChangeBlock(blockLoc);
            }
        }

        private void revertBlocks() {
            for (Location location : locations) {
                location.getBlock().setType(Material.AIR);
                changedBlocks.remove(location);
            }

            locations.clear();
        }

        private void attemptToChangeBlock(Location location) {
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
}
