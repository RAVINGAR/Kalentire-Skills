package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.SkillDamageEvent;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SkillFrenzy extends ActiveSkill {
    private String applyText;
    private String expireText;

    public SkillFrenzy(final Heroes plugin) {
        super(plugin, "Frenzy");
        setDescription("Enter a crazed Frenzy for $1 second(s). While Frenzied, you deal $2% more damage and shrug off disabling effects every $3 second(s). However, during the effect, you also take $4% more damage from all sources and suffer from severe nausea.");
        setUsage("/skill frenzy");
        setArgumentRange(0, 0);
        setIdentifiers("skill frenzy");
        setTypes(SkillType.BUFFING, SkillType.ABILITY_PROPERTY_PHYSICAL);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillHeroListener(this), plugin);
    }

    @Override
    public String getDescription(final Hero hero) {
        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 7000, false);
        final int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 500, false);

        final double incomingDamageIncrease = SkillConfigManager.getUseSetting(hero, this, "incoming-damage-increase", 0.5, false);
        final double outgoingDamageIncrease = SkillConfigManager.getUseSetting(hero, this, "outgoing-damage-increase", 0.5, false);

        final String formattedPeriod = Util.decFormat.format(period / 1000.0);
        final String formattedDuration = Util.decFormat.format(duration / 1000.0);
        final String formattedOutgoingDamageIncrease = Util.decFormat.format(outgoingDamageIncrease * 100);
        final String formattedIncomingDamageIncrease = Util.decFormat.format(incomingDamageIncrease * 100);

        return getDescription().replace("$1", formattedDuration).replace("$2", formattedOutgoingDamageIncrease).replace("$3", formattedPeriod).replace("$4", formattedIncomingDamageIncrease);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection conbfig = super.getDefaultConfig();
        conbfig.set("nausea-amplifier", 3);
        conbfig.set("outgoing-damage-increase", 0.15);
        conbfig.set("incoming-damage-increase", 0.25);
        conbfig.set(SkillSetting.DURATION.node(), 8000);
        conbfig.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% has entered a frenzy!");
        conbfig.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% is no longer in a frenzy!");
        return conbfig;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%hero% has entered a frenzy!").replace("%hero%", "$1").replace("$hero$", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%hero% is no longer in a frenzy!").replace("%hero%", "$1").replace("$hero$", "$1");
    }

    @Override
    public SkillResult use(final Hero hero, final String[] args) {
        final Player player = hero.getPlayer();

        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 7000, false);
        final int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 500, false);

        final double incomingDamageIncrease = SkillConfigManager.getUseSetting(hero, this, "incoming-damage-increase", 0.5, false);
        final double outgoingDamageIncrease = SkillConfigManager.getUseSetting(hero, this, "outgoing-damage-increase", 0.5, false);

        final int nauseaAmplifier = SkillConfigManager.getUseSetting(hero, this, "nausea-amplifier", 3, false);

        broadcastExecuteText(hero);

        hero.addEffect(new FrenzyEffect(this, player, period, duration, incomingDamageIncrease, outgoingDamageIncrease, nauseaAmplifier));

        return SkillResult.NORMAL;
    }

    public class SkillHeroListener implements Listener {

        private final Skill skill;

        public SkillHeroListener(final Skill skill) {

            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onSkillDamage(final SkillDamageEvent event) {

            // Handle outgoing
            final CharacterTemplate attackerCT = event.getDamager();
            if (attackerCT.hasEffect("Frenzy")) {
                final FrenzyEffect fEffect = (FrenzyEffect) attackerCT.getEffect("Frenzy");

                final double damageIncreasePercent = 1 + fEffect.getOutgoingDamageIncrease();
                final double newDamage = damageIncreasePercent * event.getDamage();
                event.setDamage(newDamage);
            }

            // Handle incoming
            final CharacterTemplate defenderCT = plugin.getCharacterManager().getCharacter((LivingEntity) event.getEntity());
            if (defenderCT.hasEffect("Frenzy")) {
                final FrenzyEffect fEffect = (FrenzyEffect) defenderCT.getEffect("Frenzy");

                final double damageIncreasePercent = 1 + fEffect.getIncomingDamageIncrease();
                final double newDamage = damageIncreasePercent * event.getDamage();
                event.setDamage(newDamage);
            }
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onWeaponDamage(final WeaponDamageEvent event) {

            // Handle outgoing
            final CharacterTemplate attackerCT = event.getDamager();
            if (attackerCT.hasEffect("Frenzy")) {
                final FrenzyEffect fEffect = (FrenzyEffect) attackerCT.getEffect("Frenzy");

                final double damageIncreasePercent = 1 + fEffect.getOutgoingDamageIncrease();
                final double newDamage = damageIncreasePercent * event.getDamage();
                event.setDamage(newDamage);
            }

            // Handle incoming
            final CharacterTemplate defenderCT = plugin.getCharacterManager().getCharacter((LivingEntity) event.getEntity());
            if (defenderCT.hasEffect("Frenzy")) {
                final FrenzyEffect fEffect = (FrenzyEffect) defenderCT.getEffect("Frenzy");

                final double damageIncreasePercent = 1 + fEffect.getIncomingDamageIncrease();
                final double newDamage = damageIncreasePercent * event.getDamage();
                event.setDamage(newDamage);
            }
        }
    }

    public class FrenzyEffect extends PeriodicExpirableEffect {
        private double incomingDamageIncrease;
        private double outgoingDamageIncrease;

        public FrenzyEffect(final Skill skill, final Player applier, final long period, final long duration, final double incomingDamageIncrease, final double outgoingDamageIncrease, final int nauseaAmplifier) {
            super(skill, "Frenzy", applier, period, duration, applyText, expireText);

            types.add(EffectType.PHYSICAL);
            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.NAUSEA);

            this.incomingDamageIncrease = incomingDamageIncrease;
            this.outgoingDamageIncrease = outgoingDamageIncrease;

            addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, (int) (((duration + 4000) / 1000) * 20), nauseaAmplifier), false);
        }

        @Override
        public void tickHero(final Hero hero) {
            removeDisables(hero);
        }

        @Override
        public void tickMonster(final Monster monster) {
        }

        private void removeDisables(final Hero hero) {
            for (final Effect effect : hero.getEffects()) {
                if (effect.isType(EffectType.HARMFUL)) {
                    if (effect.isType(EffectType.DISABLE) || effect.isType(EffectType.SLOW) ||
                            effect.isType(EffectType.VELOCITY_DECREASING) || effect.isType(EffectType.WALK_SPEED_DECREASING) ||
                            effect.isType(EffectType.STUN) || effect.isType(EffectType.ROOT)) {

                        hero.removeEffect(effect);
                    }
                }
            }
        }

        public double getIncomingDamageIncrease() {
            return incomingDamageIncrease;
        }

        public void setIncomingDamageIncrease(final double incomingDamageIncrease) {
            this.incomingDamageIncrease = incomingDamageIncrease;
        }

        public double getOutgoingDamageIncrease() {
            return outgoingDamageIncrease;
        }

        public void setOutgoingDamageIncrease(final double outgoingDamageIncrease) {
            this.outgoingDamageIncrease = outgoingDamageIncrease;
        }
    }
}
