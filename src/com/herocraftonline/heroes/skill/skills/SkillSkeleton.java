package com.herocraftonline.heroes.skill.skills;

import java.util.Collection;
import java.util.HashSet;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Creature;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Skeleton;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.effects.Effect;
import com.herocraftonline.heroes.effects.common.SummonEffect;
import com.herocraftonline.heroes.hero.Hero;
import com.herocraftonline.heroes.skill.ActiveSkill;
import com.herocraftonline.heroes.skill.SkillConfigManager;
import com.herocraftonline.heroes.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Setting;

public class SkillSkeleton extends ActiveSkill {

    private String expireText;

    public SkillSkeleton(Heroes plugin) {
        super(plugin, "Skeleton");
        setDescription("Summons a skeleton to fight by your side for $1 seconds.");
        setUsage("/skill skeleton");
        setArgumentRange(0, 0);
        setIdentifiers("skill skeleton");
        setTypes(SkillType.DARK, SkillType.SUMMON, SkillType.SILENCABLE);
        Bukkit.getServer().getPluginManager().registerEvents(new SummonEntityListener(), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("max-summons", 3);
        node.set(Setting.MAX_DISTANCE.node(), 5);
        node.set(Setting.DURATION.node(), 60000);
        node.set(Setting.EXPIRE_TEXT.node(), "The skeleton returns to it's hellish domain.");
        return node;
    }

    @Override
    public void init() {
        super.init();
        expireText = SkillConfigManager.getRaw(this, Setting.EXPIRE_TEXT, "The skeleton returns to it's hellish domain.");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        if (hero.getSummons().size() < SkillConfigManager.getUseSetting(hero, this, "max-summons", 3, false)) {
            int distance = SkillConfigManager.getUseSetting(hero, this, Setting.MAX_DISTANCE, 5, false);
            Location castLoc = player.getTargetBlock((HashSet<Byte>) null, distance).getLocation();
            Creature skeleton = (Creature) player.getWorld().spawnCreature(castLoc, EntityType.SKELETON);
            long duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 60000, false);
            plugin.getEffectManager().addEntityEffect(skeleton, new SummonEffect(this, duration, hero, expireText));
            broadcastExecuteText(hero);
            Messaging.send(player, "You have succesfully summoned a skeleton to fight for you.");
            return SkillResult.NORMAL;
        }

        Messaging.send(player, "You can't control anymore skeletons!");
        return SkillResult.FAIL;
    }

    public class SummonEntityListener implements Listener {

        @EventHandler(priority = EventPriority.HIGHEST)
        public void onEntityCombust(EntityCombustEvent event) {
            if (!(event.getEntity() instanceof Skeleton) || event.isCancelled()) {
                return;
            }
            Creature creature = (Creature) event.getEntity();
            // Don't allow summoned creatures to combust
            if (plugin.getEffectManager().entityHasEffect(creature, "Summon")) {
                event.setCancelled(true);
            }
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onEntityDamage(EntityDamageEvent event) {
            if (event.isCancelled() || !(event instanceof EntityDamageByEntityEvent)) {
                return;
            }
            if (event.getEntity() instanceof Player) {
                Hero hero = plugin.getHeroManager().getHero((Player) event.getEntity());
                // If this hero has no summons then ignore the event
                if (hero.getSummons().isEmpty()) {
                    return;
                }

                EntityDamageByEntityEvent subEvent = (EntityDamageByEntityEvent) event;
                LivingEntity damager = null;
                // Lets get the damager
                if (subEvent.getDamager() instanceof Projectile) {
                    damager = ((Projectile) subEvent.getDamager()).getShooter();
                } else if (subEvent.getEntity() instanceof LivingEntity) {
                    damager = (LivingEntity) subEvent.getDamager();
                }
                if (damager == null) {
                    return;
                }

                // Loop through the hero's summons and set the target
                for (LivingEntity lEntity : hero.getSummons()) {
                    if (!(lEntity instanceof Skeleton)) {
                        continue;
                    }
                    ((Skeleton) lEntity).setTarget(damager);
                }
            } else if (event.getEntity() instanceof LivingEntity) {
                // If a creature is being damaged, lets see if a player is dealing the damage to see if we need to make
                // the summon aggro
                EntityDamageByEntityEvent subEvent = (EntityDamageByEntityEvent) event;
                Player player = null;
                if (subEvent.getDamager() instanceof Player) {
                    player = (Player) subEvent.getDamager();
                } else if (subEvent.getDamager() instanceof Projectile) {
                    if (((Projectile) subEvent.getDamager()).getShooter() instanceof Player) {
                        player = (Player) ((Projectile) subEvent.getDamager()).getShooter();
                    }
                }

                if (player == null) {
                    return;
                }
                Hero hero = plugin.getHeroManager().getHero(player);
                if (hero.getSummons().isEmpty()) {
                    return;
                }
                for (LivingEntity lEntity : hero.getSummons()) {
                    if (!(lEntity instanceof Skeleton)) {
                        continue;
                    }
                    ((Skeleton) lEntity).setTarget((LivingEntity) event.getEntity());
                }
            }
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onEntityDeath(EntityDeathEvent event) {
            if (!(event.getEntity() instanceof Skeleton)) {
                return;
            }
            Skeleton defender = (Skeleton) event.getEntity();
            Collection<Hero> heroes = plugin.getHeroManager().getHeroes();
            for (Hero hero : heroes) {
                if (hero.getSummons().contains(defender)) {
                    hero.getSummons().remove(defender);
                }
            }
        }

        @EventHandler(priority = EventPriority.HIGHEST)
        public void onEntityTarget(EntityTargetEvent event) {
            if (event.isCancelled() || !(event.getEntity() instanceof Creature)) {
                return;
            }
            if (event.getTarget() instanceof Player) {
                for (Hero hero : plugin.getHeroManager().getHeroes()) {
                    if (hero.getSummons().contains(event.getEntity())) {
                        if (hero.getParty() != null) {
                            // Don't target party members either
                            for (Hero pHero : hero.getParty().getMembers()) {
                                if (pHero.getPlayer().equals(event.getTarget())) {
                                    event.setCancelled(true);
                                }
                            }
                        } else if (hero.getPlayer().equals(event.getTarget())) {
                            event.setCancelled(true);
                        }
                    }
                }
            }
        }
        
        @EventHandler(priority = EventPriority.LOWEST)
        public void onPlayerQuit(PlayerQuitEvent event) {
            // Destroy any summoned creatures when the player exits
            Hero hero = plugin.getHeroManager().getHero(event.getPlayer());
            if (hero.getSummons().isEmpty()) {
                return;
            }
            for (LivingEntity summon : hero.getSummons()) {
                if (summon instanceof Skeleton) {
                    Effect effect = plugin.getEffectManager().getEntityEffect(summon, "Summon");
                    if (effect != null) {
                        plugin.getEffectManager().removeEntityEffect(summon, effect);
                    } else {
                        summon.remove();
                    }
                }
            }
        }
    }

    
    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 10000, false);
        return getDescription().replace("$1", duration / 1000 + "");
    }
}
