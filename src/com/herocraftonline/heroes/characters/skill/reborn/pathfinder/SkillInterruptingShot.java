package com.herocraftonline.heroes.characters.skill.reborn.pathfinder;

import com.google.common.collect.Lists;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.effects.common.SilenceEffect;
import com.herocraftonline.heroes.characters.effects.common.StunEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.characters.skill.ncp.NCPFunction;
import com.herocraftonline.heroes.characters.skill.ncp.NCPUtils;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class SkillInterruptingShot extends ActiveSkill {

    private String applyText;
    private String expireText;

    private Map<Arrow, Long> interruptingShot = new LinkedHashMap<Arrow, Long>(100) {
        private static final long serialVersionUID = 4329526013158603250L;

        @Override
        protected boolean removeEldestEntry(Map.Entry<Arrow, Long> eldest) {
            return (size() > 60 || eldest.getValue() + 5000 <= System.currentTimeMillis());
        }
    };


    public SkillInterruptingShot(Heroes plugin) {
        super(plugin, "InterruptingShot");
        setDescription("For the next $1 seconds, the first target hit will silence your target for $2 seconds.");
        setUsage("/skill InterruptingShot");
        setIdentifiers("skill interruptingshot", "skill is");
        setArgumentRange(0, 0);
        setTypes(SkillType.DEBUFFING, SkillType.DISABLING);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillDamageListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 6000, false);
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
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT,
                ChatComponents.GENERIC_SKILL + "%hero% has interrupting arrows attached to their bow.")
                .replace("%hero%", "$1");

        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT,
                ChatComponents.GENERIC_SKILL + "%hero%'s bow no longer has interrupting arrows attached to their bow.")
                .replace("%hero%", "$1");

    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        final Player player = hero.getPlayer();

        broadcastExecuteText(hero);
        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 6000, false);
        hero.addEffect(new InterruptingShotBuff(this, player, duration));

        return SkillResult.NORMAL;
    }


    public class InterruptingShotBuff extends ExpirableEffect {

        InterruptingShotBuff(Skill skill, Player applier, long duration) {
            super(skill, "InterruptingShotBuff", applier, duration);

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


        @EventHandler(priority = EventPriority.MONITOR)
        public void onEntityShootBow(EntityShootBowEvent event) {
            if (event.isCancelled() || !(event.getEntity() instanceof Player) || !(event.getProjectile() instanceof Arrow)) {
                return;
            }

            Hero hero = plugin.getCharacterManager().getHero((Player) event.getEntity());
            if (hero.hasEffect("InterruptingShotBuff")) {
                interruptingShot.put((Arrow) event.getProjectile(), System.currentTimeMillis());
                hero.removeEffect(hero.getEffect("InterruptingShotBuff"));
            }
        }


        @EventHandler(priority = EventPriority.MONITOR)
        public void onEntityDamage(EntityDamageEvent event) {
            if (event.isCancelled() || !(event instanceof EntityDamageByEntityEvent) || !(event.getEntity() instanceof LivingEntity))
                return;

            EntityDamageByEntityEvent subEvent = (EntityDamageByEntityEvent) event;
            if (!(subEvent.getDamager() instanceof Arrow))
                return;

            Arrow arrow = (Arrow) subEvent.getDamager();
            if (!(arrow.getShooter() instanceof Player) || !interruptingShot.containsKey(arrow))
                return;

            interruptingShot.remove(arrow);

            Player player = (Player) arrow.getShooter();
            Hero hero = plugin.getCharacterManager().getHero(player);

            // Stun the target
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