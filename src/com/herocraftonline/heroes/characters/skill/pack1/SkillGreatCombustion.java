package com.herocraftonline.heroes.characters.skill.pack1;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.StunEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.util.Util;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import java.util.List;

public class SkillGreatCombustion extends ActiveSkill {

    public SkillGreatCombustion(Heroes plugin) {
        super(plugin, "GreatCombustion");
        setDescription("Unleash a mass of condensed flame in front of you, striking up to $1 blocks away. An enemy within $2 blocks of the explosion will be dealt $3 damage and will be stunned for $4 second(s).");
        setUsage("/skill greatcombustion");
        setArgumentRange(0, 0);
        setIdentifiers("skill greatcombustion");
        setTypes(SkillType.DAMAGING, SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.ABILITY_PROPERTY_FIRE, SkillType.SILENCEABLE, SkillType.AGGRESSIVE, SkillType.DISABLING, SkillType.INTERRUPTING);
    }

    @Override
    public String getDescription(Hero hero) {

        int distance = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 6, false);

        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 3, false);

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 90, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.2, false);
        damage += hero.getAttributeValue(AttributeType.INTELLECT) * damageIncrease;

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 750, false);
        int durationIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE_PER_CHARISMA, 32, false);
        duration += hero.getAttributeValue(AttributeType.CHARISMA) * durationIncrease;

        String formattedDamage = Util.decFormat.format(damage);
        String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription().replace("$1", distance + "").replace("$2", radius + "").replace("$3", formattedDamage).replace("$4", formattedDuration);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 4);
        node.set(SkillSetting.DURATION.node(), 750);
        node.set(SkillSetting.DURATION_INCREASE_PER_CHARISMA.node(), 32);
        node.set(SkillSetting.RADIUS.node(), 4);
        node.set(SkillSetting.DAMAGE.node(), 100);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 1.875);
        node.set(SkillSetting.DELAY.node(), 1500);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        int distance = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 6, false);

        Block targetBlock = null;
        Block tempBlock;
        BlockIterator iter;
        try {
            iter = new BlockIterator(player, distance);
        }
        catch (IllegalStateException e) {
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        while (iter.hasNext()) {
            tempBlock = iter.next();

            if (Util.transparentBlocks.contains(tempBlock.getType()))
                targetBlock = tempBlock;
            else
                break;
        }

        if (targetBlock != null) {
            Location targetLocation = targetBlock.getLocation().clone();
            targetLocation.add(new Vector(.5, .5, .5));

            //player.getWorld().spigot().playEffect(targetBlock.getLocation().add(0, 1.0, 0), Effect.LAVA_POP, 0, 0, 0, 0, 0, 1, 135, 16);
            player.getWorld().spawnParticle(Particle.LAVA, targetBlock.getLocation().add(0, 1, 0), 135, 0, 0, 0, 1);
            //player.getWorld().spigot().playEffect(targetBlock.getLocation().add(0, 0.5, 0), Effect.MOBSPAWNER_FLAMES, 0, 0, 0, 0, 0, 0, 8, 16);
            //FIXME See if this is correct
            player.getWorld().spawnParticle(Particle.FLAME, targetBlock.getLocation().add(0, 0.5, 0), 8, 0, 0, 0, 0);
            //player.getWorld().spigot().playEffect(targetBlock.getLocation().add(0, 0.5, 0), Effect.EXPLOSION_LARGE, 0, 0, 0, 0, 0, 0, 3, 16);
            player.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, targetBlock.getLocation().add(0, 0.5, 0), 3, 0, 0, 0, 0);
            player.getWorld().playSound(targetBlock.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 6.0F, 1);

            double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 90, false);
            double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.2, false);
            damage += (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

            int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 3, false);
            int radiusSquared = radius * radius;

            // Loop through nearby targets and damage / knock back one of them
            int totalDistance = distance + radius;
            final List<Entity> nearbyEntities = player.getNearbyEntities(totalDistance, totalDistance, totalDistance);
            for (Entity entity : nearbyEntities) {
                // Check to see if the entity can be damaged
                if (!(entity instanceof LivingEntity) || entity.getLocation().distanceSquared(targetLocation) > radiusSquared)
                    continue;

                if (!damageCheck(player, (LivingEntity) entity))
                    continue;

                // Damage target
                LivingEntity target = (LivingEntity) entity;

                addSpellTarget(target, hero);
                damageEntity(target, player, damage, DamageCause.MAGIC, false);

                if (target instanceof Player) {
                    Hero targetHero = plugin.getCharacterManager().getHero((Player) target);
                    int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 750, false);
                    int durationIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE_PER_CHARISMA, 32, false);
                    duration += hero.getAttributeValue(AttributeType.CHARISMA) * durationIncrease;

                    targetHero.addEffect(new StunEffect(this, player, duration));
                }

                break;       // Only hit 1 target.
            }

            broadcastExecuteText(hero);
        }

        return SkillResult.NORMAL;
    }
}
