package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.effects.standard.BleedingEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class SkillSerratedEdge extends ActiveSkill {
    private String useText = "§7You sharpened your weapons!";

    public SkillSerratedEdge(final Heroes plugin) {
        super(plugin, "SerratedEdge");
        this.setDescription("You serrate your weapons enhancing their effectiveness for $3 second(s), also extending any previous preparations by $3 second(s). " +
                "Any target hit will suffer a bleeding stack dealing $1 damage per bleed every $2 second(s) over $3 second(s)." +
                "Other preparations can postpone the expiry of this preparation.");
        this.setUsage("/skill SerratedEdge");
        this.setArgumentRange(0, 1);
        this.setTypes(SkillType.SILENCEABLE, SkillType.DAMAGING, SkillType.AGGRESSIVE);
        this.setIdentifiers("skill SerratedEdge");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DURATION.node(), 10000);
        node.set(SkillSetting.USE_TEXT.node(), useText);
        return node;
    }

    @Override
    public void init() {
        super.init();
        this.useText = SkillConfigManager.getRaw(this, SkillSetting.USE_TEXT.node(), useText);
    }

    @Override
    public String getDescription(final Hero hero) {
        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
        return this.getDescription()
                .replace("$1", Heroes.properties.standardEffectBleedingDamagePerStack + "")
                .replace("$2", Heroes.properties.standardEffectBleedingPeriod + "")
                .replace("$3", duration / 1000 + "");
    }

    @Override
    public SkillResult use(final Hero hero, final String[] strings) {
        final long duration = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.DURATION, 10000, false);
        final SkillAssassinsGuile.EffectPreparationEvent effect = new SkillAssassinsGuile.EffectPreparationEvent(hero, new SerratedEdgeEffect(this, hero.getPlayer(), duration), duration, useText);
        plugin.getServer().getPluginManager().callEvent(effect);

        return SkillResult.NORMAL;
    }


    public static class SerratedEdgeEffect extends ExpirableEffect {
        private final long duration; //This means that the duration of the applied stack can never be actually changed

        public SerratedEdgeEffect(final Skill skill, final Player applier, final long duration) {
            super(skill, "SerratedEdgeEffect", applier, duration);
            this.duration = duration;
        }

        @Override
        public void apply(final CharacterTemplate character) {
            BleedingEffect.applyStack(character, skill, applier, duration);
            character.removeEffect(this); // immediately remove!
        }
    }
}
