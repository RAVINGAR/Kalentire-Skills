package com.herocraftonline.heroes.characters.skill.unfinishedskills;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.SafeFallEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;

import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.hooks.NCPExemptionManager;

public class SkillSafefallOther extends TargettedSkill {

    private boolean ncpEnabled = false;

    public SkillSafefallOther(Heroes plugin) {
        super(plugin, "SafefallOther");
        setDescription("Stops your target from taking fall damage for $1 seconds.");
        setUsage("/skill safefallother <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill safefallother");
        setTypes(SkillType.MOVEMENT, SkillType.BUFF, SkillType.SILENCABLE);

        try {
            if (Bukkit.getServer().getPluginManager().getPlugin("NoCheatPlus") != null) {
                ncpEnabled = true;
            }
        }
        catch (Exception e) {}
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
        return getDescription().replace("$1", duration / 1000 + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DURATION.node(), 10000);
        node.set(SkillSetting.APPLY_TEXT.node(), "%target% has gained safefall!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "%target% has lost safefall!");
        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        if (!(target instanceof Player) || hero.getPlayer().equals(target))
        	return SkillResult.INVALID_TARGET;

        Hero targetHero = plugin.getCharacterManager().getHero((Player) target);
        broadcastExecuteText(hero, target);
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
        targetHero.addEffect(new NCPCompatSafeFallEffect(this, duration));

        return SkillResult.NORMAL;
    }

    private class NCPCompatSafeFallEffect extends SafeFallEffect {

        public NCPCompatSafeFallEffect(Skill skill, long duration) {
            super(skill, duration);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            final Player player = hero.getPlayer();

            if (ncpEnabled)
                NCPExemptionManager.exemptPermanently(player, CheckType.MOVING_NOFALL);
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            final Player player = hero.getPlayer();

            if (ncpEnabled)
                NCPExemptionManager.unexempt(player, CheckType.MOVING_NOFALL);
        }
    }
}
