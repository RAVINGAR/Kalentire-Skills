package com.herocraftonline.heroes.characters.skill.remastered.shadowknight;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.common.BlindEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SkillTerror extends TargettedSkill {

    private String applyText;
    private String expireText;

    public SkillTerror(Heroes plugin) {
        super(plugin, "Terror");
        setDescription("You terrify your target, impairing their movement and disabling them for $1 second(s).");
        setUsage("/skill terror");
        setArgumentRange(0, 0);
        setIdentifiers("skill terror");
        setTypes(SkillType.ABILITY_PROPERTY_DARK, SkillType.DAMAGING, SkillType.BLINDING, SkillType.DISABLING,
                SkillType.SILENCEABLE, SkillType.AGGRESSIVE);
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.DURATION, false);
        return getDescription().replace("$1", Util.decFormat.format(duration / 1000.0));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.MAX_DISTANCE.node(), 7);
        config.set("amplifier", 2);
        config.set(SkillSetting.DURATION.node(), 4000);
        config.set(SkillSetting.DURATION_INCREASE_PER_CHARISMA.node(), 75);
        config.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% has been overcome with fear!");
        config.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% has overcome his fear!");
        return config;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT.node(),
                ChatComponents.GENERIC_SKILL + "%target% is terrified!").replace("%target%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT.node(),
                ChatComponents.GENERIC_SKILL + "%target% has overcome his fear!").replace("%target%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        int amplifier = SkillConfigManager.getUseSetting(hero, this, "amplifier", 2, false);
        int duration = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.DURATION, false);

        broadcastExecuteText(hero, target);

        TerrorEffect dEffect = new TerrorEffect(this, player, duration, amplifier);

        CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
        targetCT.addEffect(dEffect);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GHAST_SCREAM, 1.2F, 2.0F);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WOLF_HOWL, 0.4F, 1.8F);
        //target.getWorld().spigot().playEffect(target.getEyeLocation(), org.bukkit.Effect.LARGE_SMOKE, 0, 0, 0.2F, 0.0F, 0.2F, 0.1F, 25, 16);
        target.getWorld().spawnParticle(Particle.SMOKE_LARGE, target.getEyeLocation(), 25, 0.2, 0, 0.2, 0.1);
        //target.getWorld().spigot().playEffect(target.getEyeLocation(), org.bukkit.Effect.EXPLOSION, 0, 0, 0.2F, 0.0F, 0.2F, 0.5F, 25, 16);
        target.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, target.getEyeLocation(), 25, 0.2, 0, 0.2, 0.5);

        return SkillResult.NORMAL;
    }

    public class TerrorEffect extends BlindEffect {

        public TerrorEffect(Skill skill, Player applier, long duration, int amplifier) {
            super(skill, "Terror", applier, duration, applyText, expireText);

            types.add(EffectType.DARK);
            types.add(EffectType.SLOW);
            types.add(EffectType.DISABLE);
            types.add(EffectType.DISPELLABLE);

            addPotionEffect(new PotionEffect(PotionEffectType.SLOW, (int) ((duration / 1000) * 20), amplifier));
        }
    }
}
