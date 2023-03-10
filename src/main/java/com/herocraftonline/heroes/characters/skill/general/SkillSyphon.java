package com.herocraftonline.heroes.characters.skill.general;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.Sound;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;

public class SkillSyphon extends TargettedSkill {

    public SkillSyphon(Heroes plugin) {
        super(plugin, "Syphon");
        setDescription("You grant your target some of your health.");
        setUsage("/skill syphon <target> <health>");
        setArgumentRange(0, 2);
        setIdentifiers("skill syphon");
        setTypes(SkillType.HEALING, SkillType.DAMAGING, SkillType.SILENCEABLE, SkillType.ABILITY_PROPERTY_DARK);
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
        if (!(target instanceof Player)) {
        	return SkillResult.INVALID_TARGET;
        }

        double transferredHealth = SkillConfigManager.getUseSetting(hero, this, "default-health", 4, false);
        if (args.length == 2) {
            try {
                transferredHealth = Double.parseDouble(args[1]);
            } catch (NumberFormatException e) {
            	throw new IllegalArgumentException("Invalid health value defined in Heroes skill - Syphon. FIX YOUR CONFIGURATION");
            }
        }
        double playerHealth = hero.getPlayer().getHealth();
        if (playerHealth - transferredHealth < 0) {
            return SkillResult.LOW_HEALTH;
        } else {
            hero.getPlayer().setHealth(playerHealth - transferredHealth);
        }

        transferredHealth *= SkillConfigManager.getUseSetting(hero, this, "multiplier", 1.0, false);

        plugin.getCharacterManager().getHero((Player) target).heal(transferredHealth);
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT , 0.8F, 1.0F);
        broadcastExecuteText(hero, target);
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }
}
