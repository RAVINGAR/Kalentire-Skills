package com.herocraftonline.heroes.characters.skill.remastered.necromancer;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.DisguiseType;
import me.libraryaddict.disguise.disguisetypes.MobDisguise;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.LinkedHashMap;
import java.util.Map;

public class SkillBecomeDeath extends ActiveSkill {

    private boolean disguiseApiLoaded = false;
    private String applyText;
    private String expireText;

    public SkillBecomeDeath(Heroes plugin) {
        super(plugin, "BecomeDeath");
        setDescription("For $1 seconds you look undead. While shrouded in death, you no longer need to breath air.");
        setUsage("/skill becomedeath");
        setArgumentRange(0, 0);
        setIdentifiers("skill becomedeath");
        setTypes(SkillType.SILENCEABLE, SkillType.BUFFING, SkillType.ABILITY_PROPERTY_DARK, SkillType.FORM_ALTERING);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(), plugin);

        if (Bukkit.getServer().getPluginManager().getPlugin("LibsDisguises") != null) {
            disguiseApiLoaded = true;
        }
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 120000, false);

        return getDescription().replace("$1", (duration / 1000) + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.USE_TEXT.node(), "");
        config.set(SkillSetting.DURATION.node(), 120000);
        config.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% shrouds themself in death!");
        config.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% no longer appears as an undead!");
        return config;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT,
                ChatComponents.GENERIC_SKILL + "%hero% shrouds themself in death!").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT,
                ChatComponents.GENERIC_SKILL + "%hero% no longer appears as an undead!").replace("%hero%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        broadcastExecuteText(hero);

        Player player = hero.getPlayer();

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 120000, false);
        hero.addEffect(new BecomeDeathEffect(this, player, duration));

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_CAT_AMBIENT, 1.0F, 1.0F);

        return SkillResult.NORMAL;
    }

    public class SkillEntityListener implements Listener {

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onEntityTarget(EntityTargetEvent event) {
            if (!(event.getEntity() instanceof LivingEntity) || !(event.getTarget() instanceof Player))
                return;

            final LivingEntity entity = (LivingEntity) event.getEntity();
            if (!(Util.isUndead(plugin, entity)))
                return;

            Hero hero = plugin.getCharacterManager().getHero((Player) event.getTarget());
            if (!hero.hasEffect("BecomeDeath"))
                return;

            BecomeDeathEffect bdEffect = (BecomeDeathEffect) hero.getEffect("BecomeDeath");
            assert bdEffect != null;
            //if (!bdEffect.hasAttackedMobs())
            if (!bdEffect.hasProvokedMob(entity))
                event.setCancelled(true);
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onEntityDamage(EntityDamageEvent event) {
            if (event.getDamage() == 0 || !(event.getEntity() instanceof LivingEntity))
                return;

            final LivingEntity entity = (LivingEntity) event.getEntity();
            if (!Util.isUndead(plugin, entity) || !(event instanceof EntityDamageByEntityEvent))
                return;

            EntityDamageByEntityEvent subEvent = (EntityDamageByEntityEvent) event;
            if (subEvent.getDamager() instanceof Player) {
                Hero hero = plugin.getCharacterManager().getHero((Player) subEvent.getDamager());
                if (hero.hasEffect("BecomeDeath")) {
                    BecomeDeathEffect bdEffect = (BecomeDeathEffect) hero.getEffect("BecomeDeath");
                    assert bdEffect != null;
                    //bdEffect.setAttackedMobs(true);
                    bdEffect.addProvokedMob(entity);
                }
            }
            else if (subEvent.getDamager() instanceof Projectile) {
                if (((Projectile) subEvent.getDamager()).getShooter() instanceof Player) {
                    Hero hero = plugin.getCharacterManager().getHero((Player) ((Projectile) subEvent.getDamager()).getShooter());
                    if (hero.hasEffect("BecomeDeath")) {
                        BecomeDeathEffect bdEffect = (BecomeDeathEffect) hero.getEffect("BecomeDeath");
                        assert bdEffect != null;
                        //bdEffect.setAttackedMobs(true);
                        bdEffect.addProvokedMob(entity);
                    }
                }
            }
        }
    }

    public class BecomeDeathEffect extends ExpirableEffect {
        private static final long cooldownMilliseconds = 5000L;
        private final Map<LivingEntity, Long> provokedMobs = new LinkedHashMap<LivingEntity, Long>(10) {
            private static final long serialVersionUID = 2196792527721771866L;

            @Override
            protected boolean removeEldestEntry(Map.Entry<LivingEntity, Long> eldest) {
                return (size() > 30 || eldest.getValue() + cooldownMilliseconds <= System.currentTimeMillis());
            }
        };

        //private boolean attackedMobs = false;

        public BecomeDeathEffect(Skill skill, Player applier, long duration) {
            super(skill, "BecomeDeath", applier, duration, applyText, expireText);

            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.DARK);
            types.add(EffectType.MAGIC);
            types.add(EffectType.WATER_BREATHING);

            addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, (int) (duration / 1000) * 20, 0));
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            Player player = hero.getPlayer();

            if (disguiseApiLoaded) {
                if (!DisguiseAPI.isDisguised(player))
                    DisguiseAPI.undisguiseToAll(player);

                //DisguiseAPI.constructDisguise(Zombie.class, true, true, true);
                MobDisguise disguise = new MobDisguise(DisguiseType.getType(EntityType.ZOMBIE), true);
                disguise.setHearSelfDisguise(true);
                disguise.setKeepDisguiseOnPlayerDeath(false);
                disguise.setHideHeldItemFromSelf(true);
                disguise.setHideArmorFromSelf(true);
                disguise.addPlayer(player);
            }
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            Player player = hero.getPlayer();

            if (disguiseApiLoaded) {
                if (DisguiseAPI.isDisguised(player))
                    DisguiseAPI.undisguiseToAll(player);
            }
            provokedMobs.clear();
        }

//        public boolean hasAttackedMobs() {
//            return attackedMobs;
//        }

//        public void setAttackedMobs(boolean attackedMobs) {
//            this.attackedMobs = attackedMobs;
//        }

        public void addProvokedMob(LivingEntity entity) {
            provokedMobs.put(entity, System.currentTimeMillis());
        }

        public boolean hasProvokedMob(LivingEntity entity) {
            return provokedMobs.containsKey(entity);
        }
    }
}
