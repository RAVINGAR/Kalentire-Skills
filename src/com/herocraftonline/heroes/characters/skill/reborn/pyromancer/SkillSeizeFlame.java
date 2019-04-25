package com.herocraftonline.heroes.characters.skill.reborn.pyromancer;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.CombatEffect;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.effects.common.interfaces.Burning;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SkillSeizeFlame extends TargettedSkill {

    private String applyText;
    private String expireText;

    public SkillSeizeFlame(Heroes plugin) {
        super(plugin, "SeizeFlame");
        setDescription("You seize all fire from a target ally and transfering it to you. " +
                "Your target will also become resistant to fire for $1 second(s).");
        setUsage("/skill seizeflame");
        setIdentifiers("skill seizeflame", "skill fshield");
        setArgumentRange(0, 0);
        setTypes(SkillType.ABILITY_PROPERTY_FIRE, SkillType.SILENCEABLE, SkillType.BUFFING, SkillType.NO_SELF_TARGETTING);
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 2000, false);

        return getDescription()
                .replace("$1", Util.decFormat.format(duration / 1000.0));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.DURATION.node(), 2000);
        config.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% has shield %target% from flame!");
        config.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% lost their shield of flame.");
        return config;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT,
                ChatComponents.GENERIC_SKILL + "%hero% has shield %target% from flame!")
                .replace("%target%", "$1")
                .replace("%hero%", "$2");

        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT,
                ChatComponents.GENERIC_SKILL + "%target% lost their shield of flame.")
                .replace("%target%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero, target);

        player.setFireTicks(player.getFireTicks() + target.getFireTicks());
        target.setFireTicks(-1);

        CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
        for (Effect effect : targetCT.getEffects()) {
            if (effect instanceof CombatEffect) {
                targetCT.removeEffect(effect);
            } else if (effect instanceof Burning) {
                targetCT.removeEffect(effect);
                hero.addEffect(effect);
            }
        }

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 2000, false);
        targetCT.addEffect(new SeizedFlameEffect(this, player, duration));

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_FIRE_AMBIENT, 0.4F, 1.0F);

        return SkillResult.NORMAL;
    }

    public class SeizedFlameEffect extends ExpirableEffect {
        SeizedFlameEffect(Skill skill, Player applier, long duration) {
            super(skill, "SeizedFlame", applier, duration, applyText, expireText);

            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.RESIST_FIRE);
            types.add(EffectType.FIRE);

            addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, (int) (duration / 50), 1));
        }
    }
}
