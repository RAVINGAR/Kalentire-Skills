package com.herocraftonline.heroes.characters.skill.reborn.hellspawn;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.BurningEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

public class SkillChaosOrb extends ActiveSkill {

    private Map<EnderPearl, Long> orbs = new LinkedHashMap<EnderPearl, Long>(100) {
        private static final long serialVersionUID = 4329526013158603250L;

        @Override
        protected boolean removeEldestEntry(Entry<EnderPearl, Long> eldest) {
            return (size() > 60 || eldest.getValue() + 5000 <= System.currentTimeMillis());
        }
    };

    public SkillChaosOrb(Heroes plugin) {
        super(plugin, "ChaosOrb");
        setDescription("You throw an orb of chaos that deals $1 damage and ignites the target, dealing $2 burning damage over the next $3 second(s). "
                + "If you are able to use Ender Pearls, you will teleport to the orb when it lands.");
        setUsage("/skill chaosorb");
        setArgumentRange(0, 0);
        setIdentifiers("skill chaosorb");
        setTypes(SkillType.ABILITY_PROPERTY_FIRE, SkillType.ABILITY_PROPERTY_DARK, SkillType.ABILITY_PROPERTY_PROJECTILE,
                SkillType.ABILITY_PROPERTY_ENDER, SkillType.SILENCEABLE, SkillType.DAMAGING, SkillType.AGGRESSIVE);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE, false);

        int burnDuration = SkillConfigManager.getUseSetting(hero, this, "burn-duration", 2000, false);
        double burnMultipliaer = SkillConfigManager.getUseSetting(hero, this, "burn-damage-multiplier", 2.0, false);
        double totalBurnDamage = plugin.getDamageManager().calculateFireTickDamage((int) (burnDuration / 50), burnMultipliaer);

        String formattedDamage = Util.decFormat.format(damage);
        String formattedBurnDamage = Util.decFormat.format(totalBurnDamage);
        String formattedBurnDuration = Util.decFormat.format(burnDuration / 1000);
        return getDescription().replace("$1", formattedDamage).replace("$2", formattedBurnDamage).replace("$3", formattedBurnDuration);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.DAMAGE.node(), 120.0);
        config.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 0.0);
        config.set("burn-duration", 2000);
        config.set("burn-damage-multiplier", 1.5);
        config.set("velocity-multiplier", 0.75);
        config.set("ticks-before-drop", 5);
        config.set("y-value-drop", 0.35);
        return config;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        final EnderPearl orb = player.launchProjectile(EnderPearl.class);
        orb.setFireTicks(100);
        orbs.put(orb, System.currentTimeMillis());

        double mult = SkillConfigManager.getUseSetting(hero, this, "velocity-multiplier", 0.4, false);
        Vector vel = orb.getVelocity().normalize().multiply(mult);

        orb.setVelocity(vel);
        orb.setShooter(player);

        int ticksBeforeDrop = SkillConfigManager.getUseSetting(hero, this, "ticks-before-drop", 8, false);
        final double yValue = SkillConfigManager.getUseSetting(hero, this, "y-value-drop", 0.4, false);

        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            public void run() {
                if (!orb.isDead()) {
                    orb.setVelocity(orb.getVelocity().setY(-yValue));
                }
            }
        }, ticksBeforeDrop);

        player.getWorld().playEffect(player.getLocation(), Effect.ENDER_SIGNAL, 3);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 0.5F, 1.0F);

        broadcastExecuteText(hero);

        return SkillResult.NORMAL;
    }

    public class SkillEntityListener implements Listener {
        private final Skill skill;

        public SkillEntityListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
        public void onEntityDamage(EntityDamageByEntityEvent event) {
            Entity projectile = event.getDamager();
            if (!(projectile instanceof EnderPearl) || !orbs.containsKey(projectile))
                return;

            orbs.remove(projectile);
            event.setCancelled(true);

            if (!(event.getEntity() instanceof LivingEntity))
                return;

            LivingEntity targetLE = (LivingEntity) event.getEntity();
            ProjectileSource dmgSource = ((Projectile) event.getDamager()).getShooter();
            if (!(dmgSource instanceof Player))
                return;

            Player player = (Player) dmgSource;
            Hero hero = plugin.getCharacterManager().getHero(player);

            if (!damageCheck(player, targetLE))
                return;

            int burnDuration = SkillConfigManager.getUseSetting(hero, skill, "burn-duration", 2000, false);
            double burnMultipliaer = SkillConfigManager.getUseSetting(hero, skill, "burn-damage-multiplier", 1.5, false);
            double damage = SkillConfigManager.getScaledUseSettingDouble(hero, skill, SkillSetting.DAMAGE, false);

            addSpellTarget(targetLE, hero);
            damageEntity(targetLE, hero.getPlayer(), damage, DamageCause.MAGIC);

            CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(targetLE);
            targetCT.addEffect(new BurningEffect(skill, player, burnDuration, burnMultipliaer));
        }
    }
}
