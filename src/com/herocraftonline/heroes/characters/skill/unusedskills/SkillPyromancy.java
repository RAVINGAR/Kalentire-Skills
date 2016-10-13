package com.herocraftonline.heroes.characters.skill.unusedskills;

import java.text.DecimalFormat;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import net.minecraft.server.v1_10_R1.*;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.craftbukkit.v1_10_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_10_R1.entity.CraftLivingEntity;
import org.bukkit.entity.Blaze;
import org.bukkit.entity.Creature;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.SkillDamageEvent;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.nms.versions.EntityUtil_v1_10_R1;
import com.herocraftonline.heroes.util.Messaging;

public class SkillPyromancy extends ActiveSkill implements Listener {

    private static SkillPyromancy instance;
    public Cache<UUID,PyromancyTargetEffect> targetEffects;

    public static SkillPyromancy getInstance() {
        return instance;
    }

    public SkillPyromancy(final Heroes plugin) {
        super(plugin, "Pyromancy");
        this.setDescription("Summons 1 Jotunn (up to a maximum of $1) that will deal $2 damage per shot to your last attacked target");
        this.setIdentifiers("skill pyromancy");
        this.setArgumentRange(0, 0);
        this.setUsage("/skill pyromancy");
        instance = this;
        this.targetEffects = CacheBuilder.newBuilder().weakValues().expireAfterAccess(5, TimeUnit.MINUTES).build(new CacheLoader<UUID,PyromancyTargetEffect>() {

            @Override
            public PyromancyTargetEffect load(UUID id) {
                Player p = Bukkit.getPlayer(id);
                CharacterTemplate cT;
                if(p != null) {
                    cT = plugin.getCharacterManager().getHero(p);
                } else {
                    cT = plugin.getCharacterManager().getMonster(id);
                }
                PyromancyTargetEffect tEffect = (PyromancyTargetEffect) cT.getEffect("PyromancyTarget");
                if(tEffect == null) {
                    tEffect = new PyromancyTargetEffect(plugin, instance);
                    cT.addEffect(tEffect);
                }
                return tEffect;
            }

        });
        plugin.getServer().getScheduler().runTask(plugin, new Runnable() {

            @Override
            public void run() {
                EntityUtil_v1_10_R1.registerCustomEntity(PyromancyBlaze.class, "SkillPyromancyBlaze", 61, false);
                EntityUtil_v1_10_R1.registerCustomEntity(PyromancyBlazeFireball.class, "SkillPyromancyFireball", 12, false);
            }

        });
        plugin.getServer().getPluginManager().registerEvents(this, this.plugin);
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        PyromancySummonsEffect effect = (PyromancySummonsEffect) hero.getEffect("PyromancySummons");
        if(effect == null) {
            effect = new PyromancySummonsEffect(this.plugin, this, hero);
        }
        if(!effect.canSummon()) {
            Messaging.send(hero.getPlayer(), "You do not have the ability to control any more summons!", new Object[] {});
            return SkillResult.INVALID_TARGET_NO_MSG;
        } else {
            Blaze blaze = summonMinion(hero.getPlayer().getEyeLocation(), effect, hero.getPlayer());
            effect.registerSummons(blaze);
            Messaging.send(hero.getPlayer(), "Summoned 1 blaze!", new Object[] {});
            return SkillResult.NORMAL;
        }
    }

    private Blaze summonMinion(Location loc, PyromancySummonsEffect effect, Player owner) {
        World world = ((CraftWorld) loc.getWorld()).getHandle();
        PyromancyBlaze summons = new PyromancyBlaze(world, effect, owner, this);
        world.addEntity(summons);
        Blaze summonedBukkit = (Blaze) summons.getBukkitEntity();
        summonedBukkit.teleport(loc, TeleportCause.UNKNOWN);
        return summonedBukkit;
    }

    @Override
    public String getDescription(Hero hero) {
        int max = (int) Math.floor(SkillConfigManager.getUseSetting(hero,this,"max-summons",3,false)
                + hero.getSkillLevel(this)
                * SkillConfigManager.getUseSetting(hero,this,"max-summons-per-level", .1, false));
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 5, false);
        damage += (SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 0.1, false) * hero.getSkillLevel(this));

        return getDescription().replace("$1",max + "").replace("$2", new DecimalFormat("##.##").format(damage));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("max-summons", 3);
        node.set("max-summons-per-level", 0.1);
        node.set(SkillSetting.DAMAGE.node(), 5);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 0.1);
        node.set("fire-ticks", 40);
        return node;
    }

    public class PyromancySummonsEffect extends Effect {

        private Map<UUID,Blaze> summons;
        private Hero player;
        private boolean removed;

        public PyromancySummonsEffect(Heroes plugin, Skill skill, Hero player) {
            super(plugin, skill, "PyromancySummons");
            this.player = player;
            this.summons = new HashMap<UUID,Blaze>();
            this.removed = false;
        }

        public void registerSummons(Blaze blaze) {
            this.summons.put(blaze.getUniqueId(), blaze);
        }

        public boolean canSummon() {
            this.clean();
            int max = (int) Math.floor(SkillConfigManager.getUseSetting(this.player,this.skill,"max-summons",3,false)
                    + this.player.getSkillLevel(this.skill)
                    * SkillConfigManager.getUseSetting(this.player,this.skill,"max-summons-per-level", .1, false));
            return this.summons.size() < max;
        }

        public boolean isValidSummons(UUID summonId) {
            return this.summons.containsKey(summonId);
        }

        public void clean() {
            Deque<UUID> remove = new LinkedList<UUID>();
            for(Blaze blaze : this.summons.values()) {
                if(!blaze.isValid()) {
                    remove.push(blaze.getUniqueId());
                }
            }
            UUID toRemove = remove.poll();
            while(toRemove != null) {
                this.summons.remove(toRemove);
                toRemove = remove.poll();
            }
        }

        @Override
        public void removeFromHero(Hero hero) {
            this.removed = true;
            super.removeFromHero(hero);
        }

        public boolean isRemoved() {
            return this.removed;
        }

        public Hero getOwner() {
            return this.player;
        }

    }

    public class PyromancyTargetEffect extends Effect {

        private LivingEntity lastTarget;

        public PyromancyTargetEffect(Heroes plugin, Skill skill) {
            super(plugin, skill, "PyromancyTarget");
            this.lastTarget = null;
        }

        public void updateTarget(LivingEntity entity) {
            if (this.lastTarget != null) {
                if (this.lastTarget.getUniqueId().equals(entity.getUniqueId())) {
                    return;
                } else {
                    this.lastTarget = entity;
                }
            } else {
                this.lastTarget = entity;
            }
        }

        public LivingEntity getLastTarget() {
            return this.lastTarget;
        }

    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWeaponDamage(WeaponDamageEvent event) {
        if (event.getDamager() instanceof Hero) {
            if(!(event.getEntity() instanceof LivingEntity)) {
                return;
            }
            Hero h = (Hero) event.getDamager();
            if(h.canUseSkill(this)) {
                PyromancyTargetEffect effect = ((LoadingCache<UUID, PyromancyTargetEffect>) this.targetEffects).getUnchecked(h.getPlayer().getUniqueId());
                effect.updateTarget((LivingEntity) event.getEntity());
                return;
            } else {
                return;
            }
        } else {
            //TODO: Add support for mobs
            return;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSkillDamage(SkillDamageEvent event) {
        if (event.getDamager() instanceof Hero) {
            if(!(event.getEntity() instanceof LivingEntity)) {
                return;
            }
            Hero h = (Hero) event.getDamager();
            if(h.canUseSkill(this)) {
                PyromancyTargetEffect effect = ((LoadingCache<UUID, PyromancyTargetEffect>) this.targetEffects).getUnchecked(h.getPlayer().getUniqueId());
                effect.updateTarget((LivingEntity) event.getEntity());
                return;
            } else {
                return;
            }
        } else {
            //TODO: Add support for mobs
            return;
        }
    }

    class PyromancyBlaze extends EntityBlaze {

        private PyromancySummonsEffect linkedEffect;
        private LivingEntity owner;
        private int br;
        private Skill skill;

        public PyromancyBlaze(World world, PyromancySummonsEffect effect, LivingEntity owner, Skill skill) {
            super(world);
            this.skill = skill;
            this.br = 0;
            this.linkedEffect = effect;
            this.goalSelector.a(0, new PathfinderGoalCheckPyroValidity(((LivingEntity)this.getBukkitEntity()), linkedEffect));
            this.goalSelector.a(1, new PathfinderGoalOrbitOwner(owner, (Creature) this.getBukkitEntity()));
        }

        //Blazes still use old AI - override
        /*
        @Override
        protected Entity findTarget() {
            if (owner instanceof Player) {
                Hero h = Heroes.getInstance().getCharacterManager().getHero((Player)owner);
                PyromancyTargetEffect effect = ((LoadingCache<UUID, PyromancyTargetEffect>) SkillPyromancy.getInstance().targetEffects).getUnchecked(h.getPlayer().getUniqueId());
                LivingEntity target = effect.getLastTarget();
                if(target != null) {
                    return ((CraftLivingEntity)target).getHandle();
                } else {
                    return null;
                }
            } else if (owner instanceof Creature) {
                return ((CraftLivingEntity)((Creature) owner).getTarget()).getHandle();
            } else { //Should not ever be called
                return null;
            }
        }
*/
        @Override
        public void a(Entity entity, float f, float nf1) {

            //ToDo: Fix this for 1.8

            /*if (this.attackTicks <= 0 && f < 2.0F && entity.getBoundingBox().e > this.getBoundingBox().b && entity.getBoundingBox().b < this.getBoundingBox().e) {
                this.attackTicks = 20;
                this.n(entity);
            } else if (f < 30.0F) {
                double d0 = entity.locX - this.locX;
                double d1 = entity.getBoundingBox().b + (double) (entity.length / 2.0F) - (this.locY + (double) (this.length / 2.0F));
                double d2 = entity.locZ - this.locZ;

                if (this.attackTicks == 0) {
                    ++this.br;
                    if (this.br == 1) {
                        this.attackTicks = 60;
                        this.a(true);
                    } else if (this.br <= 4) {
                        this.attackTicks = 6;
                    } else {
                        this.attackTicks = 100;
                        this.br = 0;
                        this.a(false);
                    }

                    if (this.br > 1) {
                        float f1 = MathHelper.c(f) * 0.5F;

                        this.world.a((EntityHuman) null, 1009, (int) this.locX, (int) this.locY, (int) this.locZ, 0);

                        for (int i = 0; i < 1; ++i) {
                            PyromancyBlazeFireball entitysmallfireball = new PyromancyBlazeFireball(this.world, this, d0 + this.random.nextGaussian() * (double) f1, d1, d2 + this.random.nextGaussian() * (double) f1, null, skill);

                            entitysmallfireball.locY = this.locY + (double) (this.length / 2.0F) + 0.5D;
                            this.world.addEntity(entitysmallfireball);
                        }
                    }
                }

                this.yaw = (float) (Math.atan2(d2, d0) * 180.0D / Math.PI) - 90.0F;
                this.bn = true;
            }
            */

            return;
        }
    }

    class PyromancyBlazeFireball extends EntityFireball {

        public PyromancyBlazeFireball(World world, PyromancyBlaze entityliving, double d0, double d1, double d2, Object object, Skill skill)
        {
            // TODO Auto-generated constructor stub
            super(world, entityliving, d0, d1, d2);
            
        }

        @Override
        protected void a(MovingObjectPosition paramMovingObjectPosition)
        {
            // TODO Auto-generated method stub
            
        }
        
    }

    class PathfinderGoalCheckPyroValidity extends PathfinderGoal {

        public PathfinderGoalCheckPyroValidity(LivingEntity livingEntity, PyromancySummonsEffect linkedEffect)
        {
            // TODO Auto-generated constructor stub
        }

        @Override
        public boolean a()
        {
            // TODO Auto-generated method stub
            return false;
        }
        
    }

    class PathfinderGoalOrbitOwner extends PathfinderGoal {

        public PathfinderGoalOrbitOwner(LivingEntity owner, Creature bukkitEntity)
        {
            // TODO Auto-generated constructor stub
        }

        @Override
        public boolean a()
        {
            // TODO Auto-generated method stub
            return false;
        }
        
    }
}