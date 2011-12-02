package com.herocraftonline.dev.heroes.skill.skills;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.api.HeroRegainHealthEvent;
import com.herocraftonline.dev.heroes.api.SkillResult;
import com.herocraftonline.dev.heroes.hero.Hero;
import com.herocraftonline.dev.heroes.skill.SkillType;
import com.herocraftonline.dev.heroes.skill.TargettedSkill;
import com.herocraftonline.dev.heroes.util.Messaging;

public class SkillChant extends TargettedSkill {

    public SkillChant(Heroes plugin) {
        super(plugin, "Chant");
        setDescription("Heals the target");
        setUsage("/skill chant <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill chant");
        setTypes(SkillType.HEAL, SkillType.SILENCABLE);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("health", 5);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();
        if (!(target instanceof Player))
        	return SkillResult.INVALID_TARGET;

        Hero targetHero = plugin.getHeroManager().getHero((Player) target);
        int hpPlus = getSetting(hero, "health", 5, false);
        double targetHealth = targetHero.getHealth();

        if (targetHealth >= targetHero.getMaxHealth()) {
            if (player.equals(targetHero.getPlayer())) {
                Messaging.send(player, "You are already at full health.");
            } else {
                Messaging.send(player, "Target is already fully healed.");
            }
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        HeroRegainHealthEvent hrhEvent = new HeroRegainHealthEvent(targetHero, hpPlus, this);
        plugin.getServer().getPluginManager().callEvent(hrhEvent);
        if (hrhEvent.isCancelled()) {
            Messaging.send(player, "Unable to heal the target at this time!");
            return SkillResult.CANCELLED;
        }

        targetHero.setHealth(targetHealth + hrhEvent.getAmount());
        targetHero.syncHealth();

        broadcastExecuteText(hero, target);
        return SkillResult.NORMAL;
    }
}
