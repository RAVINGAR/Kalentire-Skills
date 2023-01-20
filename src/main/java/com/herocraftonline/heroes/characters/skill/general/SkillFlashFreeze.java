package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.common.StunEffect;
import com.herocraftonline.heroes.characters.skill.*;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SkillFlashFreeze
        extends TargettedSkill {
    final List<Location> locations = new ArrayList<>();

    public SkillFlashFreeze(Heroes plugin) {
        super(plugin, "FlashFreeze");
        setDescription("You encase your target in a block of ice for $1 second(s). During this time they cannot attack or be damaged.");
        setUsage("/skill flashfreeze");
        setArgumentRange(0, 0);
        setIdentifiers("skill flashfreeze");
        setTypes(SkillType.SILENCEABLE, SkillType.DEBUFFING, SkillType.ABILITY_PROPERTY_ICE);
        Bukkit.getServer().getPluginManager().registerEvents(new FlashFreezeBreakListener(), plugin);
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DURATION.node(), 3000);
        node.set(SkillSetting.DURATION_INCREASE_PER_CHARISMA.node(), 100);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 3000, false);
        duration += SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE_PER_CHARISMA, 100, false)
                * hero.getAttributeValue(AttributeType.CHARISMA);

        if (!damageCheck(player, target)) return SkillResult.INVALID_TARGET_NO_MSG;

        final Location base = target.getLocation().add(0, 0.1, 0).getBlock().getLocation();

        final HashMap<Location, Material> mats = new HashMap<>();

        List<Entity> nearby = target.getNearbyEntities(1.2, 1.2, 1.2);
        for (Entity e : nearby) {
            if (!(e instanceof LivingEntity) || e == target) continue;
            LivingEntity ent = (LivingEntity) e;
            Location tLoc = target.getLocation();
            Location eLoc = ent.getLocation();
            double xDir = (tLoc.getX() - eLoc.getX()) / 3D;
            double zDir = (tLoc.getZ() - eLoc.getZ()) / 3D;
            Vector vec = new Vector(xDir, 0.3, zDir);
            ent.setVelocity(vec); // The entire point of this is to not suffocate stuff.
        }

        new BukkitRunnable() {
            boolean revert = false;

            public void run() {
                if (!revert) {
                    for (int y = 0; y < 3; y++) {
                        for (int x = -1; x <= 1; x++) {
                            for (int z = -1; z <= 1; z++) {
                                double bX = base.getBlockX();
                                double bY = base.getBlockY();
                                double bZ = base.getBlockZ();
                                bY += y;
                                bX += x;
                                bZ += z;
                                Location affected = new Location(base.getWorld(), bX, bY, bZ);
                                if (affected.getBlock().getType() != Material.BEDROCK
                                        && affected.getBlock().getType() != Material.OBSIDIAN
                                        && affected.getBlock().getState().getClass().getName().endsWith("CraftBlockState")) //solid block check
                                    locations.add(affected);
                            }
                        }
                    }
                    for (Location l : locations) {
                        Block lB = l.getBlock();
                        mats.put(l, lB.getType());
                        lB.setType(Material.PACKED_ICE);
                    }
                    revert = true;
                } else {
                    ArrayList<Location> toRemove = new ArrayList<>();
                    for (Location l : locations) {
                        Material m = mats.get(l);
                        try {
                            l.getBlock().setType(m);
                            //l.getWorld().spigot().playEffect(l, Effect.TILE_BREAK, Material.PACKED_ICE.getId(), 0, 0.5F, 0.5F, 0.5F, 0.0F, 5, 16);
                            l.getWorld().spawnParticle(Particle.BLOCK_CRACK, l, 5, 0.5, 0.5, 0.5, 0, Bukkit.createBlockData(Material.PACKED_ICE));
                            mats.remove(l);
                            toRemove.add(l);
                        } catch (NullPointerException npe) {
                            // plugin.getLogger().info("FlashFreeze threw an NPE, but it's okay, it doesn't affect anything." +
                            //        " I'm just putting this here to piss off Radicater. - Dewyn");
                        }
                    }
                    locations.removeAll(toRemove);
                }
            }
        }.runTaskTimer(plugin, 5, duration / 50);

        CharacterTemplate targCT = plugin.getCharacterManager().getCharacter(target);
        targCT.addEffect(new FlashFrozenEffect(this, player, duration + 250));

        //target.getWorld().spigot().playEffect(target.getLocation().add(0, 1.5, 0), Effect.TILE_BREAK, Material.PACKED_ICE.getId(), 0, 1.0F, 1.5F, 1.0F, 0.0F, 150, 16);
        target.getWorld().spawnParticle(Particle.BLOCK_CRACK, target.getLocation().add(0, 1.5, 0), 150, 1, 1.5, 1, 0, Bukkit.createBlockData(Material.PACKED_ICE));
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_GLASS_BREAK, 3.0F, 1.0F);

        broadcast(player.getLocation(), ChatColor.WHITE + hero.getName() + ChatColor.GRAY + " used " + ChatColor.WHITE + this.getName() + ChatColor.GRAY + " on " + ChatColor.WHITE + target.getName() + ChatColor.GRAY + "!");
        return SkillResult.NORMAL;
    }

    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 3000, false);
        duration += SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE_PER_CHARISMA, 100, false)
                * hero.getAttributeValue(AttributeType.CHARISMA);
        return getDescription().replace("$1", (duration / 1000) + "");
    }

    public class FlashFreezeBreakListener implements Listener {
        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onBlockBreak(BlockBreakEvent event) {
            if (locations.contains(event.getBlock().getLocation())) {
                event.setCancelled(true);
            }
        }
    }

    public static class FlashFrozenEffect extends StunEffect {
        public FlashFrozenEffect(Skill skill, Player applier, long duration) {
            super(skill, applier, duration);
            types.add((EffectType.INVULNERABILITY));
        }

        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            final Player p = hero.getPlayer();
            broadcast(p.getLocation(), ChatColor.WHITE + hero.getName() + ChatColor.GRAY + " was flash frozen!");
            p.setFallDistance(-20.0F);
        }

        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            final Player p = hero.getPlayer();
            broadcast(p.getLocation(), ChatColor.WHITE + hero.getName() + ChatColor.GRAY + "thaws out!");
        }
    }
}
