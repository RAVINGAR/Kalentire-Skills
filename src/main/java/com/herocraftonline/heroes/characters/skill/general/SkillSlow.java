package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.common.SlowEffect;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.Sound;

public class SkillSlow extends TargettedSkill {

    private String applyText;
    private String expireText;

    public SkillSlow(Heroes plugin) {
        super(plugin, "Slow");
        this.setDescription("You slow the target's movement & attack speed for $1 second(s).");
        this.setUsage("/skill slow");
        this.setArgumentRange(0, 1);
        this.setIdentifiers("skill slow");
        this.setTypes(SkillType.DEBUFFING, SkillType.MOVEMENT_SLOWING, SkillType.SILENCEABLE, SkillType.AGGRESSIVE);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();
        node.set("speed-multiplier", 2);
        node.set(SkillSetting.DURATION.node(), 15000);
        node.set(SkillSetting.APPLY_TEXT.node(), "%target% has been slowed by %hero%!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "%target% is no longer slowed!");
        return node;
    }


    @Override
    public void init() {
        this.applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, "%target% has been slowed by %hero%!").replace("%target%", "$1").replace("%hero%", "$2");
        this.expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "%target% is no longer slowed!").replace("%target%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        if (!(target instanceof Player)) {
            return SkillResult.INVALID_TARGET;
        }

        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 15000, false);
        int multiplier = SkillConfigManager.getUseSetting(hero, this, "speed-multiplier", 2, false);
        if (multiplier > 20) {
            multiplier = 20;
        }
        final SlowEffect effect = new SlowEffect(this, hero.getPlayer(), duration, multiplier, this.applyText, this.expireText);
        effect.types.add(EffectType.MAGIC);
        this.plugin.getCharacterManager().getHero((Player) target).addEffect(effect);
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(Hero hero) {
        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 15000, false);
        ;
        return this.getDescription().replace("$1", (duration / 1000) + "");
    }
}
