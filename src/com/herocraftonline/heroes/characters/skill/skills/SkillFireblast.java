package com.herocraftonline.heroes.characters.skill.skills;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
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
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.VisualEffect;
import com.herocraftonline.heroes.util.Util;

import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.hooks.NCPExemptionManager;

public class SkillFireblast extends ActiveSkill {
    public VisualEffect fplayer = new VisualEffect();
    private boolean ncpEnabled = false;

    public SkillFireblast(Heroes plugin) {
        super(plugin, "Fireblast");
        setDescription("You strike a block within $1 blocks with a blast of fire. Enemies within $2 blocks of the target location will be dealt $3 damage and will be knocked away from the blast.");
        setUsage("/skill fireblast");
        setArgumentRange(0, 0);
        setIdentifiers("skill fireblast");
        setTypes(SkillType.DAMAGING, SkillType.AREA_OF_EFFECT, SkillType.ABILITY_PROPERTY_FIRE, SkillType.SILENCABLE, SkillType.AGGRESSIVE);

        if (Bukkit.getServer().getPluginManager().getPlugin("NoCheatPlus") != null)
            ncpEnabled = true;
    }

    public String getDescription(Hero hero) {

        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 3, false);

        int distance = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 6, false);
        double distanceIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE_INCREASE_PER_INTELLECT, 0.1, false);
        distance += (int) (hero.getAttributeValue(AttributeType.INTELLECT) * distanceIncrease);

        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 90, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.2, false);
        damage += (int) (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        return getDescription().replace("$1", distance + "").replace("$2", radius + "").replace("$3", damage + "");
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 6);
        // node.set(SkillSetting.MAX_DISTANCE_INCREASE_PER_INTELLECT.node(), Double.valueOf(0.1));
        node.set(SkillSetting.DAMAGE.node(), Integer.valueOf(90));
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), Double.valueOf(1.2));
        node.set(SkillSetting.RADIUS.node(), Integer.valueOf(3));

        return node;
    }

    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        int distance = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 6, false);
        //        double distanceIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE_INCREASE_PER_INTELLECT, 0.1, false);
        //        distance += (int) (hero.getAttributeValue(AttributeType.INTELLECT) * distanceIncrease);

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
                fplayer.playFirework(targetLocation.getWorld(), targetLocation, FireworkEffect.builder().flicker(false).trail(true).with(FireworkEffect.Type.BURST).withColor(Color.ORANGE).withFade(Color.RED).build());
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

            double horizontalPower = SkillConfigManager.getUseSetting(hero, this, "horizontal-power", 1.1, false);
            double veticalPower = SkillConfigManager.getUseSetting(hero, this, "vertical-power", 0.5, false);

            // Loop through nearby targets and damage / knock them back
            int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 3, false);
            List<Entity> targets = getNearbyEntities(targetLocation, radius, radius, radius);
            for (Entity entity : targets) {

                // Check to see if the entity can be damaged
                if (!(entity instanceof LivingEntity) || !damageCheck(player, (LivingEntity) entity))
                    continue;

                // Damage target
                LivingEntity target = (LivingEntity) entity;

                addSpellTarget(target, hero);
                damageEntity(target, player, damage, DamageCause.MAGIC);

                // Do a knock up/back effect.
                Location targetLoc = target.getLocation();

                double xDir = targetBlock.getX() - targetLoc.getX();
                double zDir = targetBlock.getZ() - targetLoc.getZ();
                double magnitude = Math.sqrt(xDir * xDir + zDir * zDir);

                xDir = xDir / magnitude * horizontalPower;
                zDir = zDir / magnitude * horizontalPower;

                if (ncpEnabled) {
                    if (target instanceof Player) {
                        Player targetPlayer = (Player) target;
                        if (!targetPlayer.isOp()) {
                            long duration = SkillConfigManager.getUseSetting(hero, this, "ncp-exemption-duration", 500, false);
                            if (duration > 0) {
                                NCPExemptionEffect ncpExemptEffect = new NCPExemptionEffect(this, duration);
                                CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
                                targetCT.addEffect(ncpExemptEffect);
                            }
                        }
                    }
                }

                target.setVelocity(new Vector(xDir, veticalPower, zDir));
            }

            broadcastExecuteText(hero);
        }

        return SkillResult.NORMAL;
    }

    protected List<Entity> getNearbyEntities(Location targetLocation, int radiusX, int radiusY, int radiusZ) {
        List<Entity> entities = new ArrayList<Entity>();

        for (Entity entity : targetLocation.getWorld().getEntities()) {
            if (isInBorder(targetLocation, entity.getLocation(), radiusX, radiusY, radiusZ)) {
                entities.add(entity);
            }
        }
        return entities;
    }

    public boolean isInBorder(Location center, Location targetLocation, int radiusX, int radiusY, int radiusZ) {
        int x1 = center.getBlockX();
        int y1 = center.getBlockY();
        int z1 = center.getBlockZ();

        int x2 = targetLocation.getBlockX();
        int y2 = targetLocation.getBlockY();
        int z2 = targetLocation.getBlockZ();

        if (x2 >= (x1 + radiusX) || x2 <= (x1 - radiusX) || y2 >= (y1 + radiusY) || y2 <= (y1 - radiusY) || z2 >= (z1 + radiusZ) || z2 <= (z1 - radiusZ))
            return false;

        return true;
    }

    private class NCPExemptionEffect extends ExpirableEffect {

        public NCPExemptionEffect(Skill skill, long duration) {
            super(skill, "NCPExemptionEffect_MOVING", duration);
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
