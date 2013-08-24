package com.herocraftonline.heroes.characters.skill.skills;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.Vector;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;

import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.hooks.NCPExemptionManager;

public class SkillTremor extends ActiveSkill{

    private boolean ncpEnabled = false;

	public SkillTremor(Heroes plugin) {
        super(plugin, "Tremor");
        setDescription("You knock back all nearby enemies and deal $1 damage");
        setUsage("/skill tremor");
        setArgumentRange(0, 0);
        setIdentifiers("skill tremor");
        setTypes(SkillType.DAMAGING, SkillType.PHYSICAL, SkillType.MOVEMENT, SkillType.HARMFUL, SkillType.INTERRUPT);

        if (Bukkit.getServer().getPluginManager().getPlugin("NoCheatPlus") != null) {
            ncpEnabled = true;
        }
    }

    @Override
    public String getDescription(Hero hero) {
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 10, false);
        return getDescription().replace("$1", damage + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DAMAGE.node(), 10);
        node.set(SkillSetting.RADIUS.node(), 5);
        node.set("knockback-power", 6.0);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5, false);
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 10, false);
        double knockbackPower = SkillConfigManager.getUseSetting(hero, this, "knockback-power", 6.0, false);
        Location playerLoc = player.getLocation();
        
        List<Entity> entities = hero.getPlayer().getNearbyEntities(radius, radius, radius);
        long currentTime = System.currentTimeMillis();
        for (Entity entity : entities) {
            if (!(entity instanceof LivingEntity)) {
                continue;
            }
            LivingEntity target = (LivingEntity) entity;

            // Check if the target is damagable
            if (!damageCheck(player, target)) {
                continue;
            }

            Location targetLoc = target.getLocation();

            addSpellTarget(target, hero);
            damageEntity(target, player, damage, DamageCause.ENTITY_ATTACK, false);

            if(entity instanceof Player) {
            	Hero enemy = plugin.getCharacterManager().getHero((Player)entity);
            	if(enemy.getDelayedSkill() != null) {
	            	enemy.cancelDelayedSkill();
	                enemy.setCooldown("global", Heroes.properties.globalCooldown + currentTime);
            	}

                // Let's bypass the nocheat issues...
                if (ncpEnabled) {
                    if (target instanceof Player) {
                        Player targetPlayer = (Player) target;
                        if (!targetPlayer.isOp()) {
                            long ncpDuration = SkillConfigManager.getUseSetting(hero, this, "ncp-exemption-duration", 1000, false);
                            NCPExemptionEffect ncpExemptEffect = new NCPExemptionEffect(this, ncpDuration);
                            enemy.addEffect(ncpExemptEffect);
                        }
                    }
                }
            }
            
            double xDir = targetLoc.getX() - playerLoc.getX();
            double zDir = targetLoc.getZ() - playerLoc.getZ();
            Vector v = new Vector(xDir, 0, zDir).normalize().multiply(knockbackPower).setY(.5);
            target.setVelocity(v);
        }
        player.getWorld().playEffect(player.getLocation(), Effect.MOBSPAWNER_FLAMES, 3);
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.EXPLODE , 0.8F, 1.0F); 
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.ORB_PICKUP , 0.8F, 1.0F); 
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.ZOMBIE_UNFECT , 0.8F, 1.0F); 
        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
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
