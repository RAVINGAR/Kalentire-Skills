package com.herocraftonline.heroes.characters.skill.reborn.necromancer;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.effects.PeriodicEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.CylinderEffect;
import de.slikey.effectlib.util.DynamicLocation;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SkillUnholyRitual extends ActiveSkill {
    private static Color DARK_GRAY = Color.fromRGB(105, 105, 105);

    private static String effectName = "UnholyRitualing";
    private static String minionRitualEffectName = "UnholyRitual-Minion";

    private String applyText;
    private String expireText;

    public SkillUnholyRitual(Heroes plugin) {
        super(plugin, "UnholyRitual");
        setDescription("Channeling: Imbue your summoned minion(s) with power, " + 
                "granting them increased movement speeed, a $1% damage increase, and $2% damage mitigation. " +
                "This ability costs you greatly however, and will drain $3 mana and $4 stamina every $5 seconds. " +
                "Maximum channel time is $6 seconds.");
        setUsage("/skill unholyritual");
        setIdentifiers("skill unholyritual");
        setArgumentRange(0, 0);
        setTypes(SkillType.ABILITY_PROPERTY_DARK, SkillType.BUFFING);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DELAY, 4000, false);
        int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 1000, false);

        double minionDamageBoost = SkillConfigManager.getUseSetting(hero, this, "minion-damage-percent-increase", 0.75, false);
        double minionDamageMitigation = SkillConfigManager.getUseSetting(hero, skill, "minion-damage-mitigation-percent", 0.6, false);
        int manaDrain = SkillConfigManager.getUseSetting(hero, this, "mana-drain-per-tick", 30, false);
        int staminaDrain = SkillConfigManager.getUseSetting(hero, this, "stamina-drain-per-tick", 25, false);

        return getDescription()
                .replace("$1", Util.decFormat.format(minionDamageBoost * 100))
                .replace("$2", Util.decFormat.format(minionDamageMitigation * 100))
                .replace("$3", manaDrain + "")
                .replace("$4", staminaDrain + "")
                .replace("$5", Util.decFormat.format(period / 1000.0));
                .replace("$6", Util.decFormat.format(duration / 1000.0));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.PERIOD.node(), 1000);
        config.set(SkillSetting.DELAY.node(), 10000);
        config.set("mana-drain-per-tick", 30);
        config.set("stamina-drain-per-tick", 25);
        config.set("minion-speed-amplifier", 2);
        config.set("minion-damage-percent-increase", 0.75);
        config.set("minion-damage-mitigation-percent", 0.6);
        config.set(SkillSetting.INTERRUPT_TEXT.node(), "");
        config.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% is empowering their minion(s)!");
        config.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% is no longer empowering their minion(s).");
        return config;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT,
                ChatComponents.GENERIC_SKILL + "%hero% is empowering their minion(s)!")
                .replace("%hero%", "$1");

        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT,
                ChatComponents.GENERIC_SKILL + "%hero% is no longer empowering their minion(s).")
                .replace("%hero%", "$1");
    }

    @Override
    public void onWarmup(Hero hero) {
        super.onWarmup(hero);
        Player player = hero.getPlayer();
        if (hero.getSummons().isEmpty()) {
            player.sendMessage("    " + ChatComponents.GENERIC_SKILL + "You don't have any active summons to empower!");
            return;
        }

        hero.addEffect(new ChannelingEffect(this, player));
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        hero.removeEffect(hero.getEffect(effectName));
        return SkillResult.NORMAL;
    }

    class ChannelingEffect extends PeriodicEffect {
        ChannelingEffect(Skill skill, Player applier) {
            super(skill, "Channeling-" + skill.getName(), applier, 100, null, null);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);

            int regainPeriod = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.PERIOD, 1000, false);
            hero.addEffect(new RitualPulseEffect(skill, applier, regainPeriod));
        }

        @Override
        public void tickHero(Hero hero) {
            super.tickHero(hero);

            if (hero.getDelayedSkill() != null && hero.getDelayedSkill().getSkill().equals(skill))
                return;

            // We were interrupted or finished casting.
            hero.removeEffect(this);
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);

            long cooldownMillis = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.COOLDOWN, 1000, false);
            long currentTime = System.currentTimeMillis();

            Long currentCd = hero.getCooldown(skill.getName());
            long defaultCd = currentTime + cooldownMillis;
            if (currentCd == null || currentCd < defaultCd) {
                hero.setCooldown(skill.getName(), System.currentTimeMillis() + cooldownMillis);
            }

            hero.removeEffect(hero.getEffect(effectName));
        }
    }

    private class SkillListener implements Listener {
        private final Skill skill;

        SkillListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
        public void onEntityDamage(EntityDamageByEntityEvent event) {
            if (event.getDamage() <= 0 || !(event.getDamager() instanceof LivingEntity) || !(event.getEntity() instanceof LivingEntity))
                return;

            // Handle summon outgoing damage boost
            if (!(event.getDamager() instanceof Player)) {
                Monster attackerCT = plugin.getCharacterManager().getMonster((LivingEntity) event.getDamager());
                if (attackerCT.hasEffect(minionRitualEffectName)) {
                    RitualMinionEffect effect = (RitualMinionEffect) attackerMonster.getEffect(minionRitualEffectName);
                    event.setDamage(event.getDamage() * (1 + effect.getPercentDamageIncrease()));
                }
            }

            // Handle summon incoming damage mitigation
            if (!(event.getEntity() instanceof Player)) {
                CharacterTemplate defenderCT = plugin.getCharacterManager().getCharacter((LivingEntity) event.getEntity());
                if (defenderCT.hasEffect(minionRitualEffectName)) {
                    RitualMinionEffect effect = (RitualMinionEffect) defenderCT.getEffect(minionRitualEffectName);
                    event.setDamage(event.getDamage() * (1.0 - effect.getPercentDamageMitigation()));
                }
            }
        }
    }

    class RitualPulseEffect extends PeriodicEffect {
        private EffectManager effectManager;
        private CylinderEffect visualEffect;

        private double regenMultiplier;
        private double perSecondModifier;
        private int minionSpeedAmplifier;
        private double minionDamageBoost;
        private double minionDamageMitigation;
        private int manaDrain;
        private int staminaDrain;

        RitualPulseEffect(Skill skill, Player applier, long pulsePeriod) {
            super(skill, effectName, applier, pulsePeriod, applyText, expireText);

            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.DARK);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            Player player = hero.getPlayer();

            this.minionSpeedAmplifier = SkillConfigManager.getUseSetting(hero, skill, "minion-speed-amplifier", 2, false);
            this.minionDamageBoost = SkillConfigManager.getUseSetting(hero, skill, "minion-damage-percent-increase", 0.75, false);
            this.minionDamageMitigation = = SkillConfigManager.getUseSetting(hero, skill, "minion-damage-mitigation-percent", 0.6, false);
            this.manaDrain = SkillConfigManager.getUseSetting(hero, skill, "mana-drain-per-tick", 30, false);
            this.staminaDrain = SkillConfigManager.getUseSetting(hero, skill, "stamina-drain-per-tick", 25, false);

            applyVisualsEffects(player);
        }

        @Override
        public void tickHero(Hero hero) {
            for (Monster summon : hero.getSummons()) {
                summon.addEffect(new RitualMinionEffect(skill, applier, (long) (getPeriod() * 1.5),
                    minionSpeedAmplifier, minionDamageBoost, minionDamageMitigation));
            }

            int newMana = hero.getMana() - this.manaDrain;
            int newStamina = hero.getStamina() - staminaDrain;
            if (newMana < 1 || newStamina < 1) {
                hero.removeEffect(this);
            } else {
                Player player = hero.getPlayer();

                hero.setMana(newMana);
                if (hero.isVerboseMana())
                    player.sendMessage(ChatComponents.Bars.mana(newMana, hero.getMaxMana(), true));

                hero.setStamina(newStamina);
                if (hero.isVerboseStamina())
                    player.sendMessage(ChatComponents.Bars.mana(newMana, hero.getMaxMana(), true));
            }
        }

        private void applyVisualsEffects(LivingEntity target) {
            final World world = target.getWorld();
            final Location loc = target.getLocation();
            final int durationTicks = 5 * 60 * 20;
            final int displayPeriod = 2;

            this.effectManager = new EffectManager(plugin);
            this.visualEffect = new CylinderEffect(effectManager);

            DynamicLocation dynamicLoc = new DynamicLocation(target);
            visualEffect.setDynamicOrigin(dynamicLoc);
            visualEffect.disappearWithOriginEntity = true;
            visualEffect.particle = Particle.REDSTONE;
            visualEffect.color = DARK_GRAY;
            //visualEffect.particles = 50;
            visualEffect.radius = 1.75F;
            visualEffect.height = 3.0F;
            visualEffect.solid = false;
            visualEffect.enableRotation = true;
            visualEffect.angularVelocityX = 0;
            visualEffect.angularVelocityY = 2.5;
            visualEffect.angularVelocityZ = 0;
            visualEffect.period = displayPeriod;
            visualEffect.iterations = durationTicks / displayPeriod;

            effectManager.start(visualEffect);
            effectManager.disposeOnTermination();
        }

        @Override
        public void tickMonster(Monster monster) { }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);

            if (this.effectManager != null)
                this.effectManager.dispose();
        }
    }

    class RitualMinionEffect extends ExpirableEffect {
        private final double percentDamageIncrease;
        private final double percentDamageMitigation;

        RitualMinionEffect(Skill skill, Player applier, long duration, int speedAmplifier, double percentDamageIncrease, double percentDamageMitigation) {
            super(skill, minionRitualEffectName, applier, duration, null, null);
            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.DARK);

            this.percentDamageIncrease = percentDamageIncrease;
            this.percentDamageMitigation = percentDamageMitigation;
            if (speedAmplifier > -1) {
                addPotionEffect(new PotionEffect(PotionEffectType.SPEED, (int) (duration / 50), speedAmplifier));
            }
        }

        double getPercentDamageIncrease() {
            return percentDamageIncrease;
        }

        double getPercentDamageMitigation() {
            return percentDamageMitigation;
        }
    }
}