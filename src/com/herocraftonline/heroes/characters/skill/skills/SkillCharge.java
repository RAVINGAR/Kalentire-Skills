package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.hooks.NCPExemptionManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class SkillCharge
        extends TargettedSkill {
    private boolean ncpEnabled = false;

    public SkillCharge(Heroes plugin) {
        super(plugin, "Charge");
        setDescription("You execute a charging leap to your target (within $2 blocks), ramming them for $1 damage.");
        setUsage("/skill charge");
        setArgumentRange(0, 0);
        setIdentifiers(new String[]{"skill charge"});
        setTypes(new SkillType[]{SkillType.MOVEMENT_INCREASING, SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.DAMAGING});
        if (Bukkit.getServer().getPluginManager().getPlugin("NoCheatPlus") != null)
            ncpEnabled = true;
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DAMAGE.node(), Double.valueOf(20));
        node.set(SkillSetting.DAMAGE_INCREASE_PER_STRENGTH.node(), Double.valueOf(0.4));
        node.set(SkillSetting.MAX_DISTANCE.node(), Integer.valueOf(8));
        return node;
    }

    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        final Player player = hero.getPlayer();
        if (target.equals(player))
            return SkillResult.INVALID_TARGET_NO_MSG;

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 20, true);
        damage += SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 0.4, true)
                * hero.getAttributeValue(AttributeType.STRENGTH);

        if (ncpEnabled) {
            long duration = SkillConfigManager.getUseSetting(hero, this, "ncp-exemption-duration", 1500, false);
            NCPExemptionEffect ncpExemptEffect = new NCPExemptionEffect(this, player, duration);
            hero.addEffect(ncpExemptEffect);
        }

        player.setVelocity(new Vector(0, 0.5, 0));

        final LivingEntity t = target;

        this.plugin.getServer().getScheduler().scheduleSyncDelayedTask(this.plugin, new Runnable() {
            public void run() {
                final Location playerLoc = player.getLocation();
                final Location targetLoc = t.getLocation();
                double xDir = (targetLoc.getX() - playerLoc.getX()) / 4D;
                double yDir = (targetLoc.getY() - playerLoc.getY()) / 4D;
                double zDir = (targetLoc.getZ() - playerLoc.getZ()) / 4D;
                Vector v = new Vector(xDir, yDir / 2, zDir);
                player.setVelocity(v);
                player.setFallDistance(-20.0F);
                new BukkitRunnable() {
                    public void run() {
                        if (player.getLocation().distance(targetLoc) <= 2.0D) {
                            player.setVelocity(player.getVelocity().multiply(0.3));
                            cancel();
                        }
                    }
                }.runTaskTimer(plugin, 0, 2);
            }
        }, 10L);

        addSpellTarget(target, hero);
        damageEntity(target, player, damage, DamageCause.ENTITY_ATTACK);
        target.getWorld().spigot().playEffect(target.getLocation().add(0, 0.5, 0), org.bukkit.Effect.CRIT, 0, 0, 0.4F, 0.4F, 0.4F, 0.3F, 55, 16);
        target.getWorld().spigot().playEffect(target.getLocation().add(0, 0.5, 0), org.bukkit.Effect.PARTICLE_SMOKE, 0, 0, 0.4F, 0.4F, 0.4F, 0.3F, 25, 16);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0F, 1.0F);
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_GRAVEL_BREAK, 1.0F, 0.5F);

        player.getWorld().spigot().playEffect(player.getLocation().add(0, 0.2, 0), org.bukkit.Effect.CLOUD, 0, 0, 0.3F, 0.1F, 0.3F, 0.2F, 35, 16);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_HORSE_JUMP, 1.0F, 1.0F);
        broadcastExecuteText(hero, target);
        return SkillResult.NORMAL;
    }

    public class NCPExemptionEffect extends ExpirableEffect {

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

    @Override
    public String getDescription(Hero hero) {
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 20, true);
        damage += SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE, 0.4, true) * hero.getLevel();

        double range = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 8, true);

        return getDescription().replace("$1", damage + "").replace("$2", range + "");

    }
}

