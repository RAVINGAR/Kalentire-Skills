package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

public class SkillAntidote extends TargettedSkill {

    public SkillAntidote(Heroes plugin) {
        super(plugin, "Antidote");
        this.setDescription("Cures your target of poisons");
        this.setUsage("/skill antidote <target>");
        this.setArgumentRange(0, 1);
        this.setIdentifiers("skill antidote");
        this.setTypes(SkillType.SILENCEABLE, SkillType.HEALING);
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        final Player player = hero.getPlayer();
        if (target instanceof Player) {
            final Hero targetHero = this.plugin.getCharacterManager().getHero((Player) target);
            boolean cured = false;
            for (final Effect effect : targetHero.getEffects()) {
                if (effect.isType(EffectType.POISON) && !effect.isType(EffectType.BENEFICIAL)) {
                    cured = true;
                    targetHero.removeEffect(effect);
                }
            }
            if (target.hasPotionEffect(PotionEffectType.POISON)) {
                target.removePotionEffect(PotionEffectType.POISON);
                cured = true;
            }
            if (!cured) {
                player.sendMessage(ChatColor.GRAY + "Your target is not poisoned!");
                return SkillResult.INVALID_TARGET_NO_MSG;
            } else {
                this.broadcastExecuteText(hero, target);
            }
            return SkillResult.NORMAL;
        }
        return SkillResult.INVALID_TARGET;
    }

    @Override
    public String getDescription(Hero hero) {
        return this.getDescription();
    }
}
