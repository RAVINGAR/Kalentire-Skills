package com.herocraftonline.heroes.characters.skill.general;

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
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Painting;
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

    private static final Set<Location> changedBlocks = new HashSet<>();
    private String applyText;

    public SkillWeb(final Heroes plugin) {
        super(plugin, "Web");
        setDescription("You conjure a web around your target that will hinder them and any nearby targets for $1 second(s).");
        setUsage("/skill web");
        setArgumentRange(0, 0);
        setIdentifiers("skill web");
        setTypes(SkillType.ABILITY_PROPERTY_DARK, SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.SILENCEABLE, SkillType.AGGRESSIVE, SkillType.AREA_OF_EFFECT);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillBlockListener(), plugin);
    }

    @Override
    public String getDescription(final Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 4000, false);
        final int durationIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE_PER_CHARISMA, 50, false);
        duration += hero.getAttributeValue(AttributeType.CHARISMA) * durationIncrease;

        final String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription().replace("$1", formattedDuration);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 8);
        node.set(SkillSetting.MAX_DISTANCE_INCREASE_PER_INTELLECT.node(), 0.15);
        node.set(SkillSetting.DURATION.node(), 2000);
        node.set(SkillSetting.DURATION_INCREASE_PER_CHARISMA.node(), 75);
        node.set("root-duration", 500);
        node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% conjured a web at %target%'s feet!");

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%hero% conjured a web at %target%'s feet!").replace("%hero%", "$2").replace("$hero$", "$2").replace("%target%", "$1").replace("$target$", "$1");
    }

    @Override
    public SkillResult use(final Hero hero, final LivingEntity target, final String[] args) {
        final Player player = hero.getPlayer();

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 4000, false);
        final int durationIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE_PER_CHARISMA, 50, false);
        duration += hero.getAttributeValue(AttributeType.CHARISMA) * durationIncrease;

        final long rootDuration = SkillConfigManager.getUseSetting(hero, this, "root-duration", 500, false);
        final WebEffect wEffect = new WebEffect(this, player, duration, rootDuration);

        final CharacterTemplate targCT = plugin.getCharacterManager().getCharacter(target);
        targCT.addEffect(wEffect);

        player.getWorld().playEffect(player.getLocation(), Effect.POTION_BREAK, 3);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SPIDER_AMBIENT, 0.8F, 1.0F);

        return SkillResult.NORMAL;
    }

    public static class SkillBlockListener implements Listener {

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onBlockBreak(final BlockBreakEvent event) {
            if (event.getBlock().getType() != Material.COBWEB) {
                return;
            }

            if (changedBlocks.contains(event.getBlock().getLocation())) {
                event.setCancelled(true);
            }
        }
    }

    private class WebEffect extends ExpirableEffect {

        private final List<Location> locations = new ArrayList<>();
        private Location loc;

        public WebEffect(final Skill skill, final Player applier, final long webDuration, final long rootDuration) {
            super(skill, "Web", applier, webDuration, applyText, null);

            types.add(EffectType.MAGIC);
            types.add(EffectType.HARMFUL);

            if (rootDuration > 0) {
                addPotionEffect(new PotionEffect(PotionEffectType.SLOW, (int) ((rootDuration / 1000.0) * 20), 127));      // Max slowness is 127
                addPotionEffect(new PotionEffect(PotionEffectType.JUMP, (int) ((rootDuration / 1000.0) * 20), 128));      // Max negative jump boost
            }
        }

        @Override
        public void applyToMonster(final Monster monster) {
            super.applyToMonster(monster);

            loc = monster.getEntity().getLocation();

            createWeb(monster.getEntity());
        }

        @Override
        public void applyToHero(final Hero hero) {
            super.applyToHero(hero);
            createWeb(hero.getPlayer());
        }

        @Override
        public void removeFromHero(final Hero hero) {
            super.removeFromHero(hero);

            revertBlocks();
        }

        @Override
        public void removeFromMonster(final Monster monster) {
            super.removeFromMonster(monster);

            revertBlocks();
        }

        private void createWeb(final Entity placedOnEntity) {

            loc = placedOnEntity.getLocation();

            final List<Entity> entities = placedOnEntity.getNearbyEntities(10, 10, 10);
            final List<Entity> blockEntities = new ArrayList<>();
            for (final Entity entity : entities) {
                if (entity instanceof ItemFrame) {
                    blockEntities.add(entity);
                } else if (entity instanceof Painting) {
                    blockEntities.add(entity);
                }
            }

            attemptToChangeBlock(blockEntities, loc);
            final Block block = loc.getBlock();
            attemptToChangeBlock(blockEntities, block.getRelative(BlockFace.DOWN).getLocation());
            for (final BlockFace face : BlockFace.values()) {
                if (face == BlockFace.UP || face == BlockFace.DOWN) {
                    continue;
                }

                final Location currentFaceLoc = block.getRelative(face).getLocation();
                attemptToChangeBlock(blockEntities, currentFaceLoc);

                attemptToChangeBlock(blockEntities, currentFaceLoc.getBlock().getRelative(BlockFace.UP).getLocation());
                attemptToChangeBlock(blockEntities, currentFaceLoc.getBlock().getRelative(BlockFace.DOWN).getLocation());

                final Location clockwiseFaceLoc = block.getRelative(getClockwise(face)).getLocation();
                attemptToChangeBlock(blockEntities, clockwiseFaceLoc);

                attemptToChangeBlock(blockEntities, clockwiseFaceLoc.getBlock().getRelative(BlockFace.UP).getLocation());
                attemptToChangeBlock(blockEntities, clockwiseFaceLoc.getBlock().getRelative(BlockFace.DOWN).getLocation());

                if (!(face.toString().contains("_"))) {
                    final Location sideBlock = block.getRelative(face, 2).getLocation();
                    attemptToChangeBlock(blockEntities, sideBlock);

                    attemptToChangeBlock(blockEntities, sideBlock.getBlock().getRelative(BlockFace.UP).getLocation());
                    attemptToChangeBlock(blockEntities, sideBlock.getBlock().getRelative(BlockFace.DOWN).getLocation());
                }
            }
        }

        private void revertBlocks() {
            for (final Location location : locations) {
                location.getBlock().setType(Material.AIR);
                changedBlocks.remove(location);
            }

            locations.clear();
        }

        private void attemptToChangeBlock(final List<Entity> blockEntities, final Location location) {
            final Block block = location.getBlock();
            switch (block.getType()) {
                case SNOW:
                case AIR:
                    boolean isBlockEntityBlock = false;
                    for (final Entity blockEntity : blockEntities) {
                        if (blockEntity.getLocation().getBlock().equals(block)) {
                            isBlockEntityBlock = true;
                        }
                    }
                    if (!isBlockEntityBlock) {
                        changedBlocks.add(location);
                        locations.add(location);
                        location.getBlock().setType(Material.COBWEB);
                    }
                    break;
                default:
            }
        }

        private BlockFace getClockwise(final BlockFace face) {
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
