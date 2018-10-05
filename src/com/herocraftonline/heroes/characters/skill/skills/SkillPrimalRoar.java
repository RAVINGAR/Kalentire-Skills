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
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class SkillPrimalRoar extends ActiveSkill
{
	public SkillPrimalRoar(Heroes plugin)
	{
		super(plugin, "PrimalRoar");
		setDescription("Unleash a Primal Roar that deals $1 damage to all enemies within $2 blocks directly in front of you. The explosive force of the roar also stuns targets for $3 seconds.");
		setUsage("/skill primalroar");
		setArgumentRange(0, 0);
		setIdentifiers("skill primalroar", "skill roar");
		setTypes(SkillType.FORCE, SkillType.DAMAGING);
	}
	
	public String getDescription(Hero hero)
	{
		int distance = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 5, false);
        double dmg = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 40, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 0.5, false)
                * hero.getAttributeValue(AttributeType.STRENGTH);
        final double damage = dmg + damageIncrease;
        long stunDuration = SkillConfigManager.getUseSetting(hero, this, "stun-time", 2000, false);
        String formattedStun = String.valueOf(stunDuration / 1000);
        
        return getDescription().replace("$1", damage + "").replace("$2", distance + "").replace("$3", formattedStun);
	}	
	
	public ConfigurationSection getDefaultConfig() 
	{
		ConfigurationSection node = super.getDefaultConfig();

		node.set(SkillSetting.MAX_DISTANCE.node(), 5);
		node.set(SkillSetting.DAMAGE.node(), 40);
		node.set(SkillSetting.DAMAGE_INCREASE_PER_STRENGTH.node(), 0.5);
		node.set("stun-time", 2000);
		node.set("roar-delay", 2);
		node.set(SkillSetting.RADIUS.node(), 2);

		return node;
	}
	
    public SkillResult use(final Hero hero, String[] args)
    {
        final Player player = hero.getPlayer();
        
        final Skill skill = this;

        int distance = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 5, false);
        final long stunDuration = SkillConfigManager.getUseSetting(hero, this, "stun-time", 2000, false);

        Block tempBlock;
        BlockIterator iter = null;
        try {
            iter = new BlockIterator(player, distance);
        }
        catch (IllegalStateException e) {
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        broadcastExecuteText(hero);

        double dmg = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 40, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 0.5, false)
                * hero.getAttributeValue(AttributeType.STRENGTH);
        final double damage = dmg + damageIncrease;

        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 2, false);
        final int radiusSquared = radius * radius;

        int delay = SkillConfigManager.getUseSetting(hero, this, "roar-delay", 2, false);

        final List<Entity> nearbyEntities = player.getNearbyEntities(distance * 2, distance, distance * 2);
        final List<Entity> hitEnemies = new ArrayList<Entity>();

        int numBlocks = 0;
        
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0F, 1.0F);
        
        while (iter.hasNext()) 
        {        	
            tempBlock = iter.next();
            Material tempBlockType = tempBlock.getType();
            if (Util.transparentBlocks.contains(tempBlockType)) 
            {
                final Location targetLocation = tempBlock.getLocation().clone().add(new Vector(.5, 0, .5));

                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable()
                {
                    public void run() 
                    {
                    	targetLocation.getWorld().playSound(targetLocation, Sound.ENTITY_GENERIC_EXPLODE, 0.7F, 1.3F);
                    	//targetLocation.getWorld().spigot().playEffect(targetLocation, Effect.EXPLOSION_LARGE, 0, 0, 0.5F, 0.5F, 0.5F, 0.0F, 5, 20);
                        targetLocation.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, targetLocation, 5, 0.5, 0.5, 0.5, 0, true);
                    	//targetLocation.getWorld().spigot().playEffect(targetLocation, Effect.NOTE, 0, 0, 0.5F, 0.5F, 0.5F, 0.0F, 25, 20);
                        targetLocation.getWorld().spawnParticle(Particle.NOTE, targetLocation, 25, 0.5, 0.5, 0.5, 0, true);
                    	//targetLocation.getWorld().spigot().playEffect(targetLocation, Effect.MAGIC_CRIT, 0, 0, 0.5F, 0.5F, 0.5F, 0.0F, 40, 20);
                        targetLocation.getWorld().spawnParticle(Particle.CRIT_MAGIC, targetLocation, 40, 0.4, 0.4, 0.4, 0, true);
                        for (Entity entity : nearbyEntities) 
                        {
                            if (!(entity instanceof LivingEntity) || hitEnemies.contains(entity) || entity.getLocation().distanceSquared(targetLocation) > radiusSquared)
                                continue;

                            if (!damageCheck(player, (LivingEntity) entity))
                                continue;

                            LivingEntity target = (LivingEntity) entity;
                            
                            CharacterTemplate targCT = plugin.getCharacterManager().getCharacter(target);
                            StunEffect stun = new StunEffect(skill, player, stunDuration);
                            targCT.addEffect(stun);

                            addSpellTarget(target, hero);
                            damageEntity(target, player, damage, DamageCause.MAGIC);

                            hitEnemies.add(entity);
                        }
                    }
                }, numBlocks * delay);

                numBlocks++;
            }
            else
                break;
        }
        return SkillResult.NORMAL;
    }
}
