package com.herocraftonline.heroes.characters.skill.codari;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.RecastData;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

public class SkillThrowThePointyStick extends ActiveSkill implements Listener {

    private static final String PROJECTILE_METADATA_KEY = "thrown-pointy-stick";

    private static final double DEFAULT_DAMAGE = 10;

    private static final String FRONTAL_ARC_NODE = "frontal-arc";
    private static final double DEFAULT_FRONTAL_ARC = 90;

    private static final String THROW_VELOCITY_NODE = "throw-velocity";
    private static final double DEFAULT_THROW_VELOCITY = 2.5;

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
    public ConfigurationSection getDefaultConfig() {

        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DAMAGE.node(), DEFAULT_DAMAGE);
        node.set(FRONTAL_ARC_NODE, DEFAULT_FRONTAL_ARC);
        node.set(THROW_VELOCITY_NODE, DEFAULT_THROW_VELOCITY);

        return node;
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

        double throwVelocity = SkillConfigManager.getUseSetting(hero, this, THROW_VELOCITY_NODE, DEFAULT_THROW_VELOCITY, false);
        if (throwVelocity < 1) {
            throwVelocity = 1;
        }

        projectile.setVelocity(projectile.getVelocity().normalize().multiply(throwVelocity));

        projectile.setMetadata(PROJECTILE_METADATA_KEY, new FixedMetadataValue(plugin, null));

        broadcastExecuteText(hero);
        
        return SkillResult.NORMAL;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onProjectileHit(ProjectileHitEvent e) {

        if (e.getEntity() instanceof Trident && e.getEntity().hasMetadata(PROJECTILE_METADATA_KEY)) {

            Player player = (Player) e.getEntity().getShooter();
            Hero hero = plugin.getCharacterManager().getHero(player);

            endRecast(hero);

            if (e.getHitEntity() != null && e.getHitEntity() instanceof LivingEntity) {

                LivingEntity target = (LivingEntity) e.getHitEntity();
                if (damageCheck(player, target)) {

                    double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, DEFAULT_DAMAGE, false);
                    if (damage < 0) {
                        damage = 0;
                    }

                    if (damage > 0) {
                        addSpellTarget(target, hero);
                        damageEntity(target, player, damage);
                    }

                    double targetYaw = (target.getLocation().getYaw() + 360) % 360;

                    Vector tridentVelocity = e.getEntity().getVelocity();
                    double tridentYaw;
                    if (tridentVelocity.getX() != 0 && tridentVelocity.getZ() != 0) {
                        tridentYaw = (float)Math.toDegrees((Math.atan2(-tridentVelocity.getX(), tridentVelocity.getZ()) + (Math.PI * 2)) % (Math.PI * 2));
                    } else {
                        tridentYaw = targetYaw;
                    }

                    double yawDifference = Math.min(360 - Math.abs(tridentYaw - targetYaw), Math.abs(tridentYaw - targetYaw));

                    double frontalArc = SkillConfigManager.getUseSetting(hero, this, FRONTAL_ARC_NODE, DEFAULT_FRONTAL_ARC, false);
                    if (frontalArc < 10) {
                        frontalArc = 10;
                    } else if (frontalArc > 180) {
                        frontalArc = 180;
                    }

                    if (yawDifference >= (180 - (frontalArc / 2))) {
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
