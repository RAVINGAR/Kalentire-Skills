package com.herocraftonline.heroes.characters.skill.remastered.samurai;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.CustomNameManager;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.effects.PeriodicEffect;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import ru.xezard.glow.data.glow.Glow;
import ru.xezard.glow.data.glow.IGlow;
import ru.xezard.glow.data.glow.manager.GlowsManager;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

/**
 * Created By MysticMight May 23 2021
 */

public class SkillArtOfWar extends PassiveSkill {

    private static final String highlightedTargetEffectName = "ArtOfWarHighlightedTarget";
    private boolean glowApiLoaded;
    private String newTargetText;

    public SkillArtOfWar(Heroes plugin) {
        super(plugin, "ArtOfWar");
        setDescription("Every $1 seconds, randomly mark a target in $2 radius for $3% increased melee damage");
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.BUFFING, SkillType.AREA_OF_EFFECT);

        if (Bukkit.getServer().getPluginManager().getPlugin("XGlow") != null) {
            glowApiLoaded = true;
        } else {
            Heroes.debugLog(Level.INFO, "Skill ArtOfWar: XGlow API isn't loaded, add it or targets won't glow");
        }
        Bukkit.getServer().getPluginManager().registerEvents(new SkillHeroListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        double damageMultiplier = SkillConfigManager.getScaledUseSettingDouble(hero, this, "damage-multiplier", false);
        double hRadius = SkillConfigManager.getUseSettingDouble(hero, this, "horizontal-radius", false);
        long period = SkillConfigManager.getUseSettingInt(hero, this, SkillSetting.PERIOD, false);

        return getDescription()
                .replace("$1", Util.decFormat.format(period/1000.0))
                .replace("$2", Util.decFormat.format(hRadius))
                .replace("$3", Util.decFormat.format(100 * (damageMultiplier - 1)));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        //config.set(SkillSetting.DURATION.node(), 120000);
        config.set(SkillSetting.PERIOD.node(), 5000);
        config.set("horizontal-radius", 8.0);
        config.set("vertical-radius", 2.0);
        config.set("damage-multiplier", 1.05);
        config.set("damage-multiplier-per-level", 0.0);
        config.set("preference-players", false);
        config.set("new-target-text", ChatComponents.GENERIC_SKILL + "You have a new bonus target: %target%");
        config.set(SkillSetting.APPLY_TEXT.node(), "");
        config.set(SkillSetting.UNAPPLY_TEXT.node(), "");
        return config;
    }

    @Override
    public void init() {
        super.init();

        newTargetText = SkillConfigManager.getRaw(this, "new-target-text",
                ChatComponents.GENERIC_SKILL + "You have a new bonus target: %target%");
    }

    @Override
    public void apply(Hero hero) {
        // Note we don't want the default passive effect, we're making our own with a custom constructor
        double damageMultiplier = SkillConfigManager.getScaledUseSettingDouble(hero, this, "damage-multiplier", false);
        double hRadius = SkillConfigManager.getUseSettingDouble(hero, this, "horizontal-radius", false);
        double vRadius = SkillConfigManager.getUseSettingDouble(hero, this, "vertical-radius", false);
        long period = SkillConfigManager.getUseSettingInt(hero, this, SkillSetting.PERIOD, false);
        boolean preferencePlayers = SkillConfigManager.getUseSetting(hero, this, "preference-players", false);
        hero.addEffect(new ArtOfWarPeriodicPassiveEffect(this, hero.getPlayer(), period, damageMultiplier, hRadius, vRadius, preferencePlayers));
    }

    public class SkillHeroListener implements Listener {
        private final PassiveSkill skill;

        public SkillHeroListener(PassiveSkill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onWeaponDamage(WeaponDamageEvent event) {
            if (event.getDamage() == 0 || !(event.getEntity() instanceof LivingEntity) || !(event.getDamager() instanceof Hero))
                return;

            // Check attacked mob was the highlighted target
            LivingEntity entity = (LivingEntity) event.getEntity();
            final CharacterTemplate character = plugin.getCharacterManager().getCharacter(entity);
            if (!character.hasEffect(highlightedTargetEffectName))
                return;

            // Check attacker has this passive
            final Hero attackerHero = (Hero) event.getDamager();
            if (!attackerHero.hasEffect(skill.getName()))
                return;

            final ArtOfWarPeriodicPassiveEffect passiveEffect = (ArtOfWarPeriodicPassiveEffect) attackerHero.getEffect(skill.getName());
            assert passiveEffect != null;
            event.setDamage(event.getDamage() * passiveEffect.getDamageMultiplier());
        }
    }

    public class ArtOfWarPeriodicPassiveEffect extends PeriodicEffect {
        private final double damageMultiplier;
        private final double hRadius;
        private final double vRadius;
        private final boolean preferencePlayers;

        protected ArtOfWarPeriodicPassiveEffect(Skill skill, Player applier, long period, double damageMultiplier,
                                                double hRadius, double vRadius, boolean preferencePlayers) {
            super(skill, skill.getName(), applier, period);
            types.add(EffectType.INTERNAL);
            setPersistent(true);

            this.damageMultiplier = damageMultiplier;
            this.hRadius = hRadius;
            this.vRadius = vRadius;
            this.preferencePlayers = preferencePlayers;
        }

        public double getDamageMultiplier() {
            return damageMultiplier;
        }

        @Override
        public void tickHero(Hero hero) {
            final Player player = hero.getPlayer();
            final LivingEntity newTarget = getNewTarget(hero, hRadius, vRadius, preferencePlayers);
            if (newTarget == null)
                return; // No valid target in range to select, wait till next tick

            CharacterTemplate tCharacter = plugin.getCharacterManager().getCharacter(newTarget);
            tCharacter.addEffect(new HighlightedTargetEffect(skill, player, getPeriod()));
            player.sendMessage("    " + newTargetText.replace("%target%", CustomNameManager.getCustomName(newTarget)));
        }
    }

    /**
     * Get new random attackable target in range of the hero
     * @param hero hero whom we are finding a target for. Will be used for center location and damage checks
     * @param hRadius horizontal radius (x and z)
     * @param vRadius vertical radius (y)
     * @return
     */
    @Nullable
    private LivingEntity getNewTarget(Hero hero, double hRadius, double vRadius, boolean preferencePlayers) {
        final Player player = hero.getPlayer();

        // Gather list of valid damageable targets
        List<LivingEntity> validTargets = new ArrayList<>();
        List<Player> validPlayerTargets = new ArrayList<>();
        for (Entity nearbyEntity : player.getNearbyEntities(hRadius, vRadius, hRadius)) {
            if (!(nearbyEntity instanceof LivingEntity))
                continue;
            LivingEntity target = (LivingEntity) nearbyEntity;
            if (hero.isAlliedTo(target) || !damageCheck(player, target))
                continue; // Skip Allies and those that cannot be harmed

            validTargets.add((LivingEntity) nearbyEntity);
            if (target instanceof Player)
                validPlayerTargets.add((Player) target);
        }

        // Handle trivial cases
        int validCount = validTargets.size();
        if (validCount == 0) {
            return null;
        } else if (validCount == 1) {
            return validTargets.get(0); // doesn't matter if its a player, there's only one valid target either way
        }

        // Preference Players over other targets (mobs)
        if (preferencePlayers){
            int validPlayersCount = validPlayerTargets.size();
            if (validPlayersCount == 1) {
                return validPlayerTargets.get(0);
            } else if (validPlayersCount > 1) {
                // Randomly choose a player
                // may grab indexes from 0 to validPlayerCount-1, e.g. for count = 2: 0 and 1
                return validPlayerTargets.get(Util.nextInt(validPlayersCount));
            }
            // Else if no valid players continue to other valid targets
        }

        // Randomly choose a target
        // may grab indexes from 0 to validCount-1, e.g. for count = 2: 0 and 1
        return validTargets.get(Util.nextInt(validCount));
    }

    public class HighlightedTargetEffect extends ExpirableEffect {

        public HighlightedTargetEffect(Skill skill, Player applier, long duration) {
            super(skill, highlightedTargetEffectName, applier, duration, null, null);
            addEffectTypes(EffectType.SILENT_ACTIONS, EffectType.LIGHT);
        }

        @Override
        public void apply(CharacterTemplate character) {
            super.apply(character);

            if (glowApiLoaded) {
                final Glow glow = Glow.builder()
                        .animatedColor(ChatColor.WHITE)
                        .name("ArtOfWarTargetGlow")
                        .build();
                glow.addHolders(character.getEntity()); // apply glow to target
                glow.display(applier); // set applier as a viewer
            }
        }

        @Override
        public void remove(CharacterTemplate character) {
            super.remove(character);

            if (glowApiLoaded) {
                final Optional<IGlow> glowFromEntity = GlowsManager.getInstance().getGlowByEntity(character.getEntity());
                // if has glow, lets turn it off
                glowFromEntity.ifPresent(IGlow::destroy);
            }
        }
    }

}
