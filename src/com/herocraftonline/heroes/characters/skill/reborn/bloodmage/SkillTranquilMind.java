package com.herocraftonline.heroes.characters.skill.reborn.bloodmage;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class SkillTranquilMind extends PassiveSkill {
    private final String bondEffectName = "TranquilMind";
    private String applyText;
    private String expireText;

    public SkillTranquilMind(Heroes plugin) {
        super(plugin, "TranquilMind");
        setDescription("When you are not focused into any particular state of mind, you passively regenerate $1 mana every $2 seconds.");
        setArgumentRange(0, 0);
        setTypes(SkillType.BUFFING, SkillType.MANA_INCREASING, SkillType.ABILITY_PROPERTY_MAGICAL);
    }

    @Override
    public String getDescription(Hero hero) {
        int manaRegain = SkillConfigManager.getScaledUseSettingInt(hero, this, "mana-regain-per-tick", false);
        long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 2500, false);

        return getDescription()
                .replace("$1", manaRegain + "")
                .replace("$2", Util.decFormat.format(period / 1000.0));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set("mana-regain-per-tick", 15);
        config.set(SkillSetting.PERIOD.node(), 2500);
        return config;
    }

    @Override
    public void apply(Hero hero) {
        long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 2500, false);
        hero.addEffect(new TranquilMindEffect(this, hero.getPlayer(), period));
    }

    @Override
    public String getPassiveEffectName() {
        return bondEffectName;
    }

    public class TranquilMindEffect extends PeriodicEffect {
        private int manaRegain;

        TranquilMindEffect(SkillTranquilMind skill, Player applier, long period) {
            super(skill, bondEffectName, applier, period, applyText, expireText);

            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.AREA_OF_EFFECT);
            types.add(EffectType.HEALING);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            this.manaRegain = SkillConfigManager.getScaledUseSettingInt(hero, skill, "mana-regain-per-tick", false);
        }

        @Override
        public void tickHero(Hero hero) {
            boolean isUnfocused = false;
            if (!hero.hasEffect(SkillFocusOfMind.stanceEffectName)) {   // No effect means they probably just logged in or something. We consider this "unfocused"
                isUnfocused = true;
            } else {
                SkillFocusOfMind.FocusEffect focusEffect = (SkillFocusOfMind.FocusEffect) hero.getEffect(SkillFocusOfMind.stanceEffectName);
                if (focusEffect == null || focusEffect.getCurrentStance() == SkillFocusOfMind.StanceType.UNFOCUSED) {
                    isUnfocused = true;
                }
            }

            if (!isUnfocused)
                return;

            hero.tryRestoreMana(hero, skill, manaRegain);
        }
    }
}