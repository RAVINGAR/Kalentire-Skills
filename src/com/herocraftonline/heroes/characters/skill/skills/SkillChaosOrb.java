package com.herocraftonline.heroes.characters.skill.skills;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.CombustEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;

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
        setDescription("You throw an orb of chaos that deals $1 damage and ignites the target.");
        setUsage("/skill chaosorb");
        setArgumentRange(0, 0);
        setIdentifiers("skill chaosorb");
        setTypes(SkillType.ABILITY_PROPERTY_DARK, SkillType.SILENCABLE, SkillType.DAMAGING, SkillType.AGGRESSIVE, SkillType.TELEPORTING);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, Integer.valueOf(90), false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.5, false);
        damage += (int) (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        return getDescription().replace("$1", damage + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DAMAGE.node(), Integer.valueOf(90));
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), Double.valueOf(1.5));
        node.set("velocity-multiplier", 0.4);
        node.set("fire-ticks", 50);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero);

        EnderPearl pearl = player.launchProjectile(EnderPearl.class);
        pearl.setFireTicks(100);
        pearls.put(pearl, System.currentTimeMillis());

        double mult = SkillConfigManager.getUseSetting(hero, this, "velocity-multiplier", 0.4, false);
        pearl.setVelocity(pearl.getVelocity().multiply(mult));
        pearl.setShooter(player);

        player.getWorld().playEffect(player.getLocation(), Effect.ENDER_SIGNAL, 3);
        player.getWorld().playSound(player.getLocation(), Sound.SHOOT_ARROW, 0.5F, 1.0F);

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
            Entity dmger = ((EnderPearl) projectile).getShooter();

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
                damageEntity(entity, hero.getPlayer(), damage, EntityDamageEvent.DamageCause.MAGIC);

                event.setCancelled(true);
            }
        }
    }
}
