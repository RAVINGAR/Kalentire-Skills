package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Messaging;

public class SkillPurge extends TargettedSkill {

    public SkillPurge(Heroes plugin) {
        super(plugin, "Purge");
        setDescription("You purge effects from anyone near your target.");
        setUsage("/skill purge");
        setArgumentRange(0, 0);
        setIdentifiers("skill purge");
        setTypes(SkillType.SILENCABLE);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("max-removals", -1);
        node.set(SkillSetting.RADIUS.node(), 10);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();
        if (target instanceof Player && (hero.getParty() == null || !hero.getParty().isPartyMember((Player) target))) {
            if (!damageCheck(player, target))
            	return SkillResult.INVALID_TARGET;
        }

        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 10, false);
        int removalsLeft = SkillConfigManager.getUseSetting(hero, this, "max-removals", -1, true);
        int maxRemovals = removalsLeft;
        for (Entity e : target.getNearbyEntities(radius, radius, radius)) {
            if (!(e instanceof LivingEntity))
                continue;
            
            if (removalsLeft == 0)
                break;
            
            if (e instanceof Player) {
                removalsLeft = purge(plugin.getCharacterManager().getHero((Player) e), removalsLeft, hero);
            } else {
                removalsLeft = purge(plugin.getCharacterManager().getMonster((LivingEntity) e), removalsLeft, hero);
            }
        }

        if (maxRemovals != removalsLeft) {
            broadcastExecuteText(hero);
            return SkillResult.NORMAL;
        } else {
            Messaging.send(player, "No valid targets in range.");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }
    }  

    private int purge(Monster monster, int removalsLeft, Hero hero) {
        //Return immediately if this creature has no effects
        if (monster.getEffects().isEmpty()) {
            return removalsLeft;
        }
        
        boolean removeHarmful = false;
        if (hero.getSummons().contains(monster)) {
            removeHarmful = true;
        }
        
        for (Effect effect : monster.getEffects()) {
            if (removalsLeft == 0) {
                break;
            } else if (effect.isType(EffectType.HARMFUL) && effect.isType(EffectType.DISPELLABLE) && removeHarmful) {
                monster.removeEffect(effect);
                removalsLeft--;
            } else if (effect.isType(EffectType.BENEFICIAL) && effect.isType(EffectType.DISPELLABLE) && !removeHarmful) {
                monster.removeEffect(effect);
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

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }
}