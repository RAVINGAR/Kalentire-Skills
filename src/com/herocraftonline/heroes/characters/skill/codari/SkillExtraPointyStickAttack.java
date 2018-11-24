package com.herocraftonline.heroes.characters.skill.codari;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.nms.physics.FluidCollision;
import com.herocraftonline.heroes.nms.physics.NMSPhysics;
import com.herocraftonline.heroes.nms.physics.RayCastHit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.NumberConversions;
import org.bukkit.util.Vector;

import java.util.EnumSet;

public class SkillExtraPointyStickAttack extends PassiveSkill implements Listener {

    public static final String NAME = "ExtraPointyStickAttack";

    private static final double ATTACK_RANGE = 6;

    // TODO Find a unified place for this for multipul skills
    private static final EnumSet<Material> shovels = EnumSet.of(Material.WOODEN_SHOVEL, Material.STONE_SHOVEL, Material.IRON_SHOVEL, Material.GOLDEN_SHOVEL, Material.DIAMOND_SHOVEL);

    private static final String PERCENT_WEAPON_DAMAGE_NEAR_NODE = "percent-weapon-damage-near";
    private static final double DEFAULT_PERCENT_WEAPON_DAMAGE_NEAR = 0.25;

    private static final String PERCENT_WEAPON_DAMAGE_MID_NODE = "percent-weapon-damage-mid";
    private static final double DEFAULT_PERCENT_WEAPON_DAMAGE_MID = 1.0;

    private static final String PERCENT_WEAPON_DAMAGE_FAR_NODE = "percent-weapon-damage-far";
    private static final double DEFAULT_PERCENT_WEAPON_DAMAGE_FAR = 1.25;

    private HitRange hitRange;
    private Double appliedDamageMultiplier = null;

    public SkillExtraPointyStickAttack(Heroes plugin) {
        super(plugin, NAME);
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
        node.set(PERCENT_WEAPON_DAMAGE_MID_NODE, DEFAULT_PERCENT_WEAPON_DAMAGE_MID);
        node.set(PERCENT_WEAPON_DAMAGE_FAR_NODE, DEFAULT_PERCENT_WEAPON_DAMAGE_FAR);

        return node;
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onPlayerInteract(PlayerInteractEvent e) {

        if (e.useItemInHand() != Event.Result.DENY && (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK)) {

            Player player = e.getPlayer();
            Hero hero = plugin.getCharacterManager().getHero(player);

            ItemStack weapon = player.getInventory().getItemInMainHand();

            if (weapon != null) {

                Material weaponType = weapon.getType();

                if (shovels.contains(weaponType) && hasPassive(hero)) {

                    // Sorry, but if you have this skill I got to do this (no grass paths for you)
                    //TODO If ever a profession wants to provide grass paths, a toggle will need to be made to either allow this attack, or allow grass paths.
                    e.setUseInteractedBlock(Event.Result.DENY);

                    if (!hero.hasBind(weaponType)) {
                        processAttack(hero);
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onPlayerInteractEntity(PlayerInteractEntityEvent e) {

        Player player = e.getPlayer();
        Hero hero = plugin.getCharacterManager().getHero(player);

        ItemStack weapon = player.getInventory().getItemInMainHand();

        if (weapon != null) {

            Material weaponType = weapon.getType();

            if (shovels.contains(weaponType) && hasPassive(hero) && !hero.hasBind(weaponType)) {
                // The entity types here prevent `PlayerInteractEvent` from occurring, so I must do it here.
                // TODO If ever a profession needs to right click these entities with a shovel (for whatever reason), a toggle... (see above)
                switch (e.getRightClicked().getType()) {
                    // The following seem to always block `PlayerInteractEvent`
                    case LLAMA:
                    case HORSE: {
                        e.setCancelled(true);
                        processAttack(hero);
                    }
                    case WOLF: {// Tamed wolves seem to block `PlayerInteractEvent`
                        //TODO see if this true for all tamable stuff
                        if (((Wolf) e.getRightClicked()).isTamed()) {
                            e.setCancelled(true);
                            processAttack(hero);
                        }
                    }
                }
            }
        }
    }

    private void processAttack(Hero hero) {

        Player player = hero.getPlayer();
        World world = player.getWorld();
        Location eyeLocation = player.getEyeLocation();

        Vector start = eyeLocation.toVector();
        Vector end = eyeLocation.getDirection().multiply(ATTACK_RANGE).add(start);

        RayCastHit hit = NMSPhysics.instance().rayCast(world, player, start, end, null, entity -> entity instanceof LivingEntity, FluidCollision.NEVER, true);
        if (hit != null && hit.isEntity()) {

            LivingEntity target = (LivingEntity) hit.getEntity();

            if (damageCheck(player, target)) {

                Vector hitPoint = hit.getPoint();
                double hitDistanceSquared = start.distanceSquared(hitPoint);
                double percentWeaponDamage;
                boolean knockback;

                if (hitDistanceSquared <= NumberConversions.square(2)) {
                    percentWeaponDamage = SkillConfigManager.getUseSetting(hero, this, PERCENT_WEAPON_DAMAGE_NEAR_NODE, DEFAULT_PERCENT_WEAPON_DAMAGE_NEAR, false);
                    hitRange = HitRange.NEAR;
                    knockback = true;
                } else if (hitDistanceSquared > NumberConversions.square(4)) {
                    percentWeaponDamage = SkillConfigManager.getUseSetting(hero, this, PERCENT_WEAPON_DAMAGE_FAR_NODE, DEFAULT_PERCENT_WEAPON_DAMAGE_FAR, false);
                    hitRange = HitRange.FAR;
                    knockback = false;
                } else {
                    percentWeaponDamage = SkillConfigManager.getUseSetting(hero, this, PERCENT_WEAPON_DAMAGE_MID_NODE, DEFAULT_PERCENT_WEAPON_DAMAGE_MID, false);
                    hitRange = HitRange.MID;
                    knockback = false;
                }

                if (percentWeaponDamage < 0) {
                    percentWeaponDamage = 0;
                }
                appliedDamageMultiplier = percentWeaponDamage;

                // Heroes is going to overwrite the damage as long as it is not 0 so doesn't matter what I set.
                // We will apply `percentWeaponDamage` later within the `WeaponDamageEvent` below.
                damageEntity(target, player, 1.0, knockback);
                hitRange = HitRange.NO_HIT;

                target.setNoDamageTicks(target.getMaximumNoDamageTicks());
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private void onWeaponDamage(WeaponDamageEvent e) {
        if (appliedDamageMultiplier != null) {
            e.setDamage(e.getDamage() * appliedDamageMultiplier);
            appliedDamageMultiplier = null;
        }
    }

    public enum HitRange {
        NO_HIT,
        NEAR,
        MID,
        FAR
    }
}
