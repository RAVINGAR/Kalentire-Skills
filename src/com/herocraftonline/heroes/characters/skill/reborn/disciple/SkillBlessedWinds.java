package com.herocraftonline.heroes.characters.skill.reborn.disciple;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;

public class SkillBlessedWinds extends ActiveSkill {

    public SkillBlessedWinds(Heroes plugin) {
        super(plugin, "BlessedWinds");
        setDescription("Gracefully dash in the direction you are facing, striking all enemies you pass for $1 physical damage, and blessing allies you pass for $2 healing.");
        setUsage("/skill blessedwinds");
        setIdentifiers("skill blessedwinds");
        setArgumentRange(0, 0);
        setTypes(SkillType.MOVEMENT_INCREASING, SkillType.VELOCITY_INCREASING, SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.MULTI_GRESSIVE);
    }

    public String getDescription(Hero hero) {
        double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE, 40.0, true);
        double healing = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.HEALING, 50.0, true);

        return getDescription()
                .replace("$1", Util.decFormat.format(damage))
                .replace("$2", Util.decFormat.format(healing));
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.DAMAGE.node(), 40.0);
        config.set(SkillSetting.HEALING.node(), 50.0);
        config.set(SkillSetting.RADIUS.node(), 3.0);
        config.set("dash-power", 1.0);
        config.set("dash-power-per-level", 0.00125);
        return config;
    }

    public SkillResult use(Hero hero, String[] args) {
        final Player player = hero.getPlayer();

        broadcastExecuteText(hero);

        performBlessedWinds(hero, player);

        return SkillResult.NORMAL;
    }

    private void performBlessedWinds(Hero hero, Player player) {
        double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE, 40.0, true);
        double healing = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.HEALING, 50.0, true);

        final double dashPower = SkillConfigManager.getScaledUseSettingDouble(hero, this, "dash-power", 1.0, true);
        double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 3.0, true);

        final Vector velocity = player.getLocation().getDirection().clone().multiply(new Vector(dashPower, dashPower * 0.5, dashPower));
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 25, 0, 0.1, 0, 0.5);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERDRAGON_FLAP, 1.0F, 1.0F);

        final ArrayList<LivingEntity> hitEntities = new ArrayList<>();
        Skill skill = this;
        new BukkitRunnable() {
            int ticks = 0;
            int maxTicks = 5;

            public void run() {
                if (ticks == maxTicks) {
                    player.setFallDistance(-7f);
                    cancel();
                    return;
                }
                player.setVelocity(velocity);

                for (Entity ent : player.getNearbyEntities(radius, radius, radius)) {
                    if (!(ent instanceof LivingEntity) || player.equals(ent))
                        continue;

                    LivingEntity target = (LivingEntity) ent;
                    if (hitEntities.contains(target))
                        continue;

                    if (!hero.isAlliedTo(target)) {
                        if (!damageCheck(player, target)) {
                            continue;
                        }

                        addSpellTarget(target, hero);
                        damageEntity(target, player, damage, EntityDamageEvent.DamageCause.ENTITY_ATTACK, false);
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0F, 1.0F);
                    } else {
                        CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
                        if (!hero.tryHeal(targetCT, skill, healing))
                            continue;

                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EVOCATION_FANGS_ATTACK, 1.0F, 1.0F);
                    }

                    hitEntities.add(target);
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }
}
