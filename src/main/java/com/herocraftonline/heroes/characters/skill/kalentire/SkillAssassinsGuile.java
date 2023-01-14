package com.herocraftonline.heroes.characters.skill.kalentire;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.events.HeroesDamageEvent;
import com.herocraftonline.heroes.api.events.ProjectileDamageEvent;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectStack;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.effects.StackingEffect;
import com.herocraftonline.heroes.characters.skill.Listenable;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.logging.Level;

public class SkillAssassinsGuile extends PassiveSkill implements Listenable {
    public static final String EFFECT_NAME = "AssassinsPreparation";
    private final Listener listener;

    private String expireText = "§7Your preparations have expired!";
    private int maxStacks = 3;

    public SkillAssassinsGuile(Heroes plugin) {
        super(plugin, "AssassinsGuile");
        setDescription("Preparing a poison, bleed or slow creates a preparation stack. This means your next physical attack" +
                "of any kind will apply the last prepared effect to the hit target and will consume the stack. You can stack" +
                "preparations up to $1 times.");
        setEffectTypes(EffectType.BENEFICIAL, EffectType.PHYSICAL);
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL);
        listener = new AssassinsGuileListener(this);
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription().replace("$1", "" + SkillConfigManager.getScaledUseSettingInt(hero, this, "max-stacks", true));
    }

    @Override
    public void init() {
        super.init();
        this.expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, expireText);
        this.maxStacks = Integer.parseInt(SkillConfigManager.getRaw(this, "max-stacks", "" + maxStacks));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set("max-stacks", 3);
        node.set(SkillSetting.EXPIRE_TEXT.node(), "§7Your preparations have expired!");

        return node;
    }

    @NotNull
    @Override
    public Listener getListener() {
        return listener;
    }

    public static class AssassinEffect extends StackingEffect {
        private final ConcurrentLinkedDeque<ExpirableEffect> stackedEffects;
        public AssassinEffect(Skill skill, Player applier, int maxStacks, String applyText, String expireText) {
            super(skill, EFFECT_NAME, applier, maxStacks, true, applyText, expireText);
            this.stackedEffects = new ConcurrentLinkedDeque<>();
        }

        @Override
        public int removeStacks(int amount) {
            int removed = super.removeStacks(amount);
            for(int i = 0; i < removed && i < stackedEffects.size(); i++) {
                stackedEffects.pop();
            }
            return removed;
        }

        @Override
        public int removeAllStacks() {
            int removed = super.removeAllStacks();
            stackedEffects.clear();
            return removed;
        }

        /**
         * Gets the last added effect which may or may not be null. Then removes 1 stack from this effect
         * @return The last stack (nullable)
         */
        @Nullable
        public Effect consumeLastEffect() {
            ExpirableEffect effect = stackedEffects.pollFirst();
            if(effect != null) {
                // If the effect is dispellable, then its duration can be modified!
                EffectStack.Entry entry = AssassinEffect.this.effectStack.getLast();
                if(entry != null) {
                    effect.setDuration(entry.getRemainingTime());
                }
            }
            removeStack();
            return effect;
        }
    }

    public class AssassinsGuileListener implements Listener {
        private final SkillAssassinsGuile skill;
        public AssassinsGuileListener(SkillAssassinsGuile skill) {
            this.skill = skill;
        }

        @EventHandler
        public void onQueueEffect(EffectPreparationEvent event) {
            final Hero applier = event.applier;
            final long duration = event.duration;

            int i = applier.addEffectStacks(EFFECT_NAME, skill, applier.getPlayer(), duration, 1,
                    () -> new AssassinEffect(skill, applier.getPlayer(), maxStacks, "", expireText));
            if(i > 0) {
                applier.getPlayer().sendMessage(event.applyText);
                AssassinEffect effect = (AssassinEffect) applier.getEffect(EFFECT_NAME);
                if(effect == null) {
                    Heroes.log(Level.WARNING, "Could not add assassin effect as effect was not added when it should have been!");
                    return;
                }
                effect.stackedEffects.push(event.effect);
            }
            else {
                applier.getPlayer().sendMessage("§7You cannot prepare anything more!");
            }
        }

        @EventHandler
        public void onWeaponHit(WeaponDamageEvent event) {
            handleDamageEvent(event);
        }

        @EventHandler
        public void onProjectileHit(ProjectileDamageEvent event) {
            handleDamageEvent(event);
        }

        private void handleDamageEvent(HeroesDamageEvent event) {
            if (event.getAttacker() instanceof Hero) {
                Hero hero = (Hero) event.getAttacker();
                AssassinEffect effect = (AssassinEffect) hero.getEffect(EFFECT_NAME);
                Effect toApply = effect == null ? null : effect.consumeLastEffect();
                if (toApply != null) {
                    event.getDefender().addEffect(toApply);
                }
            }
        }
    }

    protected static class EffectPreparationEvent extends Event {
        protected static final HandlerList handlers = new HandlerList();

        private final Hero applier;
        private final ExpirableEffect effect;
        private final long duration;
        private final String applyText;

        public EffectPreparationEvent(Hero applier, ExpirableEffect wrappedEffect, long duration, String applyText) {
            this.applier = applier;
            this.effect = wrappedEffect;
            this.duration = duration;
            this.applyText = applyText;
        }

        @Override
        public HandlerList getHandlers() {
            return handlers;
        }

        public static HandlerList getHandlerList() {
            return handlers;
        }
    }
}
