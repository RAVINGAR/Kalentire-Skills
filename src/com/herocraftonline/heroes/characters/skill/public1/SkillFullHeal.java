package com.herocraftonline.heroes.characters.skill.public1;

import java.util.ArrayList;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Sound;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.Sound;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainHealthEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Util;

public class SkillFullHeal extends TargettedSkill {

    public SkillFullHeal(Heroes plugin) {
        super(plugin, "FullHeal");
        setDescription("You restore your target to full health. However, this ability will only heal you for $1% of your max health however. Targeting distance for this ability is increased by your Wisdom level.");
        setUsage("/skill fullheal <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill fullheal");
        setTypes(SkillType.ABILITY_PROPERTY_LIGHT, SkillType.HEALING, SkillType.SILENCEABLE);
    }

    @Override
    public String getDescription(Hero hero) {
        double modifier = SkillConfigManager.getUseSetting(hero, this, "self-heal-modifier", 0.5, false);

        String formattedModifier = Util.decFormat.format(modifier * 100);

        return getDescription().replace("$1", formattedModifier);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 8);
        node.set(SkillSetting.MAX_DISTANCE_INCREASE_PER_INTELLECT.node(), 0.15);
        node.set("self-heal-modifier", 0.5);

        return node;
    }
    
    public ArrayList<Location> helix(Location center, double height, double radius, double particleInterval)
	{
		ArrayList<Location> locations = new ArrayList<Location>();
		
		for (double y = 0; y <= height; y += particleInterval) 
		{
			double x = center.getX() + (radius * Math.cos(y));
			double z = center.getZ() + (radius * Math.sin(y));
			locations.add(new Location(center.getWorld(), x, center.getY() + y, z));
		}
		return locations;
	}

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        if (!(target instanceof Player)) {
            return SkillResult.INVALID_TARGET;
        }

        Player player = hero.getPlayer();

        Hero targetHero = plugin.getCharacterManager().getHero((Player) target);

        HeroRegainHealthEvent hrhEvent;
        if (player.equals(targetHero.getPlayer())) {
            double modifier = SkillConfigManager.getUseSetting(hero, this, "self-heal-modifier", 0.5, false);
            double healAmount = target.getMaxHealth() * modifier;
            hrhEvent = new HeroRegainHealthEvent(targetHero, healAmount, this);
        }
        else {
            double healAmount = target.getMaxHealth();
            hrhEvent = new HeroRegainHealthEvent(targetHero, healAmount, this, hero);
        }

        plugin.getServer().getPluginManager().callEvent(hrhEvent);
        if (hrhEvent.isCancelled()) {
            player.sendMessage("Unable to heal the target at this time!");
            return SkillResult.CANCELLED;
        }
        targetHero.heal(hrhEvent.getDelta());

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.9F, 1.0F);
        broadcastExecuteText(hero, target);
        
        Player targetPlayer = targetHero.getPlayer();
        targetPlayer.getWorld().spigot().playEffect(targetPlayer.getLocation().add(0, 0.3, 0), Effect.CLOUD, 0, 0, 0.5F, 0.5F, 0.5F, 0.5F, 25, 16);
        //targetPlayer.getWorld().spawnParticle(Particle.CLOUD, targetPlayer.getLocation(), 25, 0.5, 0.5, 0.5, 0.5);
        ArrayList<Location> particleLocations = helix(player.getLocation(), 3.0D, 2.0D, 0.05D);
        for (Location l : particleLocations)
        {
            player.getWorld().spigot().playEffect(l, org.bukkit.Effect.FIREWORKS_SPARK, 0, 0, 0, 0, 0, 0, 1, 16);
            //player.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, l, 0, 0, 0, 0);
        }
        targetPlayer.getWorld().spigot().playEffect(targetPlayer.getLocation().add(0, 0.3, 0), Effect.FIREWORKS_SPARK, 0, 0, 0.5F, 0.5F, 0.5F, 0.2F, 25, 16);
        //targetPlayer.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, targetPlayer.getLocation(), 25, 0.5, 0.5, 0.5, 0.2);

        return SkillResult.NORMAL;
    }
}
