package com.herocraftonline.heroes.characters.skill.reborn.pathfinder;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.effects.common.SilenceEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;

import java.util.LinkedHashMap;
import java.util.Map;

public class SkillSilencingShot extends ActiveSkill {
    private static String buffEffectName = "SilencingShotActive";

    private Map<Arrow, Long> shots = new LinkedHashMap<Arrow, Long>(100) {
        private static final long serialVersionUID = 4329526013158603250L;

        @Override
        protected boolean removeEldestEntry(Map.Entry<Arrow, Long> eldest) {
            return (size() > 60 || eldest.getValue() + 5000 <= System.currentTimeMillis());
        }
    };

    public SkillSilencingShot(Heroes plugin) {
        super(plugin, "SilencingShot");
        setDescription("For the next $1 second(s), your first arrow hit will silence the target for $2 second(s).");
        setUsage("/skill silencingshot");
        setIdentifiers("skill silencingshot");
        setArgumentRange(0, 0);
        setTypes(SkillType.DEBUFFING, SkillType.SILENCING);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillDamageListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.DURATION, false);
        int silDuration = SkillConfigManager.getUseSetting(hero, this, "silence-duration", 2000, false);

        return getDescription()
                .replace("$1", Util.decFormat.format(duration / 1000.0))
                .replace("$2", Util.decFormat.format(silDuration / 1000.0));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.DURATION.node(), 6000);
        config.set("silence-duration", 2000);
        config.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% has interrupting arrows attached to their bow.");
        config.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero%'s bow no longer has interrupting arrows attached to their bow.");
        return config;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        final Player player = hero.getPlayer();

        broadcastExecuteText(hero);
        long duration = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.DURATION, false);
        hero.addEffect(new InterruptingShotBuff(this, player, duration));

        return SkillResult.NORMAL;
    }

    public class InterruptingShotBuff extends ExpirableEffect {
        InterruptingShotBuff(Skill skill, Player applier, long duration) {
            super(skill, buffEffectName, applier, duration);

            types.add(EffectType.BENEFICIAL);
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);

        }
    }

    public class SkillDamageListener implements Listener {
        private final Skill skill;

        public SkillDamageListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onEntityShootBow(EntityShootBowEvent event) {
            if (!(event.getEntity() instanceof Player) || !(event.getProjectile() instanceof Arrow)) {
                return;
            }

            Hero hero = plugin.getCharacterManager().getHero((Player) event.getEntity());
            if (hero.hasEffect(buffEffectName)) {
                shots.put((Arrow) event.getProjectile(), System.currentTimeMillis());
                hero.removeEffect(hero.getEffect(buffEffectName));
            }
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onEntityDamage(EntityDamageByEntityEvent event) {
            if (!(event.getEntity() instanceof LivingEntity) || !(event.getDamager() instanceof Arrow))
                return;

            Arrow arrow = (Arrow) event.getDamager();
            if (!(arrow.getShooter() instanceof Player) || !shots.containsKey(arrow))
                return;

            shots.remove(arrow);

            Player player = (Player) arrow.getShooter();
            Hero hero = plugin.getCharacterManager().getHero(player);

            long duration = SkillConfigManager.getUseSetting(hero, skill, "silence-duration", 2000, false);
            SilenceEffect sEffect = new SilenceEffect(skill, player, duration);
            LivingEntity target = (LivingEntity) event.getEntity();
            plugin.getCharacterManager().getCharacter(target).addEffect(sEffect);
            playParticleEffect(target);
        }

        private void playParticleEffect(LivingEntity target) {

            Location location = target.getEyeLocation().clone();
            VisualEffect.playInstantFirework(FireworkEffect.builder()
                    .flicker(true)
                    .trail(false)
                    .with(FireworkEffect.Type.BURST)
                    .withColor(Color.BLACK)
                    .withFade(Color.WHITE)
                    .build(), location.add(0, 1.0, 0));

            target.getWorld().playSound(location, Sound.ENTITY_GHAST_DEATH, 0.15f, 0.0001f);
        }

    }
}