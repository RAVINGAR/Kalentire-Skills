package com.herocraftonline.heroes.characters.skill.codari;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.nms.physics.FluidCollision;
import com.herocraftonline.heroes.nms.physics.NMSPhysics;
import com.herocraftonline.heroes.nms.physics.RayCastHit;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.NumberConversions;
import org.bukkit.util.Vector;

import java.util.EnumSet;

public class SkillExtraPointyStickAttack extends PassiveSkill implements Listener {

    private static final double ATTACK_RANGE = 6;
    private static final EnumSet<Material> shovels = EnumSet.of(Material.WOODEN_SHOVEL, Material.STONE_SHOVEL, Material.IRON_SHOVEL, Material.GOLDEN_SHOVEL, Material.DIAMOND_SHOVEL);

    private static final String PERCENT_WEAPON_DAMAGE_NEAR_NODE = "percent-weapon-damage-near";
    private static final double DEFAULT_PERCENT_WEAPON_DAMAGE_NEAR = 0.8;

    private static final String PERCENT_WEAPON_DAMAGE_FAR_NODE = "percent-weapon-damage-far";
    private static final double DEFAULT_PERCENT_WEAPON_DAMAGE_FAR = 1.2;

    private HitRange hitRange;
    private Double appliedDamageMultiplier = null;

    public SkillExtraPointyStickAttack(Heroes plugin) {
        super(plugin, "ExtraPointyStickAttack");
        setDescription("Adds a longer ranged attack with the trident right click, and replaces trident throw");

        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL);
    }

    @Override
    public void init() {
        super.init();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public HitRange getHitRange() {
        return hitRange;
    }

    @Override
    public ConfigurationSection getDefaultConfig() {

        ConfigurationSection node = super.getDefaultConfig();

        node.set(PERCENT_WEAPON_DAMAGE_NEAR_NODE, DEFAULT_PERCENT_WEAPON_DAMAGE_NEAR);
        node.set(PERCENT_WEAPON_DAMAGE_FAR_NODE, DEFAULT_PERCENT_WEAPON_DAMAGE_FAR);

        return node;
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onPlayerInteract(PlayerInteractEvent e) {

        if (e.useItemInHand() != Event.Result.DENY && (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK)) {

            Player player = e.getPlayer();
            Hero hero = plugin.getCharacterManager().getHero(player);

            ItemStack weapon = player.getInventory().getItemInMainHand();

            if (weapon != null) {

                Material weaponType = weapon.getType();

                if (shovels.contains(weaponType)) {

                    if (!player.isSneaking()) {
                        e.setUseInteractedBlock(Event.Result.DENY);
                    }

                    if (!hero.hasBind(weaponType)) {

                        World world = player.getWorld();
                        Location eyeLocation = player.getEyeLocation();

                        Vector start = eyeLocation.toVector();
                        Vector end = eyeLocation.getDirection().multiply(ATTACK_RANGE).add(start);

                        RayCastHit hit = NMSPhysics.instance().rayCast(world, player, start, end, null, entity -> entity instanceof LivingEntity, FluidCollision.NEVER, true);
                        if (hit != null && hit.isEntity()) {

                            LivingEntity target = (LivingEntity) hit.getEntity();

                            if (target.getNoDamageTicks() <= 10 && damageCheck(player, target)) {

                                Vector hitPoint = hit.getPoint();
                                double hitDistanceSquared = start.distanceSquared(hitPoint);

                                if (hitDistanceSquared <= NumberConversions.square(2)) {
                                    // The distance is within the `near` range for less damage
                                    double percentWeaponDamageNear = SkillConfigManager.getUseSetting(hero, this, PERCENT_WEAPON_DAMAGE_NEAR_NODE, DEFAULT_PERCENT_WEAPON_DAMAGE_NEAR, false);
                                    if (percentWeaponDamageNear < 0) {
                                        percentWeaponDamageNear = 0;
                                    }
                                    hitRange = HitRange.NEAR;
                                    appliedDamageMultiplier = percentWeaponDamageNear;
                                } else if (hitDistanceSquared > NumberConversions.square(4)) {
                                    // The distance is within the `far` range for more damage
                                    double percentWeaponDamageFar = SkillConfigManager.getUseSetting(hero, this, PERCENT_WEAPON_DAMAGE_FAR_NODE, DEFAULT_PERCENT_WEAPON_DAMAGE_FAR, false);
                                    if (percentWeaponDamageFar < 0) {
                                        percentWeaponDamageFar = 0;
                                    }
                                    hitRange = HitRange.FAR;
                                    appliedDamageMultiplier = percentWeaponDamageFar;
                                } else {
                                    hitRange = HitRange.MIDDLE;
                                }

                                // Heroes is going to overwrite the damage as long as it is not 0 so doesn't matter what I set.
                                // We will override it later within `WeaponDamageEvent` if `appliedDamageMultiplier` gets set.
                                damageEntity(target, player, 1.0);
                                hitRange = HitRange.NONE;

                                target.setNoDamageTicks(target.getMaximumNoDamageTicks());
                            }
                        }
                    }
                }
            }

            player.sendMessage("INTERACT: " + e.getHand());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private void onWeaponDamage(WeaponDamageEvent e) {
        if (appliedDamageMultiplier != null) {
            e.setDamage(e.getDamage() * appliedDamageMultiplier);
            appliedDamageMultiplier = null;
            Bukkit.broadcastMessage("DAMAGE: " + e.getDamage());
        }
    }

    enum HitRange {
        NONE,
        NEAR,
        MIDDLE,
        FAR;
    }
}
