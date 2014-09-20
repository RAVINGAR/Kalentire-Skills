package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.google.common.collect.Lists;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.ncp.NCPFunction;
import com.herocraftonline.heroes.characters.skill.ncp.NCPUtils;
import com.herocraftonline.heroes.util.Util;

import fr.neatmonster.nocheatplus.checks.CheckType;

public class SkillTumble extends PassiveSkill {

    public SkillTumble(Heroes plugin) {
        super(plugin, "Tumble");
        setDescription("Passive: $1");
        setEffectTypes(EffectType.BENEFICIAL, EffectType.PHYSICAL);
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {

        String description = " ";

        double distance = SkillConfigManager.getUseSetting(hero, this, "base-distance", 0, false);
        double distanceIncrease = SkillConfigManager.getUseSetting(hero, this, "distance-increase-per-agility-level", 0.25, false);
        distance += (hero.getAttributeValue(AttributeType.AGILITY) * distanceIncrease) + 3;

        if (distance == 3)
            description = "You aren't very good at breaking your fall, and will take full fall damage when falling down a block height greater than 3.";
        else if (distance > 0 && distance < 3)
            description = "You are terrible at bracing yourself, and will take " + Util.decFormat.format(distance) + " additional blocks of fall damage when falling down a block height greater than 3!";
        else if (distance < 0)
            description = "You are extremely terrible at bracing yourself, and will take an additional " + Util.decFormat.format(3 + (distance * -1)) + " blocks of fall damage when falling down a block height greater than 3!";
        else
            description = "You are adept at bracing yourself, and will only take fall damage when falling down a block height greater than " + Util.decFormat.format(distance) + "!";

        return getDescription().replace("$1", description);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set("base-distance", 0);
        node.set("distance-increase-per-agility-level", 0.25);
        node.set("ncp-exemption-duration", 100);

        return node;
    }

    public class SkillEntityListener implements Listener {

        private Skill skill;

        SkillEntityListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.LOW)
        public void onEntityDamage(final EntityDamageEvent event) {
            if (!(event.getEntity() instanceof Player) || event.getCause() != DamageCause.FALL) {
                return;
            }
            Hero hero = plugin.getCharacterManager().getHero((Player) event.getEntity());
            if (!hero.hasEffect("Tumble") || hero.hasEffectType(EffectType.SAFEFALL)) {
                return;
            }

            double distance = SkillConfigManager.getUseSetting(hero, skill, "base-distance", 0, false);
            double distanceIncrease = SkillConfigManager.getUseSetting(hero, skill, "distance-increase-per-agility-level", 0.25, false);
            distance += hero.getAttributeValue(AttributeType.AGILITY) * distanceIncrease;

            double fallDistance = event.getDamage();

            // Heroes.log(Level.INFO, "OriginalFallDistance: " + fallDistance + ", Tumble Fall Reduction: " + distance);

            fallDistance -= distance;

            // Let's bypass the nocheat issues...
            final double fallDamage = fallDistance;
            NCPUtils.applyExemptions(event.getEntity(), new NCPFunction() {
                
                @Override
                public void execute()
                {
                    if (fallDamage <= 0) {
                        event.setCancelled(true);
                    }
                    else {
                        event.setDamage(fallDamage);
                    }
                }
            }, Lists.newArrayList(CheckType.MOVING), SkillConfigManager.getUseSetting(hero, skill, "ncp-exemption-duration", 100, false));
        }
    }
}
