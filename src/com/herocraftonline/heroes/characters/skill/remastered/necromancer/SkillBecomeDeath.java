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

    private static final String becomeDeathEffectName = "BecomeDeath";

    public SkillBecomeDeath(Heroes plugin) {
        super(plugin, "BecomeDeath");
        setDescription("Undead do not see you unless you provoke them. Additionally, you can breathe underwater.");
        setUsage("/skill becomedeath");
        setArgumentRange(0, 0);
        setIdentifiers("skill becomedeath");
        setTypes(SkillType.SILENCEABLE, SkillType.BUFFING, SkillType.ABILITY_PROPERTY_DARK);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(), plugin);
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
        config.set(SkillSetting.APPLY_TEXT.node(), "");
        config.set(SkillSetting.UNAPPLY_TEXT.node(), "");
        return config;
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
            if (!hero.hasEffect(becomeDeathEffectName))
                return;

            BecomeDeathEffect bdEffect = (BecomeDeathEffect) hero.getEffect(becomeDeathEffectName);
            assert bdEffect != null;
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
                if (hero.hasEffect(becomeDeathEffectName)) {
                    BecomeDeathEffect bdEffect = (BecomeDeathEffect) hero.getEffect(becomeDeathEffectName);
                    assert bdEffect != null;
                    bdEffect.addProvokedMob(entity);
                }
            }
            else if (subEvent.getDamager() instanceof Projectile) {
                if (((Projectile) subEvent.getDamager()).getShooter() instanceof Player) {
                    Hero hero = plugin.getCharacterManager().getHero((Player) ((Projectile) subEvent.getDamager()).getShooter());
                    if (hero.hasEffect(becomeDeathEffectName)) {
                        BecomeDeathEffect bdEffect = (BecomeDeathEffect) hero.getEffect(becomeDeathEffectName);
                        assert bdEffect != null;
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

        public BecomeDeathEffect(Skill skill, Player applier, long duration) {
            super(skill, becomeDeathEffectName, applier, duration, null, null);

            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.DARK);
            types.add(EffectType.MAGIC);
            types.add(EffectType.WATER_BREATHING);

            addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, (int) (duration / 1000) * 20, 0));
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            provokedMobs.clear();
        }

        public void addProvokedMob(LivingEntity entity) {
            provokedMobs.put(entity, System.currentTimeMillis());
        }

        public boolean hasProvokedMob(LivingEntity entity) {
            return provokedMobs.containsKey(entity);
        }
    }
}
