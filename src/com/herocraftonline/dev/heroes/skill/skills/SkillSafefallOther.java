package com.herocraftonline.dev.heroes.skill.skills;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.api.SkillResult;
import com.herocraftonline.dev.heroes.effects.common.SafeFallEffect;
import com.herocraftonline.dev.heroes.hero.Hero;
import com.herocraftonline.dev.heroes.skill.SkillType;
import com.herocraftonline.dev.heroes.skill.TargettedSkill;
import com.herocraftonline.dev.heroes.util.Setting;

public class SkillSafefallOther extends TargettedSkill {

    public SkillSafefallOther(Heroes plugin) {
        super(plugin, "SafefallOther");
        setDescription("Stops your target from taking fall damage for a short amount of time");
        setUsage("/skill safefallother <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill safefallother");
        setTypes(SkillType.MOVEMENT, SkillType.BUFF, SkillType.SILENCABLE);

    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(Setting.DURATION.node(), 10000);
        node.set(Setting.APPLY_TEXT.node(), "%target% has gained safefall!");
        node.set(Setting.EXPIRE_TEXT.node(), "%target% has lost safefall!");
        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        if (!(target instanceof Player) || hero.getPlayer().equals(target))
        	return SkillResult.INVALID_TARGET;

        Hero targetHero = plugin.getHeroManager().getHero((Player) target);
        broadcastExecuteText(hero, target);
        int duration = getSetting(hero, Setting.DURATION.node(), 10000, false);
        targetHero.addEffect(new SafeFallEffect(this, duration));

        return SkillResult.NORMAL;
    }
}
