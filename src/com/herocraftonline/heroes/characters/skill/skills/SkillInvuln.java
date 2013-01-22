package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.common.InvulnerabilityEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Setting;

public class SkillInvuln extends ActiveSkill {

    public SkillInvuln(Heroes plugin) {
        super(plugin, "Invuln");
        setDescription("You become immune to all attacks, and may not attack for $1 seconds.");
        setUsage("/skill invuln");
        setArgumentRange(0, 0);
        setIdentifiers("skill invuln");
        setTypes(SkillType.FORCE, SkillType.BUFF, SkillType.SILENCABLE, SkillType.COUNTER);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(Setting.DURATION.node(), 10000);
        node.set(Setting.APPLY_TEXT.node(), "%hero% has become invulnerable!");
        node.set(Setting.EXPIRE_TEXT.node(), "%hero% is once again vulnerable!");
        return node;
    }

    @Override
    public void init() {
        super.init();

    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        broadcastExecuteText(hero);
        int duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 10000, false);
        // Remove any harmful effects on the caster
        for (Effect effect : hero.getEffects()) {
            if (effect.isType(EffectType.HARMFUL)) {
                hero.removeEffect(effect);
            }
        }
        hero.addEffect(new InvulnerabilityEffect(this, duration));
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.LEVEL_UP , 10.0F, 1.0F); 
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 1, false);
        return getDescription().replace("$1", duration / 1000 + "");
    }
}
