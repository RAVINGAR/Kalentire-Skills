package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.*;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.SmallFireball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;

import static com.herocraftonline.heroes.util.EntityUtil.ghost;

public class SkillJolt extends ActiveSkill 
{
	private HashMap<SmallFireball, Player> fireballs = new HashMap<SmallFireball, Player>();
    
    public SkillJolt(Heroes plugin) 
    {
        super(plugin, "Jolt");
        setDescription("You lash out with a bolt of energy, striking your target for $1 damage.");
        setUsage("/skill jolt");
        setArgumentRange(0, 0);
        setIdentifiers("skill jolt");
        setTypes(SkillType.ABILITY_PROPERTY_LIGHTNING, SkillType.SILENCEABLE, SkillType.DAMAGING);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(this), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DAMAGE.node(), 40);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 0.5);
        node.set("velocity-multiplier", 2);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) 
    {
        Player player = hero.getPlayer();
        SmallFireball fireball = player.launchProjectile(SmallFireball.class);
        fireball.setIsIncendiary(false);
        fireball.setFireTicks(0);
        fireballs.put(fireball, player);
        double mult = SkillConfigManager.getUseSetting(hero, this, "velocity-multiplier", 2, false);
        final Vector velocity = player.getLocation().getDirection().normalize().multiply(mult);
        fireball.setVelocity(velocity);
        fireball.setShooter(player);
        fireball.setYield(0.0F);
        
		final SmallFireball f = fireball;
		ghost(f);

		new BukkitRunnable() // velocity check, 8 times
		{
			private int effectTicks = 0;

			public void run()
			{
				if (f.isDead()) {
                    cancel();
                    return;
                }

				if (effectTicks < 8)
				{
					f.setVelocity(velocity);
					effectTicks++;
				}
				else
				{
					f.remove();
                    fireballs.remove(f);
					f.getWorld().spigot().playEffect(f.getLocation(), Effect.LARGE_SMOKE, 0, 0, 0.4F, 0.4F, 0.4F, 0.0F, 25, 32);
					f.getWorld().spigot().playEffect(f.getLocation(), Effect.EXTINGUISH, 0, 0, 0.4F, 0.4F, 0.4F, 0.0F, 15, 32);
					cancel();
				}
			}
		}.runTaskTimer(plugin, 0, 5);
		
		new BukkitRunnable()
		{
			public void run()
			{
				if (f.isDead()) cancel();
				f.getWorld().spigot().playEffect(f.getLocation(), Effect.FIREWORKS_SPARK, 0, 0, 0.1F, 0.1F, 0.1F, 0.0F, 25, 32);
			}
		}.runTaskTimer(plugin, 0, 1);
		
		player.getWorld().spigot().playEffect(player.getLocation().add(0, 0.5, 0), Effect.EXPLOSION, 0, 0, 0.5F, 0.2F, 0.5F, 0.6F, 50, 16);
		player.getWorld().playSound(player.getLocation(), Sound.ENTITY_TNT_PRIMED, 2.0F, 0.7F);
		player.getWorld().spigot().playEffect(player.getLocation(), Effect.FIREWORKS_SPARK, 0, 0, 0.5F, 0.2F, 0.5F, 0.6F, 25, 16);
		
        broadcastExecuteText(hero); 
        return SkillResult.NORMAL;
    }

    public class SkillEntityListener implements Listener 
    {

        private final Skill skill;

        public SkillEntityListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(ignoreCancelled = true)
        public void onEntityDamage(EntityDamageEvent event) {
            if (!(event instanceof EntityDamageByEntityEvent) || !(event.getEntity() instanceof LivingEntity)) {
                return;
            }

            EntityDamageByEntityEvent subEvent = (EntityDamageByEntityEvent) event;
            Entity projectile = subEvent.getDamager();
            if (!(projectile instanceof SmallFireball) || !fireballs.containsKey(projectile)) {
                return;
            }
            fireballs.remove(projectile);
            LivingEntity entity = (LivingEntity) subEvent.getEntity();
            Entity dmger = (Entity) ((SmallFireball) projectile).getShooter();
            if (dmger instanceof Player) 
            {
                Hero hero = plugin.getCharacterManager().getHero((Player) dmger);

                if (!damageCheck((Player) dmger, entity)) {
                    event.setCancelled(true);
                    return;
                }

    			entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0F, 1.3F);
    			entity.getWorld().spigot().playEffect(entity.getEyeLocation(), Effect.EXPLOSION, 0, 0, 0.3F, 0.3F, 0.3F, 0.3F, 50, 16);
    			entity.getWorld().spigot().playEffect(entity.getEyeLocation(), Effect.EXTINGUISH, 0, 0, 0.0F, 0.0F, 0.0F, 0.3F, 15, 16);
    			
                // Damage the player
                addSpellTarget(entity, hero);
                double damage = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE, 40, false);
                damage += (double) (SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 0.5, false) * hero.getAttributeValue(AttributeType.INTELLECT));
                damageEntity(entity, hero.getPlayer(), damage, EntityDamageEvent.DamageCause.MAGIC);
                event.setCancelled(true);
            }
        }
    }

    @Override
    public String getDescription(Hero hero) {
       int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 40, false);
        damage += (double) (SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 0.5, false) * hero.getAttributeValue(AttributeType.INTELLECT));
       return getDescription().replace("$1", damage + "");
    }
}