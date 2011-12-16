package com.herocraftonline.dev.heroes.skill.skills;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.api.SkillResult;
import com.herocraftonline.dev.heroes.effects.Effect;
import com.herocraftonline.dev.heroes.effects.EffectManager;
import com.herocraftonline.dev.heroes.effects.EffectType;
import com.herocraftonline.dev.heroes.hero.Hero;
import com.herocraftonline.dev.heroes.skill.SkillConfigManager;
import com.herocraftonline.dev.heroes.skill.SkillType;
import com.herocraftonline.dev.heroes.skill.TargettedSkill;
import com.herocraftonline.dev.heroes.util.Messaging;
import com.herocraftonline.dev.heroes.util.Setting;

public class SkillPurge extends TargettedSkill {

    public SkillPurge(Heroes plugin) {
        super(plugin, "Purge");
        setDescription("You purge effects near the targets location");
        setUsage("/skill purge");
        setArgumentRange(0, 0);
        setIdentifiers("skill purge");
        setTypes(SkillType.SILENCABLE);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("max-removals", -1);
        node.set(Setting.RADIUS.node(), 10);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();
        if (target instanceof Player && (hero.getParty() == null || !hero.getParty().isPartyMember((Player) target))) {
            if (!damageCheck(player, target))
            	return SkillResult.INVALID_TARGET;
        }

        int radius = SkillConfigManager.getUseSetting(hero, this, Setting.RADIUS, 10, false);
        int removalsLeft = SkillConfigManager.getUseSetting(hero, this, "max-removals", -1, true);
        int maxRemovals = removalsLeft;
        for (Entity e : target.getNearbyEntities(radius, radius, radius)) {
            if (!(e instanceof LivingEntity))
                continue;
            
            if (removalsLeft == 0)
                break;
            
            if (e instanceof Player) {
                removalsLeft = purge(plugin.getHeroManager().getHero((Player) e), removalsLeft, hero);
            } else
                removalsLeft = purge((LivingEntity) e, removalsLeft, hero);

        }

        if (maxRemovals != removalsLeft) {
            broadcastExecuteText(hero);
            return SkillResult.NORMAL;
        } else {
            Messaging.send(player, "No valid targets in range.");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }
    }  

    private int purge(LivingEntity lEntity, int removalsLeft, Hero hero) {
        EffectManager effectManager = plugin.getEffectManager();
        //Return immediately if this creature has no effects
        if (effectManager.getEntityEffects(lEntity) == null)
            return removalsLeft;
        
        boolean removeHarmful = false;
        if (hero.getSummons().contains(lEntity))
            removeHarmful = true;
        
        for (Effect effect : effectManager.getEntityEffects(lEntity)) {
            if (removalsLeft == 0) {
                break;
            } else if (effect.isType(EffectType.HARMFUL) && effect.isType(EffectType.DISPELLABLE) && removeHarmful) {
                effectManager.removeEntityEffect(lEntity, effect);
                removalsLeft--;
            } else if (effect.isType(EffectType.BENEFICIAL) && effect.isType(EffectType.DISPELLABLE) && !removeHarmful) {
                effectManager.removeEntityEffect(lEntity, effect);
                removalsLeft--;
            }
        }
        return removalsLeft;
    }

    private int purge(Hero tHero, int removalsLeft, Hero hero) {
        boolean removeHarmful = false;
        if (tHero.equals(hero) || (hero.hasParty() && hero.getParty().isPartyMember(tHero))) {
            removeHarmful = true;
        }
        for (Effect effect : tHero.getEffects()) {
            if (removalsLeft == 0) {
                break;
            } else if (effect.isType(EffectType.HARMFUL) && effect.isType(EffectType.DISPELLABLE) && removeHarmful) {
                hero.removeEffect(effect);
                removalsLeft--;
            } else if (effect.isType(EffectType.BENEFICIAL) && effect.isType(EffectType.DISPELLABLE) && !removeHarmful) {
                hero.removeEffect(effect);
                removalsLeft--;
            }
        }
        return removalsLeft;
    }
}