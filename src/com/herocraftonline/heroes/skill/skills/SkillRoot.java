package com.herocraftonline.heroes.skill.skills;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.effects.common.RootEffect;
import com.herocraftonline.heroes.hero.Hero;
import com.herocraftonline.heroes.skill.SkillConfigManager;
import com.herocraftonline.heroes.skill.SkillType;
import com.herocraftonline.heroes.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Setting;

public class SkillRoot extends TargettedSkill {

    public SkillRoot(Heroes plugin) {
        super(plugin, "Root");
        setDescription("You root your target in place for $1 seconds.");
        setUsage("/skill root <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill root");
        setTypes(SkillType.MOVEMENT, SkillType.DEBUFF, SkillType.SILENCABLE, SkillType.EARTH, SkillType.HARMFUL);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(Setting.DURATION.node(), 5000);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        long duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 5000, false);
        RootEffect rEffect = new RootEffect(this, duration);

        if (target instanceof Player) {
            plugin.getHeroManager().getHero((Player) target).addEffect(rEffect);
        } else
            plugin.getEffectManager().addEntityEffect(target, rEffect);

        broadcastExecuteText(hero, target);
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 5000, false);
        return getDescription().replace("$1", duration / 1000 + "");
    }
}
