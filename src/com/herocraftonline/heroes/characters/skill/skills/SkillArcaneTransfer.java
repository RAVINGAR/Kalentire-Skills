package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;

public class SkillArcaneTransfer extends TargettedSkill {

    public SkillArcaneTransfer(Heroes plugin) {
        super(plugin, "ArcaneTransfer");
        setDescription("Transfer up to $1 of your harmful effects to your target.");
        setUsage("/skill arcanetransfer");
        setArgumentRange(0, 0);
        setIdentifiers("skill arcanetransfer");
        setTypes(SkillType.SILENCEABLE, SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.AGGRESSIVE);
    }

    @Override
    public String getDescription(Hero hero) {
        int maxTransfers = SkillConfigManager.getUseSetting(hero, this, "max-transfers", 2, false);
        double maxTransfersIncrease = SkillConfigManager.getUseSetting(hero, this, "max-transfers-increase-per-intellect", 2, false);
        maxTransfers += (int) (hero.getAttributeValue(AttributeType.INTELLECT) * maxTransfersIncrease);

        return getDescription().replace("$1", maxTransfers + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 8);
        node.set("max-transfers", 1);
        node.set("max-transfers-increase-per-intellect", 0.025);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        ArrayList<Effect> possibleEffects = new ArrayList<>();
        for (Effect effect : hero.getEffects()) {
            if (effect.isType(EffectType.HARMFUL) && effect.isType(EffectType.DISPELLABLE)) {
                possibleEffects.add(effect);
            }
        }

        if (possibleEffects.isEmpty() && player.getFireTicks() < 1) {
            Messaging.send(player, "You have no effects to transfer!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        CharacterTemplate targCT = plugin.getCharacterManager().getCharacter(target);


        int maxTransfers = SkillConfigManager.getUseSetting(hero, this, "max-transfers", 2, false);
        double maxTransfersIncrease = SkillConfigManager.getUseSetting(hero, this, "max-transfers-increase-per-intellect", 2, false);
        maxTransfers += (int) (hero.getAttributeValue(AttributeType.INTELLECT) * maxTransfersIncrease);

        int removedEffects = 0;
        if (player.getFireTicks() > 0 && maxTransfers > 0) {
            // Transfer fire ticks
            int fireTicks = player.getFireTicks();
            player.setFireTicks(0);
            target.setFireTicks(fireTicks);
            removedEffects++;
        }

        // Transfer debuffs
        for (int i = removedEffects; i < maxTransfers && possibleEffects.size() > 0; i++) {
            Effect stolenEffect = possibleEffects.get(Util.nextInt(possibleEffects.size()));
            hero.removeEffect(stolenEffect);

            if (stolenEffect instanceof ExpirableEffect) {
                ((ExpirableEffect) stolenEffect).setApplier(player);
            }

            targCT.addEffect(stolenEffect);
            possibleEffects.remove(stolenEffect);
        }

        player.getWorld().playSound(player.getLocation(), Sound.GHAST_MOAN, 0.8F, 1.0F);
        broadcastExecuteText(hero, target);

        return SkillResult.NORMAL;
    }
}