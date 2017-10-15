package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.CompatSound;
import com.herocraftonline.heroes.util.Messaging;

public class SkillAntidote extends TargettedSkill {

    public SkillAntidote(Heroes plugin) {
        super(plugin, "Antidote");
        setDescription("You cure your target of poisons");
        setUsage("/skill antidote <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill antidote");
        setTypes(SkillType.SILENCEABLE, SkillType.DISPELLING);
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();
        if (target instanceof Player) {
            hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), CompatSound.ENTITY_EXPERIENCE_ORB_PICKUP.value() , 0.5F, 1.0F); 
            Hero targetHero = plugin.getCharacterManager().getHero((Player) target);
            boolean cured = false;
            for (Effect effect : targetHero.getEffects()) {
                if (effect.isType(EffectType.POISON) && effect.isType(EffectType.HARMFUL) && effect.isType(EffectType.DISPELLABLE)) {
                    cured = true;
                    targetHero.removeEffect(effect);
                }
            }
            Player targetPlayer = targetHero.getPlayer();
            if (targetPlayer.hasPotionEffect(PotionEffectType.POISON)) {
                targetPlayer.removePotionEffect(PotionEffectType.POISON);
                cured = true;
            }
            if (!cured) {
                Messaging.send(player, "Your target is not poisoned!");
                return SkillResult.INVALID_TARGET_NO_MSG;
            } else {
                broadcastExecuteText(hero, target);

                player.getWorld().playEffect(player.getLocation().add(0, 2.5, 0), org.bukkit.Effect.HAPPY_VILLAGER, 3);
                player.getWorld().playEffect(player.getLocation().add(0, 2.5, 0), org.bukkit.Effect.HAPPY_VILLAGER, 3);
                player.getWorld().playEffect(player.getLocation().add(0, 2.5, 0), org.bukkit.Effect.HAPPY_VILLAGER, 3);
            }
            return SkillResult.NORMAL;
        }
        return SkillResult.INVALID_TARGET;
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }
}
