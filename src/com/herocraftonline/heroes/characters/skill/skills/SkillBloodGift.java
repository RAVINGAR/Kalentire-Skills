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
import com.herocraftonline.heroes.characters.effects.BloodUnionEffect;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.characters.skill.VisualEffect;
import com.herocraftonline.heroes.util.Messaging;

public class SkillBloodGift extends TargettedSkill {
    public VisualEffect fplayer = new VisualEffect();

    public SkillBloodGift(Heroes plugin) {
        super(plugin, "BloodGift");
        setDescription("You gift an ally with your own blood, restoring $1 of their health. Healing is increased by $4% per level of Blood Union. This ability cannot be used on yourself, and costs $2 health and $3 mana to use.");
        setUsage("/skill bloodgift <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill bloodgift");
        setTypes(SkillType.HEALING, SkillType.SILENCABLE, SkillType.NO_SELF_TARGETTING, SkillType.ABILITY_PROPERTY_DARK, SkillType.ABILITY_PROPERTY_LIGHT);
    }

    public String getDescription(Hero hero) {

        int healing = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALTH.node(), 130, false);
        double healingIncrease = (hero.getAttributeValue(AttributeType.WISDOM) * SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING_INCREASE_PER_WISDOM.node(), 1.8, false));
        healing += healingIncrease;
        
        int healthCost = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALTH_COST.node(), 85, false);
        int manacost = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MANA.node(), 20, false);

        int healIncrease = (int) (SkillConfigManager.getUseSetting(hero, this, "health-increase-percent-per-blood-union", 0.02, false) * 100);

        return getDescription().replace("$1", healing + "").replace("$2", healthCost + "").replace("$3", manacost + "").replace("$4", healIncrease + "");
    }

    public ConfigurationSection getDefaultConfig() {

        ConfigurationSection node = super.getDefaultConfig();

        node.set("health-increase-percent-per-blood-union", Double.valueOf(0.02));
        node.set(SkillSetting.HEALTH.node(), Integer.valueOf(130));
        node.set(SkillSetting.HEALING_INCREASE_PER_WISDOM.node(), Double.valueOf(1.8));
        node.set(SkillSetting.HEALTH_COST.node(), Integer.valueOf(85));
        node.set(SkillSetting.MANA.node(), Integer.valueOf(25));

        return node;
    }

    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();
        if (!(target instanceof Player)) {
            return SkillResult.INVALID_TARGET;
        }

        Hero targetHero = plugin.getCharacterManager().getHero((Player) target);

        // Don't allow self targeting
        if (targetHero.equals(hero)) {
            Messaging.send(player, "You cannot use this ability on yourself!", new Object[0]);
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        double targetHealth = target.getHealth();

        // Check to see if they are at full health
        if (targetHealth >= target.getMaxHealth()) {
            Messaging.send(player, "Target is already at full health.", new Object[0]);
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        double healAmount = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALTH.node(), 130, false);
        double wisHealIncrease = (hero.getAttributeValue(AttributeType.WISDOM) * SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING_INCREASE_PER_WISDOM.node(), 1.8, false));
        healAmount += wisHealIncrease;

        // Get Blood Union Level
        int bloodUnionLevel = 0;
        if (hero.hasEffect("BloodUnionEffect")) {
            BloodUnionEffect buEffect = (BloodUnionEffect) hero.getEffect("BloodUnionEffect");
            bloodUnionLevel = buEffect.getBloodUnionLevel();
        }

        // Increase healing based on blood union level
        double healIncrease = SkillConfigManager.getUseSetting(hero, this, "health-increase-percent-per-blood-union", 0.02, false);
        healIncrease = 1 + (healIncrease *= bloodUnionLevel);
        healAmount *= healIncrease;

        // Ensure they can be healed.
        HeroRegainHealthEvent hrhEvent = new HeroRegainHealthEvent(targetHero, healAmount, this, hero);
        this.plugin.getServer().getPluginManager().callEvent(hrhEvent);
        if (hrhEvent.isCancelled()) {
            Messaging.send(player, "Unable to heal the target at this time!", new Object[0]);
            return SkillResult.CANCELLED;
        }

        broadcastExecuteText(hero, target);

        // Heal target
        targetHero.heal(hrhEvent.getAmount());

        // Play effect
        try {
            this.fplayer.playFirework(player.getWorld(), target.getLocation().add(0.0D, 1.5D, 0.0D), FireworkEffect.builder().flicker(false).trail(false).with(FireworkEffect.Type.BURST).withColor(Color.MAROON).withFade(Color.WHITE).build());
        }
        catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return SkillResult.NORMAL;
    }
}