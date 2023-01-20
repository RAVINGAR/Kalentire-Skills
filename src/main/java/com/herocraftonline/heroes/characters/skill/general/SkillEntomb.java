package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.Listenable;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.chat.ChatComponents;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.material.Attachable;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class SkillEntomb extends TargettedSkill implements Listenable {
    public static final String ENTOMB_EFFECT = "Entombed";
    private static final Random random = new Random(System.currentTimeMillis());
    private static final List<Material> blockTypes = Arrays.asList(
            Material.STONE,
            Material.COARSE_DIRT,
            Material.DIRT,
            Material.GRASS_BLOCK,
            Material.ANDESITE,
            Material.DIORITE,
            Material.GRANITE
    );
    private final BukkitScheduler scheduler;
    private final SkillBlockListener listener = new SkillBlockListener();
    private String applyText;
    private String expireText;

    public SkillEntomb(final Heroes plugin) {
        super(plugin, "Entomb");
        this.scheduler = plugin.getServer().getScheduler();
        setDescription("Entombs a target for $1 second(s). Dealing $2 damage if the effect is not interrupted.");
        setUsage("/skill entomb");
        setArgumentRange(0, 0);
        setIdentifiers("skill entomb");
        setTypes(SkillType.ABILITY_PROPERTY_EARTH, SkillType.SILENCEABLE, SkillType.BLOCK_CREATING, SkillType.MULTI_GRESSIVE);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.RADIUS.node(), 3);
        node.set(SkillSetting.MAX_DISTANCE.node(), 12);
        node.set(SkillSetting.DURATION.node(), 5000);
        node.set(SkillSetting.DAMAGE.node(), 0);
        node.set(SkillSetting.DAMAGE_INCREASE.node(), 0);
        node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% has been entombed!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% is no longer entombed!");

        return node;
    }

    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, "%hero% has been entombed!").replace("%hero%", "$1").replace("$hero$", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "%hero% is no longer entombed!").replace("%hero%", "$1").replace("$hero$", "$1");
    }

    @Override
    public String getDescription(final Hero hero) {
        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
        final double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 0.0, false)
                + (SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE, 0.0, false) * hero.getHeroLevel(this));
        return getDescription().replace("$1", String.valueOf(duration)).replace("$2", String.valueOf(damage));
    }

    @Override
    public SkillResult use(final Hero hero, final LivingEntity targetEnt, final String[] strings) {
        final Player player = hero.getPlayer();

        final int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 4, false);
        final long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
        final double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 0.0, false)
                + (SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE, 0.0, false) * hero.getHeroLevel(this));

        if (!(damageCheck(player, targetEnt))) {
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        final ExpirableEffect effect = new EntombedEffect(this, player, duration, radius, damage);
        final CharacterTemplate character = plugin.getCharacterManager().getCharacter(targetEnt);
        character.addEffect(effect);

        return SkillResult.NORMAL;
    }

    @NotNull
    @Override
    public Listener getListener() {
        return listener;
    }

    public class EntombedEffect extends ExpirableEffect {
        private final int radius;
        private final double damage;

        private final Set<Block> blocksToLock = new HashSet<>();
        private final Set<Block> changedBlocks = new HashSet<>();

        public EntombedEffect(final Skill skill, final Player applier, final long duration, final int radius, final double damage) {
            super(skill, ENTOMB_EFFECT, applier, duration, applyText, expireText);
            this.radius = radius;
            this.damage = damage;

            types.add(EffectType.HARMFUL);
            types.add(EffectType.EARTH);
            types.add(EffectType.MAGIC);
        }

        @Override
        public void applyToMonster(final Monster monster) {
            super.applyToMonster(monster);
            entombTarget(monster);
        }

        @Override
        public void applyToHero(final Hero hero) {
            super.applyToHero(hero);
            entombTarget(hero);
        }

        private void entombTarget(final CharacterTemplate character) {
            final LivingEntity target = character.getEntity();
            scheduler.scheduleSyncDelayedTask(plugin, () -> {
                if (character.hasEffect(ENTOMB_EFFECT)) {
                    damageEntity(target, applier, damage, EntityDamageEvent.DamageCause.MAGIC, 0.0f);
                }
            }, getDuration() / 1000 * 20);
            final Location location = target.getLocation();
            final World world = location.getWorld();
            final Set<BoundingBox> hangingBlockBoxes = new HashSet<>();
            final Collection<Entity> nearbyEntities = world.getNearbyEntities(location, radius, radius, radius);
            for (final Entity entity : nearbyEntities) {
                if (entity == target) {
                    continue;
                }
                if (entity instanceof Attachable) {
                    hangingBlockBoxes.add(entity.getBoundingBox());
                }
            }

            final List<Block> sphereBlocks = getBlocksInSphere(location, radius, true);
            for (final Block block : sphereBlocks) {
                blocksToLock.add(block);
                if (!block.isEmpty() || hangingBlockBoxes.stream().anyMatch(x -> x.contains(block.getX(), block.getY(), block.getZ()))) {
                    continue;
                }

                final int randomIndex = random.nextInt(blockTypes.size() - 1);
                block.setType(blockTypes.get(randomIndex));
                changedBlocks.add(block);
            }
            listener.trackedEffects.add(this);
        }

        @Override
        public void removeFromMonster(final Monster monster) {
            super.removeFromMonster(monster);
            revertBlocks();
        }

        @Override
        public void removeFromHero(final Hero hero) {
            super.removeFromHero(hero);
            revertBlocks();
        }

        private List<Block> getBlocksInSphere(final Location centerLocation, final int radius, final boolean hollow) {
            final List<Block> circleBlocks = new ArrayList<>();
            final World world = centerLocation.getWorld();
            final int bx = centerLocation.getBlockX();
            final int by = centerLocation.getBlockY();
            final int bz = centerLocation.getBlockZ();

            for (int x = bx - radius; x <= bx + radius; x++) {
                for (int y = by - radius; y <= by + radius; y++) {
                    for (int z = bz - radius; z <= bz + radius; z++) {
                        final double distance = ((bx - x) * (bx - x) + ((bz - z) * (bz - z)) + ((by - y) * (by - y)));
                        if (distance < radius * radius && !(hollow && distance < ((radius - 1) * (radius - 1)))) {
                            final Block block = world.getBlockAt(x, y, z);
                            circleBlocks.add(block);
                        }
                    }
                }
            }
            return circleBlocks;
        }

        private void revertBlocks() {
            clearBlocks();
            listener.trackedEffects.remove(this);
        }

        private void clearBlocks() {
            for (final Block block : changedBlocks) {
                block.setType(Material.AIR);
            }
            //changedBlocks.clear(); might cause concurrency issues
            //blocksToLock.clear();
        }
    }

    public class SkillBlockListener implements Listener {
        private final List<EntombedEffect> trackedEffects = new LinkedList<>();

        public SkillBlockListener() {

        }

        public boolean isBlockLocked(final Block block) {
            for (final EntombedEffect effect : trackedEffects) {
                if (effect.blocksToLock.contains(block)) {
                    return true;
                }
            }
            return false;
        }


        @EventHandler(priority = EventPriority.MONITOR)
        public void onPluginDisable(final PluginDisableEvent e) {
            if (e.getPlugin() != plugin) {
                return;
            }
            trackedEffects.forEach(EntombedEffect::clearBlocks);
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onBlockPlace(final BlockSpreadEvent event) {
            if (isBlockLocked(event.getBlock())) {
                event.setCancelled(true);
            }
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onBlockBreak(final BlockBreakEvent event) {
            if (isBlockLocked(event.getBlock())) {
                event.setCancelled(true);
            }
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onBlockFromTo(final BlockFromToEvent event) {
            final Block fromBlock = event.getBlock();
            final Block toBlock = event.getToBlock();
            if (isBlockLocked(toBlock) || isBlockLocked(fromBlock)) {
                event.setCancelled(true);
            }
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onBlockPistonRetract(final BlockPistonRetractEvent event) {
            if (event.getBlocks().stream().anyMatch(this::isBlockLocked)) {
                event.setCancelled(true);
            }
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onHangingBreak(final HangingBreakEvent event) {
            final Block block = event.getEntity().getLocation().getBlock();
            if (isBlockLocked(block)) {
                event.setCancelled(true);
            }
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onBlockPistonExtend(final BlockPistonExtendEvent event) {
            if (event.getBlocks().stream().anyMatch(this::isBlockLocked)) {
                event.setCancelled(true);
            }
        }
    }
}
