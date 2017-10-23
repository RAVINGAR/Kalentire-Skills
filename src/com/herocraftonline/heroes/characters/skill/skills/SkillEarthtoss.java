package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.StunEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import static com.herocraftonline.heroes.util.GeometryUtil.circle;

public class SkillEarthtoss extends ActiveSkill {
    private HashMap<FallingBlock, Player> blocks = new HashMap<FallingBlock, Player>();

    private ArrayList<Material> naturalMaterials = new ArrayList<Material>();

    public SkillEarthtoss(Heroes plugin) {
        super(plugin, "Earthtoss");
        setDescription("You grab the earth under you and toss it in all directions. Enemies hit will be dealt $1 damage and stunned for $2 seconds.");
        setUsage("/skill earthtoss");
        setArgumentRange(0, 0);
        setIdentifiers("skill earthtoss");
        setTypes(SkillType.DAMAGING, SkillType.ABILITY_PROPERTY_EARTH);
        naturalMaterials.addAll(Arrays.asList(Material.DIRT,
                Material.GRASS, Material.GRAVEL, Material.STONE));
        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(this), plugin);
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DAMAGE.node(), 45);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 1.2);
        node.set(SkillSetting.DURATION.node(), 2000);
        return node;
    }

    public String getDescription(Hero hero) {
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 45, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.2, false);
        damage += (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));
        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 2000, false);

        String formattedDamage = Util.decFormat.format(damage);
        String formattedDuration = Util.decFormat.format(duration);

        return getDescription().replace("$1", formattedDamage)
                .replace("$2", Util.decFormat.format((double) duration / 1000));
    }

    public SkillResult use(Hero hero, String[] args) {
        final ArrayList<LivingEntity> damagedEntities = new ArrayList<LivingEntity>();
        Player player = hero.getPlayer();
        Location pLoc = player.getLocation();
        ArrayList<Location> to = circle(pLoc.add(0, 1.0, 0), 24, 4); // "to" locations for vector calculation
        for (Location l : to) {
            FallingBlock dirt;
            if (naturalMaterials.contains(player.getLocation().getBlock().getRelative(BlockFace.DOWN).getType())) {
                dirt = (FallingBlock) player.getWorld().spawnFallingBlock(pLoc.add(0, 0.2, 0), player.getLocation().getBlock().getRelative(BlockFace.DOWN).getType().getId(), (byte) 0);
            } else {
                dirt = (FallingBlock) player.getWorld().spawnFallingBlock(pLoc.add(0, 0.2, 0), Material.DIRT.getId(), (byte) 0);
            }

            // NOT MATH NOOOOOOOOOOO
            double dX = pLoc.getX() - l.getX();
            double dY = pLoc.getY() - l.getY();
            double dZ = pLoc.getZ() - l.getZ();
            double yaw = Math.atan2(dZ, dX);
            double pitch = Math.atan2(Math.sqrt(dZ * dZ + dX * dX), dY) + Math.PI;
            double X = Math.sin(pitch) * Math.cos(yaw);
            double Y = Math.sin(pitch) * Math.sin(yaw);
            double Z = Math.cos(pitch);

            Vector velocity = new Vector(X, Z, Y);
            // Thank you Google for this. I couldn't have gotten the pitch/yaw part on my own.

            dirt.setVelocity(velocity);

            final Player p = player;
            final Hero h = hero;
            final FallingBlock b = dirt;
            b.setDropItem(false);
            final ArrayList<LivingEntity> damaged = new ArrayList<LivingEntity>();
            blocks.put(b, player);

            new BukkitRunnable() {
                public void run() {
                    if (b.isDead()) cancel();
                    if (!b.getNearbyEntities(0.5F, 0.5F, 0.5F).isEmpty()) {
                        for (Entity e : b.getNearbyEntities(0.5F, 0.5F, 0.5F)) {
                            if (!(e instanceof LivingEntity) || !damageCheck(p, (LivingEntity) e)) {
                                return;
                            }
                            if (!damagedEntities.contains(e)) {
                                strikeEntity(h, (LivingEntity) e, damaged);
                                damagedEntities.add((LivingEntity) e);
                                b.remove();
                                blocks.remove(b);
                                cancel();
                            }
                        }
                    }
                }
            }.runTaskTimer(this.plugin, 0, 1);
        }
        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

    public void strikeEntity(Hero hero, LivingEntity target, ArrayList<LivingEntity> damagedEntities) {
        if (!(damagedEntities.contains(target))) {
            Player player = hero.getPlayer();
            double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 45, true);
            double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.2, false);
            damage += (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

            addSpellTarget(target, hero);
            damagedEntities.add(target);
            damageEntity(target, player, damage, DamageCause.MAGIC, false);
            CharacterTemplate targCT = this.plugin.getCharacterManager().getCharacter(target);
            StunEffect stun = new StunEffect(this, player, 2000);
            targCT.addEffect(stun);
            target.getWorld().spigot().playEffect(target.getEyeLocation(), Effect.TILE_BREAK, Material.DIRT.getId(), 0, 0.4F, 0.4F, 0.4F, 0.5F, 45, 16);
            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0F, 1.0F);
            target.getWorld().playSound(target.getLocation(), Sound.BLOCK_GRAVEL_BREAK, 1.0F, 1.0F);
        }
    }

    public class SkillEntityListener implements Listener {
        private final Skill skill;

        public SkillEntityListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler
        public void onFallingBlockLand(final EntityChangeBlockEvent event) {
            if (event.getEntity() instanceof FallingBlock) {
                FallingBlock fallingBlock = (FallingBlock) event.getEntity();

                Block b = event.getBlock();

                if (blocks.containsKey(fallingBlock)) {
                    fallingBlock.setDropItem(false);
                    b.getWorld().spigot().playEffect(b.getLocation(), Effect.TILE_BREAK, fallingBlock.getBlockId(), 0, 0.4F, 0.4F, 0.4F, 0.5F, 45, 16);
                    b.getWorld().playSound(b.getLocation(), Sound.BLOCK_GRASS_BREAK, 1.0F, 1.0F);
                    fallingBlock.remove();
                    event.setCancelled(true);
                }
            }
        }
    }

}