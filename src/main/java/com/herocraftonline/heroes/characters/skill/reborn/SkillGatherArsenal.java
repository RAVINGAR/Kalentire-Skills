package com.herocraftonline.heroes.characters.skill.reborn;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainStaminaEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Color;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.logging.Level;

public class SkillGatherArsenal extends ActiveSkill {
    private static Color MANA_BLUE = Color.fromRGB(0, 191, 255);

    private String applyText;
    private String expireText;
    private String effectName = "GatheringArsenal";

    public SkillGatherArsenal(Heroes plugin) {
        super(plugin, "GatherArsenal");
        setDescription("Channeling: You take a moment to regenerate stamina and prepare a few extra sets of chains. " +
                "Adds $1 extra chain(s) to your belt every $2 second(s) over the next $3 second(s). " +
                "Stamina regeneration is increased by $4% while active. " +
                "You can go over your normal maximum chain belt limit with this skill.");
        setUsage("/skill gatherarsenal");
        setIdentifiers("skill gatherarsenal");
        setArgumentRange(0, 0);
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.STAMINA_INCREASING, SkillType.BUFFING);
    }

    @Override
    public String getDescription(Hero hero) {
        int numChainsPerTick = SkillConfigManager.getUseSetting(hero, this, "num-chains-per-tick", 1, false);
        long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 1000, false);
        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DELAY, 5000, false);
        double regenMultiplier = SkillConfigManager.getUseSetting(hero, this, "regen-multiplier", 4.0, false);

        return getDescription()
                .replace("$1", numChainsPerTick + "")
                .replace("$2", Util.decFormat.format((double) period / 1000))
                .replace("$3", Util.decFormat.format((double) duration / 1000))
                .replace("$4", Util.decFormat.format(regenMultiplier * 100));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set("num-chains-per-tick", 1);
        config.set("regen-multiplier", 4.0);
        config.set(SkillSetting.PERIOD.node(), 1000);
        config.set(SkillSetting.DELAY.node(), 5000);
        config.set(SkillSetting.INTERRUPT_TEXT.node(), "");
        config.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% is preparing their arsenal!");
        config.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% is no longer gathering an arsenal.");
        return config;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT,
                ChatComponents.GENERIC_SKILL + "%hero% is gathering their arsenal!")
                .replace("%hero%", "$1");

        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT,
                ChatComponents.GENERIC_SKILL + "%hero% is no longer gathering an arsenal.")
                .replace("%hero%", "$1");
    }

    @Override
    public void onWarmup(Hero hero) {
        super.onWarmup(hero);
        Player player = hero.getPlayer();

        hero.addEffect(new ChannelingEffect(this, player));
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        hero.removeEffect(hero.getEffect(effectName));
        return SkillResult.NORMAL;
    }

    private class ChannelingEffect extends PeriodicEffect {
        public ChannelingEffect(Skill skill, Player applier) {
            super(skill, "Channeling-" + skill.getName(), applier, 100, null, null);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);

            int regainPeriod = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.PERIOD, 1000, false);
            hero.addEffect(new PrepareChainsEffect(skill, applier, regainPeriod));
        }

        @Override
        public void tickHero(Hero hero) {
            super.tickHero(hero);
            if (hero.getDelayedSkill() == null || !hero.getDelayedSkill().getSkill().equals(skill)) {
                // We were interrupted or finished casting.
                hero.removeEffect(this);
            }
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            hero.removeEffect(hero.getEffect(effectName));
        }
    }

    private class PrepareChainsEffect extends PeriodicEffect {

        private SkillChainBelt.ChainBeltEffect chainBelt;
        private int numChainsPerTick;
        private double regenMultiplier;
        private double perSecondModifier;

        public PrepareChainsEffect(Skill skill, Player applier, long regainPeriod) {
            super(skill, effectName, applier, regainPeriod, applyText, expireText);

            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.PHYSICAL);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);

            if (!hero.hasEffect(SkillChainBelt.effectName)) {
                hero.removeEffect(this);
                return;
            }

            this.numChainsPerTick = SkillConfigManager.getUseSetting(hero, skill, "num-chains-per-tick", 1, false);
            this.regenMultiplier = SkillConfigManager.getUseSetting(hero, skill, "regen-multiplier", 4.0, false);
            this.perSecondModifier = (1000.0 / getPeriod());

            this.chainBelt = (SkillChainBelt.ChainBeltEffect) hero.getEffect(SkillChainBelt.effectName);
            if (this.chainBelt == null) {
                Heroes.log(Level.SEVERE, SkillChainBelt.skillName + " is missing from the server. " + getName() + " will no longer work. "
                        + SkillChainBelt.skillName + "_must_ be available to the class that has " + getName() + ".");
            }
        }

        @Override
        public void tickHero(Hero hero) {
            regainStamina(hero);
            if (this.chainBelt != null)
                this.chainBelt.forceAddChains(this.numChainsPerTick);
            applier.getWorld().playSound(applier.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.5F, 2.0F);
        }

        @Override
        public void tickMonster(Monster monster) { }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
        }

        private void regainStamina(Hero hero) {
            int staminaIncreaseAmount = (int) (hero.getStaminaRegen() * regenMultiplier * perSecondModifier);   // Recalculate every tick for better compatibility with other skills..
            HeroRegainStaminaEvent staminaEvent = new HeroRegainStaminaEvent(hero, staminaIncreaseAmount, skill);
            plugin.getServer().getPluginManager().callEvent(staminaEvent);
            if (!staminaEvent.isCancelled()) {
                hero.setStamina(staminaEvent.getDelta() + hero.getStamina());
                if (hero.isVerboseStamina())
                    hero.getPlayer().sendMessage(ChatComponents.Bars.stamina(hero.getStamina(), hero.getMaxStamina(), true));
            }
        }
    }
}