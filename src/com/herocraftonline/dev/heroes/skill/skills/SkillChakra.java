package com.herocraftonline.dev.heroes.skill.skills;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.api.SkillResult;
import com.herocraftonline.dev.heroes.effects.Effect;
import com.herocraftonline.dev.heroes.effects.EffectType;
import com.herocraftonline.dev.heroes.hero.Hero;
import com.herocraftonline.dev.heroes.skill.ActiveSkill;
import com.herocraftonline.dev.heroes.skill.SkillConfigManager;
import com.herocraftonline.dev.heroes.skill.SkillType;
import com.herocraftonline.dev.heroes.util.Setting;

public class SkillChakra extends ActiveSkill {

    public SkillChakra(Heroes plugin) {
        super(plugin, "Chakra");
        setDescription("Dispels and heals party members near you");
        setUsage("/skill chakra");
        setArgumentRange(0, 0);
        setIdentifiers("skill chakra");
        setTypes(SkillType.SILENCABLE, SkillType.HEAL, SkillType.LIGHT);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("heal_amount", 10);
        node.set(Setting.RADIUS.node(), 7);
        node.set("max-removals", -1);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        Location castLoc = player.getLocation().clone();
        int radius = SkillConfigManager.getUseSetting(hero, this, Setting.RADIUS, 7, false);
        int radiusSquared = radius * radius;
        int healAmount = SkillConfigManager.getUseSetting(hero, this, "heal-amount", 10, false);
        int removals = SkillConfigManager.getUseSetting(hero, this, "max-removals", -1, true);
        if (hero.hasParty()) {
            for (Hero p : hero.getParty().getMembers()) {
                if (!castLoc.getWorld().equals(p.getPlayer().getWorld())) {
                    continue;
                }
                if (castLoc.distanceSquared(p.getPlayer().getLocation()) <= radiusSquared) {
                    healDispel(p, removals, healAmount);
                }
            }
        } else {
            healDispel(hero, removals, healAmount);
        }
        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

    private void healDispel(Hero hero, int removals, int healAmount) {
        if (hero.getHealth() < hero.getMaxHealth()) {
            hero.setHealth(hero.getHealth() + healAmount);
            hero.syncHealth();
        }
        if (removals == 0)
            return;

        if (hero.getPlayer().getFireTicks() > 0) {
            removals--;
            hero.getPlayer().setFireTicks(0);
            if (removals == 0)
                return;
        }

        for (Effect effect : hero.getEffects()) {
            if (effect.isType(EffectType.HARMFUL)) {
                hero.removeEffect(effect);
                removals--;
                if (removals == 0) {
                    break;
                }
            }
        }
    }

}