package com.herocraftonline.heroes.characters.skill.pack3;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.party.HeroParty;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SkillBalance extends ActiveSkill {

    public SkillBalance(Heroes plugin) {
        super(plugin, "Balance");
        setDescription("On use, balances the percent max health of everyone in the party within a $1 block radius.");
        setUsage("/skill balance");
        setIdentifiers("skill balance");
        setArgumentRange(0, 0);
        setTypes(SkillType.SILENCEABLE, SkillType.AREA_OF_EFFECT, SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.ABILITY_PROPERTY_DARK, SkillType.ABILITY_PROPERTY_LIGHT);
    }

    @Override
    public String getDescription(Hero h) {
        int range = SkillConfigManager.getUseSetting(h, this, "maxrange", 7, false);

        return getDescription().replace("$1", range + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.RADIUS.node(), 7);
        node.set(SkillSetting.RADIUS_INCREASE_PER_WISDOM.node(), 0.005);

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
    public SkillResult use(Hero hero, String[] arg1) {
        Player player = hero.getPlayer();

        if (!(hero.hasParty())) {
            player.sendMessage(ChatColor.GRAY + "You are not in a party!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        HeroParty heroParty = hero.getParty();

        if (heroParty.getMembers().size() < 2) {
            player.sendMessage(ChatColor.GRAY + "You must have other players in your party to use this ability!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        double maxHealthTotal = 0;
        double currentHealthTotal = 0;
        Iterator<Hero> partyMembers = heroParty.getMembers().iterator();
        Location playerLocation = player.getLocation();

        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 7, false);
        double radiusIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS_INCREASE_PER_WISDOM, 0.1, false);
        radius += (int) Math.floor(radiusIncrease * hero.getAttributeValue(AttributeType.WISDOM));
        int radiusSquared = radius * radius;

        boolean skipRangeCheck = (radius == 0);						//0 for no maximum range
        while (partyMembers.hasNext()) {
            Hero member = partyMembers.next();
            Location memberLocation = member.getPlayer().getLocation();
            if (skipRangeCheck || (memberLocation.getWorld().equals(playerLocation.getWorld()) && memberLocation.distanceSquared(playerLocation) < radiusSquared)) {
                maxHealthTotal += member.getPlayer().getMaxHealth();
                currentHealthTotal += member.getPlayer().getHealth();
            }
        }

        if (Double.compare(maxHealthTotal, player.getMaxHealth()) == 0) {
            player.sendMessage("There is nobody in range to balance with!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        double healthMultiplier = currentHealthTotal * Math.pow(maxHealthTotal, -1);

        Iterator<Hero> applyHealthIterator = heroParty.getMembers().iterator();
        while (applyHealthIterator.hasNext()) {
            Hero applyHero = applyHealthIterator.next();
            Location applyHeroLocation = applyHero.getPlayer().getLocation();

            if (skipRangeCheck || (applyHeroLocation.getWorld().equals(playerLocation.getWorld()) && applyHeroLocation.distanceSquared(playerLocation) < radiusSquared)) {
                applyHero.getPlayer().setHealth((applyHero.getPlayer().getMaxHealth() * healthMultiplier));
                if (applyHero.getName().equals(hero.getName())) {
                    player.sendMessage(ChatColor.GRAY + "You used Balance!");
                }
                else {
                    applyHero.getPlayer().sendMessage(ChatColor.GRAY + hero.getName() + " balanced your health with that of your party!");
                }
                List<Location> circle = circle(applyHero.getPlayer().getLocation().add(0, 0.5, 0), 36, 1.5);
                for (int i = 0; i < circle.size(); i++)
        		{
        			//applyHero.getPlayer().getWorld().spigot().playEffect(circle(applyHero.getPlayer().getLocation().add(0, 0.5, 0), 36, radius / 2).get(i), org.bukkit.Effect.INSTANT_SPELL, 0, 0, 0, 0, 0, 0, 16, 16);
        		    applyHero.getPlayer().getWorld().spawnParticle(Particle.SPELL_INSTANT, circle.get(i), 16, 0, 0, 0, 0);
        		}
            }
        }
        player.getWorld().playSound(playerLocation, Sound.ENTITY_PLAYER_LEVELUP, 0.9F, 1.0F);
        return SkillResult.NORMAL;
    }
}
