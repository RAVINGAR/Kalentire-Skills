package com.herocraftonline.heroes.characters.skill.reborn.defender;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.common.SpeedEffect;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SkillMindandBody extends TargettedSkill {

    public SkillMindandBody(Heroes plugin) {
        super(plugin, "MindandBody");
        setDescription("You break yourself or a allied target free of any effects that impede movement.");
        setUsage("/skill mindandbody <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill mindandbody", "skill mind_and_body", "skill mind");
        setTypes(SkillType.MOVEMENT_PREVENTION_COUNTERING, SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.STEALTHY);
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("speed-multiplier", 0);
        node.set("target-only-allies", true);
        node.set(SkillSetting.DURATION.node(), 1000);
        node.set(SkillSetting.DELAY.node(), 0);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();
        if (!(target instanceof Player)) {
            player.sendMessage("You can only use Mind and Body on yourself and ally players.");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        // Check for ally requirement
        boolean targetOnlyAllies = SkillConfigManager.getUseSetting(hero, this, "target-only-allies", true);
        if (targetOnlyAllies && !hero.isAlliedTo(target))
            return SkillResult.INVALID_TARGET_NO_MSG;

        Player targetPlayer = (Player) target;
        Hero targetHero = plugin.getCharacterManager().getHero(targetPlayer);
        boolean selfTarget = player.equals(targetPlayer);

        // Remove impeding effects if any
        boolean removed = false;
        for (Effect effect : targetHero.getEffects()) {
            if (effect.isType(EffectType.DISPELLABLE) && (effect.isType(EffectType.SLOW) || effect.isType(EffectType.VELOCITY_DECREASING)
                    || effect.isType(EffectType.WALK_SPEED_DECREASING) || effect.isType(EffectType.ROOT))) {
                removed = true;
                targetHero.removeEffect(effect);
            }
        }

        if (removed) {
            int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 7000, false);
            int multiplier = SkillConfigManager.getUseSetting(hero, this, "speed-multiplier", 2, false);
            if (duration > 0 && multiplier > 0) {
                targetHero.addEffect(new SpeedEffect(this, getName(), player, duration, multiplier,
                        "$1 gained a burst of speed!", "$1 returned to normal speed!"));
                broadcastExecuteText(hero, targetPlayer);
                targetPlayer.getWorld().playSound(targetPlayer.getLocation(), Sound.ENTITY_BAT_DEATH, 0.8F, 1.0F);
            }
        } else {
            player.sendMessage("There is no effect impeding " + (selfTarget ? "your" : "their") + " movement!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }
        return SkillResult.NORMAL;
    }
}
