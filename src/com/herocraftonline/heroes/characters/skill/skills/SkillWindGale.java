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
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class SkillWindGale extends ActiveSkill {

    private boolean ncpEnabled = false;
    
    public VisualEffect fplayer = new VisualEffect();

    public SkillWindGale(Heroes plugin) {
        super(plugin, "WindGale");
        setDescription("You unleash strong gales of wind front of you, up to $1 blocks. Targets hit will will be dealt $2 damage and knocked back based on your Intellect.");
        setUsage("/skill windgale");
        setArgumentRange(0, 0);
        setIdentifiers("skill windgale");
        setTypes(SkillType.DAMAGING, SkillType.ABILITY_PROPERTY_AIR, SkillType.AREA_OF_EFFECT, SkillType.SILENCEABLE, SkillType.AGGRESSIVE);
        
        if (Bukkit.getServer().getPluginManager().getPlugin("NoCheatPlus") != null) {
            ncpEnabled = true;
        }
    }

    @Override
    public String getDescription(Hero hero) {

        int distance = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 6, false);

        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 90, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.2, false);
        damage += (int) (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        return getDescription().replace("$1", distance + "").replace("$2", damage + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 6);
        node.set(SkillSetting.DAMAGE.node(), Integer.valueOf(50));
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), Double.valueOf(0.2));
        node.set(SkillSetting.RADIUS.node(), Integer.valueOf(3));
        node.set("wind-travel-delay", Integer.valueOf(1));
        node.set("push-horizontal-power", Double.valueOf(1.5));
        node.set("push-horizontal-power-increase-per-wisdom", Double.valueOf(0.0375));
        node.set("push-vertical-power", Double.valueOf(0.25));
        node.set("push-vertical-power-increase-per-wisdom", Double.valueOf(0.0075));
        node.set("ncp-exemption-duration", 1500);
        node.set("push-delay", Double.valueOf(0.2));

        return node;
    }

    @Override
    public SkillResult use(final Hero hero, String[] args) {
        final Player player = hero.getPlayer();

        int distance = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 10, false);

        Block tempBlock;
        BlockIterator iter = null;
        try {
            iter = new BlockIterator(player, distance);
        }
        catch (IllegalStateException e) {
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        broadcastExecuteText(hero);

        boolean isXDirection = is_X_Direction(player);

        double tempDamage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 50, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 0.2, false);
        tempDamage += damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT);
        final double damage = tempDamage;

        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 2, false);
        final int radiusSquared = radius * radius;

        int delay = SkillConfigManager.getUseSetting(hero, this, "wind-travel-delay", 1, false);

        final List<Entity> nearbyEntities = player.getNearbyEntities(distance * 2, distance, distance * 2);
        final List<Entity> hitEnemies = new ArrayList<Entity>();

        int numBlocks = 0;
        while (iter.hasNext()) {
            tempBlock = iter.next();

            if (Util.transparentBlocks.contains(tempBlock.getType())) {
                final List<Location> locations = new ArrayList<Location>();
                if (isXDirection) {
                    for (int xDir = -1; xDir < 1 + 1; xDir++) {
                        Block radiusBlocks = tempBlock.getRelative(xDir, 0, 0);

                        if (Util.transparentBlocks.contains(radiusBlocks.getType())) {
                            locations.add(radiusBlocks.getLocation().clone().add(new Vector(.5, 0, .5)));
                        }
                    }
                }
                else {
                    for (int zDir = -1; zDir < 1 + 1; zDir++) {
                        Block radiusBlocks = tempBlock.getRelative(0, 0, zDir);

                        if (Util.transparentBlocks.contains(radiusBlocks.getType())) {
                            locations.add(radiusBlocks.getLocation().clone().add(new Vector(.5, 0, .5)));
                        }
                    }
                }
                
                final SkillWindGale skill = this;
                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                    public void run() {
                        for (Location location : locations) {
                            location.getWorld().playSound(location, Sound.FIZZ, 0.25F, 1.0F);
                            location.getWorld().spigot().playEffect(location, Effect.CLOUD, 0, 0, 0, 0, 0, 0.1F, 25, 16);
                        }


                        for (Entity entity : nearbyEntities) {
                            if (!(entity instanceof LivingEntity) || hitEnemies.contains(entity))
                                continue;

                            boolean exitLoop = true;
                            for (Location location : locations) {
                                if (entity.getLocation().distanceSquared(location) <= radiusSquared) {
                                    exitLoop = false;
                                    break;
                                }
                            }

                            if (exitLoop)
                                continue;
                            
                            // Check to see if the entity can be damaged
                            if (!damageCheck(player, (LivingEntity) entity))
                                continue;
                            
                            // Damage target
                            final LivingEntity target = (LivingEntity) entity;

                            addSpellTarget(target, hero);
                            damageEntity(target, player, damage, DamageCause.MAGIC);
                            
                            hitEnemies.add(entity);
                            
                            // Let's bypass the nocheat issues...
                            if (ncpEnabled) {
                                if (entity instanceof Player) {
                                    Player targetPlayer = (Player) entity;
                                    if (!targetPlayer.isOp()) {
                                        long duration = SkillConfigManager.getUseSetting(hero, skill, "ncp-exemption-duration", 1500, false);
                                        if (duration > 0) {
                                            NCPExemptionEffect ncpExemptEffect = new NCPExemptionEffect(skill, targetPlayer, duration);
                                            CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(targetPlayer);
                                            targetCT.addEffect(ncpExemptEffect);
                                        }
                                    }
                                }
                            }
                            
                            // And now we're into the code clone of ForcePush
                            Location playerLoc = player.getLocation();
                            Location targetLoc = target.getLocation();

                            Material mat = targetLoc.getBlock().getRelative(BlockFace.DOWN).getType();

                            boolean weakenVelocity = false;
                            switch (mat) {
                                case STATIONARY_WATER:
                                case STATIONARY_LAVA:
                                case WATER:
                                case LAVA:
                                case SOUL_SAND:
                                    weakenVelocity = true;
                                    break;
                                default:
                                    break;
                            }

                            double tempVPower = SkillConfigManager.getUseSetting(hero, skill, "push-vertical-power", Double.valueOf(0.25), false);
                            double vPowerIncrease = SkillConfigManager.getUseSetting(hero, skill, "push-vertical-power-increase-per-wisdom", Double.valueOf(0.0075), false);
                            tempVPower += (vPowerIncrease * hero.getAttributeValue(AttributeType.WISDOM));

                            if (weakenVelocity)
                                tempVPower *= 0.75;

                            final double vPower = tempVPower;

                            Vector pushUpVector = new Vector(0, vPower, 0);
                            target.setVelocity(pushUpVector);

                            final double xDir = targetLoc.getX() - playerLoc.getX();
                            final double zDir = targetLoc.getZ() - playerLoc.getZ();

                            double tempHPower = SkillConfigManager.getUseSetting(hero, skill, "push-horizontal-power", Double.valueOf(1.5), false);
                            double hPowerIncrease = SkillConfigManager.getUseSetting(hero, skill, "push-horizontal-power-increase-per-wisdom", Double.valueOf(0.0375), false);
                            tempHPower += hPowerIncrease * hero.getAttributeValue(AttributeType.WISDOM);

                            if (weakenVelocity)
                                tempHPower *= 0.75;

                            final double hPower = tempHPower;

                            // Push them "up" first. THEN we can push them away.
                            double delay = SkillConfigManager.getUseSetting(hero, skill, "push-delay", 0.2, false);
                            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                                public void run() {
                                    // Push them away
                                    //double yDir = player.getVelocity().getY();
                                    Vector pushVector = new Vector(xDir, 0, zDir).normalize().multiply(hPower).setY(vPower);
                                    target.setVelocity(pushVector);
                                }
                            }, (long) (delay * 20));
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

    private boolean is_X_Direction(Player player) {
        Vector u = player.getLocation().getDirection();
        u = new Vector(u.getX(), 0.0D, u.getZ()).normalize();
        Vector v = new Vector(0, 0, -1);
        double magU = Math.sqrt(Math.pow(u.getX(), 2.0D) + Math.pow(u.getZ(), 2.0D));
        double magV = Math.sqrt(Math.pow(v.getX(), 2.0D) + Math.pow(v.getZ(), 2.0D));
        double angle = Math.acos(u.dot(v) / (magU * magV));
        angle = angle * 180.0D / Math.PI;
        angle = Math.abs(angle - 180.0D);

        return (angle <= 45.0D) || (angle > 135.0D);
    }
    
    private class NCPExemptionEffect extends ExpirableEffect {

        public NCPExemptionEffect(Skill skill, Player applier, long duration) {
            super(skill, "NCPExemptionEffect_MOVING", applier, duration, null, null);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            Player player = hero.getPlayer();

            if (ncpEnabled)
                NCPExemptionManager.exemptPermanently(player, CheckType.MOVING);
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            Player player = hero.getPlayer();

            if (ncpEnabled)
                NCPExemptionManager.unexempt(player, CheckType.MOVING);

        }
    }
}
