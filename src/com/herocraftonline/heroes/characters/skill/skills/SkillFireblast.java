package com.herocraftonline.heroes.characters.skill.skills;

import java.util.List;

import org.bukkit.Color;
import org.bukkit.Effect;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import com.google.common.collect.Lists;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.VisualEffect;
import com.herocraftonline.heroes.characters.skill.ncp.NCPFunction;
import com.herocraftonline.heroes.characters.skill.ncp.NCPUtils;
import com.herocraftonline.heroes.util.Util;

import fr.neatmonster.nocheatplus.checks.CheckType;

public class SkillFireblast extends ActiveSkill {
    public VisualEffect fplayer = new VisualEffect();

    public SkillFireblast(Heroes plugin) {
        super(plugin, "Fireblast");
        setDescription("You strike a location within $1 blocks of you with a blast of fire. An enemy within the explosion will be dealt $2 damage and will be knocked away from the blast.");
        setUsage("/skill fireblast");
        setArgumentRange(0, 0);
        setIdentifiers("skill fireblast");
        setTypes(SkillType.DAMAGING, SkillType.ABILITY_PROPERTY_FIRE, SkillType.SILENCEABLE, SkillType.AGGRESSIVE);
    }

    public String getDescription(Hero hero) {

        int distance = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 6, false);
        double distanceIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE_INCREASE_PER_INTELLECT, 0.1, false);
        distance += (int) (hero.getAttributeValue(AttributeType.INTELLECT) * distanceIncrease);

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 90, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.2, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT);

        String formattedDamage = Util.decFormat.format(damage);

        return getDescription().replace("$1", distance + "").replace("$2", formattedDamage);
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 6);
        node.set(SkillSetting.MAX_DISTANCE_INCREASE_PER_INTELLECT.node(), 0.1);
        node.set(SkillSetting.DAMAGE.node(), 90);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 1.2);
        node.set(SkillSetting.RADIUS.node(), 3);
        node.set("horizontal-power", 1.2);
        node.set("vertical-power", 0.5);
        node.set("ncp-exemption-duration", 500);

        return node;
    }

    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        int distance = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 6, false);
        double distanceIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE_INCREASE_PER_INTELLECT, 0.1, false);
        distance += (int) (hero.getAttributeValue(AttributeType.INTELLECT) * distanceIncrease);

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

            if (Util.transparentBlocks.contains(tempBlock.getType()))
                targetBlock = tempBlock;
            else
                break;
        }

        if (targetBlock != null) {
            Location blastLocation = targetBlock.getLocation().clone();
            blastLocation.add(new Vector(.5, 0, .5));
            
            /*try {
                fplayer.playFirework(blastLocation.getWorld(), blastLocation, FireworkEffect.builder().flicker(false).trail(true)
                        .with(FireworkEffect.Type.BURST).withColor(Color.ORANGE).withFade(Color.RED).build());
            } catch (Exception e) {
                e.printStackTrace();
            }*/
            
            player.getWorld().spigot().playEffect(blastLocation, Effect.LAVA_POP, 0, 0, 1, 1, 1, 1, 75, 16);
            player.getWorld().playSound(blastLocation, Sound.EXPLODE, 10.0F, 16);
            player.getWorld().playSound(blastLocation, Sound.FIRE, 10.0F, 16);

            double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 90, false);
            double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.2, false);
            damage += damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT);

            double horizontalPower = SkillConfigManager.getUseSetting(hero, this, "horizontal-power", 1.1, false);
            final double veticalPower = SkillConfigManager.getUseSetting(hero, this, "vertical-power", 0.5, false);

            int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 3, false);
            int radiusSquared = radius * radius;

            // Loop through nearby targets and damage / knock back one of them
            int totalDistance = distance + radius;
            final List<Entity> nearbyEntities = player.getNearbyEntities(totalDistance, totalDistance, totalDistance);
            for (Entity entity : nearbyEntities) {
                // Check to see if the entity can be damaged
                if (!(entity instanceof LivingEntity) || entity.getLocation().distanceSquared(blastLocation) > radiusSquared)
                    continue;

                if (!damageCheck(player, (LivingEntity) entity))
                    continue;

                // Damage target
                final LivingEntity target = (LivingEntity) entity;

                addSpellTarget(target, hero);
                damageEntity(target, player, damage, DamageCause.MAGIC, false);

                // Do a knock up/back effect.
                Location targetLoc = target.getLocation();

                double xDir = targetLoc.getX() - blastLocation.getX();
                double zDir = targetLoc.getZ() - blastLocation.getZ();
                double magnitude = Math.sqrt(xDir * xDir + zDir * zDir);

                final double x = xDir / magnitude * horizontalPower;
                final double z = zDir / magnitude * horizontalPower;

                NCPUtils.applyExemptions(target, new NCPFunction() {

                    @Override
                    public void execute()
                    {
                        target.setVelocity(new Vector(x, veticalPower, z));
                    }
                }, Lists.newArrayList(CheckType.MOVING), SkillConfigManager.getUseSetting(hero, this, "ncp-exemption-duration", 500, false));

                break;       // Only hit 1 target.
            }

            broadcastExecuteText(hero);
        }

        return SkillResult.NORMAL;
    }
}
