package com.herocraftonline.heroes.characters.skill.remastered.bard;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.SilenceEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.util.ArrayList;
import java.util.List;

public class SkillVoidsong extends ActiveSkill {

    public SkillVoidsong(Heroes plugin) {
        super(plugin, "Voidsong");
        setDescription("You create a void dealing $1 magic damage and silencing everyone within $2 blocks for $3 second(s).");
        setUsage("/skill voidsong");
        setArgumentRange(0, 0);
        setIdentifiers("skill voidsong");
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.DISABLING, SkillType.ABILITY_PROPERTY_SONG, SkillType.AREA_OF_EFFECT, SkillType.SILENCEABLE, SkillType.SILENCING, SkillType.INTERRUPTING, SkillType.AGGRESSIVE);
    }

    @Override
    public String getDescription(Hero hero) {
        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS.node(), 5, false);

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 17, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_CHARISMA, 0.125, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.CHARISMA);

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 2500, false);
        int durationIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE_PER_CHARISMA, 38, false);
        duration += hero.getAttributeValue(AttributeType.CHARISMA) * durationIncrease;

        String formattedDamage = Util.decFormat.format(damage);
        String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription().replace("$1", formattedDamage).replace("$2", radius + "").replace("$3", formattedDuration);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.RADIUS.node(), 5);
        node.set(SkillSetting.DURATION.node(), 2500);
        node.set(SkillSetting.DURATION_INCREASE_PER_CHARISMA.node(), 38);

        return node;
    }

    public ArrayList<Location> helix(Location center, double height, double radius, double particleInterval)
    {
        ArrayList<Location> locations = new ArrayList<Location>();

        for (double y = 0; y <= height; y += particleInterval)
        {
            double x = center.getX() + (radius * Math.cos(y));
            double z = center.getZ() + (radius * Math.sin(y));
            locations.add(new Location(center.getWorld(), x, center.getY(), z));
        }
        return locations;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero);

        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5, false);

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 2500, false);
        int durationIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE_PER_CHARISMA, 38, false);
        duration += hero.getAttributeValue(AttributeType.CHARISMA) * durationIncrease;

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 17, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_CHARISMA, 0.125, false);
        damage += (damageIncrease * hero.getAttributeValue(AttributeType.CHARISMA));

        List<Entity> entities = hero.getPlayer().getNearbyEntities(radius, radius, radius);
        for (Entity entity : entities) {
            if (!(entity instanceof LivingEntity)) {
                continue;
            }

            LivingEntity target = (LivingEntity) entity;
            if (!damageCheck(player, target)) {
                continue;
            }

            addSpellTarget(target, hero);
            damageEntity(target, player, damage, DamageCause.MAGIC);

            if (target instanceof Player) {
                Hero targetHero = plugin.getCharacterManager().getHero((Player) target);
                targetHero.addEffect(new SilenceEffect(this, player, duration));
            }
        }

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_DEATH, 0.5F, 1.0F);

        ArrayList<Location> particleLocations = helix(player.getLocation().add(0, 0.5, 0), 10.0D, 3.5D, 0.1D);
        for (Location l : particleLocations)
        {
            //player.getWorld().spigot().playEffect(l, org.bukkit.Effect.NOTE, 0, 0, 0, 0, 0, 0, 1, 16);
            player.getWorld().spawnParticle(Particle.NOTE, l, 1, 0, 0, 0, 0);
        }


        //FIXME Is it a particle or a sound
//        player.getWorld().playEffect(player.getLocation().add(0, 2.5, 0), org.bukkit.Effect.NOTE, 3);
//        player.getWorld().playEffect(player.getLocation().add(0, 2.5, 0), org.bukkit.Effect.NOTE, 3);
//        player.getWorld().playEffect(player.getLocation().add(0, 2.5, 0), org.bukkit.Effect.NOTE, 3);
        return SkillResult.NORMAL;
    }
}