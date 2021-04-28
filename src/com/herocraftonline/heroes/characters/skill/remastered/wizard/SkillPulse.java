package com.herocraftonline.heroes.characters.skill.remastered.wizard;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.util.ArrayList;
import java.util.List;

public class SkillPulse extends ActiveSkill {

    public SkillPulse(Heroes plugin) {
        super(plugin, "Pulse");
        setDescription("You deal $1 damage to all enemies within $2 blocks.");
        setUsage("/skill pulse");
        setArgumentRange(0, 0);
        setIdentifiers("skill pulse");
        setTypes(SkillType.DAMAGING, SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.SILENCEABLE, SkillType.AGGRESSIVE, SkillType.AREA_OF_EFFECT);
    }

    @Override
    public String getDescription(Hero hero) {
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 60, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.0, false);
        damage += (int) (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5, false);

        return getDescription().replace("$1", damage + "").replace("$2", radius + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DAMAGE.node(), 60);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 1.0);
        node.set(SkillSetting.RADIUS.node(), 5);
        node.set("max-targets", 5);

        return node;
    }
    
    public ArrayList<Location> circle(Location centerPoint, int particleAmount, double circleRadius)
	{
		World world = centerPoint.getWorld();

		double increment = (2 * Math.PI) / particleAmount;

		ArrayList<Location> locations = new ArrayList<Location>();

		for (int i = 0; i < particleAmount; i++)
		{
			double angle = i * increment;
			double x = centerPoint.getX() + (circleRadius * Math.cos(angle));
			double z = centerPoint.getZ() + (circleRadius * Math.sin(angle));
			locations.add(new Location(world, x, centerPoint.getY(), z));
		}
		return locations;
	}

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero);

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 60, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.0, false);
        damage += (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5, false);

        int maxTargets = SkillConfigManager.getUseSetting(hero, this, "max-targets", 0, false);
        int targetsHit = 0;
        List<Entity> entities = hero.getPlayer().getNearbyEntities(radius, radius, radius);
        for (Entity entity : entities) {
            // Check to see if we've exceeded the max targets
            if (maxTargets > 0 && targetsHit >= maxTargets) {
                break;
            }
            
            if (!(entity instanceof LivingEntity)) {
                continue;
            }

            LivingEntity target = (LivingEntity) entity;
            if (!damageCheck(player, target))
                continue;

            addSpellTarget(target, hero);
            damageEntity(target, player, damage, DamageCause.MAGIC);       
            
            targetsHit++;
        }
        
        for (double r = 1; r < radius * 2; r++)
		{
			ArrayList<Location> particleLocations = circle(player.getLocation(), 45, r / 2);
			for (int i = 0; i < particleLocations.size(); i++)
			{
				//player.getWorld().spigot().playEffect(particleLocations.get(i), Effect.MAGIC_CRIT, 0, 0, 0, 0.1F, 0, 0.1F, 1, 16);
                player.getWorld().spawnParticle(Particle.CRIT_MAGIC, particleLocations.get(i), 1, 0, 0.1, 0, 0.1);
			}
		}
        
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.2F);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_CHICKEN_EGG, 1.0F, 1.2F);

        return SkillResult.NORMAL;
    }
}
