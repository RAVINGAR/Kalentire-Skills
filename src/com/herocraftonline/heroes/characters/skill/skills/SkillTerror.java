package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.common.BlindEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

public class SkillTerror extends TargettedSkill {

    private String applyText;
    private String expireText;

    public SkillTerror(Heroes plugin) {
        super(plugin, "Terror");
        setDescription("You terrify your target, impairing their movement and disabling them for $1 seconds.");
        setUsage("/skill terror");
        setArgumentRange(0, 0);
        setIdentifiers("skill terror");
        setTypes(SkillType.ABILITY_PROPERTY_DARK, SkillType.DAMAGING, SkillType.BLINDING, SkillType.DISABLING, SkillType.SILENCABLE, SkillType.AGGRESSIVE);
    }

    @Override
    public String getDescription(Hero hero) {

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 4000, false);
        int durationIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE_PER_CHARISMA, 50, false);
        duration += hero.getAttributeValue(AttributeType.CHARISMA) * durationIncrease;

        String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription().replace("$1", formattedDuration);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), Integer.valueOf(7));
        node.set("amplifier", Integer.valueOf(2));
        node.set(SkillSetting.DURATION.node(), Integer.valueOf(4000));
        node.set(SkillSetting.DURATION_INCREASE_PER_CHARISMA.node(), Integer.valueOf(75));
        node.set(SkillSetting.APPLY_TEXT.node(), Messaging.getSkillDenoter() + "%target% has been overcome with fear!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), Messaging.getSkillDenoter() + "%target% has overcome his fear!");

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT.node(), Messaging.getSkillDenoter() + "%target% is terrified!").replace("%target%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT.node(), Messaging.getSkillDenoter() + "%target% has overcome his fear!").replace("%target%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        int amplifier = SkillConfigManager.getUseSetting(hero, this, "amplifier", 2, false);

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 4000, false);
        int durationIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE_PER_CHARISMA, 50, false);
        duration += hero.getAttributeValue(AttributeType.CHARISMA) * durationIncrease;

        broadcastExecuteText(hero, target);

        TerrorEffect dEffect = new TerrorEffect(this, player, duration, amplifier);

        CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
        targetCT.addEffect(dEffect);

        player.getWorld().playSound(player.getLocation(), Sound.GHAST_SCREAM2, 0.8F, 1.0F);

        return SkillResult.NORMAL;
    }

    public class TerrorEffect extends BlindEffect {

        public TerrorEffect(Skill skill, Player applier, long duration, int amplifier) {
            super(skill, "Terror", applier, duration, applyText, expireText);

            types.add(EffectType.DARK);
            types.add(EffectType.SLOW);
            types.add(EffectType.DISABLE);
            types.add(EffectType.DISPELLABLE);

            addMobEffect(2, (int) ((duration / 1000) * 20), amplifier, false);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
        }
    }
}
