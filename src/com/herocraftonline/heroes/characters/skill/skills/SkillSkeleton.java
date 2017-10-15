package com.herocraftonline.heroes.characters.skill.skills;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
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
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.common.SummonEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;

public class SkillSkeleton extends ActiveSkill {
    private String expireText;

    public SkillSkeleton(Heroes paramHeroes)
    {
        super(paramHeroes, "Skeleton");
        setDescription("Summons a skeleton to fight by your side");
        setUsage("/skill skeleton");
        setArgumentRange(0, 0);
        setIdentifiers(new String[] { "skill skeleton" });
        setTypes(SkillType.ABILITY_PROPERTY_DARK, SkillType.SUMMONING, SkillType.SILENCEABLE);
    }

    @Override
    public String getDescription(Hero arg0) {
        return super.getDescription();
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 5);
        node.set(SkillSetting.DURATION.node(), 60000);
        node.set(SkillSetting.EXPIRE_TEXT.node(), "The skeleton returns to it's hellish domain.");
        node.set("max-summons", 3);
        node.set(SkillSetting.RADIUS.node(), 7);
        node.set(SkillSetting.RADIUS_INCREASE_PER_WISDOM.node(), 0.005);

        return node;
    }

    public void init()
    {
        super.init();
        this.expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "The skeleton returns to it's hellish domain.");
    }

    public SkillResult use(Hero paramHero, String[] paramArrayOfString)
    {
        Player localPlayer = paramHero.getPlayer();
        if (paramHero.getSummons().size() < SkillConfigManager.getUseSetting(paramHero, this, "max-summons", 3, false))
        {
            int i = SkillConfigManager.getUseSetting(paramHero, this, SkillSetting.MAX_DISTANCE, 5, false);
            Location localLocation = localPlayer.getTargetBlock((HashSet<Material>)null, i).getLocation();
            CharacterTemplate localCreature = plugin.getCharacterManager().getCharacter((LivingEntity) localPlayer.getWorld().spawnEntity(localLocation, EntityType.SKELETON));
            long l = SkillConfigManager.getUseSetting(paramHero, this, SkillSetting.DURATION, 60000, false);
            localCreature.addEffect(new SummonEffect(this, l, paramHero, this.expireText));
            broadcastExecuteText(paramHero);
            Messaging.send(localPlayer, "You have succesfully summoned a skeleton to fight for you.", new Object[0]);
            return SkillResult.NORMAL;
        }
        Messaging.send(localPlayer, "You can't control anymore skeletons!", new Object[0]);
        return SkillResult.FAIL;
    }

    protected class SummonPlayerListener
               implements Listener
    {
        public SummonPlayerListener() {
            Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
        }

        @EventHandler(priority=EventPriority.LOWEST)
        public void onPlayerQuit(PlayerQuitEvent paramPlayerQuitEvent)
        {
            Hero localHero = plugin.getCharacterManager().getHero(paramPlayerQuitEvent.getPlayer());
            if (localHero.getSummons().isEmpty())
            {
                return;
            }
            Iterator<Monster> localIterator = localHero.getSummons().iterator();
            while (localIterator.hasNext())
            {
                Monster localLivingEntity = localIterator.next();
                if (localLivingEntity.getEntity() instanceof Skeleton)
                {
                    Effect localEffect = localLivingEntity.getEffect("Summon");
                    if (localEffect != null) {
                        localLivingEntity.removeEffect(localEffect);
                    } else {
                        localLivingEntity.getEntity().remove();
                    }
                }
            }
        }
    }

    protected class SummonEntityListener
               implements Listener
    {
        public SummonEntityListener() {
            Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
        }

        @EventHandler(priority=EventPriority.HIGHEST)
        public void onEntityCombust(EntityCombustEvent paramEntityCombustEvent)
        {
            if ((!(paramEntityCombustEvent.getEntity() instanceof Skeleton)) || (paramEntityCombustEvent.isCancelled()))
            {
                return;
            }
            CharacterTemplate localCreature = plugin.getCharacterManager().getCharacter((LivingEntity) paramEntityCombustEvent.getEntity());
            if (localCreature.hasEffect("Summon")) {
                paramEntityCombustEvent.setCancelled(true);
            }
        }

        @EventHandler(priority=EventPriority.MONITOR)
        public void onEntityDamage(EntityDamageEvent paramEntityDamageEvent)
        {
            if ((paramEntityDamageEvent.isCancelled()) || (!(paramEntityDamageEvent instanceof EntityDamageByEntityEvent)))
            {
                return;
            }
            Object localObject1;
            Object localObject2;
            Object localObject3;
            Iterator<Monster> localIterator;
            LivingEntity localLivingEntity;
            if ((paramEntityDamageEvent.getEntity() instanceof Player))
            {
                localObject1 = SkillSkeleton.this.plugin.getCharacterManager().getHero((Player)paramEntityDamageEvent.getEntity());
                if (((Hero)localObject1).getSummons().isEmpty())
                {
                    return;
                }
                localObject2 = (EntityDamageByEntityEvent)paramEntityDamageEvent;
                localObject3 = null;
                if ((((EntityDamageByEntityEvent)localObject2).getDamager() instanceof Projectile)) {
                    localObject3 = ((Projectile)((EntityDamageByEntityEvent)localObject2).getDamager()).getShooter();
                } else if ((((EntityDamageByEntityEvent)localObject2).getEntity() instanceof LivingEntity)) {
                    localObject3 = (LivingEntity)((EntityDamageByEntityEvent)localObject2).getDamager();
                }
                if (localObject3 == null)
                {
                    return;
                }
                localIterator = ((Hero)localObject1).getSummons().iterator();
                while (localIterator.hasNext())
                {
                    localLivingEntity = (LivingEntity)localIterator.next();
                    if ((localLivingEntity instanceof Skeleton)) {
                        ((Skeleton)localLivingEntity).setTarget((LivingEntity)localObject3);
                    }
                }
            }
            else if ((paramEntityDamageEvent.getEntity() instanceof LivingEntity))
            {
                localObject1 = (EntityDamageByEntityEvent)paramEntityDamageEvent;
                localObject2 = null;
                if ((((EntityDamageByEntityEvent)localObject1).getDamager() instanceof Player)) {
                    localObject2 = (Player)((EntityDamageByEntityEvent)localObject1).getDamager();
                } else if (((((EntityDamageByEntityEvent)localObject1).getDamager() instanceof Projectile)) && ((((Projectile)((EntityDamageByEntityEvent)localObject1).getDamager()).getShooter() instanceof Player))) {
                    localObject2 = (Player)((Projectile)((EntityDamageByEntityEvent)localObject1).getDamager()).getShooter();
                }
                if (localObject2 == null)
                {
                    return;
                }
                localObject3 = SkillSkeleton.this.plugin.getCharacterManager().getHero((Player)localObject2);
                if (((Hero)localObject3).getSummons().isEmpty())
                {
                    return;
                }
                localIterator = ((Hero)localObject3).getSummons().iterator();
                while (localIterator.hasNext())
                {
                    localLivingEntity = (LivingEntity)localIterator.next();
                    if ((localLivingEntity instanceof Skeleton)) {
                        ((Skeleton)localLivingEntity).setTarget((LivingEntity)paramEntityDamageEvent.getEntity());
                    }
                }
            }
        }

        @EventHandler(priority=EventPriority.MONITOR)
        public void onEntityDeath(EntityDeathEvent paramEntityDeathEvent)
        {
            if (!(paramEntityDeathEvent.getEntity() instanceof Skeleton))
            {
                return;
            }
            Skeleton localSkeleton = (Skeleton)paramEntityDeathEvent.getEntity();
            Collection<Hero> localCollection = SkillSkeleton.this.plugin.getCharacterManager().getHeroes();
            Iterator<Hero> localIterator = localCollection.iterator();
            while (localIterator.hasNext())
            {
                Hero localHero = localIterator.next();
                if (localHero.getSummons().contains(localSkeleton)) {
                    localHero.getSummons().remove(localSkeleton);
                }
            }
        }

        @EventHandler(priority=EventPriority.HIGHEST)
        public void onEntityTarget(EntityTargetEvent paramEntityTargetEvent)
        {
            if ((paramEntityTargetEvent.isCancelled()) || (!(paramEntityTargetEvent.getEntity() instanceof Creature)))
            {
                return;
            }
            if ((paramEntityTargetEvent.getTarget() instanceof Player))
            {
                Iterator<Hero> localIterator1 = SkillSkeleton.this.plugin.getCharacterManager().getHeroes().iterator();
                while (localIterator1.hasNext())
                {
                    Hero localHero1 = (Hero)localIterator1.next();
                    if (localHero1.getSummons().contains(paramEntityTargetEvent.getEntity())) {
                        if (localHero1.getParty() != null)
                        {
                            Iterator<Hero> localIterator2 = localHero1.getParty().getMembers().iterator();
                            while (localIterator2.hasNext())
                            {
                                Hero localHero2 = (Hero)localIterator2.next();
                                if (localHero2.getPlayer().equals(paramEntityTargetEvent.getTarget())) {
                                    paramEntityTargetEvent.setCancelled(true);
                                }
                            }
                        }
                        else if (localHero1.getPlayer().equals(paramEntityTargetEvent.getTarget()))
                        {
                            paramEntityTargetEvent.setCancelled(true);
                        }
                    }
                }
            }
        }
    }
}
