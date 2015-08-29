package com.herocraftonline.heroes.characters.skill.unusedskills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.VisualEffect;

import org.bukkit.*;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.util.ArrayList;
import java.util.List;

public class SkillTempest extends ActiveSkill {

    public SkillTempest(Heroes plugin) {
        super(plugin, "Tempest");
        setIdentifiers("skill tempest");
        setUsage("/skill tempest");
        setArgumentRange(0, 0);
        setDescription("Conjure a Tempest in the area around your feet, dealing $1 damage to all targets within a $2 block radius.");
        setTypes(SkillType.AREA_OF_EFFECT, SkillType.AGGRESSIVE, SkillType.DAMAGING, SkillType.ABILITY_PROPERTY_LIGHTNING);
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DAMAGE.node(), 300);
        node.set(SkillSetting.RADIUS.node(), 10);
        node.set(SkillSetting.DELAY.node(), 5000);
        node.set(SkillSetting.USE_TEXT.node(), ChatColor.GRAY + "[" + ChatColor.DARK_GREEN + "Skill" + ChatColor.GRAY + "] %hero% has unleashed a powerful " + ChatColor.BOLD + "Tempest!");
        node.set("effect-height", 4);
        node.set("lightning-volume", 0.0F);
        node.set("max-targets", 5);

        return node;
    }

    @Override
    public String getDescription(Hero hero) {

        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 300, false);
        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 10, false);

        return getDescription().replace("$1", damage + "").replace("$1", radius + "");
    }

    @Override
    public SkillResult use(final Hero hero, String[] args) {

        final Player player = hero.getPlayer();

        // Get config settings
        final double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 300, false);
        final int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 10, false);
        final int height = SkillConfigManager.getUseSetting(hero, this, "effect-height", 5, false);
        
        final float lightningVolume = (float) SkillConfigManager.getUseSetting(hero, this, "lightning-volume", 0.0F, false);

        broadcastExecuteText(hero);

        // Create a cicle of firework locations, based on skill radius.
        List<Location> fireworkLocations = circle(player.getLocation(), radius, 1, true, false, height);
        int fireworksSize = fireworkLocations.size();
        long ticksPerFirework = 100 / fireworksSize;

        // Save player location for the center of the blast and max target count
        final Location centerLocation = player.getLocation();
        final int maxTargets = SkillConfigManager.getUseSetting(hero, this, "max-targets", 0, false);

        // Damage all entities near the center after the fireworks finish playing
        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                int targetsHit = 0;
                for (Entity entity : getNearbyEntities(centerLocation, radius, radius, radius)) {
                    // Check to see if we've exceeded the max targets
                    if (maxTargets > 0 && targetsHit >= maxTargets)
                        break;
                    
                    // Check to see if the entity can be damaged
                    if (!(entity instanceof LivingEntity) || !damageCheck(player, (LivingEntity) entity))
                        continue;

                    // Damage the target
                    addSpellTarget(entity, hero);
                    damageEntity((LivingEntity) entity, player, damage, DamageCause.MAGIC, false);
                    // Lightning like this is too annoying.
                    // entity.getWorld().strikeLightningEffect(entity.getLocation());
                    entity.getWorld().spigot().strikeLightningEffect(entity.getLocation(), true);
                    entity.getWorld().playSound(entity.getLocation(), Sound.AMBIENCE_THUNDER, lightningVolume, 1.0F);
                    
                    // Increase counter
                    targetsHit++;
                }
            }

        }, ticksPerFirework * fireworksSize);

        // Finish
        return SkillResult.NORMAL;
    }

    protected List<Entity> getNearbyEntities(Location targetLocation, int radiusX, int radiusY, int radiusZ) {
        List<Entity> entities = new ArrayList<>();

        for (Entity entity : targetLocation.getWorld().getEntities()) {
            if (isInBorder(targetLocation, entity.getLocation(), radiusX, radiusY, radiusZ)) {
                entities.add(entity);
            }
        }
        return entities;
    }

    public boolean isInBorder(Location center, Location targetLocation, int radiusX, int radiusY, int radiusZ) {
        int x1 = center.getBlockX();
        int y1 = center.getBlockY();
        int z1 = center.getBlockZ();

        int x2 = targetLocation.getBlockX();
        int y2 = targetLocation.getBlockY();
        int z2 = targetLocation.getBlockZ();

        return !(x2 >= (x1 + radiusX) || x2 <= (x1 - radiusX) || y2 >= (y1 + radiusY) || y2 <= (y1 - radiusY) || z2 >= (z1 + radiusZ) || z2 <= (z1 - radiusZ));

    }

    protected List<Location> circle(Location loc, Integer r, Integer h, boolean hollow, boolean sphere, int plus_y) {
        List<Location> circleblocks = new ArrayList<Location>();
        int cx = loc.getBlockX();
        int cy = loc.getBlockY();
        int cz = loc.getBlockZ();
        for (int x = cx - r; x <= cx + r; x++)
            for (int z = cz - r; z <= cz + r; z++)
                for (int y = (sphere ? cy - r : cy); y < (sphere ? cy + r : cy + h); y++) {
                    double dist = (cx - x) * (cx - x) + (cz - z) * (cz - z) + (sphere ? (cy - y) * (cy - y) : 0);
                    if (dist < r * r && !(hollow && dist < (r - 1) * (r - 1))) {
                        Location l = new Location(loc.getWorld(), x, y + plus_y, z);
                        circleblocks.add(l);
                    }
                }

        return circleblocks;
    }
}