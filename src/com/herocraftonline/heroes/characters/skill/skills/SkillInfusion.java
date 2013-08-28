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
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.characters.skill.VisualEffect;
import com.herocraftonline.heroes.util.Messaging;

public class SkillInfusion extends TargettedSkill {
    public VisualEffect fplayer = new VisualEffect();

    public SkillInfusion(Heroes plugin) {
        super(plugin, "Infusion");
        setDescription("Infuse your target with life, restoring $1 of their health and negating their bleeding. Healing is improved by $2% per level of Blood Union. This ability costs $3 health and $4 mana to use.");
        setUsage("/skill infusion <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill infusion");
        setTypes(SkillType.HEALING, SkillType.SILENCABLE, SkillType.ABILITY_PROPERTY_MAGICAL);
    }

    public String getDescription(Hero hero) {

        int healing = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING.node(), Integer.valueOf(130), false);
        double healingIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING_INCREASE_PER_WISDOM.node(), Double.valueOf(1.8), false);
        healing += (int) (hero.getAttributeValue(AttributeType.WISDOM) * healingIncrease);

        int healthCost = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALTH_COST.node(), Integer.valueOf(85), false);
        int manacost = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MANA.node(), Integer.valueOf(110), false);

        int healIncrease = (int) (SkillConfigManager.getUseSetting(hero, this, "health-increase-percent-per-blood-union", Double.valueOf(0.04), false) * 100);

        return getDescription().replace("$1", healing + "").replace("$2", healIncrease + "").replace("$3", healthCost + "").replace("$4", manacost + "");
    }

    public ConfigurationSection getDefaultConfig() {

        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), Integer.valueOf(8));
        node.set("health-increase-percent-per-blood-union", Double.valueOf(0.03));
        node.set(SkillSetting.HEALING.node(), Integer.valueOf(75));
        node.set(SkillSetting.HEALING_INCREASE_PER_WISDOM.node(), Double.valueOf(1.0));
        node.set(SkillSetting.HEALTH_COST.node(), Integer.valueOf(35));
        node.set(SkillSetting.MANA.node(), Integer.valueOf(80));

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
            if (player.equals(targetHero.getPlayer()))
                Messaging.send(player, "You are already at full health.", new Object[0]);
            else {
                Messaging.send(player, "Target is already at full health.", new Object[0]);
            }

            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        double healAmount = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING.node(), Integer.valueOf(75), false);
        double wisHealIncrease = (hero.getAttributeValue(AttributeType.WISDOM) * SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING_INCREASE_PER_WISDOM.node(), Double.valueOf(1.0), false));
        healAmount += wisHealIncrease;

        // Get Blood Union Level
        int bloodUnionLevel = 0;
        if (hero.hasEffect("BloodUnionEffect")) {
            BloodUnionEffect buEffect = (BloodUnionEffect) hero.getEffect("BloodUnionEffect");

            bloodUnionLevel = buEffect.getBloodUnionLevel();
        }

        // Increase healing based on blood union level
        double healIncrease = SkillConfigManager.getUseSetting(hero, this, "health-increase-percent-per-blood-union", Double.valueOf(0.03), false);
        healIncrease = 1 + (healIncrease *= bloodUnionLevel);
        healAmount *= healIncrease;

        // Ensure they can be healed.
        HeroRegainHealthEvent hrhEvent = new HeroRegainHealthEvent(targetHero, healAmount, this, hero);
        this.plugin.getServer().getPluginManager().callEvent(hrhEvent);
        if (hrhEvent.isCancelled()) {
            Messaging.send(player, "Unable to heal your target at this time!", new Object[0]);
            return SkillResult.CANCELLED;
        }

        broadcastExecuteText(hero, target);

        // Heal target
        targetHero.heal(hrhEvent.getAmount());

        // Remove bleeds
        for (Effect effect : targetHero.getEffects()) {
            if (effect.isType(EffectType.BLEED) && effect.isType(EffectType.HARMFUL)) {
                targetHero.removeEffect(effect);
            }
        }

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