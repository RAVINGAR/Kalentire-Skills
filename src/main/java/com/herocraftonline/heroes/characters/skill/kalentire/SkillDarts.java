package com.herocraftonline.heroes.characters.skill.kalentire;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Listenable;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class SkillDarts extends ActiveSkill implements Listenable {
    private final DartRunner runner;
    private final Listener listener;

    public SkillDarts(Heroes plugin) {
        super(plugin, "Darts");
        setDescription("Throw a dart dealing $1 damage at a range of $2 blocks. This dart will consume the last preparation" +
                "applying its effects for the remaining duration of the stack.");
        setUsage("/skill darts");
        setArgumentRange(0, 0);
        setIdentifiers("skill darts");
        setTypes(SkillType.DAMAGING, SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.ARMOR_PIERCING, SkillType.AGGRESSIVE);
        this.runner = new DartRunner();
        this.listener = new DartListener();

        this.runner.runTaskTimer(plugin, 20, 2);
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
        Location location = player.getEyeLocation();
        Vector direction = location.getDirection();

        Item item = location.getWorld().dropItemNaturally(location, new ItemStack(Material.OAK_BUTTON));
        ArmorStand dart = (ArmorStand) location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);
        dart.addPassenger(item);
        dart.setInvisible(true);
        dart.setInvulnerable(true);
        dart.setSmall(true);
        dart.setArms(true);
        dart.setAI(false);
        dart.getEquipment().setItemInMainHand(new ItemStack(Material.TIPPED_ARROW));
        dart.setRightArmPose(new EulerAngle(4.71239, 0, 0.785398));
        dart.setRotation(location.getYaw() + 90, location.getPitch());
        direction.multiply(distance / 5);
        Vector initial = dart.getVelocity();
        Vector velocity = new Vector(initial.getX() / 2.0D - direction.getX(),
                initial.getY(),
                initial.getZ() / 2.0D - direction.getZ());
        dart.setVelocity(velocity);

        SkillAssassinsGuile.AssassinEffect effect = (SkillAssassinsGuile.AssassinEffect) hero.getEffect(SkillAssassinsGuile.EFFECT_NAME);
        Effect lastEffect = effect == null ? null : effect.consumeLastEffect();
        DartThrow dartThrow = new DartThrow(hero, dart, item, damage, lastEffect, 2000);
        runner.throwList.put(item.getUniqueId(), dartThrow);

        return SkillResult.NORMAL;
    }

    @NotNull
    @Override
    public Listener getListener() {
        return listener;
    }


    private static class DartRunner extends BukkitRunnable {
        private final Map<UUID, DartThrow> throwList;

        public DartRunner() {
            throwList = new ConcurrentHashMap<>();
        }

        @Override
        public void run() {
            List<UUID> toBeRemoved = new ArrayList<>();
            throwList.forEach((uuid, dart) -> {
                if(dart.checkForRemoval()) {
                    Heroes.log(Level.WARNING, "DEBUG Removing dart!");
                    toBeRemoved.add(uuid);
                }
            });
            toBeRemoved.forEach(throwList::remove);
        }
    }

    private class DartListener implements Listener {
        @EventHandler
        public void onItemPickup(EntityPickupItemEvent event) {
            LivingEntity entity = event.getEntity();
            if(event.getEntity() instanceof Player) {
                Item item = event.getItem();
                DartThrow dart = runner.throwList.get(item.getUniqueId());
                if(dart == null) {
                    return;
                }
                item.remove();
                dart.dart.remove();
                if(dart.effect != null) {
                    CharacterTemplate character = plugin.getCharacterManager().getCharacter(entity);
                    character.addEffect(dart.effect);
                }
                addSpellTarget(entity, dart.thrower);
                SkillDarts.this.damageEntity(entity, dart.thrower.getEntity(), dart.damage, EntityDamageEvent.DamageCause.PROJECTILE, 0.0f);
            }
        }
    }

    private static class DartThrow {
        private final Hero thrower;
        private final Entity dart;
        private final Item indicator;
        private final double damage;
        private final Effect effect;

        private final long readyTime;

        public DartThrow(Hero thrower, Entity dart, Item indicator, double damage, @Nullable Effect effect, long expiry) {
            this.thrower = thrower;
            this.damage = damage;
            this.dart = dart;
            this.effect = effect;
            this.indicator = indicator;
            this.readyTime = System.currentTimeMillis() + expiry;
        }

        public boolean checkForRemoval() {
            if(dart.isOnGround()) {
                return true;
            }
            if(!indicator.isValid()) {
                return true;
            }
            return System.currentTimeMillis() > readyTime;
        }
    }

}
