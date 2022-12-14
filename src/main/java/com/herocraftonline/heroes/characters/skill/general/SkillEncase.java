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
    private String applyText;
    private String expireText;
    private final BukkitScheduler scheduler;
    private final SkillListener listener;

    public SkillEncase(Heroes plugin) {
        super(plugin, "Encase");
        this.scheduler = plugin.getServer().getScheduler();
        setDescription("Encase your target in glass for $1 seconds, dealing $2 damage when the effect ends.");
        setUsage("/skill encase");
        setIdentifiers("skill encase");
        setTypes(SkillType.SILENCEABLE, SkillType.ABILITY_PROPERTY_LIGHT, SkillType.DEBUFFING);
        listener = new SkillListener();
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DAMAGE.node(), 0);
        node.set(SkillSetting.DAMAGE_INCREASE.node(), 0.5D);
        node.set(SkillSetting.DURATION.node(), 10000);
        node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% has been encased!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "Encasement removed from %hero%!");
        return node;
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 0.0, false)
                + (SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE, 0.0, false) * hero.getHeroLevel(this));
        return getDescription().replace("$1", String.valueOf(duration)).replace("$2", String.valueOf(damage));
    }
    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, "%hero% has been encased!").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "Encasement removed from %hero%!").replace("%hero%", "$1");
    }

    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();
        if (player == target)
            return SkillResult.INVALID_TARGET;

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 0.0, false)
                + (SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE, 0.0, false) * hero.getHeroLevel(this));

        if (!(damageCheck(player, target)))
            return SkillResult.INVALID_TARGET_NO_MSG;

        EncaseEffect effect = new EncaseEffect(this, player, duration,
                damage);
        CharacterTemplate character = plugin.getCharacterManager().getCharacter(target);
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

        private Location loc;

        private final double damage;

        public EncaseEffect(Skill skill, Player applier, long duration, double damage) {
            super(skill, "EncaseEffect", applier, 100L, duration, applyText, expireText);
            this.types.add(EffectType.DISABLE);
            this.types.add(EffectType.STUN);
            this.types.add(EffectType.LIGHT);
            this.damage = damage;
            this.blocks = new HashSet<>(20);
        }

        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            entomb(hero);
        }

        public void applyToMonster(Monster monster) {
            super.applyToMonster(monster);
            entomb(monster);
        }

        private void entomb(CharacterTemplate character) {
            LivingEntity entity = character.getEntity();
            scheduler.scheduleSyncDelayedTask(plugin, () -> {
                if(character.hasEffect("EncaseEffect")) {
                    skill.damageEntity(entity, applier,
                            this.damage, EntityDamageEvent.DamageCause.MAGIC, 0);
                }
            }, getDuration() / 1000 * 20L);

            Location pLoc = entity.getLocation();
            Location pBlockLoc = pLoc.getBlock().getLocation();
            Location tpLoc = new Location(pLoc.getWorld(),
                    pBlockLoc.getX() + 0.5D, pBlockLoc.getY(),
                    pBlockLoc.getZ() + 0.5D);
            tpLoc.setYaw(pLoc.getYaw());
            tpLoc.setPitch(pLoc.getPitch());
            entity.teleport(tpLoc);
            this.loc = tpLoc;

            Block glsLoc = character.getEntity().getLocation().getBlock();
            for (int y = 0; y < 2; y++) {
                for (int x = -1; x < 2; x++) {
                    for (int z = -1; z < 2; z++) {
                        if (glsLoc.getRelative(x, y, z).isEmpty()) {
                            Block iBlock = glsLoc.getRelative(x, y, z);
                            iBlock.setType(Material.GLASS);
                            blocks.add(iBlock);
                        }
                    }
                }
            }
            listener.effects.add(this);
        }

        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            removeBlocks();
            listener.effects.remove(this);
        }

        public void removeFromMonster(Monster monster) {
            super.removeFromMonster(monster);
            removeBlocks();
            listener.effects.remove(this);
        }

        public void removeBlocks() {
            for (Block bChange : this.blocks) {
                if (bChange.getType() == Material.GLASS)
                    bChange.setType(Material.AIR);
            }
        }

        public void tickHero(Hero hero) {
            tpBack(hero.getPlayer());
        }

        public void tickMonster(Monster monster) {
            tpBack(monster.getEntity());
        }

        public void tpBack(LivingEntity entity) {
            try {
                Location location = entity.getLocation();
                if (location.getX() != this.loc.getX() ||
                        location.getY() != this.loc.getY() ||
                        location.getZ() != this.loc.getZ()) {
                    this.loc.setYaw(location.getYaw());
                    this.loc.setPitch(location.getPitch());
                    entity.teleport(this.loc);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public class SkillListener implements Listener {
        private final List<EncaseEffect> effects;

        public SkillListener() {
            this.effects = new LinkedList<>();
        }

        public boolean isBlockLocked(Block block) {
            for(EncaseEffect effect : effects) {
                if(effect.blocks.contains(block)) {
                    return true;
                }
            }
            return false;
        }


        @EventHandler(priority = EventPriority.MONITOR)
        public void onPluginDisable(PluginDisableEvent e) {
            if (e.getPlugin() != plugin) {
                return;
            }
            effects.forEach(EncaseEffect::removeBlocks);
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onBlockPlace(BlockSpreadEvent event) {
            if (isBlockLocked(event.getBlock())) {
                event.setCancelled(true);
            }
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onBlockBreak(BlockBreakEvent event) {
            if (isBlockLocked(event.getBlock())) {
                event.setCancelled(true);
            }
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onBlockFromTo(BlockFromToEvent event) {
            Block fromBlock = event.getBlock();
            Block toBlock = event.getToBlock();
            if (isBlockLocked(toBlock) || isBlockLocked(fromBlock))
                event.setCancelled(true);
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onBlockPistonRetract(BlockPistonRetractEvent event) {
            if (event.getBlocks().stream().anyMatch(this::isBlockLocked))
                event.setCancelled(true);
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onHangingBreak(HangingBreakEvent event) {
            Block block = event.getEntity().getLocation().getBlock();
            if (isBlockLocked(block))
                event.setCancelled(true);
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onBlockPistonExtend(BlockPistonExtendEvent event) {
            if (event.getBlocks().stream().anyMatch(this::isBlockLocked))
                event.setCancelled(true);
        }
    }
}
