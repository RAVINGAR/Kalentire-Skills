package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Listenable;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SkillFeatheredShot extends ActiveSkill implements Listenable {

    private final String shotEffect = "HasFeatheredArrows";
    private final Listener listener;
    private String applyText;
    private String expireText;

    public SkillFeatheredShot(final Heroes plugin) {
        super(plugin, "FeatheredShot");
        setDescription("For the next $1 second(s), your arrows will apply a jump boost and slow fall to slow down your target.");
        setUsage("/skill featheredshot");
        setIdentifiers("skill featheredshot");
        setArgumentRange(0, 0);
        setTypes(SkillType.DEBUFFING, SkillType.BUFFING);

        listener = new SkillDamageListener(this);
    }

    @Override
    public String getDescription(final Hero hero) {
        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
        final double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 5, false);

        final String formattedDamage = Util.decFormat.format(damage);
        final String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription()
                .replace("$1", formattedDuration)
                .replace("$2", formattedDamage);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.DURATION.node(), 10000);
        config.set(SkillSetting.DAMAGE.node(), 15.0);
        config.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 0.0);
        config.set("shot-duration", 4000);
        config.set("slow-fall-amplifier", 3);
        config.set("jump-boost-amplifier", 4);
        config.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% has deadly feathers attached to their bow.");
        config.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero%'s bow no longer has deadly feathers attached to their bow.");
        return config;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT,
                        ChatComponents.GENERIC_SKILL + "%hero% has deadly feathers attached to their bow.")
                .replace("%hero%", "$1").replace("$hero$", "$1");

        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT,
                        ChatComponents.GENERIC_SKILL + "%hero%'s bow no longer has deadly feathers attached to their bow.")
                .replace("%hero%", "$1").replace("$hero$", "$1");
    }

    @Override
    public SkillResult use(final Hero hero, final String[] args) {
        broadcastExecuteText(hero);

        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
        hero.addEffect(new FeatherArrowsEffect(this, hero.getPlayer(), duration));

        return SkillResult.NORMAL;
    }

    @Override
    public Listener getListener() {
        return listener;
    }

    public static class FeatheredEffect extends ExpirableEffect {

        public FeatheredEffect(final Skill skill, final Player applier, final long duration, final int slowFallAmplifier, final int jumpBoostAmplifier) {
            super(skill, "FeatherShotted", applier, duration);

            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.HARMFUL);

            addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, (int) (duration / 1000 * 20), slowFallAmplifier));
            addPotionEffect(new PotionEffect(PotionEffectType.JUMP, (int) (duration / 1000 * 20), jumpBoostAmplifier));
        }
    }

    public class FeatherArrowsEffect extends ExpirableEffect {

        FeatherArrowsEffect(final Skill skill, final Player applier, final long duration) {
            super(skill, shotEffect, applier, duration, applyText, expireText);
            types.add(EffectType.IMBUE);
            types.add(EffectType.BENEFICIAL);
        }

        @Override
        public void applyToHero(final Hero hero) {
            super.applyToHero(hero);

            for (final com.herocraftonline.heroes.characters.effects.Effect effect : hero.getEffects()) {
                if (effect.equals(this)) {
                    continue;
                }

                if (effect.isType(EffectType.IMBUE)) {
                    hero.removeEffect(effect);
                }
            }
        }

        @Override
        public void removeFromHero(final Hero hero) {
            super.removeFromHero(hero);
        }
    }

    public class SkillDamageListener implements Listener {
        private final Skill skill;

        public SkillDamageListener(final Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onEntityDamage(final EntityDamageByEntityEvent event) {
            if (!(event.getEntity() instanceof LivingEntity)) {
                return;
            }

            if (!(event.getDamager() instanceof Arrow)) {
                return;
            }

            final Arrow arrow = (Arrow) event.getDamager();
            if (!(arrow.getShooter() instanceof Player)) {
                return;
            }

            final Player player = (Player) arrow.getShooter();
            final Hero hero = plugin.getCharacterManager().getHero(player);
            if (!hero.hasEffect(shotEffect)) {
                return;
            }

            final CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter((LivingEntity) event.getEntity());
            final int duration = SkillConfigManager.getUseSetting(hero, skill, "shot-duration", 4000, false);
            final int slowFall = SkillConfigManager.getUseSetting(hero, skill, "slow-fall-amplifier", 3, false);
            final int jumpBoost = SkillConfigManager.getUseSetting(hero, skill, "jump-boost-amplifier", 4, false);
            targetCT.addEffect(new FeatheredEffect(skill, player, duration, slowFall, jumpBoost));
        }
    }
}




