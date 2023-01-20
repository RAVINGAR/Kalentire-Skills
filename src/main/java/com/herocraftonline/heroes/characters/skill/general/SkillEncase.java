package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.skill.Listenable;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.chat.ChatComponents;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
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
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class SkillEncase extends TargettedSkill implements Listenable {
    private final BukkitScheduler scheduler;
    private final SkillListener listener;
    private String applyText;
    private String expireText;

    public SkillEncase(final Heroes plugin) {
        super(plugin, "Encase");
        this.scheduler = plugin.getServer().getScheduler();
        setDescription("Encase your target in glass for $1 seconds, dealing $2 damage when the effect ends.");
        setUsage("/skill encase");
        setIdentifiers("skill encase");
        setTypes(SkillType.SILENCEABLE, SkillType.ABILITY_PROPERTY_LIGHT, SkillType.DEBUFFING);
        listener = new SkillListener();
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DAMAGE.node(), 0);
        node.set(SkillSetting.DAMAGE_INCREASE.node(), 0.5D);
        node.set(SkillSetting.DURATION.node(), 10000);
        node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% has been encased!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "Encasement removed from %hero%!");
        return node;
    }

    @Override
    public String getDescription(final Hero hero) {
        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
        final double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 0.0, false)
                + (SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE, 0.0, false) * hero.getHeroLevel(this));
        return getDescription().replace("$1", String.valueOf(duration)).replace("$2", String.valueOf(damage));
    }

    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, "%hero% has been encased!").replace("%hero%", "$1").replace("$hero$", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "Encasement removed from %hero%!").replace("%hero%", "$1").replace("$hero$", "$1");
    }

    @Override
    public SkillResult use(final Hero hero, final LivingEntity target, final String[] args) {
        final Player player = hero.getPlayer();
        if (player == target) {
            return SkillResult.INVALID_TARGET;
        }

        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
        final double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 0.0, false)
                + (SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE, 0.0, false) * hero.getHeroLevel(this));

        if (!(damageCheck(player, target))) {
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        final EncaseEffect effect = new EncaseEffect(this, player, duration,
                damage);
        final CharacterTemplate character = plugin.getCharacterManager().getCharacter(target);
        character.addEffect(effect);
        return SkillResult.NORMAL;
    }

    @NotNull
    @Override
    public Listener getListener() {
        return listener;
    }

    public class EncaseEffect extends PeriodicExpirableEffect {
        private final Set<Block> blocks;
        private final double damage;
        private Location loc;

        public EncaseEffect(final Skill skill, final Player applier, final long duration, final double damage) {
            super(skill, "EncaseEffect", applier, 100L, duration, applyText, expireText);
            this.types.add(EffectType.DISABLE);
            this.types.add(EffectType.STUN);
            this.types.add(EffectType.LIGHT);
            this.damage = damage;
            this.blocks = new HashSet<>(20);
        }

        @Override
        public void applyToHero(final Hero hero) {
            super.applyToHero(hero);
            entomb(hero);
        }

        @Override
        public void applyToMonster(final Monster monster) {
            super.applyToMonster(monster);
            entomb(monster);
        }

        private void entomb(final CharacterTemplate character) {
            final LivingEntity entity = character.getEntity();
            scheduler.scheduleSyncDelayedTask(plugin, () -> {
                if (character.hasEffect("EncaseEffect")) {
                    skill.damageEntity(entity, applier,
                            this.damage, EntityDamageEvent.DamageCause.MAGIC, 0);
                }
            }, getDuration() / 1000 * 20L);

            final Location pLoc = entity.getLocation();
            final Location pBlockLoc = pLoc.getBlock().getLocation();
            final Location tpLoc = new Location(pLoc.getWorld(),
                    pBlockLoc.getX() + 0.5D, pBlockLoc.getY(),
                    pBlockLoc.getZ() + 0.5D);
            tpLoc.setYaw(pLoc.getYaw());
            tpLoc.setPitch(pLoc.getPitch());
            entity.teleport(tpLoc);
            this.loc = tpLoc;

            final Block glsLoc = character.getEntity().getLocation().getBlock();
            for (int y = 0; y < 2; y++) {
                for (int x = -1; x < 2; x++) {
                    for (int z = -1; z < 2; z++) {
                        if (glsLoc.getRelative(x, y, z).isEmpty()) {
                            final Block iBlock = glsLoc.getRelative(x, y, z);
                            iBlock.setType(Material.GLASS);
                            blocks.add(iBlock);
                        }
                    }
                }
            }
            listener.effects.add(this);
        }

        @Override
        public void removeFromHero(final Hero hero) {
            super.removeFromHero(hero);
            removeBlocks();
            listener.effects.remove(this);
        }

        @Override
        public void removeFromMonster(final Monster monster) {
            super.removeFromMonster(monster);
            removeBlocks();
            listener.effects.remove(this);
        }

        public void removeBlocks() {
            for (final Block bChange : this.blocks) {
                if (bChange.getType() == Material.GLASS) {
                    bChange.setType(Material.AIR);
                }
            }
        }

        @Override
        public void tickHero(final Hero hero) {
            tpBack(hero.getPlayer());
        }

        @Override
        public void tickMonster(final Monster monster) {
            tpBack(monster.getEntity());
        }

        public void tpBack(final LivingEntity entity) {
            try {
                final Location location = entity.getLocation();
                if (location.getX() != this.loc.getX() ||
                        location.getY() != this.loc.getY() ||
                        location.getZ() != this.loc.getZ()) {
                    this.loc.setYaw(location.getYaw());
                    this.loc.setPitch(location.getPitch());
                    entity.teleport(this.loc);
                }
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
    }

    public class SkillListener implements Listener {
        private final List<EncaseEffect> effects;

        public SkillListener() {
            this.effects = new LinkedList<>();
        }

        public boolean isBlockLocked(final Block block) {
            for (final EncaseEffect effect : effects) {
                if (effect.blocks.contains(block)) {
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
            effects.forEach(EncaseEffect::removeBlocks);
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
