package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainHealthEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Setting;

public class SkillHealingChorus extends ActiveSkill {

    public SkillHealingChorus(Heroes plugin) {
        super(plugin, "HealingChorus");
        setDescription("You restore $1 health and dispel negative effects from all nearby party-members.");
        setUsage("/skill healingchorus");
        setArgumentRange(0, 0);
        setIdentifiers("skill healingchorus");
        setTypes(SkillType.SILENCABLE, SkillType.HEAL, SkillType.LIGHT);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("heal-amount", 10);
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
                    healDispel(p, removals, healAmount, hero);
                }
            }
        } else {
            healDispel(hero, removals, healAmount, hero);
        }
        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

    private void healDispel(Hero targetHero, int removals, int healAmount, Hero hero) {
        HeroRegainHealthEvent hrhEvent = new HeroRegainHealthEvent(targetHero, healAmount, this, hero);
        if (!hrhEvent.isCancelled()) {
            if (targetHero.getHealth() < targetHero.getMaxHealth()) {
                targetHero.setHealth(targetHero.getHealth() + hrhEvent.getAmount());
                targetHero.syncHealth();
            }
        }
        if (removals == 0)
            return;

        if (targetHero.getPlayer().getFireTicks() > 0) {
            removals--;
            targetHero.getPlayer().setFireTicks(0);
            if (removals == 0)
                return;
        }

        for (Effect effect : targetHero.getEffects()) {
            if (effect.isType(EffectType.HARMFUL)) {
                targetHero.removeEffect(effect);
                removals--;
                if (removals == 0) {
                    break;
                }
            }
        }
    }

    @Override
    public String getDescription(Hero hero) {
        int amount = SkillConfigManager.getUseSetting(hero, this, "heal-amount", 10, false);
        return getDescription().replace("$1", amount + "");
    }

}