package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.Vector;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.skills.totem.SkillBaseTotem;
import com.herocraftonline.heroes.characters.skill.skills.totem.Totem;

import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.hooks.NCPExemptionManager;

public class SkillTremorTotem extends SkillBaseTotem {

    private boolean ncpEnabled = false;

	public SkillTremorTotem(Heroes plugin) {
        super(plugin, "TremorTotem");
        setDescription("Places a Tremor totem at the target location that deals $1 physical damage and knocks back targets in a $2 radius. Lasts for $3 seconds.");
        setUsage("/skill tremortotem");
        setArgumentRange(0, 0);
        setIdentifiers("skill tremortotem");
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.FORCE, SkillType.DAMAGING, SkillType.AGGRESSIVE, SkillType.SILENCEABLE);
        material = Material.QUARTZ_BLOCK;

        if (Bukkit.getServer().getPluginManager().getPlugin("NoCheatPlus") != null) {
            ncpEnabled = true;
        }
    }

    @Override
    public String getDescription(Hero hero) {
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 25, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, Double.valueOf(0.3), false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT);
    	
    	return getDescription()
    			.replace("$1", damage + "")
    			.replace("$2", getRange(hero) + "")
    			.replace("$3", getDuration(hero)*0.001 + "");
    }

    @Override
    public ConfigurationSection getSpecificDefaultConfig(ConfigurationSection node) {

        node.set(SkillSetting.DAMAGE.node(), Integer.valueOf(25));
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), Double.valueOf(0.3));
        node.set("horizontal-power", Double.valueOf(1.5));
        node.set("horizontal-power-increase-per-wisdom", Double.valueOf(0.0375));
        node.set("vertical-power", Double.valueOf(0.25));
        node.set("vertical-power-increase-per-wisdom", Double.valueOf(0.0075));
        node.set("ncp-exemption-duration", Integer.valueOf(1500));

        return node;
    }

    @Override
    public void usePower(Hero hero, Totem totem) {
        Player player = hero.getPlayer();


        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 25, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, Double.valueOf(0.3), false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT);

        double hPower = SkillConfigManager.getUseSetting(hero, this, "horizontal-power", Double.valueOf(2.8), false);
        double vPower = SkillConfigManager.getUseSetting(hero, this, "vertical-power", Double.valueOf(0.5), false);
        
        for (Entity entity : totem.getTargets(hero)) {
            if (!(entity instanceof LivingEntity)) {
                continue;
            }

            // Check if the target is damagable
            if (!damageCheck(player, (LivingEntity) entity)) {
                continue;
            }

            LivingEntity target = (LivingEntity) entity;

            double individualHPower = hPower;
            double individualVPower = vPower;

            Material mat = target.getLocation().getBlock().getRelative(BlockFace.DOWN).getType();

            switch (mat) {
                case STATIONARY_WATER:
                case STATIONARY_LAVA:
                case WATER:
                case LAVA:
                case SOUL_SAND:
                    individualHPower /= 2;
                    individualVPower /= 2;
                    break;
                default:
                    break;
            }

            // Damage the target
            addSpellTarget(target, hero);
            damageEntity(target, player, damage, DamageCause.MAGIC, false);

            // Do our knockback
            Location playerLoc = player.getLocation();
            Location targetLoc = target.getLocation();

            double xDir = targetLoc.getX() - playerLoc.getX();
            double zDir = targetLoc.getZ() - playerLoc.getZ();
            double magnitude = Math.sqrt(xDir * xDir + zDir * zDir);

            xDir = xDir / magnitude * individualHPower;
            zDir = zDir / magnitude * individualHPower;
            
            // The effect code is up here because the targets are being sent flying up. Can't accurately put the effect where we want it then.
            @SuppressWarnings("deprecation")
            int id = entity.getLocation().getBlock().getRelative(BlockFace.DOWN).getTypeId();
            /* This is the new Particle API system for Spigot - the first few int = id, data, offsetX/Y/Z, speed, count, radius)
             * offset controls how spread out the particles are
             * id and data only work for two particles: ITEM_BREAK and TILE_BREAK
             * */
            entity.getWorld().spigot().playEffect(entity.getLocation().add(0, 0.6, 0), Effect.TILE_BREAK, id, 0, 0, 0, 0, 1, 25, 16);
            entity.getWorld().spigot().playEffect(entity.getLocation().add(0, 0.7, 0), Effect.TILE_BREAK, id, 0, 0, 0, 0, 1, 25, 16);
            entity.getWorld().spigot().playEffect(entity.getLocation().add(0, 0.9, 0), Effect.TILE_BREAK, id, 0, 0, 0, 0, 1, 25, 16);
            entity.getWorld().spigot().playEffect(entity.getLocation().add(0, 1.0, 0), Effect.TILE_BREAK, id, 0, 0, 0, 0, 1, 25, 16);
            entity.getWorld().spigot().playEffect(entity.getLocation().add(0, 1.1, 0), Effect.TILE_BREAK, id, 0, 0, 0, 0, 1, 25, 16);
            entity.getWorld().spigot().playEffect(entity.getLocation().add(0, 1.2, 0), Effect.TILE_BREAK, id, 0, 0, 0, 0, 1, 25, 16);
            
            // Let's bypass the nocheat issues...
            if (ncpEnabled) {
                if (target instanceof Player) {
                    Player targetPlayer = (Player) target;
                    Hero targetHero = plugin.getCharacterManager().getHero(targetPlayer);
                    if (!targetPlayer.isOp()) {
                        long ncpDuration = SkillConfigManager.getUseSetting(hero, this, "ncp-exemption-duration", 500, false);
                        NCPExemptionEffect ncpExemptEffect = new NCPExemptionEffect(this, targetPlayer, ncpDuration);
                        targetHero.addEffect(ncpExemptEffect);
                    }
                }
            }
            
            target.setVelocity(new Vector(xDir, individualVPower, zDir));
        }
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
