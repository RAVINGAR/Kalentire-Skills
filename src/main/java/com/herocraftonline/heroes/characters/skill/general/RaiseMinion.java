package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.common.SummonEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import io.lumine.mythic.api.exceptions.InvalidMobTypeException;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.bukkit.adapters.BukkitEntity;
import io.lumine.mythic.core.mobs.ActiveMob;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
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
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Optional;

public class RaiseMinion extends ActiveSkill implements Listenable {
    private String expireText;
    private final Listener listener;

    public RaiseMinion(Heroes paramHeroes)
    {
        super(paramHeroes, "RaiseMinion");
        setDescription("Summons an undead creature to fight by your side");
        setUsage("/skill raiseminion");
        setArgumentRange(0, 0);
        setIdentifiers("skill raiseminion");
        setTypes(SkillType.ABILITY_PROPERTY_DARK, SkillType.SUMMONING, SkillType.SILENCEABLE);
        listener = new MinionListener(this);
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
        node.set("mythic-mob-type", "NecroDemon");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "The creature returns to it's hellish domain.");
        node.set("max-summons", 3);
        node.set(SkillSetting.RADIUS.node(), 7);
        node.set(SkillSetting.RADIUS_INCREASE_PER_WISDOM.node(), 0.005);

        return node;
    }

    public void init()
    {
        super.init();
        this.expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "The creature returns to it's hellish domain.");
    }

    public SkillResult use(Hero paramHero, String[] paramArrayOfString)
    {
        Player localPlayer = paramHero.getPlayer();
        if (paramHero.getSummons().size() < SkillConfigManager.getUseSetting(paramHero, this, "max-summons", 3, false))
        {
            int i = SkillConfigManager.getUseSetting(paramHero, this, SkillSetting.MAX_DISTANCE, 5, false);
            Location localLocation = localPlayer.getTargetBlock(null, i).getLocation();
            try {
                localLocation.getWorld().spawnParticle(Particle.WARPED_SPORE, localLocation.add(0, 0.5, 0), 40, 1, 1, 1, 0.5);
                localLocation.getWorld().spawnParticle(Particle.CLOUD, localLocation.add(0, 0, 0), 10, 1, 1, 1, 0.5);
                LivingEntity summon = (LivingEntity) MythicBukkit.inst().getAPIHelper().spawnMythicMob(SkillConfigManager.getUseSetting(paramHero, this, "mythic-mob-type", "NecroDemon"), localLocation);
                CharacterTemplate localCreature = plugin.getCharacterManager().getCharacter(summon);
                long l = SkillConfigManager.getUseSetting(paramHero, this, SkillSetting.DURATION, 60000, false);
                localCreature.addEffect(new SummonEffect(this, l, paramHero, this.expireText));
                summon.getEquipment().setItemInMainHand(new ItemStack(Material.NETHERITE_SWORD, 1));
                broadcastExecuteText(paramHero);
                localPlayer.sendMessage(ChatComponents.GENERIC_SKILL + "A hellish creature rises from the ground");
            }
            catch(InvalidMobTypeException e) {
                return SkillResult.FAIL;
            }
            return SkillResult.NORMAL;
        }
        localPlayer.sendMessage("You can't control any more skeletons!");
        return SkillResult.FAIL;
    }

    @NotNull
    @Override
    public Listener getListener() {
        return listener;
    }

    public class MinionListener implements Listener {
        private Skill skill;

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
            if (localCreature.isSummonedMob()) {
                paramEntityCombustEvent.setCancelled(true);
            }
        }

        @EventHandler(priority=EventPriority.MONITOR)
        public void onEntityDamage(EntityDamageEvent paramEntityDamageEvent)
        {
            if ((paramEntityDamageEvent.isCancelled()) || (!(paramEntityDamageEvent instanceof EntityDamageByEntityEvent entityDamageByEntityEvent))) {
                return;
            }
            if ((paramEntityDamageEvent.getEntity() instanceof Player))
            {
                //If player was attacked
                Hero hero = RaiseMinion.this.plugin.getCharacterManager().getHero((Player)paramEntityDamageEvent.getEntity());
                if (hero.getSummons().isEmpty()) {
                    return;
                }

                Entity damager = entityDamageByEntityEvent.getDamager();
                if(damager instanceof Projectile proj) {
                    damager = (Entity) proj.getShooter();
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
                //Player attacks a mob
                Entity damager = entityDamageByEntityEvent.getDamager();
                Hero hero = null;
                if(damager instanceof Player player) {
                    hero = skill.plugin.getCharacterManager().getHero(player);
                }
                else if (damager instanceof Projectile projectilej) {
                    Entity source = (Entity) projectilej.getShooter();
                    if(source instanceof Player player) {
                        hero = skill.plugin.getCharacterManager().getHero(player);
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
            Hero hero = local.getSummoner();
            new ArrayList<>(hero.getSummons()).forEach(summon -> {
                if(local.equals(summon)) {
                    hero.getSummons().remove(summon);
                }
            });
        }

        @EventHandler(priority=EventPriority.HIGHEST)
        public void onEntityTarget(EntityTargetEvent paramEntityTargetEvent)
        {
            if ((paramEntityTargetEvent.isCancelled()) || (!(paramEntityTargetEvent.getEntity() instanceof LivingEntity))) {
                return;
            }
            if ((paramEntityTargetEvent.getTarget() instanceof Player)) {
                Monster entity = plugin.getCharacterManager().getMonster(paramEntityTargetEvent.getEntity().getUniqueId());
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
