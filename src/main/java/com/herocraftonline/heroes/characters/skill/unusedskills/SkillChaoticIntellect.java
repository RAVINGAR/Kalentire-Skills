package com.herocraftonline.heroes.characters.skill.unusedskills;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.Sound;

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
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;

public class SkillChaoticIntellect extends ActiveSkill {
    private String applyText;
    private String expireText;

    public final VisualEffect fplayer = new VisualEffect();

    public SkillChaoticIntellect(Heroes plugin) {
        super(plugin, "ChaoticIntellect");
        setDescription("Imbue yourself with Chaotic Intellect, granting you $1 Intellect, but draining your Constitution by $2 for $3 second(s).");
        setArgumentRange(0, 0);
        setUsage("/skill chaoticintellect");
        setIdentifiers("skill chaoticintellect");
        setTypes(SkillType.BUFFING, SkillType.ABILITY_PROPERTY_FIRE, SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.SILENCEABLE);
    }

    @Override
    public String getDescription(Hero hero) {

        int intellectGain = SkillConfigManager.getUseSetting(hero, this, "intellect-buff", 15, false);
        int constitutionDrain = SkillConfigManager.getUseSetting(hero, this, "constitution-drain", 15, false);

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 30000, false);

        String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription().replace("$1", intellectGain + "").replace("$2", constitutionDrain + "").replace("$3", formattedDuration);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DURATION.node(), 15000);
        node.set("intellect-buff", 16);
        node.set("constitution-drain", 20);
        node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% is imbued with Chaotic Intellect!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero%'s Chaotic Intellect fades.");

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%hero% is imbued with Chaotic Intellect!").replace("%hero%", "$1").replace("$hero$", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%hero%'s Chaotic Intellect fades.").replace("%hero%", "$1").replace("$hero$", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        int intellectGain = SkillConfigManager.getUseSetting(hero, this, "intellect-buff", 22, false);
        int constitutionDrain = SkillConfigManager.getUseSetting(hero, this, "constitution-drain", 15, false);

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 30000, false);

        hero.addEffect(new ChaoticIntellectEffect(this, player, duration, intellectGain, constitutionDrain));

        try {
            Location playerLocation = player.getLocation();
            fplayer.playFirework(playerLocation.getWorld(), playerLocation, FireworkEffect.builder().flicker(false).trail(true).with(FireworkEffect.Type.STAR).withColor(Color.BLACK).withFade(Color.ORANGE).build());
        } catch (Exception e) {
            e.printStackTrace();
        }

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5F, 1.0F);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 1.3F, 1.2F);

        return SkillResult.NORMAL;
    }

    public class ChaoticIntellectEffect extends ExpirableEffect {

        private final int intGain;
        private final int conDrain;

        public ChaoticIntellectEffect(Skill skill, Player applier, long duration, int intGain, int conDrain) {
            super(skill, "ChaoticIntellect", applier, duration, applyText, expireText);

            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.MAGIC);

            this.intGain = intGain;
            this.conDrain = conDrain;
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            Player player = hero.getPlayer();

            AttributeIncreaseEffect intBuff = new AttributeIncreaseEffect(skill, "ChaoticIntellectStrGain", player, this.getDuration(), AttributeType.INTELLECT, intGain, null, null);
            AttributeDecreaseEffect conDebuff = new AttributeDecreaseEffect(skill, "ChaoticIntellectConDrain", player, this.getDuration(), AttributeType.CONSTITUTION, conDrain, null, null);

            hero.addEffect(intBuff);
            hero.addEffect(conDebuff);

            hero.rebuildAttribute(AttributeType.INTELLECT);
            hero.rebuildAttribute(AttributeType.CONSTITUTION);
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);

            hero.removeEffect(hero.getEffect("ChaoticIntellectStrGain"));
            hero.removeEffect(hero.getEffect("ChaoticIntellectConDrain"));

            hero.rebuildAttribute(AttributeType.INTELLECT);
            hero.rebuildAttribute(AttributeType.CONSTITUTION);
        }
    }
}
