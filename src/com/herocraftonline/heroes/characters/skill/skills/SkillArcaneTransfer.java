package com.herocraftonline.heroes.characters.skill.skills;

import java.util.ArrayList;

import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicDamageEffect;
import com.herocraftonline.heroes.characters.effects.PeriodicHealEffect;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

public class SkillArcaneTransfer extends TargettedSkill {

    public SkillArcaneTransfer(Heroes plugin) {
        super(plugin, "ArcaneTransfer");
        setDescription("Transfer up to $1 of your harmful effects to your target.");
        setUsage("/skill arcanetransfer");
        setArgumentRange(0, 0);
        setIdentifiers("skill arcanetransfer");
        setTypes(SkillType.SILENCABLE, SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.AGGRESSIVE);
    }

    @Override
    public String getDescription(Hero hero) {
        int removals = SkillConfigManager.getUseSetting(hero, this, "max-transfers", 2, false);

        return getDescription().replace("$1", removals + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set("max-transfers", 2);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        ArrayList<Effect> possibleEffects = new ArrayList<Effect>();
        for (Effect effect : hero.getEffects()) {
            if (effect.isType(EffectType.HARMFUL) && effect.isType(EffectType.DISPELLABLE)) {
                possibleEffects.add(effect);
            }
        }

        if (possibleEffects.isEmpty() && player.getFireTicks() < 1) {
            Messaging.send(player, "You have no effects to transfer!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        CharacterTemplate targCT = plugin.getCharacterManager().getCharacter((LivingEntity) target);

        // Transfer fire ticks
        int maxTransfers = SkillConfigManager.getUseSetting(hero, this, "max-transfers", 2, false);
        int removedEffects = 0;
        if (player.getFireTicks() > 0 && maxTransfers > 0) {
            int fireTicks = player.getFireTicks();
            player.setFireTicks(0);
            target.setFireTicks(fireTicks);
            removedEffects++;
        }

        // Transfer debuffs
        for (int i = removedEffects; i < maxTransfers && possibleEffects.size() > 0; i++) {
            Effect stolenEffect = possibleEffects.get(Util.nextInt(possibleEffects.size()));
            hero.removeEffect(stolenEffect);

            if (stolenEffect instanceof PeriodicDamageEffect) {
                ((PeriodicDamageEffect) stolenEffect).setApplierHero(hero);
            }
            else if (stolenEffect instanceof PeriodicHealEffect) {
                ((PeriodicHealEffect) stolenEffect).setApplierHero(hero);
            }

            targCT.addEffect(stolenEffect);
            possibleEffects.remove(stolenEffect);
        }

        player.getWorld().playSound(player.getLocation(), Sound.GHAST_MOAN, 0.8F, 1.0F);
        broadcastExecuteText(hero, target);

        return SkillResult.NORMAL;
    }
}