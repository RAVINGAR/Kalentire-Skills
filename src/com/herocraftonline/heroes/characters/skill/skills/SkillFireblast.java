package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.util.Util;
import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.hooks.NCPExemptionManager;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import java.util.List;

public class SkillFireblast extends ActiveSkill {
    public VisualEffect fplayer = new VisualEffect();
    private boolean ncpEnabled = false;

    public SkillFireblast(Heroes plugin) {
        super(plugin, "Fireblast");
        setDescription("You strike a location within $1 blocks of you with a blast of fire. An enemy within the explosion will be dealt $2 damage and will be knocked away from the blast.");
        setUsage("/skill fireblast");
        setArgumentRange(0, 0);
        setIdentifiers("skill fireblast");
        setTypes(SkillType.DAMAGING, SkillType.ABILITY_PROPERTY_FIRE, SkillType.SILENCEABLE, SkillType.AGGRESSIVE);

        if (Bukkit.getServer().getPluginManager().getPlugin("NoCheatPlus") != null)
            ncpEnabled = true;
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
            
            try {
                fplayer.playFirework(blastLocation.getWorld(), blastLocation, FireworkEffect.builder().flicker(false).trail(true)
                        .with(FireworkEffect.Type.BURST).withColor(Color.ORANGE).withFade(Color.RED).build());
            } catch (Exception e) {
                e.printStackTrace();
            }

            double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 90, false);
            double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.2, false);
            damage += damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT);

            double horizontalPower = SkillConfigManager.getUseSetting(hero, this, "horizontal-power", 1.1, false);
            double veticalPower = SkillConfigManager.getUseSetting(hero, this, "vertical-power", 0.5, false);

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
                LivingEntity target = (LivingEntity) entity;

                addSpellTarget(target, hero);
                damageEntity(target, player, damage, DamageCause.MAGIC, false);

                // Do a knock up/back effect.
                Location targetLoc = target.getLocation();

                double xDir = targetLoc.getX() - blastLocation.getX();
                double zDir = targetLoc.getZ() - blastLocation.getZ();
                double magnitude = Math.sqrt(xDir * xDir + zDir * zDir);

                xDir = xDir / magnitude * horizontalPower;
                zDir = zDir / magnitude * horizontalPower;

                if (ncpEnabled) {
                    if (target instanceof Player) {
                        Player targetPlayer = (Player) target;
                        if (!targetPlayer.isOp()) {
                            long duration = SkillConfigManager.getUseSetting(hero, this, "ncp-exemption-duration", 500, false);
                            if (duration > 0) {
                                NCPExemptionEffect ncpExemptEffect = new NCPExemptionEffect(this, targetPlayer, duration);
                                CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
                                targetCT.addEffect(ncpExemptEffect);
                            }
                        }
                    }
                }

                target.setVelocity(new Vector(xDir, veticalPower, zDir));

                break;       // Only hit 1 target.
            }

            broadcastExecuteText(hero);
        }

        return SkillResult.NORMAL;
    }

    private class NCPExemptionEffect extends ExpirableEffect {

        public NCPExemptionEffect(Skill skill, Player applier, long duration) {
            super(skill, "NCPExemptionEffect_MOVING", applier, duration);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            final Player player = hero.getPlayer();

            NCPExemptionManager.exemptPermanently(player, CheckType.MOVING);
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            final Player player = hero.getPlayer();

            NCPExemptionManager.unexempt(player, CheckType.MOVING);
        }
    }
}
