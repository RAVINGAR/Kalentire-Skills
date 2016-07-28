package com.herocraftonline.heroes.characters.skill.pack2;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

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
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

public class SkillFrenzy extends ActiveSkill {
    private String applyText;
    private String expireText;

    public SkillFrenzy(Heroes plugin) {
        super(plugin, "Frenzy");
        setDescription("Enter a crazed Frenzy for $1 seconds. While Frenzied, you deal $2% more damage and shrug off disabling effects every $3 seconds. However, during the effect, you also take $4% more damage from all sources and suffer from severe nausea.");
        setUsage("/skill frenzy");
        setArgumentRange(0, 0);
        setIdentifiers("skill frenzy");
        setTypes(SkillType.BUFFING, SkillType.ABILITY_PROPERTY_PHYSICAL);
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 7000, false);
        int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 500, false);

        double incomingDamageIncrease = SkillConfigManager.getUseSetting(hero, this, "incoming-damage-increase", 0.5, false);
        double outgoingDamageIncrease = SkillConfigManager.getUseSetting(hero, this, "outgoing-damage-increase", 0.5, false);

        String formattedPeriod = Util.decFormat.format(period / 1000.0);
        String formattedDuration = Util.decFormat.format(duration / 1000.0);
        String formattedOutgoingDamageIncrease = Util.decFormat.format(outgoingDamageIncrease * 100);
        String formattedIncomingDamageIncrease = Util.decFormat.format(incomingDamageIncrease * 100);

        return getDescription().replace("$1", formattedDuration).replace("$2", formattedOutgoingDamageIncrease).replace("$3", formattedPeriod).replace("$4", formattedIncomingDamageIncrease);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set("nausea-amplifier", 3);
        node.set("outgoing-damage-increase", 0.15);
        node.set("incoming-damage-increase", 0.25);
        node.set(SkillSetting.DURATION.node(), 8000);
        node.set(SkillSetting.APPLY_TEXT.node(), Messaging.getSkillDenoter() + "%hero% has entered a frenzy!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), Messaging.getSkillDenoter() + "%hero% is no longer in a frenzy!");

        return node;
    }

    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, Messaging.getSkillDenoter() + "%hero% has entered a frenzy!").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, Messaging.getSkillDenoter() + "%hero% is no longer in a frenzy!").replace("%hero%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 7000, false);
        int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 500, false);

        double incomingDamageIncrease = SkillConfigManager.getUseSetting(hero, this, "incoming-damage-increase", 0.5, false);
        double outgoingDamageIncrease = SkillConfigManager.getUseSetting(hero, this, "outgoing-damage-increase", 0.5, false);

        int nauseaAmplifier = SkillConfigManager.getUseSetting(hero, this, "nausea-amplifier", 3, false);

        broadcastExecuteText(hero);

        hero.addEffect(new FrenzyEffect(this, player, period, duration, incomingDamageIncrease, outgoingDamageIncrease, nauseaAmplifier));

        return SkillResult.NORMAL;
    }

    public class SkillHeroListener implements Listener {

        public SkillHeroListener() {}

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onSkillDamage(SkillDamageEvent event) {

            // Handle outgoing
            CharacterTemplate attackerCT = event.getDamager();
            if (attackerCT.hasEffect("Frenzy")) {
                FrenzyEffect fEffect = (FrenzyEffect) attackerCT.getEffect("Frenzy");

                double damageIncreasePercent = 1 + fEffect.getOutgoingDamageIncrease();
                double newDamage = damageIncreasePercent * event.getDamage();
                event.setDamage(newDamage);
            }

            // Handle incoming
            CharacterTemplate defenderCT = plugin.getCharacterManager().getCharacter((LivingEntity) event.getEntity());
            if (defenderCT.hasEffect("Frenzy")) {
                FrenzyEffect fEffect = (FrenzyEffect) defenderCT.getEffect("Frenzy");

                double damageIncreasePercent = 1 + fEffect.getIncomingDamageIncrease();
                double newDamage = damageIncreasePercent * event.getDamage();
                event.setDamage(newDamage);
            }
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onWeaponDamage(WeaponDamageEvent event) {

            // Handle outgoing
            CharacterTemplate attackerCT = event.getDamager();
            if (attackerCT.hasEffect("Frenzy")) {
                FrenzyEffect fEffect = (FrenzyEffect) attackerCT.getEffect("Frenzy");

                double damageIncreasePercent = 1 + fEffect.getOutgoingDamageIncrease();
                double newDamage = damageIncreasePercent * event.getDamage();
                event.setDamage(newDamage);
            }

            // Handle incoming
            CharacterTemplate defenderCT = plugin.getCharacterManager().getCharacter((LivingEntity) event.getEntity());
            if (defenderCT.hasEffect("Frenzy")) {
                FrenzyEffect fEffect = (FrenzyEffect) defenderCT.getEffect("Frenzy");

                double damageIncreasePercent = 1 + fEffect.getIncomingDamageIncrease();
                double newDamage = damageIncreasePercent * event.getDamage();
                event.setDamage(newDamage);
            }
        }
    }

    public class FrenzyEffect extends PeriodicExpirableEffect {
        private double incomingDamageIncrease;
        private double outgoingDamageIncrease;

        public FrenzyEffect(Skill skill, Player applier, long period, long duration, double incomingDamageIncrease, double outgoingDamageIncrease, int nauseaAmplifier) {
            super(skill, "Frenzy", applier, period, duration, applyText, expireText);

            types.add(EffectType.PHYSICAL);
            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.NAUSEA);

            this.incomingDamageIncrease = incomingDamageIncrease;
            this.outgoingDamageIncrease = outgoingDamageIncrease;

            addMobEffect(9, (int) (((duration + 4000) / 1000) * 20), nauseaAmplifier, false);
        }

        @Override
        public void tickHero(Hero hero) {
            removeDisables(hero);
        }

        @Override
        public void tickMonster(Monster monster) {}

        private void removeDisables(Hero hero) {
            for (Effect effect : hero.getEffects()) {
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

        public void setIncomingDamageIncrease(double incomingDamageIncrease) {
            this.incomingDamageIncrease = incomingDamageIncrease;
        }

        public double getOutgoingDamageIncrease() {
            return outgoingDamageIncrease;
        }

        public void setOutgoingDamageIncrease(double outgoingDamageIncrease) {
            this.outgoingDamageIncrease = outgoingDamageIncrease;
        }
    }
}
