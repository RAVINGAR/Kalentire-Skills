package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

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
import com.herocraftonline.heroes.characters.skill.VisualEffect;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

public class SkillChaoticStrength extends ActiveSkill {
    private String applyText;
    private String expireText;

    public VisualEffect fplayer = new VisualEffect();

    public SkillChaoticStrength(Heroes plugin) {
        super(plugin, "ChaoticStrength");
        setDescription("Imbue yourself with Chaotic Strength, granting you $1 Strength, but draining your Intellect by $2 for $3 seconds.");
        setArgumentRange(0, 0);
        setUsage("/skill chaoticstrength");
        setIdentifiers("skill chaoticstrength");
        setTypes(SkillType.BUFFING, SkillType.ABILITY_PROPERTY_FIRE, SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.SILENCEABLE);
    }

    @Override
    public String getDescription(Hero hero) {

        int strengthGain = SkillConfigManager.getUseSetting(hero, this, "strength-buff", Integer.valueOf(15), false);
        int intellectDrain = SkillConfigManager.getUseSetting(hero, this, "intellect-drain", Integer.valueOf(15), false);

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, Integer.valueOf(30000), false);

        String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription().replace("$1", strengthGain + "").replace("$2", intellectDrain + "").replace("$3", formattedDuration);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DURATION.node(), Integer.valueOf(15000));
        node.set("strength-buff", Integer.valueOf(18));
        node.set("intellect-drain", Integer.valueOf(15));
        node.set(SkillSetting.APPLY_TEXT.node(), Messaging.getSkillDenoter() + "%hero% is imbued with Chaotic Strength!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), Messaging.getSkillDenoter() + "%hero%'s Chaotic Strength fades.");

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, Messaging.getSkillDenoter() + "%hero% is imbued with Chaotic Strength!").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, Messaging.getSkillDenoter() + "%hero%'s Chaotic Strength fades.").replace("%hero%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        int strengthGain = SkillConfigManager.getUseSetting(hero, this, "strength-buff", Integer.valueOf(22), false);
        int intellectDrain = SkillConfigManager.getUseSetting(hero, this, "intellect-drain", Integer.valueOf(15), false);

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, Integer.valueOf(30000), false);

        hero.addEffect(new ChaoticStrengthEffect(this, player, duration, strengthGain, intellectDrain));

        try {
            Location playerLocation = player.getLocation();
            fplayer.playFirework(playerLocation.getWorld(), playerLocation, FireworkEffect.builder().flicker(false).trail(false).with(FireworkEffect.Type.STAR).withColor(Color.BLACK).withFade(Color.ORANGE).build());
        }
        catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        player.getWorld().playSound(player.getLocation(), Sound.WITHER_SPAWN, 0.5F, 1.0F);
        player.getWorld().playSound(player.getLocation(), Sound.COW_HURT, 1.3F, 0.5F);

        return SkillResult.NORMAL;
    }

    public class ChaoticStrengthEffect extends ExpirableEffect {

        private int strGain;
        private int intDrain;

        public ChaoticStrengthEffect(Skill skill, Player applier, long duration, int strGain, int intDrain) {
            super(skill, "ChaoticStrength", applier, duration, applyText, expireText);

            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.MAGIC);

            this.strGain = strGain;
            this.intDrain = intDrain;
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            Player player = hero.getPlayer();

            AttributeIncreaseEffect strBuff = new AttributeIncreaseEffect(skill, "ChaoticStrengthStrGain", player, this.getDuration(), AttributeType.STRENGTH, strGain, null, null);
            AttributeDecreaseEffect intDebuff = new AttributeDecreaseEffect(skill, "ChaoticStrengthIntDrain", player, this.getDuration(), AttributeType.INTELLECT, intDrain, null, null);

            hero.addEffect(strBuff);
            hero.addEffect(intDebuff);

            hero.rebuildAttribute(AttributeType.STRENGTH);
            hero.rebuildAttribute(AttributeType.INTELLECT);
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);

            hero.removeEffect(hero.getEffect("ChaoticStrengthStrGain"));
            hero.removeEffect(hero.getEffect("ChaoticStrengthIntDrain"));

            hero.rebuildAttribute(AttributeType.STRENGTH);
            hero.rebuildAttribute(AttributeType.INTELLECT);
        }
    }
}
