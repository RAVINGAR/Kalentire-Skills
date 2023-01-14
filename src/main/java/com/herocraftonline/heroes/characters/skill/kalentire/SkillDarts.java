package com.herocraftonline.heroes.characters.skill.kalentire;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Passive;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SkillDarts extends ActiveSkill implements Passive {
    private final DartRunner runner;

    private final BukkitScheduler scheduler;

    private final static String META = "dart-armour-stand";

    private final Map<UUID, ArmorStand> mountedArmourStands;

    public SkillDarts(Heroes plugin) {
        super(plugin, "Darts");
        setDescription("Throw a dart dealing $1 damage at a range of $2 blocks. This dart will consume the last preparation" +
                "applying its effects for the remaining duration of the stack.");
        setUsage("/skill darts");
        setArgumentRange(0, 0);
        setIdentifiers("skill darts");
        setTypes(SkillType.DAMAGING, SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.ARMOR_PIERCING, SkillType.AGGRESSIVE);
        this.scheduler = plugin.getServer().getScheduler();
        this.mountedArmourStands = new ConcurrentHashMap<>();
        this.runner = new DartRunner();
        this.runner.runTaskTimer(plugin, 20, 2);

        plugin.getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onPluginDisable(PluginDisableEvent event) {
                if(plugin.getName().equalsIgnoreCase("Heroes")) {
                    mountedArmourStands.values().forEach(Entity::remove);
                }
            }
        }, plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 6);
        node.set(SkillSetting.MAX_DISTANCE_INCREASE_PER_DEXTERITY.node(), 0.5);
        node.set(SkillSetting.DAMAGE.node(), 12);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_STRENGTH.node(), 0.5);

        return node;
    }

    @Override
    public String getDescription(Hero hero) {
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 12, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 0.5, false);
        damage += (int) (damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH));

        double distance = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 6, false);
        double distanceIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_DEXTERITY, 0.5, false);
        distance += (int) (distanceIncrease * hero.getAttributeValue(AttributeType.DEXTERITY));

        return getDescription().replace("$1", "" + damage).replace("$2", "" + distance);
    }

    @Override
    public SkillResult use(Hero hero, String[] strings) {

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 12, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 0.5, false);
        damage += (int) (damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH));

        double distance = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 6, false);
        double distanceIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_DEXTERITY, 0.5, false);
        distance += (distanceIncrease * hero.getAttributeValue(AttributeType.DEXTERITY));

        Player player = hero.getPlayer();

        ArmorStand dart = getArmourStand(player);

        Location eyeLocation = player.getEyeLocation();

        throwArmourStand(player, eyeLocation, dart, distance);

        scheduler.scheduleSyncDelayedTask(plugin, () -> getArmourStand(player));

        player.getWorld().playSound(eyeLocation, Sound.ITEM_ARMOR_EQUIP_LEATHER, SoundCategory.PLAYERS, 1.0F, 1.1f);
        player.getWorld().playSound(eyeLocation, Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 0.9F, 0.8f);

        SkillAssassinsGuile.AssassinEffect effect = (SkillAssassinsGuile.AssassinEffect) hero.getEffect(SkillAssassinsGuile.EFFECT_NAME);
        Effect lastEffect = effect == null ? null : effect.consumeLastEffect();
        DartThrow dartThrow = new DartThrow(hero, dart, damage, lastEffect, 5000);
        runner.throwList.put(dart.getUniqueId(), dartThrow);

        return SkillResult.NORMAL;
    }

    @NotNull
    public ArmorStand getArmourStand(Player player) {
        UUID uuid = player.getUniqueId();
        ArmorStand dart = mountedArmourStands.get(uuid);
        if(dart == null) {
            Location eyeLocation = player.getEyeLocation();
            dart = (ArmorStand) eyeLocation.getWorld().spawnEntity(eyeLocation.subtract(0, 5, 0), EntityType.ARMOR_STAND);
            dart.setInvisible(true);
            dart.setMarker(true);
            for(EquipmentSlot slot : EquipmentSlot.values()) {
                dart.addEquipmentLock(slot, ArmorStand.LockType.ADDING_OR_CHANGING);
                dart.addEquipmentLock(slot, ArmorStand.LockType.REMOVING_OR_CHANGING);
            }
            dart.setMetadata(META, new FixedMetadataValue(plugin, player.getUniqueId()));
            dart.setRightArmPose(new EulerAngle(4.71239, 0, 0.785398));
            dart.setInvulnerable(true);
            dart.setArms(true);
            dart.setAI(false);

            player.addPassenger(dart);
            mountedArmourStands.put(uuid, dart);
        }
        return dart;
    }

    public void throwArmourStand(Player player, Location eyeLocation, ArmorStand dart, double distance) {
        mountedArmourStands.remove(player.getUniqueId());
        player.removePassenger(dart);
        dart.setMarker(false);
        dart.teleport(eyeLocation.clone().subtract(0, 2, 0));
        dart.setRotation(eyeLocation.getYaw() + 90, eyeLocation.getPitch());
        dart.getEquipment().setItemInMainHand(new ItemStack(Material.TIPPED_ARROW));
        Vector direction = eyeLocation.getDirection();
        direction.multiply(distance / -5);
        Vector initialVelocity = dart.getVelocity();

        // TODO these velocity calculations are wrong! The dart should always be thrown in the direction the player is looking!
        Vector velocity = new Vector(initialVelocity.getX() / 2.0D - direction.getX(),
                initialVelocity.getY() / -1.0D,
                initialVelocity.getZ() / 2.0D -direction.getZ());
        dart.setVelocity(velocity);
    }

    @Override
    public void tryApplying(Hero hero) {
        if(hero.canUseSkill(this)) {
            apply(hero);
        }
        else {
            unapply(hero);
        }
    }

    @Override
    public void apply(Hero hero) {
        Player player = hero.getPlayer();
        getArmourStand(player);
    }

    @Override
    public void unapply(Hero hero) {
        ArmorStand stand = mountedArmourStands.remove(hero.getPlayer().getUniqueId());
        if(stand != null) {
            stand.remove();
        }
    }


    private static class DartRunner extends BukkitRunnable {
        private final Map<UUID, DartThrow> throwList;

        public DartRunner() {
            throwList = new ConcurrentHashMap<>();
        }

        @Override
        public void run() {
            List<DartThrow> toBeRemoved = new ArrayList<>();
            throwList.values().forEach((dart) -> {
                if(dart.tick()) {
                    toBeRemoved.add(dart);
                }
            });
            toBeRemoved.forEach(dart -> {
                throwList.remove(dart.dart.getUniqueId());
                dart.dart.remove();
            });
        }
    }

    private class DartThrow {
        private final Hero thrower;
        private final Entity dart;
        private final double damage;
        private final Effect effect;

        private final long readyTime;

        public DartThrow(Hero thrower, Entity dart, double damage, @Nullable Effect effect, long expiry) {
            this.thrower = thrower;
            this.damage = damage;
            this.dart = dart;
            this.effect = effect;
            this.readyTime = System.currentTimeMillis() + expiry;
        }

        public boolean tick() {
            Location location = dart.getLocation();
            location.getWorld().spawnParticle(Particle.CRIT_MAGIC, location, 3, 0.05, 0.05, 0.05, 0);
            if(dart.isOnGround()) {
                return true;
            }
            if(System.currentTimeMillis() > readyTime) {
                return true;
            }
            List<Entity> entities = dart.getNearbyEntities(0.5, 0.5, 0.5);
            for(Entity entity : entities) {
                if(!entity.getUniqueId().equals(thrower.getEntity().getUniqueId())) {
                    if(entity instanceof LivingEntity) {
                        doDamage((LivingEntity) entity);
                        return true;
                    }
                }
            }
            return false;
        }

        public void doDamage(LivingEntity target) {
            scheduler.scheduleSyncDelayedTask(plugin, () -> {
                if(damageCheck(thrower.getPlayer(), target)) {
                    if(effect != null) {
                        CharacterTemplate character = plugin.getCharacterManager().getCharacter(target);
                        character.addEffect(effect);
                    }
                    addSpellTarget(target, thrower);
                    SkillDarts.this.damageEntity(target, thrower.getEntity(), damage, EntityDamageEvent.DamageCause.PROJECTILE, 0.0f);
                }
            });
        }
    }

}
