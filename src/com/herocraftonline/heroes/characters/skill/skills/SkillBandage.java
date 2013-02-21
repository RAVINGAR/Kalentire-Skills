package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainHealthEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Setting;

public class SkillBandage extends TargettedSkill {

    public SkillBandage(Heroes plugin) {
        super(plugin, "Bandage");
        setDescription("Bandages your target, restoring $1 health.");
        setUsage("/skill bandage <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill bandage");
        setTypes(SkillType.HEAL, SkillType.PHYSICAL);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection section = super.getDefaultConfig();
        section.set(Setting.HEALTH.node(), 5);
        section.set(Setting.HEALTH_INCREASE.node(), 0);
        section.set(Setting.MAX_DISTANCE.node(), 5);
        section.set(Setting.REAGENT.node(), 339);
        section.set(Setting.REAGENT_COST.node(), 1);
        return section;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();
        if (!(target instanceof Player)) {
            return SkillResult.INVALID_TARGET;
        }

        Hero targetHero = plugin.getCharacterManager().getHero((Player) target);
        int hpPlus = SkillConfigManager.getUseSetting(hero, this, Setting.HEALTH, 5, false);
        hpPlus += (SkillConfigManager.getUseSetting(hero, this, Setting.HEALTH_INCREASE, 0, false) * hero.getSkillLevel(this));
        int targetHealth = target.getHealth();

        if (targetHealth >= target.getMaxHealth()) {
            if (player.equals(targetHero.getPlayer())) {
                Messaging.send(player, "You are already at full health.");
            } else {
                Messaging.send(player, "Target is already fully healed.");
            }
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        HeroRegainHealthEvent hrhEvent = new HeroRegainHealthEvent(targetHero, hpPlus, this, hero);
        Bukkit.getPluginManager().callEvent(hrhEvent);
        if (hrhEvent.isCancelled()) {
            Messaging.send(player, "Unable to heal the target at this time!");
            return SkillResult.CANCELLED;
        }
        targetHero.heal(hrhEvent.getAmount()); 

        // Bandage cures Bleeding!
        for (Effect effect : targetHero.getEffects()) {
            if (effect.isType(EffectType.BLEED)) {
                targetHero.removeEffect(effect);
            }
        }
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.EAT , 0.5F, 0.01F); 
        broadcastExecuteText(hero, target);
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(Hero hero) {
        double amount = SkillConfigManager.getUseSetting(hero, this, Setting.HEALTH, 5, false);
        amount += (SkillConfigManager.getUseSetting(hero, this, Setting.HEALTH_INCREASE, 0, false) * hero.getSkillLevel(this));
        return getDescription().replace("$1", amount + "");
    }
}
