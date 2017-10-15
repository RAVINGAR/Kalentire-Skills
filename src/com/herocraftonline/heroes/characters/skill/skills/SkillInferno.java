package com.herocraftonline.heroes.characters.skill.skills;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.VisualEffect;
import com.herocraftonline.heroes.util.Util;

public class SkillInferno extends ActiveSkill {

	public VisualEffect fplayer = new VisualEffect();

	public SkillInferno(Heroes plugin) {
		super(plugin, "Inferno");
		setDescription("Unleash an inferno upon your enemies, randomly dealing spurts of $1 damage around you within a $2 block radius for the next $3 seconds.");
		setUsage("/skill inferno");
		setArgumentRange(0, 0);
		setIdentifiers("skill inferno");
        setTypes(SkillType.ABILITY_PROPERTY_FIRE, SkillType.DAMAGING, SkillType.AGGRESSIVE, SkillType.AREA_OF_EFFECT);
	}

    public String getDescription(Hero hero) {

        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 12, false);
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 90, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.2, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT);

        String formattedDuration = Util.decFormat.format(duration / 1000.0);
        String formattedDamage = Util.decFormat.format(damage);

        return getDescription().replace("$1", formattedDamage).replace("$2", radius + "").replace("$3", formattedDuration);
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.RADIUS.node(), 12);
        node.set(SkillSetting.DURATION.node(), 6000);
        node.set(SkillSetting.DAMAGE.node(), 13);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 0.425);
        node.set("explosion-hitbox-radius", 3);
        node.set("inferno-spawn-delay", 1);

        return node;
    }

    public SkillResult use(final Hero hero, String[] args) {
        final Player player = hero.getPlayer();

        broadcastExecuteText(hero);

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);

        hero.addEffect(new InfernoEffect(this, player, duration));

        return SkillResult.NORMAL;
    }

    public class InfernoEffect extends ExpirableEffect {

        public InfernoEffect(Skill skill, Player applier, long duration) {
            super(skill, "InfernoEffect", applier, duration, null, null);

            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.FIRE);
            
            addPotionEffect(new PotionEffect(PotionEffectType.SLOW, (int) (duration / 1000) * 20, 127), false);      // Max slowness is 127
            addPotionEffect(new PotionEffect(PotionEffectType.JUMP, (int) (duration / 1000) * 20, 128), false);      // Max negative jump boost
        }

        @Override
        public void applyToHero(final Hero hero) {
            super.applyToHero(hero);

            final Player player = hero.getPlayer();
            
            final int radius = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.RADIUS, 12, false);
            final int hitboxRadius = SkillConfigManager.getUseSetting(hero, skill, "explosion-hitbox-radius", 3, false);
            int spawnDelay = SkillConfigManager.getUseSetting(hero, skill, "inferno-spawn-delay", 1, false);

            double tempDamage = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE, 90, false);
            double damageIncrease = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.2, false);
            tempDamage += damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT);
            final double damage = tempDamage;

            int numExplosions = 0;
            List<Location> explosionLocations = circle(player, player.getLocation(), radius, 1, false, false, 0);
            for (final Location location : explosionLocations) {
                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                    public void run() {
                        final Location explosionLocation = location.clone().add(new Vector(.5, .5, .5));
                        try {
                            fplayer.playFirework(explosionLocation.getWorld(), explosionLocation, FireworkEffect.builder().flicker(false).trail(true).with(FireworkEffect.Type.BURST).withColor(Color.ORANGE).withFade(Color.MAROON).build());
                            fplayer.playFirework(explosionLocation.getWorld(), explosionLocation, FireworkEffect.builder().flicker(false).trail(true).with(FireworkEffect.Type.BALL_LARGE).withColor(Color.MAROON).withFade(Color.ORANGE).build());
                        }
                        catch (IllegalArgumentException e) {
                            e.printStackTrace();
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }

                        final List<Entity> nearbyEntities = player.getNearbyEntities(radius * 2, radius * 2, radius * 2);
                        for (Entity entity : nearbyEntities) {
                            // Check to see if the entity can be damaged
                            if (!(entity instanceof LivingEntity) || entity.getLocation().distance(explosionLocation) > hitboxRadius)
                                continue;

                            if (!damageCheck(player, (LivingEntity) entity))
                                continue;

                            // Damage target
                            LivingEntity target = (LivingEntity) entity;

                            addSpellTarget(target, hero);
                            damageEntity(target, player, damage, DamageCause.MAGIC);
                        }
                    }
                }, numExplosions * spawnDelay);

                numExplosions++;
            }
        }
    }
    
    public boolean isInBorder(Location center, Location targetLocation, int radiusX, int radiusY, int radiusZ) {
        int x1 = center.getBlockX();
        int y1 = center.getBlockY();
        int z1 = center.getBlockZ();

        int x2 = targetLocation.getBlockX();
        int y2 = targetLocation.getBlockY();
        int z2 = targetLocation.getBlockZ();

        if (x2 >= (x1 + radiusX) || x2 <= (x1 - radiusX) || y2 >= (y1 + radiusY) || y2 <= (y1 - radiusY) || z2 >= (z1 + radiusZ) || z2 <= (z1 - radiusZ))
            return false;

        return true;
    }

    protected List<Location> circle(Player player, Location loc, Integer r, Integer h, boolean hollow, boolean sphere, int plus_y) {
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