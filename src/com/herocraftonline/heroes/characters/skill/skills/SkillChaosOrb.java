package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.CombustEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

public class SkillChaosOrb extends ActiveSkill {

    private Map<EnderPearl, Long> pearls = new LinkedHashMap<EnderPearl, Long>(100) {
        private static final long serialVersionUID = 4329526013158603250L;

        @Override
        protected boolean removeEldestEntry(Entry<EnderPearl, Long> eldest) {
            return (size() > 60 || eldest.getValue() + 5000 <= System.currentTimeMillis());
        }
    };

    public SkillChaosOrb(Heroes plugin) {
        super(plugin, "ChaosOrb");
        setDescription("You throw an orb of chaos that deals $1 damage and ignites the target. If you are able to use Ender Pearls, you will teleport to the orb when it makes contact with an object.");
        setUsage("/skill chaosorb");
        setArgumentRange(0, 0);
        setIdentifiers("skill chaosorb");
        setTypes(SkillType.ABILITY_PROPERTY_DARK, SkillType.SILENCEABLE, SkillType.DAMAGING, SkillType.AGGRESSIVE, SkillType.TELEPORTING);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 90, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.5, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT);

        String formattedDamage = Util.decFormat.format(damage);

        return getDescription().replace("$1", formattedDamage);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DAMAGE.node(), 120);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 1.625);
        node.set("velocity-multiplier", 0.7);
        node.set("ticks-before-drop", 5);
        node.set("y-value-drop", 0.35);
        node.set("fire-ticks", 50);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero);

        final EnderPearl pearl = player.launchProjectile(EnderPearl.class);
        pearl.setFireTicks(100);
        pearls.put(pearl, System.currentTimeMillis());

        double mult = SkillConfigManager.getUseSetting(hero, this, "velocity-multiplier", 0.4, false);
        Vector vel = pearl.getVelocity().normalize().multiply(mult);

        pearl.setVelocity(vel);
        pearl.setShooter(player);

        int ticksBeforeDrop = SkillConfigManager.getUseSetting(hero, this, "ticks-before-drop", 8, false);
        final double yValue = SkillConfigManager.getUseSetting(hero, this, "y-value-drop", 0.4, false);

        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            public void run() {
                if (!pearl.isDead()) {
                    pearl.setVelocity(pearl.getVelocity().setY(-yValue));
                }
            }
        }, ticksBeforeDrop);

        player.getWorld().playEffect(player.getLocation(), Effect.ENDER_SIGNAL, 3);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 0.5F, 1.0F);

        return SkillResult.NORMAL;
    }

    public class SkillEntityListener implements Listener {

        private final Skill skill;

        public SkillEntityListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler()
        public void onEntityDamage(EntityDamageEvent event) {
            if (event.isCancelled() || !(event instanceof EntityDamageByEntityEvent) || !(event.getEntity() instanceof LivingEntity)) {
                return;
            }

            EntityDamageByEntityEvent subEvent = (EntityDamageByEntityEvent) event;
            Entity projectile = subEvent.getDamager();
            if (!(projectile instanceof EnderPearl) || !pearls.containsKey(projectile)) {
                return;
            }

            pearls.remove(projectile);
            LivingEntity entity = (LivingEntity) subEvent.getEntity();
            ProjectileSource source = ((Projectile) subEvent.getDamager()).getShooter();
            if (!(source instanceof LivingEntity))
                return;
            Entity dmger = (LivingEntity) source;
            if (dmger instanceof Player) {
                Hero hero = plugin.getCharacterManager().getHero((Player) dmger);

                if (!damageCheck((Player) dmger, entity)) {
                    event.setCancelled(true);
                    return;
                }

                // Ignite the player
                entity.setFireTicks(SkillConfigManager.getUseSetting(hero, skill, "fire-ticks", 50, false));
                plugin.getCharacterManager().getCharacter(entity).addEffect(new CombustEffect(skill, (Player) dmger));

                // Damage the player
                double damage = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE, 90, false);
                double damageIncrease = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.5, false);
                damage += (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

                addSpellTarget(entity, hero);
                damageEntity(entity, hero.getPlayer(), damage, DamageCause.MAGIC);

                event.setCancelled(true);
            }
        }
    }
}
