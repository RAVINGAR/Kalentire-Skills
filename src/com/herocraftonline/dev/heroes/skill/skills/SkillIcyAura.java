package com.herocraftonline.dev.heroes.skill.skills;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.api.SkillResult;
import com.herocraftonline.dev.heroes.effects.EffectType;
import com.herocraftonline.dev.heroes.effects.PeriodicExpirableEffect;
import com.herocraftonline.dev.heroes.effects.common.SlowEffect;
import com.herocraftonline.dev.heroes.hero.Hero;
import com.herocraftonline.dev.heroes.skill.ActiveSkill;
import com.herocraftonline.dev.heroes.skill.Skill;
import com.herocraftonline.dev.heroes.skill.SkillConfigManager;
import com.herocraftonline.dev.heroes.skill.SkillType;
import com.herocraftonline.dev.heroes.util.Setting;

public class SkillIcyAura extends ActiveSkill {

    private String applyText;
    private String expireText;
    private static Map<Hero, Map<Location, Material>> changedBlocks = new HashMap<Hero, Map<Location, Material>>();
    private static final Set<Material> allowedBlocks;

    public SkillIcyAura(Heroes plugin) {
        super(plugin, "IcyAura");
        setDescription("You emit an aura of ice damaging nearby enemies for $1 seconds.");
        setUsage("/skill icyaura");
        setArgumentRange(0, 0);
        setIdentifiers("skill icyaura");
        setTypes(SkillType.BUFF, SkillType.SILENCABLE, SkillType.ICE);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillBlockListener(), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(Setting.DURATION.node(), 10000);
        node.set(Setting.PERIOD.node(), 2000);
        node.set("tick-damage", 1);
        node.set(Setting.RADIUS.node(), 10);
        node.set("amplitude", 2);
        node.set(Setting.APPLY_TEXT.node(), "%hero% is emitting ice!");
        node.set(Setting.EXPIRE_TEXT.node(), "%hero% has stopped emitting ice!");
        return node;
    }

    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, Setting.APPLY_TEXT.node(), "%hero% is emitting ice!").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, Setting.EXPIRE_TEXT.node(), "%hero% has stopped emitting ice!").replace("%hero%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        broadcastExecuteText(hero);

        long duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION.node(), 10000, false);
        long period = SkillConfigManager.getUseSetting(hero, this, Setting.PERIOD.node(), 500, true);
        int tickDamage = SkillConfigManager.getUseSetting(hero, this, "tick-damage", 1, false);
        int range = SkillConfigManager.getUseSetting(hero, this, Setting.RADIUS.node(), 10, false);
        hero.addEffect(new IcyAuraEffect(this, duration, period, tickDamage, range));
        return SkillResult.NORMAL;
    }

    static {
        allowedBlocks = new HashSet<Material>();
        allowedBlocks.add(Material.STONE);
        allowedBlocks.add(Material.SAND);
        allowedBlocks.add(Material.SNOW);
        allowedBlocks.add(Material.SNOW_BLOCK);
        allowedBlocks.add(Material.DIRT);
        allowedBlocks.add(Material.GRASS);
        allowedBlocks.add(Material.SOIL);
        allowedBlocks.add(Material.CLAY);
        allowedBlocks.add(Material.WATER);
        allowedBlocks.add(Material.STATIONARY_WATER);
    }

    public class SkillBlockListener implements Listener {

        @EventHandler(priority = EventPriority.HIGHEST)
        public void onBlockBreak(BlockBreakEvent event) {
            if (event.isCancelled()) {
                return;
            }

            // Check out mappings to see if this block was a changed block, if so lets deny breaking it.
            for (Map<Location, Material> blockMap : changedBlocks.values()) {
                for (Location loc : blockMap.keySet())
                    if (event.getBlock().getLocation().equals(loc)) {
                        event.setCancelled(true);
                    }
            }
        }
    }

    public class IcyAuraEffect extends PeriodicExpirableEffect {

        private final int tickDamage;
        private final int range;

        public IcyAuraEffect(SkillIcyAura skill, long duration, long period, int tickDamage, int range) {
            super(skill, "IcyAura", period, duration);
            this.tickDamage = tickDamage;
            this.range = range;
            this.types.add(EffectType.DISPELLABLE);
            this.types.add(EffectType.BENEFICIAL);
            this.types.add(EffectType.ICE);
        }

        @Override
        public void apply(Hero hero) {
            super.apply(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), applyText, player.getDisplayName());
        }

        @Override
        public void remove(Hero hero) {
            super.remove(hero);
            Player player = hero.getPlayer();
            if (changedBlocks.get(hero) != null) {
                for (Entry<Location, Material> entry : changedBlocks.get(hero).entrySet()) {
                    entry.getKey().getBlock().setType(entry.getValue());
                }

                // CleanUp
                changedBlocks.get(hero).clear();
                changedBlocks.remove(hero);
            }
            broadcast(player.getLocation(), expireText, player.getDisplayName());
        }

        @Override
        public void tick(Hero hero) {
            super.tick(hero);

            Player player = hero.getPlayer();
            Location loc = player.getLocation().clone();
            loc.setY(loc.getY() - 1);
            changeBlock(loc, hero);

            int amplitude = SkillConfigManager.getUseSetting(hero, skill, "amplitude", 2, false);
            SlowEffect sEffect = new SlowEffect(skill, this.getPeriod(), amplitude, true, null, null, hero);
            for (Entity entity : player.getNearbyEntities(range, range, range)) {
                if (entity instanceof LivingEntity) {
                    LivingEntity lEntity = (LivingEntity) entity;

                    // Check if the target is damagable
                    if (!damageCheck(player, lEntity)) {
                        continue;
                    }

                    addSpellTarget(lEntity, hero);
                    Skill.damageEntity(lEntity, player, tickDamage, DamageCause.MAGIC);
                    loc = lEntity.getLocation().clone();
                    loc.setY(loc.getY() - 1);
                    changeBlock(loc, hero);
                    if (lEntity instanceof Player) {
                        plugin.getHeroManager().getHero((Player) lEntity).addEffect(sEffect);
                    } else
                        plugin.getEffectManager().addEntityEffect(lEntity, sEffect);

                }
            }

        }

        private void changeBlock(Location loc, Hero hero) {
            Map<Location, Material> heroChangedBlocks = changedBlocks.get(hero);
            if (heroChangedBlocks == null) {
                changedBlocks.put(hero, new HashMap<Location, Material>());
            }
            if (loc.getBlock().getType() != Material.ICE && allowedBlocks.contains(loc.getBlock().getTypeId())) {
                changedBlocks.get(hero).put(loc, loc.getBlock().getType());
                loc.getBlock().setType(Material.ICE);
            }
        }
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 10000, false);
        return getDescription().replace("$1", duration / 1000 + "");
    }
}
