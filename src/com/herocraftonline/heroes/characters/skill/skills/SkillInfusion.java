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
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Messaging;

public class SkillInfusion extends TargettedSkill {

    public SkillInfusion(Heroes plugin) {
        super(plugin, "Infusion");
        setDescription("Infuses your target, restoring $1 health and removing poisons.");
        setUsage("/skill infusion <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill infusion");
        setTypes(SkillType.HEAL, SkillType.DARK, SkillType.SILENCABLE);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection section = super.getDefaultConfig();
        section.set(SkillSetting.HEALTH.node(), 5);
        section.set(SkillSetting.HEALTH_INCREASE.node(), 0);
        section.set(SkillSetting.MAX_DISTANCE.node(), 5);
        section.set(SkillSetting.REAGENT.node(), 339);
        section.set(SkillSetting.REAGENT_COST.node(), 1);
        return section;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();
        if (!(target instanceof Player)) {
            return SkillResult.INVALID_TARGET;
        }

        Hero targetHero = plugin.getCharacterManager().getHero((Player) target);
        int hpPlus = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALTH, 5, false);
        hpPlus += (SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALTH_INCREASE, 0, false) * hero.getSkillLevel(this));
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

        // Infusion cures Bleeding!
        for (Effect effect : targetHero.getEffects()) {
            if (effect.isType(EffectType.POISON)) {
                targetHero.removeEffect(effect);
            }
        }
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.FUSE , 0.5F, 0.01F); 
        broadcastExecuteText(hero, target);
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(Hero hero) {
        double amount = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALTH, 5, false);
        amount += (SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALTH_INCREASE, 0, false) * hero.getSkillLevel(this));
        return getDescription().replace("$1", amount + "");
    }
}
