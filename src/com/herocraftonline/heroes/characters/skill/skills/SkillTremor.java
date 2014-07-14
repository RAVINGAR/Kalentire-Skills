package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.hooks.NCPExemptionManager;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.Vector;

import java.util.List;

public class SkillTremor extends ActiveSkill{

    private boolean ncpEnabled = false;

	public SkillTremor(Heroes plugin) {
        super(plugin, "Tremor");
        setDescription("Strike the ground with a powerful tremor, affecting all targets within $1 blocks. All targets hit with the tremor are dealt $2 physical damage and knocked back a great distance.");
        setUsage("/skill tremor");
        setArgumentRange(0, 0);
        setIdentifiers("skill tremor");
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.FORCE, SkillType.DAMAGING, SkillType.AGGRESSIVE);

        if (Bukkit.getServer().getPluginManager().getPlugin("NoCheatPlus") != null) {
            ncpEnabled = true;
        }
    }

    @Override
    public String getDescription(Hero hero) {
        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, Integer.valueOf(5), false);

        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, Integer.valueOf(50), false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 1.6, false);
        damage += (int) (damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH));

        return getDescription().replace("$1", radius + "").replace("$2", damage + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DAMAGE.node(), 50);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_STRENGTH.node(), 1.125);
        node.set(SkillSetting.RADIUS.node(), 5);
        node.set("horizontal-power", 0.0);
        node.set("vertical-power", 0.4);
        node.set("ncp-exemption-duration", 1500);
        node.set(SkillSetting.DELAY.node(), 50);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, Integer.valueOf(5), false);

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 50, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 1.125, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH);

        double hPower = SkillConfigManager.getUseSetting(hero, this, "horizontal-power", 2.8, false);
        double vPower = SkillConfigManager.getUseSetting(hero, this, "vertical-power", 0.5, false);

        broadcastExecuteText(hero);
        
        List<Entity> entities = hero.getPlayer().getNearbyEntities(radius, radius, radius);
        for (Entity entity : entities) {
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
            damageEntity(target, player, damage, DamageCause.ENTITY_ATTACK, false);

            // Do our knockback
            Location playerLoc = player.getLocation();
            Location targetLoc = target.getLocation();

            double xDir = targetLoc.getX() - playerLoc.getX();
            double zDir = targetLoc.getZ() - playerLoc.getZ();
            double magnitude = Math.sqrt(xDir * xDir + zDir * zDir);

            xDir = xDir / magnitude * individualHPower;
            zDir = zDir / magnitude * individualHPower;

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

        //player.getWorld().playSound(player.getLocation(), Sound.HURT, 1.3F, 0.5F);
        player.getWorld().playEffect(player.getLocation(), Effect.EXPLOSION, 3);
        player.getWorld().playSound(player.getLocation(), Sound.EXPLODE, 0.5F, 1.0F);
        player.getWorld().playSound(player.getLocation(), Sound.ORB_PICKUP, 0.8F, 1.0F);
        player.getWorld().playSound(player.getLocation(), Sound.ZOMBIE_UNFECT, 0.8F, 1.0F);

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
            NCPExemptionManager.exemptPermanently(player, CheckType.COMBINED);
            NCPExemptionManager.exemptPermanently(player, CheckType.FIGHT);
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            final Player player = hero.getPlayer();

            NCPExemptionManager.unexempt(player, CheckType.MOVING);
            NCPExemptionManager.unexempt(player, CheckType.FIGHT);
            NCPExemptionManager.unexempt(player, CheckType.COMBINED);
        }
    }
}
