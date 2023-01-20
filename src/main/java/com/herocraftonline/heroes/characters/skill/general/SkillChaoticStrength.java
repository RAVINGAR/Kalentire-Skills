package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.effects.common.AttributeDecreaseEffect;
import com.herocraftonline.heroes.characters.effects.common.AttributeIncreaseEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class SkillChaoticStrength extends ActiveSkill {
    private String applyText;
    private String expireText;

    public SkillChaoticStrength(final Heroes plugin) {
        super(plugin, "ChaoticStrength");
        setDescription("Imbue yourself with Chaotic Strength, granting you $1 Strength, but draining your Intellect by $2 for $3 second(s).");
        setArgumentRange(0, 0);
        setUsage("/skill chaoticstrength");
        setIdentifiers("skill chaoticstrength");
        setTypes(SkillType.BUFFING, SkillType.ABILITY_PROPERTY_FIRE, SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.SILENCEABLE);
    }

    @Override
    public String getDescription(final Hero hero) {

        final int strengthGain = SkillConfigManager.getUseSetting(hero, this, "strength-buff", 15, false);
        final int intellectDrain = SkillConfigManager.getUseSetting(hero, this, "intellect-drain", 15, false);

        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 30000, false);

        final String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription().replace("$1", strengthGain + "").replace("$2", intellectDrain + "").replace("$3", formattedDuration);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DURATION.node(), 15000);
        node.set("strength-buff", 18);
        node.set("intellect-drain", 15);
        node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% is imbued with Chaotic Strength!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero%'s Chaotic Strength fades.");

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%hero% is imbued with Chaotic Strength!").replace("%hero%", "$1").replace("$hero$", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%hero%'s Chaotic Strength fades.").replace("%hero%", "$1").replace("$hero$", "$1");
    }

    @Override
    public SkillResult use(final Hero hero, final String[] args) {
        final Player player = hero.getPlayer();

        final int strengthGain = SkillConfigManager.getUseSetting(hero, this, "strength-buff", 22, false);
        final int intellectDrain = SkillConfigManager.getUseSetting(hero, this, "intellect-drain", 15, false);

        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 30000, false);

        hero.addEffect(new ChaoticStrengthEffect(this, player, duration, strengthGain, intellectDrain));

        //FIXME What am I doing here?
//        player.getWorld().playEffect(player.getLocation().add(0, 0.5, 0), org.bukkit.Effect.NOTE, 3);
//        player.getWorld().playEffect(player.getLocation().add(0, 0.5, 0), org.bukkit.Effect.NOTE, 3);
//        player.getWorld().playEffect(player.getLocation().add(0, 0.5, 0), org.bukkit.Effect.NOTE, 3);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5F, 1.0F);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_COW_HURT, 1.3F, 0.5F);

        return SkillResult.NORMAL;
    }

    public class ChaoticStrengthEffect extends ExpirableEffect {

        private final int strGain;
        private final int intDrain;

        public ChaoticStrengthEffect(final Skill skill, final Player applier, final long duration, final int strGain, final int intDrain) {
            super(skill, "ChaoticStrength", applier, duration, applyText, expireText);

            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.MAGIC);

            this.strGain = strGain;
            this.intDrain = intDrain;
        }

        @Override
        public void applyToHero(final Hero hero) {
            super.applyToHero(hero);
            final Player player = hero.getPlayer();

            final AttributeIncreaseEffect strBuff = new AttributeIncreaseEffect(skill, "ChaoticStrengthStrGain", player, this.getDuration(), AttributeType.STRENGTH, strGain, null, null);
            final AttributeDecreaseEffect intDebuff = new AttributeDecreaseEffect(skill, "ChaoticStrengthIntDrain", player, this.getDuration(), AttributeType.INTELLECT, intDrain, null, null);

            hero.addEffect(strBuff);
            hero.addEffect(intDebuff);

            hero.rebuildAttribute(AttributeType.STRENGTH);
            hero.rebuildAttribute(AttributeType.INTELLECT);
        }

        @Override
        public void removeFromHero(final Hero hero) {
            super.removeFromHero(hero);

            hero.removeEffect(hero.getEffect("ChaoticStrengthStrGain"));
            hero.removeEffect(hero.getEffect("ChaoticStrengthIntDrain"));

            hero.rebuildAttribute(AttributeType.STRENGTH);
            hero.rebuildAttribute(AttributeType.INTELLECT);
        }
    }
}
