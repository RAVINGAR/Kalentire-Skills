package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.common.AttributeDecreaseEffect;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.skills.totem.SkillBaseTotem;
import com.herocraftonline.heroes.characters.skill.skills.totem.Totem;
import com.herocraftonline.heroes.chat.ChatComponents;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class SkillEngulfingTotem extends SkillBaseTotem {

    Map<Hero, List<LivingEntity>> afflictedTargets;
    
    public SkillEngulfingTotem(Heroes plugin) {
        super(plugin, "EngulfingTotem");
        setArgumentRange(0,0);
        setUsage("/skill engulfingtotem");
        setIdentifiers("skill engulfingtotem");
        setDescription("Places an engulfing totem at target location that reduces the dexterity of non-partied entites in a $1 radius by $2. Lasts for $3 seconds.");
        setTypes(SkillType.MOVEMENT_SLOWING, SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.SILENCEABLE, SkillType.AGGRESSIVE, SkillType.AREA_OF_EFFECT);
        material = Material.SOUL_SAND;
        afflictedTargets = new HashMap<Hero, List<LivingEntity>>();
    }

    @Override
    public String getDescription(Hero h) {
        return getDescription()
                .replace("$1", getRange(h) + "")
                .replace("$2", getDexterityReduceAmount(h) + "")
                .replace("$3", getDuration(h)*0.001 + "");
    }

    @Override
    public void usePower(Hero hero, Totem totem) {
        Player heroP = hero.getPlayer();
        List<LivingEntity> heroTargets = afflictedTargets.containsKey(hero) ? afflictedTargets.get(hero) : new ArrayList<LivingEntity>(); 
        List<LivingEntity> totemTargets = totem.getTargets(hero);
        if(!heroTargets.isEmpty()) {
            Iterator<LivingEntity> iter = heroTargets.iterator();
            while(iter.hasNext()) {
                LivingEntity entity = iter.next();
                // If they're still in the totem range, do nothing.
                if(totemTargets.contains(entity)) {
                    continue;
                }
                // Can't work with an invalid entity...
                if(!entity.isValid()) {
                    iter.remove();
                    continue;
                }
                // If the character of it has the effect, we know they're out of range. Remove the effect.
                CharacterTemplate character = plugin.getCharacterManager().getCharacter(entity);
                if(character.hasEffect("EngulfingTotemDexterityEffect")) {
                    EngulfingTotemDexterityEffect oldEffect = (EngulfingTotemDexterityEffect) character.getEffect("EngulfingTotemDexterityEffect");
                    if(oldEffect.getApplier() == heroP) {
                        oldEffect.expire();
                        iter.remove();
                    }
                }
            }
        }
        for(LivingEntity entity : totemTargets) {
            CharacterTemplate character = plugin.getCharacterManager().getCharacter(entity);
            if(character.hasEffect("EngulfingTotemDexterityEffect") ||!damageCheck(heroP, entity)) {
                continue;
            }
            if(entity instanceof Player) {
                character.addEffect(new EngulfingTotemDexterityEffect(this, hero, totem.getEffect().getRemainingTime(), getDexterityReduceAmount(hero), getSlownessAmplitude(hero), null, getExpireText())); //TODO Implicit broadcast() call - may need changes?
                broadcast(entity.getLocation(), getApplyText().replace("$1", entity.getName()).replace("$2", heroP.getName()));
            }
            else {
                character.addEffect(new EngulfingTotemDexterityEffect(this, hero, totem.getEffect().getRemainingTime(), getDexterityReduceAmount(hero), getSlownessAmplitude(hero)));
            }
            heroTargets.add(entity);
        }
        afflictedTargets.put(hero, heroTargets);
    }

    @Override
    public void totemDestroyed(Hero hero, Totem totem) {
        Player heroP = hero.getPlayer();
        List<LivingEntity> heroTargets = afflictedTargets.containsKey(hero) ? afflictedTargets.get(hero) : new ArrayList<LivingEntity>(); 
        if(!heroTargets.isEmpty()) {
            Iterator<LivingEntity> iter = heroTargets.iterator();
            while(iter.hasNext()) {
                LivingEntity entity = iter.next();
                // Can't work with an invalid entity...
                if(entity.isValid()) {
                    CharacterTemplate character = plugin.getCharacterManager().getCharacter(entity);
                    if(character.hasEffect("EngulfingTotemDexterityEffect")) {
                        EngulfingTotemDexterityEffect oldEffect = (EngulfingTotemDexterityEffect) character.getEffect("EngulfingTotemDexterityEffect");
                        if(oldEffect.getApplier() == heroP) {
                            oldEffect.expire();
                        }
                    }
                }
                iter.remove();
            }
        }
    }
    
    @Override
    public ConfigurationSection getSpecificDefaultConfig(ConfigurationSection node) {
        node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "    " + "$1 is engulfed by a totem's power!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "$1 is no longer engulfed by a totem's power.");
        node.set("dexterity-reduce-amount", 3);
        node.set("slowness-amplitude", 2);
        return node;
    }

    // Methods to grab config info that is specific to this skill
    public int getDexterityReduceAmount(Hero h) {
        return SkillConfigManager.getUseSetting(h, this, "dexterity-reduce-amount", 3, false);
    }

    public int getSlownessAmplitude(Hero h) {
        return SkillConfigManager.getUseSetting(h, this, "slowness-amplitude", 2, false);
    }

    public String getApplyText() {
        return SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "$1 is engulfed by a totem's power!");
    }

    public String getExpireText() {
        return SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "$1 is no longer engulfed by a totem's power.");
    }

    private class EngulfingTotemDexterityEffect extends AttributeDecreaseEffect {

        public EngulfingTotemDexterityEffect(SkillEngulfingTotem skill, Hero applier, long duration, int decreaseValue, int slownessAmplitude) {
            this(skill, applier, duration, decreaseValue, slownessAmplitude, null, null);
        }

        public EngulfingTotemDexterityEffect(SkillEngulfingTotem skill, Hero applier, long duration, int decreaseValue, int slownessAmplitude, String applyText, String expireText) {
            super(skill, "EngulfingTotemDexterityEffect", applier.getPlayer(), duration, AttributeType.DEXTERITY, decreaseValue, applyText, expireText);
            types.add(EffectType.SLOW);

            int tickDuration = (int) ((duration / 1000) * 20);
            addPotionEffect(new PotionEffect(PotionEffectType.SLOW, tickDuration, slownessAmplitude), false);
        }
    }
}