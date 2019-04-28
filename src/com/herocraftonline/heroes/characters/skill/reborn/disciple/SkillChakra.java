package com.herocraftonline.heroes.characters.skill.reborn.disciple;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.GeometryUtil;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.List;

public class SkillChakra extends ActiveSkill {

    public SkillChakra(Heroes plugin) {
        super(plugin, "Chakra");
        setDescription("You restore $1 health and dispel up to $2 negative effects from all party-members within $3 blocks. " +
                "You are only healed for $4 health from this ability however.");
        setUsage("/skill chakra");
        setArgumentRange(0, 0);
        setIdentifiers("skill chakra");
        setTypes(SkillType.SILENCEABLE, SkillType.AREA_OF_EFFECT, SkillType.HEALING, SkillType.DISPELLING, SkillType.ABILITY_PROPERTY_LIGHT);
    }

    @Override
    public String getDescription(Hero hero) {
        int wisdom = hero.getAttributeValue(AttributeType.WISDOM);

        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5, false);
        double radiusIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS_INCREASE_PER_WISDOM, 0.125, false);
        radius += (int) (wisdom * radiusIncrease);

        double healing = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING, 75, false);
        healing = getScaledHealing(hero, healing);
        double healingIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING_INCREASE_PER_WISDOM, 0.875, false);
        healing += (wisdom * healingIncrease);

        int removals = SkillConfigManager.getUseSetting(hero, this, "max-removals", 0, true);
        double removalsIncrease = SkillConfigManager.getUseSetting(hero, this, "max-removals-increase-per-wisdom", 0.05, false);
        removals += Math.floor((wisdom * removalsIncrease));     // Round down

        String formattedHealing = Util.decFormat.format(healing);
        String formattedSelfHealing = Util.decFormat.format(healing * Heroes.properties.selfHeal);

        return getDescription().replace("$1", formattedHealing).replace("$2", removals + "").replace("$3", radius + "").replace("$4", formattedSelfHealing);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.RADIUS.node(), 5);
        node.set(SkillSetting.RADIUS_INCREASE_PER_WISDOM.node(), 0.125);
        node.set(SkillSetting.HEALING.node(), 75);
        node.set(SkillSetting.HEALING_INCREASE_PER_WISDOM.node(), 0.875);
        node.set("max-removals", 0);
        node.set("max-removals-increase-per-wisdom", 0.05);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        Location castLoc = player.getLocation().clone();

        int wisdom = hero.getAttributeValue(AttributeType.WISDOM);

        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5, false);
        double radiusIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS_INCREASE_PER_WISDOM, 0.125, false);
        radius += (int) (wisdom * radiusIncrease);
        int radiusSquared = radius * radius;

        double healing = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING, 75, false);
        healing = getScaledHealing(hero, healing);
        double healingIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING_INCREASE_PER_WISDOM, 0.875, false);
        healing += (wisdom * healingIncrease);

        int removals = SkillConfigManager.getUseSetting(hero, this, "max-removals", 0, true);
        double removalsIncrease = SkillConfigManager.getUseSetting(hero, this, "max-removals-increase-per-wisdom", 0.05, false);
        removals += Math.floor(wisdom * removalsIncrease);     // Round down

        if (hero.hasParty()) {
            for (Hero p : hero.getParty().getMembers()) {
                if (!castLoc.getWorld().equals(p.getPlayer().getWorld())) {
                    continue;
                }
                if (castLoc.distanceSquared(p.getPlayer().getLocation()) <= radiusSquared) {
                    healDispel(p, removals, healing, hero);
                }
            }
        } else {
            healDispel(hero, removals, healing, hero);
        }

        broadcastExecuteText(hero);

        List<Location> circle1 = GeometryUtil.circle(player.getLocation(), 72, radius);
        for (int i = 0; i < circle1.size(); i++) {
            //player.getWorld().spigot().playEffect(circle(player.getLocation(), 72, radius).get(i), org.bukkit.Effect.INSTANT_SPELL, 0, 0, 0, 0, 0, 0, 16, 16);
            player.getWorld().spawnParticle(Particle.SPELL_INSTANT, circle1.get(i), 16, 0, 0, 0, 0);
        }

        List<Location> circle2 = GeometryUtil.circle(player.getLocation(), 36, radius / 2f);
        for (int i = 0; i < circle2.size(); i++) {
            //player.getWorld().spigot().playEffect(circle(player.getLocation(), 36, radius / 2).get(i), org.bukkit.Effect.INSTANT_SPELL, 0, 0, 0, 0, 0, 0, 16, 16);
            player.getWorld().spawnParticle(Particle.SPELL_INSTANT, circle2.get(i), 16, 0, 0, 0, 0);
        }

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8F, 1.0F);

        return SkillResult.NORMAL;
    }

    private void healDispel(Hero targetHero, int removals, double healAmount, Hero hero) {
        targetHero.tryHeal(hero, this, healAmount);
        if (removals == 0)
            return;

        for (Effect effect : targetHero.getEffects()) {
            if (effect.isType(EffectType.HARMFUL) && effect.isType(EffectType.DISPELLABLE)) {
                targetHero.removeEffect(effect);
                removals--;
                if (removals == 0) {
                    break;
                }
            }
        }
    }
}
