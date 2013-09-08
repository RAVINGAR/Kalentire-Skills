package com.herocraftonline.heroes.characters.skill.skills;

import java.util.List;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.StunEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.VisualEffect;
import com.herocraftonline.heroes.util.Util;

public class SkillGreatCombustion extends ActiveSkill {
    public VisualEffect fplayer = new VisualEffect();

    public SkillGreatCombustion(Heroes plugin) {
        super(plugin, "GreatCombustion");
        setDescription("Unleash a mass of condensed flame in front of you, striking up to $1 blocks away. An enemy within $2 blocks of the explosion will be dealt $3 damage and will be stunned for $4 seconds.");
        setUsage("/skill greatcombustion");
        setArgumentRange(0, 0);
        setIdentifiers("skill greatcombustion");
        setTypes(SkillType.DAMAGING, SkillType.ABILITY_PROPERTY_FIRE, SkillType.SILENCABLE, SkillType.AGGRESSIVE);
    }

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

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), Integer.valueOf(4));
        node.set(SkillSetting.RADIUS.node(), Integer.valueOf(4));
        node.set(SkillSetting.DAMAGE.node(), Integer.valueOf(100));
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), Double.valueOf(1.875));
        node.set(SkillSetting.DURATION.node(), Integer.valueOf(750));
        node.set(SkillSetting.DURATION_INCREASE_PER_CHARISMA.node(), Integer.valueOf(32));
        node.set(SkillSetting.DELAY.node(), Integer.valueOf(1500));

        return node;
    }

    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        int distance = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 6, false);

        Block targetBlock = null;
        Block tempBlock;
        BlockIterator iter = null;
        try {
            iter = new BlockIterator(player, distance);
        }
        catch (IllegalStateException e) {
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        while (iter.hasNext()) {
            tempBlock = iter.next();

            if ((Util.transparentBlocks.contains(tempBlock.getType())
            && (Util.transparentBlocks.contains(tempBlock.getRelative(BlockFace.UP).getType())
            || Util.transparentBlocks.contains(tempBlock.getRelative(BlockFace.DOWN).getType())))) {
                targetBlock = tempBlock;
            }
            else
                break;
        }

        if (targetBlock != null) {
            Location targetLocation = targetBlock.getLocation().clone();
            targetLocation.add(new Vector(.5, .5, .5));

            try {
                fplayer.playFirework(targetLocation.getWorld(), targetLocation, FireworkEffect.builder().flicker(false).trail(true).with(FireworkEffect.Type.BURST).withColor(Color.RED).withFade(Color.ORANGE).build());
                fplayer.playFirework(targetLocation.getWorld(), targetLocation, FireworkEffect.builder().flicker(false).trail(true).with(FireworkEffect.Type.BALL).withColor(Color.ORANGE).withFade(Color.RED).build());
            }
            catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 90, false);
            double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.2, false);
            damage += (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

            int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 3, false);
            int radiusSquared = radius * radius;

            // Loop through nearby targets and damage / knock back one of them
            final List<Entity> nearbyEntities = player.getNearbyEntities(distance, distance, distance);
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