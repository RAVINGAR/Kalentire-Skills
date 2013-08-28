package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.PeriodicHealEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Util;

public class SkillHealingChorus extends ActiveSkill {
    public SkillHealingChorus(Heroes plugin) {
        super(plugin, "HealingChorus");
        setDescription("You sing a chorus of healing, affecting party members within $1 blocks. The chorus heals them for $2 health over $3 seconds.");
        setUsage("/skill healingchorus");
        setIdentifiers("skill healingchorus");
        setTypes(SkillType.UNINTERRUPTIBLE, SkillType.AREA_OF_EFFECT, SkillType.HEALING, SkillType.ABILITY_PROPERTY_SONG);
        setArgumentRange(0, 0);
    }

    public String getDescription(Hero hero) {
        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, Integer.valueOf(15), false);

        int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, Integer.valueOf(1500), false);
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), Integer.valueOf(3000), false);

        double healing = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING_TICK, Integer.valueOf(17), false);
        double healingIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING_INCREASE_PER_CHARISMA, Double.valueOf(0.175), false);
        healing += (hero.getAttributeValue(AttributeType.CHARISMA) * healingIncrease);

        String formattedHealing = Util.decFormat.format(healing * ((double) duration / (double) period));
        String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription().replace("$1", radius + "").replace("$2", formattedHealing).replace("$3", formattedDuration);
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DURATION.node(), Integer.valueOf(3000));
        node.set(SkillSetting.RADIUS.node(), Integer.valueOf(15));
        node.set(SkillSetting.PERIOD.node(), Integer.valueOf(1500));
        node.set(SkillSetting.HEALING_TICK.node(), Integer.valueOf(17));
        node.set(SkillSetting.HEALING_INCREASE_PER_CHARISMA.node(), Double.valueOf(0.175));
        node.set(SkillSetting.DELAY.node(), Integer.valueOf(1000));

        return node;
    }

    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, Integer.valueOf(15), false);
        int radiusSquared = radius * radius;
        double healing = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING_TICK, Integer.valueOf(17), false);
        double healingIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING_INCREASE_PER_CHARISMA, Double.valueOf(0.175), false);
        healing += (hero.getAttributeValue(AttributeType.CHARISMA) * healingIncrease);

        int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, Integer.valueOf(1500), false);
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), Integer.valueOf(3000), false);

        broadcastExecuteText(hero);

        // Check if the hero has a party
        if (hero.hasParty()) {
            Location playerLocation = player.getLocation();
            // Loop through the player's party members and add the effect as necessary
            for (Hero member : hero.getParty().getMembers()) {
                // Ensure the party member is in the same world.
                if (member.getPlayer().getLocation().getWorld().equals(playerLocation.getWorld())) {
                    // Check to see if they are close enough to the player to receive the buff
                    if (member.getPlayer().getLocation().distanceSquared(playerLocation) <= radiusSquared) {
                        // Add the effect
                        member.addEffect(new PeriodicHealEffect(this, "HealingChorus", period, duration, healing, player));
                    }
                }
            }
        }
        else {
            // Add the effect to just the player
            hero.addEffect(new PeriodicHealEffect(this, "HealingChorus", period, duration, healing, player));
        }

        return SkillResult.NORMAL;
    }
}