package com.herocraftonline.heroes.characters.skill.reborn.bloodmage;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.effects.common.QuickenEffect;
import com.herocraftonline.heroes.characters.effects.common.SlowEffect;
import com.herocraftonline.heroes.characters.effects.common.StunEffect;
import com.herocraftonline.heroes.characters.effects.standard.SlownessEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
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
        setArgumentRange(0, 1);
        setTypes(SkillType.DEBUFFING, SkillType.BUFFING, SkillType.NO_SELF_TARGETTING, SkillType.AGGRESSIVE);

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
        config.set("speed-multiplier", 3);
        config.set(SkillSetting.DURATION.node(), 5000);
        config.set("slowness-duration", 5000);
        return config;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        long slownessduration = SkillConfigManager.getUseSetting(hero, this, "slowness-duration", 30, false);

        //slow
        addSpellTarget(target, hero);
        plugin.getCharacterManager().getCharacter(target).addEffect(new SlownessEffect(this, player, slownessduration, 3));

        //speed
        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
        int multiplier = SkillConfigManager.getUseSetting(hero, this, "speed-multiplier", 3, false);
        if (multiplier > 20) {
            multiplier = 20;
        }
        hero.addEffect(new QuickenEffect(this, this.getName(), hero.getPlayer(), duration, multiplier, this.applyText, this.expireText));
        //player.getWorld().spigot().playEffect(target.getLocation().add(0, 0.5, 0), org.bukkit.Effect.SPELL, 0, 0, 0, 0, 0, 1, 50, 16);
        player.getWorld().spawnParticle(Particle.SPELL, target.getLocation().add(0, 0.5, 0), 50, 0, 0, 0, 1);
        return SkillResult.NORMAL;
    }

}


