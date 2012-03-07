package com.herocraftonline.heroes.characters.skill.skills;

import net.minecraft.server.EntityPlayer;
import net.minecraft.server.MobEffectList;

import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Messaging;

public class SkillAntidote extends TargettedSkill {

    public SkillAntidote(Heroes plugin) {
        super(plugin, "Antidote");
        setDescription("Cures your target of poisons");
        setUsage("/skill antidote <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill antidote");
        setTypes(SkillType.SILENCABLE, SkillType.HEAL);
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();
        if (target instanceof Player) {
            Hero targetHero = plugin.getCharacterManager().getHero((Player) target);
            boolean cured = false;
            for (Effect effect : targetHero.getEffects()) {
                if (effect.isType(EffectType.POISON) && !effect.isType(EffectType.BENEFICIAL)) {
                    cured = true;
                    targetHero.removeEffect(effect);
                }
            }
            EntityPlayer tp = ((CraftPlayer) targetHero.getPlayer()).getHandle();
            if (tp.hasEffect(MobEffectList.POISON)) {
                tp.effects.remove(MobEffectList.POISON.id);
                cured = true;
            }
            if (!cured) {
                Messaging.send(player, "Your target is not poisoned!");
                return SkillResult.INVALID_TARGET_NO_MSG;
            } else {
                broadcastExecuteText(hero, target);
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
