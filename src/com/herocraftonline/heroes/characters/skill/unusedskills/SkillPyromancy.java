/*package com.herocraftonline.heroes.characters.skill.unusedskills;

        import java.text.DecimalFormat;
        import java.util.Deque;
        import java.util.HashMap;
        import java.util.LinkedList;
        import java.util.Map;
        import java.util.UUID;
        import java.util.concurrent.TimeUnit;

        import com.herocraftonline.heroes.api.Entity.EntityUtil;
        //import net.kingdomsofarden.andrew2060.heroes.skills.pyromancy.PyromancyBlaze;
        //import net.kingdomsofarden.andrew2060.heroes.skills.pyromancy.PyromancyBlazeFireball;
        import net.minecraft.server.v1_7_R4.World;

        import org.bukkit.Bukkit;
        import org.bukkit.Location;
        import org.bukkit.configuration.ConfigurationSection;
        import org.bukkit.craftbukkit.v1_7_R4.CraftWorld;
        import org.bukkit.entity.Blaze;
        import org.bukkit.entity.LivingEntity;
        import org.bukkit.entity.Player;
        import org.bukkit.event.EventHandler;
        import org.bukkit.event.EventPriority;
        import org.bukkit.event.Listener;
        import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

        import com.google.common.cache.Cache;
        import com.google.common.cache.CacheBuilder;
        import com.google.common.cache.CacheLoader;
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
                EntityUtil.registerCustomEntity(PyromancyBlaze.class, "SkillPyromancyBlaze", 61, false);
                EntityUtil.registerCustomEntity(PyromancyBlazeFireball.class, "SkillPyromancyFireball", 12, false);
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
        node.set("max-summons", Integer.valueOf(3));
        node.set("max-summons-per-level", Double.valueOf(0.1));
        node.set(SkillSetting.DAMAGE.node(), Integer.valueOf(5));
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), Double.valueOf(0.1));
        node.set("fire-ticks", Integer.valueOf(40));
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
                PyromancyTargetEffect effect = this.targetEffects.getUnchecked(h.getPlayer().getUniqueId());
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
                PyromancyTargetEffect effect = this.targetEffects.getUnchecked(h.getPlayer().getUniqueId());
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

}*/