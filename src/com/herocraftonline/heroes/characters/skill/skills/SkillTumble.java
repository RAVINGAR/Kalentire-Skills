package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Util;

import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.hooks.NCPExemptionManager;

public class SkillTumble extends PassiveSkill {

    private boolean ncpEnabled = false;

    public SkillTumble(Heroes plugin) {
        super(plugin, "Tumble");
        setDescription("PASSIVE: $1");
        setEffectTypes(EffectType.BENEFICIAL, EffectType.PHYSICAL);
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(this), plugin);

        if (Bukkit.getServer().getPluginManager().getPlugin("NoCheatPlus") != null) {
            ncpEnabled = true;
        }
    }

    @Override
    public String getDescription(Hero hero) {

        String description = getDescription();

        double distance = SkillConfigManager.getUseSetting(hero, this, "base-distance", Integer.valueOf(0), false);
        double distanceIncrease = SkillConfigManager.getUseSetting(hero, this, "distance-increase-per-agility-level", Double.valueOf(0.25), false);
        distance += (hero.getAttributeValue(AttributeType.AGILITY) * distanceIncrease) + 3;

        if (distance == 3)
            description.replace("$1", "You aren't very good at breaking your fall, and will take full fall damage when falling down a block height greater than 3.");
        else if (distance > 0 && distance < 3)
            description.replace("$1", "You are terrible at bracing yourself, and will take " + Util.decFormat.format(distance) + " additional blocks of fall damage when falling down a block height greater than 3!");
        else if (distance < 0)
            description.replace("$1", "You are extremely terrible at bracing yourself, and will take an additional " + Util.decFormat.format(3 + (distance * -1)) + " blocks of fall damage when falling down a block height greater than 3!");
        else
            description.replace("$1", "You are adept at bracing yourself, and will only take fall damage when falling down a block height greater than " + Util.decFormat.format(distance) + "!");

        return description;
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set("base-distance", Integer.valueOf(0));
        node.set("distance-increase-per-agility-level", Double.valueOf(0.25));
        node.set("ncp-exemption-duration", Integer.valueOf(100));

        return node;
    }

    public class SkillEntityListener implements Listener {

        private Skill skill;

        SkillEntityListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.LOW)
        public void onEntityDamage(EntityDamageEvent event) {
            if (!(event.getEntity() instanceof Player) || event.getCause() != DamageCause.FALL) {
                return;
            }
            Hero hero = plugin.getCharacterManager().getHero((Player) event.getEntity());
            if (!hero.hasEffect("Tumble") || hero.hasEffectType(EffectType.SAFEFALL)) {
                return;
            }

            double distance = SkillConfigManager.getUseSetting(hero, skill, "base-distance", Integer.valueOf(0), false);
            double distanceIncrease = SkillConfigManager.getUseSetting(hero, skill, "distance-increase-per-agility-level", Double.valueOf(0.25), false);
            distance += hero.getAttributeValue(AttributeType.AGILITY) * distanceIncrease;

            double fallDistance = event.getDamage();

            // Heroes.log(Level.INFO, "OriginalFallDistance: " + fallDistance + ", Tumble Fall Reduction: " + distance);

            fallDistance -= distance;

            // Let's bypass the nocheat issues...
            if (ncpEnabled) {
                Player player = (Player) event.getEntity();
                if (!player.isOp()) {
                    long duration = SkillConfigManager.getUseSetting(hero, skill, "ncp-exemption-duration", Integer.valueOf(100), false);
                    NCPExemptionEffect ncpExemptEffect = new NCPExemptionEffect(skill, (Player) event.getEntity(), duration);
                    hero.addEffect(ncpExemptEffect);
                }
            }

            if (fallDistance <= 0) {
                event.setCancelled(true);
            }
            else {
                event.setDamage(fallDistance);
            }
        }
    }

    private class NCPExemptionEffect extends ExpirableEffect {

        public NCPExemptionEffect(Skill skill, Player applier, long duration) {
            super(skill, "NCPExemptionEffect_MOVING", applier, duration);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            final Player player = hero.getPlayer();

            NCPExemptionManager.exemptPermanently(player, CheckType.MOVING_NOFALL);
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            final Player player = hero.getPlayer();

            NCPExemptionManager.unexempt(player, CheckType.MOVING_NOFALL);
        }
    }
}
