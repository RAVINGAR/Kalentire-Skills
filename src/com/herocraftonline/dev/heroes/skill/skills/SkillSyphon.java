package com.herocraftonline.dev.heroes.skill.skills;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.config.ConfigurationNode;

import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.hero.Hero;
import com.herocraftonline.dev.heroes.skill.SkillType;
import com.herocraftonline.dev.heroes.skill.TargettedSkill;
import com.herocraftonline.dev.heroes.util.Messaging;

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
    public ConfigurationNode getDefaultConfig() {
        ConfigurationNode node = super.getDefaultConfig();
        node.setProperty("multiplier", 1d);
        node.setProperty("default-health", 4);
        return node;
    }

    @Override
    public boolean use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();
        if (!(target instanceof Player)) {
            Messaging.send(player, "Your need a target!");
            return false;
        }

        Hero targetHero = plugin.getHeroManager().getHero((Player) target);

        double transferredHealth = getSetting(hero, "default-health", 4, false);
        if (args.length == 2) {
            try {
                transferredHealth = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                player.sendMessage("Sorry, that's an incorrect health value!");
                return false;
            }
        }
        double playerHealth = hero.getHealth();
        double targetHealth = targetHero.getHealth();
        hero.setHealth(playerHealth - transferredHealth);
        hero.syncHealth();

        transferredHealth *= getSetting(hero, "multiplier", 1.0, false);
        targetHero.setHealth(targetHealth + transferredHealth);
        targetHero.syncHealth();

        broadcastExecuteText(hero, target);
        return true;
    }

}
