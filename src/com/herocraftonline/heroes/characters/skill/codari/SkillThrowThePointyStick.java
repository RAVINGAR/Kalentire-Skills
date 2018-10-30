package com.herocraftonline.heroes.characters.skill.codari;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.RecastData;
import org.bukkit.Bukkit;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.metadata.FixedMetadataValue;

public class SkillThrowThePointyStick extends ActiveSkill implements Listener {

    private static final String PROJECTILE_METADATA_KEY = "thrown-pointy-stick";

    public SkillThrowThePointyStick(Heroes plugin) {
        super(plugin, "ThrowThePointyStick");
        setDescription("");

        setUsage("/skill " + getName());
        setArgumentRange(0, 0);
        setIdentifiers("skill " + getName());
    }

    @Override
    public void init() {
        super.init();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

    @Override
    public SkillResult use(Hero hero, String[] strings) {

        Player player = hero.getPlayer();

        RecastData recastData = new RecastData("Spear Throw");
        recastData.setNeverReady();
        startRecast(hero, recastData);

        Trident projectile = player.launchProjectile(Trident.class);
        projectile.setPickupStatus(Arrow.PickupStatus.DISALLOWED);
        projectile.setVelocity(projectile.getVelocity().multiply(0.5));

        double damage = 5;

        projectile.setMetadata(PROJECTILE_METADATA_KEY, new FixedMetadataValue(plugin, damage));

        return SkillResult.NORMAL;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onProjectileHit(ProjectileHitEvent e) {

        if (e.getEntity() instanceof Trident && e.getEntity().hasMetadata(PROJECTILE_METADATA_KEY)) {

            Player player = (Player) e.getEntity().getShooter();
            Hero hero = plugin.getCharacterManager().getHero(player);

            player.sendMessage("----------------------------------------------------");
            player.sendMessage("PROJECTILE HIT: isEntity=" + (e.getHitEntity() != null));
            endRecast(hero);

            if (e.getHitEntity() != null && e.getHitEntity() instanceof LivingEntity) {

                LivingEntity target = (LivingEntity) e.getHitEntity();
                if (damageCheck(player, target)) {

                    double tridentYaw = (e.getEntity().getLocation().getYaw() + 540) % 360;
                    player.sendMessage("YAW TRIDENT: " + tridentYaw);
                    double targetYaw = (target.getLocation().getYaw() + 360) % 360;
                    player.sendMessage("YAW TARGET: " + targetYaw);

                    double yawDifference = Math.min(360 - Math.abs(tridentYaw - targetYaw), Math.abs(tridentYaw - targetYaw));
                    player.sendMessage("YAW DIFFERENCE: " + yawDifference);

                    if (yawDifference <= 90) {
                        player.sendMessage("LOOKING AT YOU");
                    } else {
                        player.sendMessage("LOOKING AWAY FROM YOU");
                    }
                }
            }

            if (e.getHitBlock() != null) {
                e.getEntity().remove();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onWeaponDamage(WeaponDamageEvent e) {
        if (e.isProjectile() && e.getAttackerEntity() instanceof Trident && e.getAttackerEntity().hasMetadata(PROJECTILE_METADATA_KEY)) {
            e.setCancelled(true);
        }
    }
}
