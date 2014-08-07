/*package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.common.SummonEffect;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

public class SkillSkeleton extends ActiveSkill {
    private String expireText;
    private SummonEntityListener eListener;
    private SummonPlayerListener pListener;

    public SkillSkeleton(Heroes paramHeroes)
    {
        super(paramHeroes, "Skeleton");
        setDescription("Summons a skeleton to fight by your side");
        setUsage("/skill skeleton");
        setArgumentRange(0, 0);
        setIdentifiers(new String[] { "skill skeleton" });
        setTypes(SkillType.ABILITY_PROPERTY_DARK, SkillType.SUMMONING, SkillType.SILENCEABLE);
        this.eListener = new SummonEntityListener();
        this.pListener = new SummonPlayerListener();
        registerEvent(Event.Type.ENTITY_DEATH, this.eListener, Event.Priority.Monitor);
        registerEvent(Event.Type.ENTITY_TARGET, this.eListener, Event.Priority.Highest);
        registerEvent(Event.Type.ENTITY_COMBUST, this.eListener, Event.Priority.Highest);
        registerEvent(Event.Type.ENTITY_DAMAGE, this.eListener, Event.Priority.Monitor);
        registerEvent(Event.Type.PLAYER_QUIT, this.pListener, Event.Priority.Lowest);
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
        this.expireText = SkillConfigManager.getRaw(this, Setting.EXPIRE_TEXT, "The skeleton returns to it's hellish domain.");
    }

    public SkillResult use(Hero paramHero, String[] paramArrayOfString)
    {
        Player localPlayer = paramHero.getPlayer();
        if (paramHero.getSummons().size() < SkillConfigManager.getUseSetting(paramHero, this, "max-summons", 3, false))
        {
            int i = SkillConfigManager.getUseSetting(paramHero, this, Setting.MAX_DISTANCE, 5, false);
            Location localLocation = localPlayer.getTargetBlock((HashSet)null, i).getLocation();
            Creature localCreature = (Creature)localPlayer.getWorld().spawnCreature(localLocation, CreatureType.SKELETON);
            long l = SkillConfigManager.getUseSetting(paramHero, this, Setting.DURATION, 60000, false);
            this.plugin.getEffectManager().addEntityEffect(localCreature, new SummonEffect(this, l, paramHero, this.expireText));
            broadcastExecuteText(paramHero);
            Messaging.send(localPlayer, "You have succesfully summoned a skeleton to fight for you.", new Object[0]);
            return SkillResult.NORMAL;
        }
        Messaging.send(localPlayer, "You can't control anymore skeletons!", new Object[0]);
        return SkillResult.FAIL;
    }

    public class SummonPlayerListener
            extends PlayerListener
    {
        public SummonPlayerListener() {}

        public void onPlayerQuit(PlayerQuitEvent paramPlayerQuitEvent)
        {
            Heroes.debug.startTask("HeroesSkillListener.Skeleton");
            Hero localHero = SkillSkeleton.this.plugin.getHeroManager().getHero(paramPlayerQuitEvent.getPlayer());
            if (localHero.getSummons().isEmpty())
            {
                Heroes.debug.stopTask("HeroesSkillListener.Skeleton");
                return;
            }
            Iterator localIterator = localHero.getSummons().iterator();
            while (localIterator.hasNext())
            {
                LivingEntity localLivingEntity = (LivingEntity)localIterator.next();
                if ((localLivingEntity instanceof Skeleton))
                {
                    Effect localEffect = SkillSkeleton.this.plugin.getEffectManager().getEntityEffect(localLivingEntity, "Summon");
                    if (localEffect != null) {
                        SkillSkeleton.this.plugin.getEffectManager().removeEntityEffect(localLivingEntity, localEffect);
                    } else {
                        localLivingEntity.remove();
                    }
                }
            }
            Heroes.debug.stopTask("HeroesSkillListener.Skeleton");
        }
    }

    public class SummonEntityListener
            extends EntityListener
    {
        public SummonEntityListener() {}

        public void onEntityCombust(EntityCombustEvent paramEntityCombustEvent)
        {
            Heroes.debug.startTask("HeroesSkillListener.Skeleton");
            if ((!(paramEntityCombustEvent.getEntity() instanceof Skeleton)) || (paramEntityCombustEvent.isCancelled()))
            {
                Heroes.debug.stopTask("HeroesSkillListener.Skeleton");
                return;
            }
            Creature localCreature = (Creature)paramEntityCombustEvent.getEntity();
            if (SkillSkeleton.this.plugin.getEffectManager().entityHasEffect(localCreature, "Summon")) {
                paramEntityCombustEvent.setCancelled(true);
            }
            Heroes.debug.stopTask("HeroesSkillListener.Skeleton");
        }

        public void onEntityDamage(EntityDamageEvent paramEntityDamageEvent)
        {
            Heroes.debug.startTask("HeroesSkillListener.Skeleton");
            if ((paramEntityDamageEvent.isCancelled()) || (!(paramEntityDamageEvent instanceof EntityDamageByEntityEvent)))
            {
                Heroes.debug.stopTask("HeroesSkillListener.Skeleton");
                return;
            }
            Object localObject1;
            Object localObject2;
            Object localObject3;
            Iterator localIterator;
            LivingEntity localLivingEntity;
            if ((paramEntityDamageEvent.getEntity() instanceof Player))
            {
                localObject1 = SkillSkeleton.this.plugin.getHeroManager().getHero((Player)paramEntityDamageEvent.getEntity());
                if (((Hero)localObject1).getSummons().isEmpty())
                {
                    Heroes.debug.stopTask("HeroesSkillListener.Skeleton");
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
                    Heroes.debug.stopTask("HeroesSkillListener.Skeleton");
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
                    Heroes.debug.stopTask("HeroesSkillListener.Skeleton");
                    return;
                }
                localObject3 = SkillSkeleton.this.plugin.getHeroManager().getHero((Player)localObject2);
                if (((Hero)localObject3).getSummons().isEmpty())
                {
                    Heroes.debug.stopTask("HeroesSkillListener.Skeleton");
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
                Heroes.debug.stopTask("HeroesSkillListener.Skeleton");
            }
        }

        public void onEntityDeath(EntityDeathEvent paramEntityDeathEvent)
        {
            Heroes.debug.startTask("HeroesSkillListener.Skeleton");
            if (!(paramEntityDeathEvent.getEntity() instanceof Skeleton))
            {
                Heroes.debug.stopTask("HeroesSkillListener.Skeleton");
                return;
            }
            Skeleton localSkeleton = (Skeleton)paramEntityDeathEvent.getEntity();
            Collection localCollection = SkillSkeleton.this.plugin.getHeroManager().getHeroes();
            Iterator localIterator = localCollection.iterator();
            while (localIterator.hasNext())
            {
                Hero localHero = (Hero)localIterator.next();
                if (localHero.getSummons().contains(localSkeleton)) {
                    localHero.getSummons().remove(localSkeleton);
                }
            }
            Heroes.debug.stopTask("HeroesSkillListener.Skeleton");
        }

        public void onEntityTarget(EntityTargetEvent paramEntityTargetEvent)
        {
            Heroes.debug.startTask("HeroesSkillListener.Skeleton");
            if ((paramEntityTargetEvent.isCancelled()) || (!(paramEntityTargetEvent.getEntity() instanceof Creature)))
            {
                Heroes.debug.stopTask("HeroesSkillListener.Skeleton");
                return;
            }
            if ((paramEntityTargetEvent.getTarget() instanceof Player))
            {
                Iterator localIterator1 = SkillSkeleton.this.plugin.getHeroManager().getHeroes().iterator();
                while (localIterator1.hasNext())
                {
                    Hero localHero1 = (Hero)localIterator1.next();
                    if (localHero1.getSummons().contains(paramEntityTargetEvent.getEntity())) {
                        if (localHero1.getParty() != null)
                        {
                            Iterator localIterator2 = localHero1.getParty().getMembers().iterator();
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
            Heroes.debug.stopTask("HeroesSkillListener.Skeleton");
        }
    }
}
*/