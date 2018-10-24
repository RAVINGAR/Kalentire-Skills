package com.herocraftonline.heroes.characters.skill.codari;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.effects.standard.StandardBleedEffect;
import com.herocraftonline.heroes.characters.effects.standard.StandardSlowEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.nms.physics.NMSPhysics;
import com.herocraftonline.heroes.nms.physics.collision.Sphere;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public class SkillPair extends ActiveSkill implements Listener {

    private static final String RIPOSTE_RECAST_NAME = "Riposte";

    private static final String DAMAGE_IMMUNITY_DURATION_NODE = "damage-immunity-duration";
    private static final int DEFAULT_DAMAGE_IMMUNITY_DURATION = 1000;

    private static final String RIPOSTE_RADIUS_NODE = "riposte-radius";
    private static final double DEFAULT_RIPOSTE_RADIUS = 3;

    private static final String RIPOSTE_DAMAGE_NODE = "riposte-damage";
    private static final double DEFAULT_RIPOSTE_DAMAGE = 10;

    private static final String RIPOSTE_BLEED_STACK_AMOUNT_NODE = "riposte-bleed-stack-amount";
    private static final int DEFAULT_RIPOSTE_BLEED_STACK_AMOUNT = 0;

    private static final String RIPOSTE_BLEED_STACK_DURATION_NODE = "riposte-bleed-stack-duration";
    private static final int DEFAULT_RIPOSTE_BLEED_STACK_DURATION = 3000;

    private static final String RIPOSTE_SLOW_STRENGTH_NODE = "riposte-slow-strength";
    private static final int DEFAULT_RIPOSTE_SLOW_STRENGTH = 0;

    private static final String RIPOSTE_SLOW_DURATION_NODE = "riposte-slow-duration";
    private static final int DEFAULT_RIPOSTE_SLOW_DURATION = 3000;

    private static final String COOLDOWN_REDUCTION_PERCENTAGE_ON_RIPOSTE_HIT_NODE = "cooldown-reduction-percentage-on-riposte-hit";
    private static final double DEFAULT_COOLDOWN_REDUCTION_PERCENTAGE_ON_RIPOSTE_HIT = 0.8;

    private final Set<UUID> damageBlockedSet = new HashSet<>();
    private final Set<UUID> riposteHit = new HashSet<>();

    public SkillPair(Heroes plugin) {
        super(plugin, "Pair");
        setDescription("Stuff");

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

        node.set(DAMAGE_IMMUNITY_DURATION_NODE, DEFAULT_DAMAGE_IMMUNITY_DURATION);
        node.set(RIPOSTE_RADIUS_NODE, DEFAULT_RIPOSTE_RADIUS);
        node.set(RIPOSTE_DAMAGE_NODE, DEFAULT_RIPOSTE_DAMAGE);
        node.set(RIPOSTE_BLEED_STACK_AMOUNT_NODE, DEFAULT_RIPOSTE_BLEED_STACK_AMOUNT);
        node.set(RIPOSTE_BLEED_STACK_DURATION_NODE, DEFAULT_RIPOSTE_BLEED_STACK_DURATION);
        node.set(RIPOSTE_SLOW_STRENGTH_NODE, DEFAULT_RIPOSTE_SLOW_STRENGTH);
        node.set(RIPOSTE_SLOW_DURATION_NODE, DEFAULT_RIPOSTE_SLOW_DURATION);
        node.set(COOLDOWN_REDUCTION_PERCENTAGE_ON_RIPOSTE_HIT_NODE, DEFAULT_COOLDOWN_REDUCTION_PERCENTAGE_ON_RIPOSTE_HIT);

        return node;
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

    @Override
    public SkillResult use(Hero hero, String[] strings) {

        int damageImmunityDuration = SkillConfigManager.getUseSetting(hero, this, DAMAGE_IMMUNITY_DURATION_NODE, DEFAULT_DAMAGE_IMMUNITY_DURATION, false);
        if (damageImmunityDuration < 100) {
            damageImmunityDuration = 100;
        }

        startRecast(hero, damageImmunityDuration, new RecastData(RIPOSTE_RECAST_NAME));
        broadcastExecuteText(hero);

        return SkillResult.NORMAL;
    }

    @Override
    protected void recast(Hero hero, RecastData data) {

        Player player = hero.getPlayer();
        UUID playerId = player.getUniqueId();

        if (damageBlockedSet.contains(playerId)) {

            World world = player.getWorld();

            double riposteRadius = SkillConfigManager.getUseSetting(hero, this, RIPOSTE_RADIUS_NODE, DEFAULT_RIPOSTE_RADIUS, false);
            if (riposteRadius < 1) {
                riposteRadius = 1;
            }

            Vector riposteCenter = NMSPhysics.instance().getEntityAABB(player).getCenter();
            Sphere riposteEffectArea = NMSPhysics.instance().createSphere(riposteCenter, riposteRadius);

            world.playSound(riposteCenter.toLocation(world), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1, 1);

            List<Entity> targets = NMSPhysics.instance().getEntitiesInVolume(player.getWorld(), player, riposteEffectArea,
                    entity -> entity instanceof LivingEntity && damageCheck(player, (LivingEntity) entity));

            if (!targets.isEmpty()) {

                double riposteDamage = SkillConfigManager.getUseSetting(hero, this, RIPOSTE_DAMAGE_NODE, DEFAULT_RIPOSTE_DAMAGE, false);

                int riposteBleedStackAmount = SkillConfigManager.getUseSetting(hero, this, RIPOSTE_BLEED_STACK_AMOUNT_NODE, DEFAULT_RIPOSTE_BLEED_STACK_AMOUNT, false);
                int riposteBleedStackDuration = SkillConfigManager.getUseSetting(hero, this, RIPOSTE_BLEED_STACK_DURATION_NODE, DEFAULT_RIPOSTE_BLEED_STACK_DURATION, false);

                int riposteSlowStrength = SkillConfigManager.getUseSetting(hero, this, RIPOSTE_SLOW_STRENGTH_NODE, DEFAULT_RIPOSTE_SLOW_STRENGTH, false);
                int riposteSlowDuration = SkillConfigManager.getUseSetting(hero, this, RIPOSTE_SLOW_DURATION_NODE, DEFAULT_RIPOSTE_SLOW_DURATION, false);

                for (Entity entity : targets) {

                    LivingEntity target = (LivingEntity) entity;
                    CharacterTemplate targetCharacter = plugin.getCharacterManager().getCharacter(target);

                    if (riposteDamage > 0) {
                        addSpellTarget(entity, hero);
                        damageEntity(target, player, riposteDamage);
                    }

                    if (riposteBleedStackAmount > 0) {
                        StandardBleedEffect.applyStacks(targetCharacter, this, player, riposteBleedStackDuration, riposteBleedStackAmount);
                    }

                    if (riposteSlowStrength > 0) {
                        StandardSlowEffect.addDuration(targetCharacter, this, player, riposteSlowDuration, riposteSlowStrength);
                    }
                }

                riposteHit.add(playerId);
            }
        }

        endRecast(hero);
    }

    @Override
    protected int alterAppliedCooldown(Hero hero, int cooldown) {
        if (riposteHit.remove(hero.getPlayer().getUniqueId())) {
            double cooldownReductionPercentage = SkillConfigManager.getUseSetting(hero, this,
                    COOLDOWN_REDUCTION_PERCENTAGE_ON_RIPOSTE_HIT_NODE, DEFAULT_COOLDOWN_REDUCTION_PERCENTAGE_ON_RIPOSTE_HIT, false);
            return cooldown - (int)(cooldown * cooldownReductionPercentage);
        } else {
            return cooldown;
        }
    }

    @Override
    protected void onRecastEnd(Hero hero, RecastData data) {
        damageBlockedSet.remove(hero.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private void onDamage(EntityDamageByEntityEvent e) {

        if (e.getEntity() instanceof Player) {

            Player player = (Player) e.getEntity();
            Hero hero = plugin.getCharacterManager().getHero(player);

            if (isRecasting(hero, RIPOSTE_RECAST_NAME)) {
                processDamageBlock(hero);
                e.setCancelled(true);
            }
        }
    }

    private void processDamageBlock(Hero hero) {
        damageBlockedSet.add(hero.getPlayer().getUniqueId());
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getEyeLocation(), Sound.ITEM_SHIELD_BLOCK, 1, 5f);
    }
}
