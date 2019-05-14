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
        double radius = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.RADIUS, false);
        double healing = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.HEALING, false);
        healing = getScaledHealing(hero, healing);
        int removals = SkillConfigManager.getScaledUseSettingInt(hero, this, "max-removals", true);

        return getDescription()
                .replace("$1", Util.decFormat.format(healing))
                .replace("$2", removals + "")
                .replace("$3", Util.decFormat.format(radius))
                .replace("$4", Util.decFormat.format(healing * Heroes.properties.selfHeal));
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


        double radius = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.RADIUS, false);
        double radiusSquared = radius * radius;

        double healing = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.HEALING, false);
        healing = getScaledHealing(hero, healing);

        int removals = SkillConfigManager.getScaledUseSettingInt(hero, this, "max-removals", true);

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

    private void healDispel(Hero targetHero, int removals, double healAmount, Hero healer) {
        targetHero.tryHeal(healer, this, healAmount);
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
