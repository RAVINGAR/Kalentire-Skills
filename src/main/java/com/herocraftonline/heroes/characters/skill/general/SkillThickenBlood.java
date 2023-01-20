package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SkillThickenBlood extends TargettedSkill {
    private String applyText;
    private String expireText;

    public SkillThickenBlood(final Heroes plugin) {
        super(plugin, "ThickenBlood");
        setDescription("Thicken the blood of your target, causing them to be unable to use stamina for $1 second(s).");
        setUsage("/skill thickenblood");
        setArgumentRange(0, 0);
        setIdentifiers("skill thickenblood");
        setTypes(SkillType.SILENCEABLE, SkillType.AGGRESSIVE, SkillType.ABILITY_PROPERTY_DARK, SkillType.STAMINA_FREEZING, SkillType.DEBUFFING);
    }

    @Override
    public String getDescription(final Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 2000, false);
        final int durationIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE_PER_INTELLECT, 75, false);
        duration += hero.getAttributeValue(AttributeType.INTELLECT) * durationIncrease;

        final String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription().replace("$1", formattedDuration);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 7);
        node.set(SkillSetting.DURATION.node(), 2000);
        node.set(SkillSetting.DURATION_INCREASE_PER_INTELLECT.node(), 75);
        node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target%'s blood has thickened!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target%'s blood returns to normal.");

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%target%'s blood has thickened!").replace("%target%", "$1").replace("$target$", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%target%'s blood returns to normal.").replace("%target%", "$1").replace("$target$", "$1");
    }

    @Override
    public SkillResult use(final Hero hero, final LivingEntity target, final String[] args) {

        if (!(target instanceof Player)) {
            return SkillResult.INVALID_TARGET;
        }

        final Player player = hero.getPlayer();

        broadcastExecuteText(hero, target);

        // Get Debuff values
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 2000, false);
        final int durationIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE_PER_INTELLECT, 75, false);
        duration += hero.getAttributeValue(AttributeType.INTELLECT) * durationIncrease;

        final ThickenBloodEffect tbEffect = new ThickenBloodEffect(this, player, duration);
        final CharacterTemplate targCT = plugin.getCharacterManager().getCharacter(target);
        targCT.addEffect(tbEffect);

        return SkillResult.NORMAL;
    }

    public class ThickenBloodEffect extends ExpirableEffect {
        private int originalStamina;

        public ThickenBloodEffect(final Skill skill, final Player applier, final int duration) {
            super(skill, "ThickenedBlood", applier, duration);

            types.add(EffectType.MAGIC);
            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.HARMFUL);
            types.add(EffectType.HUNGER);
            types.add(EffectType.STAMINA_FREEZING);

            addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, duration / 1000 * 20, 0), false);
        }

        @Override
        public void applyToMonster(final Monster monster) {
        }

        @Override
        public void applyToHero(final Hero hero) {
            super.applyToHero(hero);
            final Player player = hero.getPlayer();

            this.originalStamina = hero.getStamina();
            hero.setStamina(0);

            broadcast(player.getLocation(), "    " + applyText, player.getName());
        }

        @Override
        public void removeFromMonster(final Monster monster) {
        }

        @Override
        public void removeFromHero(final Hero hero) {
            super.removeFromHero(hero);
            final Player player = hero.getPlayer();

            hero.setStamina(originalStamina);

            broadcast(player.getLocation(), "    " + expireText, player.getName());
        }
    }
}