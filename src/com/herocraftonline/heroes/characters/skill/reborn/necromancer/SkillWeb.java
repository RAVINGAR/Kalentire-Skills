package com.herocraftonline.heroes.characters.skill.reborn.necromancer;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SkillWeb extends TargettedSkill {

    private String applyText;
    private static Set<Location> changedBlocks = new HashSet<Location>();

    public SkillWeb(Heroes plugin) {
        super(plugin, "Web");
        setDescription("You conjure a web around your target that will hinder them and any nearby targets for $1 second(s).");
        setUsage("/skill web");
        setArgumentRange(0, 0);
        setIdentifiers("skill web");
        setTypes(SkillType.ABILITY_PROPERTY_DARK, SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.SILENCEABLE, SkillType.AGGRESSIVE, SkillType.AREA_OF_EFFECT);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillBlockListener(), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 4000, false);
        int durationIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE_PER_CHARISMA, 50, false);
        duration += hero.getAttributeValue(AttributeType.CHARISMA) * durationIncrease;

        String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription().replace("$1", formattedDuration);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.MAX_DISTANCE.node(), 8);
        config.set(SkillSetting.MAX_DISTANCE_INCREASE_PER_INTELLECT.node(), 0.0);
        config.set(SkillSetting.DURATION.node(), 2000);
        config.set(SkillSetting.DURATION_INCREASE_PER_CHARISMA.node(), 0);
        config.set("root-duration", 500);
        config.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% conjured a web at %target%'s feet!");
        return config;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT,
                ChatComponents.GENERIC_SKILL + "%hero% conjured a web at %target%'s feet!")
                .replace("%hero%", "$2")
                .replace("%target%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 4000, false);
        int durationIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE_PER_CHARISMA, 0, false);
        duration += hero.getAttributeValue(AttributeType.CHARISMA) * durationIncrease;

        long rootDuration = SkillConfigManager.getUseSetting(hero, this, "root-duration", 500, false);
        WebEffect wEffect = new WebEffect(this, player, duration, rootDuration);

        CharacterTemplate targCT = plugin.getCharacterManager().getCharacter(target);
        targCT.addEffect(wEffect);

        player.getWorld().playEffect(player.getLocation(), Effect.POTION_BREAK, 3);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SPIDER_AMBIENT, 0.8F, 1.0F);

        return SkillResult.NORMAL;
    }

    public class SkillBlockListener implements Listener {

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onBlockBreak(BlockBreakEvent event) {
            if (event.getBlock().getType() != Material.WEB)
                return;

            if (changedBlocks.contains(event.getBlock().getLocation())) {
                event.setCancelled(true);
            }
        }
    }

    private class WebEffect extends ExpirableEffect {

        private List<Location> locations = new ArrayList<>();
        private Location loc;

        public WebEffect(Skill skill, Player applier, long webDuration, long duration) {
            super(skill, "Web", applier, webDuration, applyText, null);

            types.add(EffectType.MAGIC);
            types.add(EffectType.HARMFUL);

            if (duration > 0) {
                addPotionEffect(new PotionEffect(PotionEffectType.SLOW, (int) duration / 50, 127), false);      // Max slowness is 127
                addPotionEffect(new PotionEffect(PotionEffectType.JUMP, (int) duration / 50, 128), false);      // Max negative jump boost
            }
        }

        @Override
        public void applyToMonster(Monster monster) {
            super.applyToMonster(monster);

            loc = monster.getEntity().getLocation();
            createWeb(monster.getEntity());
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);

            createWeb(hero.getPlayer());
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

        private void createWeb(Entity placedOnEntity) {
            loc = placedOnEntity.getLocation();

            List<Entity> entities = placedOnEntity.getNearbyEntities(10, 10, 10);
            List<Entity> ignoreEntities = new ArrayList<>();
            for (Entity entity : entities) {
                if (entity instanceof Hanging) {
                    ignoreEntities.add(entity);
                }
            }

            attemptToChangeBlock(ignoreEntities, loc);
            Block block = loc.getBlock();
            attemptToChangeBlock(ignoreEntities, block.getRelative(BlockFace.DOWN).getLocation());
            for (BlockFace face : BlockFace.values()) {
                if (face == BlockFace.UP || face == BlockFace.DOWN) {
                    continue;
                }

                Location currentFaceLoc = block.getRelative(face).getLocation();
                attemptToChangeBlock(ignoreEntities, currentFaceLoc);

                attemptToChangeBlock(ignoreEntities, currentFaceLoc.getBlock().getRelative(BlockFace.UP).getLocation());
                attemptToChangeBlock(ignoreEntities, currentFaceLoc.getBlock().getRelative(BlockFace.DOWN).getLocation());

                Location clockwiseFaceLoc = block.getRelative(getClockwise(face)).getLocation();
                attemptToChangeBlock(ignoreEntities, clockwiseFaceLoc);

                attemptToChangeBlock(ignoreEntities, clockwiseFaceLoc.getBlock().getRelative(BlockFace.UP).getLocation());
                attemptToChangeBlock(ignoreEntities, clockwiseFaceLoc.getBlock().getRelative(BlockFace.DOWN).getLocation());

                if (!(face.toString().contains("_"))) {
                    Location sideBlock = block.getRelative(face, 2).getLocation();
                    attemptToChangeBlock(ignoreEntities, sideBlock);

                    attemptToChangeBlock(ignoreEntities, sideBlock.getBlock().getRelative(BlockFace.UP).getLocation());
                    attemptToChangeBlock(ignoreEntities, sideBlock.getBlock().getRelative(BlockFace.DOWN).getLocation());
                }
            }
        }

        private void revertBlocks() {
            for (Location location : locations) {
                location.getBlock().setType(Material.AIR);
                changedBlocks.remove(location);
            }

            locations.clear();
        }

        private void attemptToChangeBlock(List<Entity> blockEntities, Location location) {
            Block block = location.getBlock();
            switch (block.getType()) {
                case SNOW:
                case AIR:
                    boolean isBlockEntityBlock = false;
                    for (Entity blockEntity : blockEntities) {
                        if (blockEntity.getLocation().getBlock().equals(block))
                            isBlockEntityBlock = true;
                    }
                    if (!isBlockEntityBlock) {
                        changedBlocks.add(location);
                        locations.add(location);
                        location.getBlock().setType(Material.WEB);
                    }
                    break;
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
