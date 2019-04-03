package com.herocraftonline.heroes.characters.skill.reborn.chronomancer;

import com.google.common.collect.EvictingQueue;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.*;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class SkillRewind extends ActiveSkill implements Passive {

    private final String trackerEffectName = "RewindTracker";

    public SkillRewind(Heroes plugin) {
        super(plugin, "Rewind");
        setDescription("You rewind in $1 second(s) in time, returning your stats and location to what they were back then.");
        setUsage("/skill rewind");
        setArgumentRange(0, 0);
        setIdentifiers("skill rewind");
        setTypes(SkillType.BUFFING, SkillType.ABILITY_PROPERTY_TEMPORAL, SkillType.SILENCEABLE);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillListener(), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        long rewindDuration = SkillConfigManager.getUseSetting(hero, this, "rewind-duration", 4000, false);

        return getDescription()
                .replace("$1", Util.decFormat.format((double) rewindDuration / 1000.0));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set("rewind-duration", 4000);
        config.set("record-period", 250);
        return config;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        if (!hero.hasEffect(trackerEffectName)) {
            return SkillResult.INVALID_TARGET;
        }

        RewindTrackerEffect effect = (RewindTrackerEffect) hero.getEffect(trackerEffectName);
        if (effect == null) {
            return SkillResult.INVALID_TARGET;
        }

        SavedPlayerState rewoundState = effect.stateQueue.peek();
        if (rewoundState == null) {
            return SkillResult.INVALID_TARGET;
        }

        int healthCost = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALTH_COST, 0, false);
        int stamCost = SkillConfigManager.getUseSetting(hero, this, SkillSetting.STAMINA, 0, false);
        int manaCost = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MANA, 0, false);

        playTeleportEffect(player.getLocation().clone());

        hero.setMana(rewoundState.previousMana - manaCost);
        hero.setStamina(rewoundState.previousStamina - stamCost);
        player.setHealth(rewoundState.previousHealth - healthCost);
        player.setFallDistance(rewoundState.fallDistance);
        player.teleport(rewoundState.previousLocation, PlayerTeleportEvent.TeleportCause.PLUGIN);

        effect.stateQueue.clear();

        playTeleportEffect(player.getLocation().clone());

        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

    private void playTeleportEffect(Location preTeleportLoc) {
        preTeleportLoc.getWorld().playEffect(preTeleportLoc, Effect.ENDER_SIGNAL, 3);
        preTeleportLoc.getWorld().playSound(preTeleportLoc, Sound.BLOCK_BEACON_ACTIVATE, 2.0F, 0.7F);
    }

    private class RewindTrackerEffect extends PeriodicEffect {

        final EvictingQueue<SavedPlayerState> stateQueue;

        RewindTrackerEffect(Skill skill, int period, int rewindDuration) {
            super(skill, trackerEffectName, period);

            types.add(EffectType.INTERNAL);

            stateQueue = EvictingQueue.create(rewindDuration / period);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);

            storeCurrentState(hero);
        }

        @Override
        public void tickHero(Hero hero) {
            storeCurrentState(hero);
        }

        private void storeCurrentState(Hero hero) {
            Player player = hero.getPlayer();

            SavedPlayerState currentState = new SavedPlayerState();
            currentState.previousLocation = player.getLocation().clone();
            currentState.fallDistance = player.getFallDistance();
            currentState.previousHealth = player.getHealth();
            currentState.previousMana = hero.getMana();
            currentState.previousStamina = hero.getStamina();

            stateQueue.add(currentState);
        }

        @Override
        public void tickMonster(Monster monster) {}
    }

    private class SavedPlayerState {
        public double previousHealth;
        public int previousMana;
        public int previousStamina;
        public float fallDistance;
        public Location previousLocation;
    }

    @Override
    public void tryApplying(Hero hero) {
        Player player = hero.getPlayer();
        if (hero.canUseSkill(this)) {
            hero.removeEffect(hero.getEffect(trackerEffectName));
            this.apply(hero);
        } else {
            this.unapply(hero);
        }
    }

    @Override
    public void apply(Hero hero) {
        int rewindDuration = SkillConfigManager.getUseSetting(hero, this, "rewind-duration", 4000, false);
        int recordPeriod = SkillConfigManager.getUseSetting(hero, this, "record-period", 250, false);
        RewindTrackerEffect effect = new RewindTrackerEffect(this, recordPeriod, rewindDuration);
        effect.setPersistent(true);
        hero.addEffect(effect);
    }

    @Override
    public void unapply(Hero hero) {
        hero.removeEffect(hero.getEffect(trackerEffectName));
    }

    public class SkillListener implements Listener {

        @EventHandler(priority = EventPriority.MONITOR)
        public void onAfterClassChange(AfterClassChangeEvent event) {
            tryApplying(event.getHero());
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onHeroChangeLevel(HeroChangeLevelEvent event) {
            tryApplying(event.getHero());
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onSkillLearnEvent(SkillLearnEvent event) {
            tryApplying(event.getHero());
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onSkillUnlearnEvent(SkillUnlearnEvent event) {
            tryApplying(event.getHero());
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onHeroSkillPrepare(SkillPrepareEvent event) {
            tryApplying(event.getHero());
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onHeroSkillUnprepare(SkillUnprepareEvent event) {
            tryApplying(event.getHero());
        }
    }
}
