package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.StunEffect;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Creature;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.scheduler.BukkitRunnable;

public class SkillDimensionalStrike extends TargettedSkill {

    public SkillDimensionalStrike(Heroes plugin) {
        super(plugin, "DimensionalStrike");
        setDescription("Teleport towards your target dealing $1 damage and stunning them for $3 second(s) before teleporting back after $4 second(s).");
        setUsage("/skill dimensionalstrike");
        setArgumentRange(0, 0);
        setIdentifiers("skill dimensionalstrike");
        setTypes(SkillType.TELEPORTING, SkillType.AGGRESSIVE, SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.DAMAGING, SkillType.DEBUFFING);
    }

    @Override
    public String getDescription(Hero hero) {
        double damage = (SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE.node(), 6, false));
        damage = damage > 0 ? damage : 0;
        int radius = (SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS.node(), 3, false));
        radius = Math.max(radius, 0);
        int duration = (SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 3000, false)) / 1000;
        duration = Math.max(duration, 0);
        int delay = (SkillConfigManager.getUseSetting(hero, this, "teleport-delay", 500, false));
        String description = getDescription().replace("$1", damage + "").replace("$2", radius + "").replace("$3", duration + "");
        return description;
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.RADIUS.node(), 3);
        node.set(SkillSetting.DURATION.node(), 3000);
        node.set(SkillSetting.DAMAGE.node(), 6);
        node.set("teleport-delay", 500);
        return node;
    }
    
    @Override
    public SkillResult use(Hero hero, LivingEntity target, String args[]) {
        Player player = hero.getPlayer();
        if (target instanceof Player && ((Player) target).equals(player)) {
            return SkillResult.INVALID_TARGET;
        }
        Location oLoc = player.getLocation();
        World world = oLoc.getWorld();
        player.teleport(target.getLocation());
        broadcastExecuteText(hero, target);
        long duration = (SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 3000, false));
        double damage = (SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE.node(), 6, false));

        if (target instanceof Player tPlayer) {
            Hero tHero = plugin.getCharacterManager().getHero(tPlayer);
            if (damageCheck(player, target)) {
                if (duration > 0) {
                    tHero.addEffect(new StunEffect(this, hero.getPlayer(), duration));
                }
                if (damage > 0) {
                    world.playSound(hero.getPlayer().getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.8F, 1.5F);
                    world.playSound(tPlayer.getLocation(), Sound.ENTITY_GHAST_SCREAM, 0.8F, 1.5F);
                    world.spawnParticle(Particle.ELECTRIC_SPARK, tPlayer.getLocation(), 40, 1, 1, 1, 0.4);
                    damageEntity(tPlayer, player, damage, DamageCause.ENTITY_ATTACK);
                    //tPlayer.damage(damage, player);
                }
            }
        } else if (target instanceof Creature) {
            if (damage > 0) {
                damageEntity(target, player, damage, DamageCause.ENTITY_ATTACK);
                //le.damage(damage, player);
            }
        }

        new BukkitRunnable() {

            @Override
            public void run() {
                world.playSound(oLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8F, 1.5F);
                world.spawnParticle(Particle.ELECTRIC_SPARK, oLoc, 40, 1, 1, 1, 0.4);
                player.teleport(oLoc);
            }
        }.runTaskLater(plugin, (long) (SkillConfigManager.getUseSetting(hero, this, "teleport-delay", 500, false) / 1000.0 * 20.0));
        return SkillResult.NORMAL;
    }
}
