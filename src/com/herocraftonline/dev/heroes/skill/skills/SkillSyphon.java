package com.herocraftonline.dev.heroes.skill.skills;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.api.SkillResult;
import com.herocraftonline.dev.heroes.hero.Hero;
import com.herocraftonline.dev.heroes.skill.SkillConfigManager;
import com.herocraftonline.dev.heroes.skill.SkillType;
import com.herocraftonline.dev.heroes.skill.TargettedSkill;

public class SkillSyphon extends TargettedSkill {

    public SkillSyphon(Heroes plugin) {
        super(plugin, "Syphon");
        setDescription("Gives your health to the target");
        setUsage("/skill syphon <target> <health>");
        setArgumentRange(0, 2);
        setIdentifiers("skill syphon");
        setTypes(SkillType.HEAL, SkillType.DAMAGING, SkillType.SILENCABLE, SkillType.DARK);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("multiplier", 1d);
        node.set("default-health", 4);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        if (!(target instanceof Player))
        	return SkillResult.INVALID_TARGET;

        Hero targetHero = plugin.getHeroManager().getHero((Player) target);

        double transferredHealth = SkillConfigManager.getUseSetting(hero, this, "default-health", 4, false);
        if (args.length == 2) {
            try {
                transferredHealth = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
            	throw new IllegalArgumentException("Invalid health value defined in Heroes skill - Syphon. FIX YOUR CONFIGURATION");
            }
        }
        double playerHealth = hero.getHealth();
        double targetHealth = targetHero.getHealth();
        hero.setHealth(playerHealth - transferredHealth);
        hero.syncHealth();

        transferredHealth *= SkillConfigManager.getUseSetting(hero, this, "multiplier", 1.0, false);
        targetHero.setHealth(targetHealth + transferredHealth);
        targetHero.syncHealth();

        broadcastExecuteText(hero, target);
        return SkillResult.NORMAL;
    }

}
