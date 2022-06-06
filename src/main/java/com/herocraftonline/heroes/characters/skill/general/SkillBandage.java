package com.herocraftonline.heroes.characters.skill.general;

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
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SkillBandage extends TargettedSkill {

    public SkillBandage(Heroes plugin) {
        super(plugin, "Bandage");
        this.setDescription("Bandages your target, restoring $1 health.");
        this.setUsage("/skill bandage <target>");
        this.setArgumentRange(0, 1);
        this.setIdentifiers("skill bandage");
        this.setTypes(SkillType.HEALING, SkillType.ABILITY_PROPERTY_PHYSICAL);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection section = super.getDefaultConfig();
        section.set(SkillSetting.HEALTH.node(), 5);
        section.set(SkillSetting.HEALTH_INCREASE.node(), 0);
        section.set(SkillSetting.MAX_DISTANCE.node(), 5);
        section.set(SkillSetting.REAGENT.node(), "PAPER");
        section.set(SkillSetting.REAGENT_COST.node(), 1);
        return section;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        final Player player = hero.getPlayer();
        if (!(target instanceof Player)) {
            return SkillResult.INVALID_TARGET;
        }

        final Hero targetHero = this.plugin.getCharacterManager().getHero((Player) target);
        double hpPlus = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALTH, 5, false);
        hpPlus += (SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALTH_INCREASE, 0, false) * hero.getHeroLevel(this));
        final double targetHealth = targetHero.getPlayer().getHealth();

        if (targetHealth >= targetHero.getPlayer().getMaxHealth()) {
            if (player.equals(targetHero.getPlayer())) {
                player.sendMessage(ChatColor.GRAY + "You are already at full health.");
            } else {
                player.sendMessage(ChatColor.GRAY + "Target is already fully healed.");
            }
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        final HeroRegainHealthEvent hrhEvent = new HeroRegainHealthEvent(targetHero, hpPlus, this, hero);
        this.plugin.getServer().getPluginManager().callEvent(hrhEvent);
        if (hrhEvent.isCancelled()) {
            player.sendMessage(ChatColor.GRAY + "Unable to heal the target at this time!");
            return SkillResult.CANCELLED;
        }

        targetHero.heal(hrhEvent.getDelta());

        // Bandage cures Bleeding!
        for (final Effect effect : targetHero.getEffects()) {
            if (effect.isType(EffectType.BLEED)) {
                targetHero.removeEffect(effect);
            }
        }

        this.broadcastExecuteText(hero, target);
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(Hero hero) {
        double amount = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALTH, 5, false);
        amount += (SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALTH_INCREASE, 0, false) * hero.getHeroLevel(this));
        return this.getDescription().replace("$1", amount + "");
    }
}
