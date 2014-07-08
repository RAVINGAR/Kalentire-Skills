package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainHealthEvent;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.uncommon.BloodUnionEffect;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.characters.skill.VisualEffect;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

public class SkillBloodRitual extends TargettedSkill {
    public VisualEffect fplayer = new VisualEffect();

    public SkillBloodRitual(Heroes plugin) {
        super(plugin, "BloodRitual");
        setDescription("Perform a Ritual of Blood with your target, restoring $1% of your target's health per level of Blood Union. Removes all Blood Union on use.");
        setUsage("/skill bloodritual <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill bloodritual");
        setTypes(SkillType.HEALING, SkillType.SILENCEABLE, SkillType.ABILITY_PROPERTY_DARK);
    }

    public String getDescription(Hero hero) {
        double healthMultiplier = SkillConfigManager.getUseSetting(hero, this, "blood-union-health-multiplier", 0.1, false);
        double healthMultiplierIncrease = SkillConfigManager.getUseSetting(hero, this, "blood-union-health-multiplier-increase-per-wisdom", 0.1, false);
        healthMultiplier += hero.getAttributeValue(AttributeType.WISDOM) * healthMultiplierIncrease;

        String formattedHealthMultiplier = Util.decFormat.format(healthMultiplier * 100);

        return getDescription().replace("$1", formattedHealthMultiplier);
    }

    public ConfigurationSection getDefaultConfig() {

        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 8);
        node.set("blood-union-health-multiplier", 0.0625);
        node.set("blood-union-health-multiplier-increase-per-wisdom", 0.0016);

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
            Messaging.send(player, "Target is already at full health.");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        if (!hero.hasEffect("BloodUnionEffect")) {
            return SkillResult.FAIL;
        }

        // Get Blood Union Level
        BloodUnionEffect buEffect = (BloodUnionEffect) hero.getEffect("BloodUnionEffect");
        int bloodUnionLevel = buEffect.getBloodUnionLevel();

        if (bloodUnionLevel < 1) {
            // Display No Blood Union Error Text
            Messaging.send(player, "You must have at least 1 Blood Union to use this ability!");
            return SkillResult.FAIL;
        }

        // Increase healing based on wisdom and blood union level
        double healthMultiplier = SkillConfigManager.getUseSetting(hero, this, "blood-union-health-multiplier", 0.1, false);
        double healthMultiplierIncrease = SkillConfigManager.getUseSetting(hero, this, "blood-union-health-multiplier-increase-per-wisdom", 0.1, false);
        healthMultiplier += hero.getAttributeValue(AttributeType.WISDOM) * healthMultiplierIncrease;
        healthMultiplier *= bloodUnionLevel;

        double healAmount = healthMultiplier * target.getMaxHealth();

        // Ensure they can be healed.
        HeroRegainHealthEvent hrhEvent = new HeroRegainHealthEvent(targetHero, healAmount, this, hero);
        plugin.getServer().getPluginManager().callEvent(hrhEvent);
        if (hrhEvent.isCancelled()) {
            Messaging.send(player, "Unable to heal the target at this time!");
            return SkillResult.CANCELLED;
        }

        broadcastExecuteText(hero, target);

        // Heal target
        targetHero.heal(hrhEvent.getAmount());

        // Set Blood Union to 0
        buEffect.setBloodUnionLevel(0);

        // Play effect
        try {
            fplayer.playFirework(player.getWorld(), target.getLocation().add(0.0D, 1.5D, 0.0D), FireworkEffect.builder().flicker(false)
                    .trail(false).with(FireworkEffect.Type.BURST).withColor(Color.MAROON).withFade(Color.WHITE).build());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return SkillResult.NORMAL;
    }
}