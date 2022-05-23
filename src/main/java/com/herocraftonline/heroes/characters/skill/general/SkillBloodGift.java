package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainHealthEvent;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.BloodUnionEffect;
import com.herocraftonline.heroes.characters.skill.*;
import org.bukkit.Effect;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.Sound;

public class SkillBloodGift extends TargettedSkill {

    public SkillBloodGift(Heroes plugin) {
        super(plugin, "BloodGift");
        setDescription("You gift an ally with your own blood, restoring $1 of their health. Healing is increased by $4% per level of Blood Union. This ability cannot be used on yourself, and costs $2 health and $3 mana to use.");
        setUsage("/skill bloodgift <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill bloodgift");
        setTypes(SkillType.HEALING, SkillType.SILENCEABLE, SkillType.NO_SELF_TARGETTING, SkillType.ABILITY_PROPERTY_DARK, SkillType.ABILITY_PROPERTY_LIGHT);
    }

    public String getDescription(Hero hero) {

        double healing = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING.node(), 130, false);
        healing = getScaledHealing(hero, healing);
        double healingIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING_INCREASE_PER_WISDOM.node(), 1.8, false);
        healing += (hero.getAttributeValue(AttributeType.WISDOM) * healingIncrease);
        
        int healthCost = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALTH_COST.node(), 85, false);
        int manacost = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MANA.node(), 110, false);

        int healIncrease = (int) (SkillConfigManager.getUseSetting(hero, this, "health-increase-percent-per-blood-union", 0.04, false) * 100);

        return getDescription().replace("$1", ((int)healing) + "").replace("$2", healthCost + "").replace("$3", manacost + "").replace("$4", healIncrease + "");
    }

    public ConfigurationSection getDefaultConfig() {

        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 10);
        node.set("health-increase-percent-per-blood-union", 0.04);
        node.set(SkillSetting.HEALING.node(), 130);
        node.set(SkillSetting.HEALING_INCREASE_PER_WISDOM.node(), 1.8);
        node.set(SkillSetting.HEALTH_COST.node(), 85);
        node.set(SkillSetting.MANA.node(), 110);

        return node;
    }

    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();
        if (!(target instanceof Player)) {
            return SkillResult.INVALID_TARGET;
        }

        Hero targetHero = plugin.getCharacterManager().getHero((Player) target);

        double targetHealth = target.getHealth();

        // Check to see if they are at full health
        if (targetHealth >= target.getMaxHealth()) {
            player.sendMessage("Target is already at full health.");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        broadcastExecuteText(hero, target);

        double healAmount = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING.node(), 130, false);
        healAmount = getScaledHealing(hero, healAmount);
        double wisHealIncrease = (hero.getAttributeValue(AttributeType.WISDOM) * SkillConfigManager.getUseSetting(hero, this,
                SkillSetting.HEALING_INCREASE_PER_WISDOM.node(), 1.8, false));
        healAmount += wisHealIncrease;

        // Get Blood Union Level
        int bloodUnionLevel = 0;
        if (hero.hasEffect("BloodUnionEffect")) {
            BloodUnionEffect buEffect = (BloodUnionEffect) hero.getEffect("BloodUnionEffect");
            bloodUnionLevel = buEffect.getBloodUnionLevel();
        }

        // Increase healing based on blood union level
        double healIncrease = SkillConfigManager.getUseSetting(hero, this, "health-increase-percent-per-blood-union", 0.04, false);
        healIncrease = 1 + (healIncrease *= bloodUnionLevel);
        healAmount *= healIncrease;

        // Ensure they can be healed.
        HeroRegainHealthEvent hrhEvent = new HeroRegainHealthEvent(targetHero, healAmount, this, hero);
        this.plugin.getServer().getPluginManager().callEvent(hrhEvent);
        if (hrhEvent.isCancelled()) {
            player.sendMessage("Unable to heal the target at this time!");
            return SkillResult.CANCELLED;
        }

        // Heal target
        targetHero.heal(hrhEvent.getDelta());

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 0.5F, 1.0F);
        //target.getWorld().spigot().playEffect(target.getLocation(), Effect.HEART, 1, 1, 0F, 1F, 0F, 50F, 30, 10);
        target.getWorld().spawnParticle(Particle.HEART, target.getLocation(), 30, 0F, 1F, 0F, 50);
        return SkillResult.NORMAL;
    }
}