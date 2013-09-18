package com.herocraftonline.heroes.characters.skill.skills;

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
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
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
        setDescription("Enter a crazed Frenzy for $1 seconds. While Frenzied, you move much faster, but take $2% more damage from all attacks and suffer from severe nausea.");
        setUsage("/skill frenzy");
        setArgumentRange(0, 0);
        setIdentifiers("skill frenzy");
        setTypes(SkillType.BUFFING, SkillType.ABILITY_PROPERTY_PHYSICAL);
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, Integer.valueOf(7000), false);
        double incomingDamageIncrease = SkillConfigManager.getUseSetting(hero, this, "incoming-damage-increase", Double.valueOf(0.5), false);

        String formattedDuration = Util.decFormat.format(duration / 1000.0);
        String formattedIncomingDamageIncrease = Util.decFormat.format(incomingDamageIncrease * 100);

        return getDescription().replace("$1", formattedDuration).replace("$2", formattedIncomingDamageIncrease);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set("speed-amplifier", Integer.valueOf(2));
        node.set("nausea-amplifier", Integer.valueOf(3));
        node.set("incoming-damage-increase", Double.valueOf(0.5));
        node.set(SkillSetting.DURATION.node(), Integer.valueOf(8000));
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

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, Integer.valueOf(7000), false);
        int speedAmplifier = SkillConfigManager.getUseSetting(hero, this, "speed-amplifier", Integer.valueOf(2), false);
        int nauseaAmplifier = SkillConfigManager.getUseSetting(hero, this, "nausea-amplifier", Integer.valueOf(3), false);
        double incomingDamageIncrease = SkillConfigManager.getUseSetting(hero, this, "incoming-damage-increase", Double.valueOf(0.5), false);

        broadcastExecuteText(hero);

        hero.addEffect(new FrenzyEffect(this, player, duration, incomingDamageIncrease, speedAmplifier, nauseaAmplifier));

        return SkillResult.NORMAL;
    }

    public class SkillHeroListener implements Listener {

        public SkillHeroListener() {}

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onSkillDamage(SkillDamageEvent event) {

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

    public class FrenzyEffect extends ExpirableEffect {
        private double incomingDamageIncrease;

        public FrenzyEffect(Skill skill, Player applier, long duration, double incomingDamageIncrease, int speedAmplifier, int nauseaAmplifier) {
            super(skill, "Frenzy", applier, duration, applyText, expireText);

            types.add(EffectType.PHYSICAL);
            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.SPEED);
            types.add(EffectType.NAUSEA);

            this.incomingDamageIncrease = incomingDamageIncrease;

            addMobEffect(1, (int) ((duration / 1000) * 20), speedAmplifier, false);
            addMobEffect(9, (int) (((duration + 4000) / 1000) * 20), nauseaAmplifier, false);
        }

        public double getIncomingDamageIncrease() {
            return incomingDamageIncrease;
        }

        public void setIncomingDamageIncrease(double incomingDamageIncrease) {
            this.incomingDamageIncrease = incomingDamageIncrease;
        }
    }
}
