package com.herocraftonline.heroes.characters.skill.remastered.a1new;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;

public class SkillCauterize extends TargettedSkill {

    public SkillCauterize(Heroes plugin) {
        super(plugin, "Cauterize");
        setDescription("Cauterize the wounds of your target, extinguishing their fire ticks and removing bleeds.");
        setUsage("/skill cauterize <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill cauterize");
        setTypes(SkillType.SILENCEABLE, SkillType.DISPELLING, SkillType.ABILITY_PROPERTY_FIRE);
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();
        if (!(target instanceof Player)) {
            return SkillResult.INVALID_TARGET;
        }

        Player targetPlayer = (Player) target;
        Hero targetHero = plugin.getCharacterManager().getHero(targetPlayer);
        ArrayList<Effect> possibleEffects = new ArrayList<>();
        for (Effect effect : targetHero.getEffects()) {
            if (effect.isType(EffectType.HARMFUL) && effect.isType(EffectType.BLEED)) {
                possibleEffects.add(effect);
            }
        }

        if (possibleEffects.isEmpty() && targetPlayer.getFireTicks() < 1) {
            player.sendMessage("Your target has nothing to Cauterize!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        broadcastExecuteText(hero, target);

        targetPlayer.setFireTicks(0);

        // Remove bleeds
        if (!possibleEffects.isEmpty()) {
            for (Effect removableEffect : possibleEffects) {
                targetHero.removeEffect(removableEffect);
            }
        }

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_BURN, 1.6F, 1.3F);
        //player.getWorld().spigot().playEffect(target.getLocation(), org.bukkit.Effect.SMOKE, 0, 0, 0.3F, 0.6F, 0.3F, 0.0F, 25, 16);
        player.getWorld().spawnParticle(Particle.SMOKE_NORMAL, target.getLocation(), 25, 0.3, 0.3, 0.3, 0);

        return SkillResult.NORMAL;
    }

}