package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.common.SummonEffect;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Messaging;

public class SkillDispel extends TargettedSkill {

    public SkillDispel(Heroes plugin) {
        super(plugin, "Dispel");
        setDescription("You remove up to $1 magical effects from your target.");
        setUsage("/skill dispel");
        setArgumentRange(0, 1);
        setIdentifiers("skill dispel");
        setTypes(SkillType.SILENCABLE);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("max-removals", 3);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        boolean removed = false;
        int maxRemovals = SkillConfigManager.getUseSetting(hero, this, "max-removals", 3, false);
        // if player is targetting itself
        if (target.equals(player)) {
            for (Effect effect : hero.getEffects()) {
                if (effect.isType(EffectType.DISPELLABLE) && effect.isType(EffectType.HARMFUL)) {
                    hero.removeEffect(effect);
                    removed = true;
                    maxRemovals--;
                    if (maxRemovals == 0) {
                        break;
                    }
                }
            }
        } else {
            CharacterTemplate character = plugin.getCharacterManager().getCharacter(target);
            boolean removeHarmful = false;
            if (hero.hasParty() && character instanceof Hero) {
                // If the target is a partymember lets make sure we only remove harmful effects
                if (hero.getParty().isPartyMember((Hero) character)) {
                    removeHarmful = true;
                }
            } else if (character.hasEffect("Summon")) {
                removeHarmful = ((SummonEffect) character.getEffect("Summon")).getSummoner().equals(hero);
            }
            for (Effect effect : character.getEffects()) {
                if (effect.isType(EffectType.DISPELLABLE)) {
                    if (removeHarmful && effect.isType(EffectType.HARMFUL)) {
                        character.removeEffect(effect);
                        removed = true;
                        maxRemovals--;
                        if (maxRemovals == 0) {
                            break;
                        }
                    } else if (!removeHarmful && effect.isType(EffectType.BENEFICIAL)) {
                        character.removeEffect(effect);
                        removed = true;
                        maxRemovals--;
                        if (maxRemovals == 0) {
                            break;
                        }
                    }
                }
            }
        }

        if (removed) {
            broadcastExecuteText(hero, target);
            return SkillResult.NORMAL;
        }
        Messaging.send(player, "The target has nothing to dispel!");
        return SkillResult.INVALID_TARGET_NO_MSG;
    }

    @Override
    public String getDescription(Hero hero) {
        int removals = SkillConfigManager.getUseSetting(hero, this, "max-removals", 3, false);
        return getDescription().replace("$1", removals + "");
    }

}
