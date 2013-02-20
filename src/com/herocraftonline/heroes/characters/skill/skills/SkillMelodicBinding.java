package com.herocraftonline.heroes.characters.skill.skills;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.effects.common.SlowEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Setting;

public class SkillMelodicBinding extends ActiveSkill {

    private String applyText;
    private String expireText;
    private static Map<Hero, Map<Location, Material>> changedBlocks = new HashMap<Hero, Map<Location, Material>>();
    private static final Set<Material> allowedBlocks;

    public SkillMelodicBinding(Heroes plugin) {
        super(plugin, "MelodicBinding");
        setDescription("You resonate melodic bindings, slowing and damaging nearby enemies for $1 seconds.");
        setUsage("/skill melodicbinding");
        setArgumentRange(0, 0);
        setIdentifiers("skill melodicbinding");
        setTypes(SkillType.DEBUFF, SkillType.MOVEMENT, SkillType.SILENCABLE, SkillType.HARMFUL);
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
        applyText = SkillConfigManager.getRaw(this, Setting.APPLY_TEXT.node(), "%hero% produces a binding melody").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, Setting.EXPIRE_TEXT.node(), "%hero% stops producing a melody!").replace("%hero%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        broadcastExecuteText(hero);

        long duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION.node(), 10000, false);
        long period = SkillConfigManager.getUseSetting(hero, this, Setting.PERIOD.node(), 500, true);
        int tickDamage = SkillConfigManager.getUseSetting(hero, this, "tick-damage", 1, false);
        int range = SkillConfigManager.getUseSetting(hero, this, Setting.RADIUS.node(), 10, false);
        hero.addEffect(new MelodicBindingEffect(this, duration, period, tickDamage, range));
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.NOTE_PIANO , 0.8F, 6.0F); 
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.NOTE_PIANO , 0.8F, 2.0F); 
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.NOTE_PIANO , 0.8F, 8.0F); 
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.NOTE_PIANO , 0.8F, 3.0F);  
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

    public class MelodicBindingEffect extends PeriodicExpirableEffect {

        private final int tickDamage;
        private final int range;

        public MelodicBindingEffect(SkillMelodicBinding skill, long duration, long period, int tickDamage, int range) {
            super(skill, "MelodicBinding", period, duration);
            this.tickDamage = tickDamage;
            this.range = range;
            this.types.add(EffectType.DISPELLABLE);
            this.types.add(EffectType.BENEFICIAL);
            this.types.add(EffectType.ICE);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), applyText, player.getDisplayName());
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
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
        public void tickHero(Hero hero) {
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
                    plugin.getCharacterManager().getCharacter(lEntity).addEffect(sEffect);
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

        @Override
        public void tickMonster(Monster monster) { }
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 10000, false);
        return getDescription().replace("$1", duration / 1000 + "");
    }
}
