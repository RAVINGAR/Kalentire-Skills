package com.herocraftonline.heroes.characters.skill.public1;

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
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SkillDispel extends TargettedSkill {

    public SkillDispel(Heroes plugin) {
        super(plugin, "Dispel");
        this.setDescription("You remove up to $1 magical effects from your target.");
        this.setUsage("/skill dispel");
        this.setArgumentRange(0, 1);
        this.setIdentifiers("skill dispel");
        this.setTypes(SkillType.SILENCEABLE);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();
        node.set("max-removals", 3);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        final Player player = hero.getPlayer();

        boolean removed = false;
        int maxRemovals = SkillConfigManager.getUseSetting(hero, this, "max-removals", 3, false);
        // if player is targetting itself
        if (target.equals(player)) {
            for (final Effect effect : hero.getEffects()) {
                if (effect.isType(EffectType.DISPELLABLE) && effect.isType(EffectType.HARMFUL) && effect.isType(EffectType.MAGIC)) {
                    hero.removeEffect(effect);
                    removed = true;
                    maxRemovals--;
                    if (maxRemovals == 0) {
                        break;
                    }
                }
            }
        } else {
            final CharacterTemplate character = this.plugin.getCharacterManager().getCharacter(target);
            boolean removeHarmful = false;
            if (hero.hasParty() && (character instanceof Hero)) {
                // If the target is a partymember lets make sure we only remove harmful effects
                if (hero.getParty().isPartyMember((Hero) character)) {
                    removeHarmful = true;
                }
            } else if (character.hasEffect("Summon")) {
                removeHarmful = ((SummonEffect) character.getEffect("Summon")).getSummoner().equals(hero);
            }
            for (final Effect effect : character.getEffects()) {
                if (effect.isType(EffectType.DISPELLABLE) && effect.isType(EffectType.MAGIC)) {
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
            this.broadcastExecuteText(hero, target);
            return SkillResult.NORMAL;
        }
        player.sendMessage(ChatColor.GRAY + "The target has nothing to dispel!");
        return SkillResult.INVALID_TARGET_NO_MSG;
    }

    @Override
    public String getDescription(Hero hero) {
        final int removals = SkillConfigManager.getUseSetting(hero, this, "max-removals", 3, false);
        return this.getDescription().replace("$1", removals + "");
    }

}
