package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.skill.*;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.bukkit.adapters.BukkitEntity;
import io.lumine.mythic.core.mobs.ActiveMob;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.Optional;

public class SkillSummonAssist extends ActiveSkill {
    private String expireText;

    public SkillSummonAssist(Heroes paramHeroes)
    {
        super(paramHeroes, "SummonAssist");
        setDescription("Contains API to handle summoned mobs.");
        setUsage("/skill summonassist");
        setArgumentRange(0, 0);
        setIdentifiers("skill summonassist");
        setTypes(SkillType.ABILITY_PROPERTY_DARK, SkillType.SUMMONING, SkillType.SILENCEABLE);
        Bukkit.getPluginManager().registerEvents(new MinionListener(this),plugin);
    }

    @Override
    public String getDescription(Hero arg0) {
        return super.getDescription();
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        return super.getDefaultConfig();
    }

    public void init()
    {
        super.init();
        this.expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "The creature returns to it's hellish domain.");
    }

    public SkillResult use(Hero paramHero, String[] paramArrayOfString)
    {
        return SkillResult.NORMAL;
    }

    public class MinionListener implements Listener {
        private final Skill skill;

        public MinionListener(Skill skill) {
            this.skill = skill;
        }

        private Optional<ActiveMob> getActiveMob(Entity entity) {
            if(entity == null) {
                return Optional.empty();
            }
            return MythicBukkit.inst().getMobManager().getActiveMob(entity.getUniqueId());
        }

        @EventHandler(priority=EventPriority.LOWEST)
        public void onPlayerQuit(PlayerQuitEvent paramPlayerQuitEvent)
        {
            Hero localHero = plugin.getCharacterManager().getHero(paramPlayerQuitEvent.getPlayer());
            for (Monster localLivingEntity : localHero.getSummons()) {
                Effect localEffect = localLivingEntity.getEffect("Summon");
                if (localEffect != null) {
                    localLivingEntity.removeEffect(localEffect);
                } else {
                    localLivingEntity.getEntity().remove();
                }
            }
        }

        @EventHandler(priority=EventPriority.HIGHEST)
        public void onEntityCombust(EntityCombustEvent paramEntityCombustEvent)
        {
            if ((!(paramEntityCombustEvent.getEntity() instanceof LivingEntity)) || (paramEntityCombustEvent.isCancelled())) {
                return;
            }
            Monster localCreature = plugin.getCharacterManager().getMonster(paramEntityCombustEvent.getEntity().getUniqueId());
            if (localCreature != null && localCreature.isSummonedMob()) {
                paramEntityCombustEvent.setCancelled(true);
            }
        }

        @EventHandler(priority=EventPriority.MONITOR)
        public void onEntityDamage(EntityDamageEvent paramEntityDamageEvent)
        {
            if ((paramEntityDamageEvent.isCancelled()) || (!(paramEntityDamageEvent instanceof EntityDamageByEntityEvent))) {
                return;
            }
            if ((paramEntityDamageEvent.getEntity() instanceof Player))
            {
                //If player was attacked
                Hero hero = SkillSummonAssist.this.plugin.getCharacterManager().getHero((Player)paramEntityDamageEvent.getEntity());
                if (hero.getSummons().isEmpty()) {
                    return;
                }

                EntityDamageByEntityEvent event = (EntityDamageByEntityEvent) paramEntityDamageEvent;
                Entity damager = event.getDamager();
                if(damager instanceof Projectile) {
                    damager = (Entity) ((Projectile)damager).getShooter();
                }
                Entity finalDamager = damager;
                hero.getSummons().forEach(summon -> {
                    Optional<ActiveMob> mob = getActiveMob(summon.getEntity());
                    if (mob.isPresent()) {
                        ActiveMob localEnt = mob.get();
                        if(localEnt.hasThreatTable()) {
                            localEnt.getThreatTable().Taunt(new BukkitEntity(finalDamager));
                        }
                        else {
                            localEnt.setTarget(new BukkitEntity(finalDamager));
                        }
                    }
                });
            }
            else if ((paramEntityDamageEvent.getEntity() instanceof LivingEntity))
            {
                EntityDamageByEntityEvent event = (EntityDamageByEntityEvent) paramEntityDamageEvent;
                //Player attacks a mob
                Entity damager = event.getDamager();
                Hero hero = null;
                if(damager instanceof Player) {
                    hero = skill.plugin.getCharacterManager().getHero((Player)damager);
                }
                else if (damager instanceof Projectile) {
                    Entity source = (Entity) ((Projectile)damager).getShooter();
                    if(source instanceof Player) {
                        hero = skill.plugin.getCharacterManager().getHero((Player) source);
                    }
                }
                Entity target = paramEntityDamageEvent.getEntity();

                if(hero != null) {
                    hero.getSummons().forEach(summon -> {
                        Optional<ActiveMob> mob = getActiveMob(summon.getEntity());
                        if (mob.isPresent()) {
                            ActiveMob localEnt = mob.get();
                            if(localEnt.hasThreatTable()) {
                                localEnt.getThreatTable().Taunt(new BukkitEntity(target));
                            }
                            else {
                                localEnt.setTarget(new BukkitEntity(target));
                            }
                        }
                    });
                }
            }
        }

        @EventHandler(priority=EventPriority.LOWEST)
        public void onEntityDeath(EntityDeathEvent paramEntityDeathEvent)
        {
            Monster local = plugin.getCharacterManager().getMonster(paramEntityDeathEvent.getEntity().getUniqueId());
            if(local != null) {
                Hero hero = local.getSummoner();
                if(hero != null) {
                    new ArrayList<>(hero.getSummons()).forEach(summon -> {
                        if(local.equals(summon)) {
                            hero.getSummons().remove(summon);
                        }
                    });
                }
            }
        }

        @EventHandler(priority=EventPriority.HIGHEST)
        public void onEntityTarget(EntityTargetEvent paramEntityTargetEvent)
        {
            if ((paramEntityTargetEvent.isCancelled()) || (!(paramEntityTargetEvent.getEntity() instanceof LivingEntity))) {
                return;
            }
            if ((paramEntityTargetEvent.getTarget() instanceof Player)) {
                Monster entity = plugin.getCharacterManager().getMonster(paramEntityTargetEvent.getEntity().getUniqueId());
                if(entity == null) {
                    return;
                }
                Hero hero = entity.getSummoner();
                if(hero != null) {
                    Optional<ActiveMob> mob = getActiveMob(entity.getEntity());

                    if (hero.getParty() != null)
                    {
                        for (Hero member : hero.getParty().getMembers()) {
                            if (member.getPlayer().equals(paramEntityTargetEvent.getTarget())) {
                                mob.ifPresent(ActiveMob::resetTarget);
                                paramEntityTargetEvent.setCancelled(true);
                            }
                        }
                    }
                    else if (hero.getPlayer().equals(paramEntityTargetEvent.getTarget()))
                    {
                        mob.ifPresent(ActiveMob::resetTarget);
                        paramEntityTargetEvent.setCancelled(true);
                    }
                }
            }
        }
    }
}
