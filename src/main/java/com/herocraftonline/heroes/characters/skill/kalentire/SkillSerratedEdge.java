package com.herocraftonline.heroes.characters.skill.kalentire;

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
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SkillSerratedEdge extends ActiveSkill {

    private String applyText = "§7You are bleeding!";
    private String expireText;
    private String useText = "§7You sharpened your weapons!";

    public SkillSerratedEdge(Heroes plugin) {
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
        node.set(SkillSetting.APPLY_TEXT.node(), applyText);
        return node;
    }

    @Override
    public void init() {
        super.init();
        this.applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT.node(), applyText);
        this.useText = SkillConfigManager.getRaw(this, SkillSetting.USE_TEXT.node(), useText);
    }

    @Override
    public String getDescription(Hero hero) {
        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
        return this.getDescription()
                .replace("$1", Heroes.properties.standardEffectBleedingDamagePerStack + "")
                .replace("$2", Heroes.properties.standardEffectBleedingPeriod + "")
                .replace("$3", duration / 1000 + "");
    }

    @Override
    public SkillResult use(Hero hero, String[] strings) {
        long duration = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.DURATION, 10000, false);
        SkillAssassinsGuile.EffectPreparationEvent effect = new SkillAssassinsGuile.EffectPreparationEvent(hero, new SerratedEdgeEffect(this, hero.getPlayer(), duration), duration, applyText);
        plugin.getServer().getPluginManager().callEvent(effect);

        return SkillResult.NORMAL;
    }


    public static class SerratedEdgeEffect extends ExpirableEffect {
        private final long duration; //This means that the duration of the applied stack can never be actually changed

        public SerratedEdgeEffect(Skill skill, Player applier, long duration) {
            super(skill, "SerratedEdgeEffect", applier, duration);
            this.duration = duration;
        }

        @Override
        public void apply(CharacterTemplate character) {
            BleedingEffect.applyStack(character, skill, applier, duration);
            character.removeEffect(this); // immediately remove!
        }
    }
}
