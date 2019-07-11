package com.herocraftonline.heroes.characters.skill.reborn.bloodmage;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.effects.common.QuickenEffect;
import com.herocraftonline.heroes.characters.effects.common.SlowEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SkillVitalityTheft extends TargettedSkill {

    private String applyText = "";
    private String expireText = "";
    private String shotEffectName = "HasFeatheredArrows";

    public SkillVitalityTheft(Heroes plugin) {
        super(plugin, "VitalityTheft");
        setDescription(""); //TODO description
        setUsage("/skill vitalitytheft");
        setIdentifiers("skill vitalitytheft", "skill VitalityTheft", "skill vt");
        setArgumentRange(0, 0);
        setTypes(SkillType.DEBUFFING, SkillType.BUFFING);

    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.DURATION, false);
        int slownessDuration = SkillConfigManager.getUseSetting(hero, this, "slowness-duration", 1500, false);
        return getDescription()
                .replace("$1", Util.decFormat.format(duration / 1000.0))
                .replace("$2", Util.decFormat.format(slownessDuration / 1000.0));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set("speed-multiplier", 2);
        config.set(SkillSetting.DURATION.node(), 5000);
        config.set("slowness-duration", 5000);
        return config;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {

        Player player = hero.getPlayer();
        broadcastExecuteText(hero, target);

        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
        int multiplier = SkillConfigManager.getUseSetting(hero, this, "speed-multiplier", 2, false);
        if (multiplier > 20) {
            multiplier = 20;
        }
        hero.addEffect(new QuickenEffect(this, this.getName(), hero.getPlayer(), duration, multiplier, this.applyText, this.expireText));
        VitalitySlownessEffect effect = new VitalitySlownessEffect(this, player, duration);
        CharacterTemplate targCT = plugin.getCharacterManager().getCharacter(target);
        targCT.addEffect(effect);

        return SkillResult.NORMAL;

    }


    public class VitalitySlownessEffect extends SlowEffect {
        private int originalStamina;

        public VitalitySlownessEffect(Skill skill, Player applier, int duration) {
            super(skill, "Slowed", applier, duration, 3, applyText, expireText);
//            types.add(EffectType.SLOW);

        }

//        @Override
//        public void applyToMonster(Monster monster) {
//            addPotionEffect(new PotionEffect(PotionEffectType.SLOW,(int) (getDuration() / 50) , 3));
//
//            super.applyToMonster(monster);
//        }
//
//        @Override
//        public void applyToHero(Hero hero) {
//            super.applyToHero(hero);
//            final Player player = hero.getPlayer();
//            addPotionEffect(new PotionEffect(PotionEffectType.SLOW, (int) (getDuration() / 50), 3), false);
//        }


        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            final Player player = hero.getPlayer();
            hero.setStamina(originalStamina);
        }
    }

}




