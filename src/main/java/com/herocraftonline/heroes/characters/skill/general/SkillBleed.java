package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CustomNameManager;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicDamageEffect;
import com.herocraftonline.heroes.characters.effects.standard.BleedingEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SkillBleed extends TargettedSkill {

    private String applyText;
    private String expireText;

    public SkillBleed(Heroes plugin) {
        super(plugin, "Bleed");
        this.setDescription("You cause your target to bleed, dealing $1 damage per bleed every $2 second(s) over $3 second(s).");
        this.setUsage("/skill bleed <target>");
        this.setArgumentRange(0, 1);
        this.setTypes(SkillType.SILENCEABLE, SkillType.DAMAGING, SkillType.AGGRESSIVE);
        this.setIdentifiers("skill bleed");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DURATION.node(), 10000);
        node.set(SkillSetting.APPLY_TEXT.node(), "%target% is bleeding!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "%target% has stopped bleeding!");
        return node;
    }

    @Override
    public void init() {
        super.init();
        this.applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT.node(), "%target% is bleeding!").replace("%target%", "$1");
        this.expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT.node(), "%target% has stopped bleeding!").replace("%target%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        final Player player = hero.getPlayer();
        if (target.equals(player)) {
            return SkillResult.INVALID_TARGET;
        }
        BleedingEffect.applyStack(this.plugin.getCharacterManager().getCharacter(target), this, player, SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false));
        this.broadcastExecuteText(hero, target);
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(Hero hero) {
        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
        return this.getDescription()
                .replace("$1", Heroes.properties.standardEffectBleedingDamagePerStack + "")
                .replace("$2", Heroes.properties.standardEffectBleedingPeriod + "")
                .replace("$3", duration / 1000 + "");
    }
}
