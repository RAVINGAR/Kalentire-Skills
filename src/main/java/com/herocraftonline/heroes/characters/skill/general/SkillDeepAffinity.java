package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.effects.common.WaterBreathingEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SkillDeepAffinity extends ActiveSkill {

    private String applyText;
    private String expireText;

    public SkillDeepAffinity(Heroes plugin) {
        super(plugin, "DeepAffinity");
        setDescription("You are able to breath, see, and swim underwater for $1 second(s).");
        setUsage("/skill deepaffinity");
        setArgumentRange(0, 0);
        setIdentifiers("skill deepaffinity");
        setTypes(SkillType.SILENCEABLE, SkillType.BUFFING);
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 30000, false);

        String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription().replace("$1", formattedDuration);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DURATION.node(), 60000);
        node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% has grown a set of gills!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% lost their gills!");

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%hero% has grown a set of gills!").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%hero% lost their gills!").replace("%hero%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        broadcastExecuteText(hero);

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 60000, false);
        hero.addEffect(new WaterBreathingEffect(this, player, duration, applyText, expireText));


        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_SPLASH, 0.8F, 1.0F);

        return SkillResult.NORMAL;
    }

    private class DeepAffinity extends ExpirableEffect {

        public DeepAffinity(Skill skill, Player applier, long duration, String applyText, String expireText) {
            super(skill, "DeepAffinity", applier, duration, applyText, expireText);

            this.types.add(EffectType.BENEFICIAL);
            this.types.add(EffectType.WATER_BREATHING);
            this.types.add(EffectType.WATER);
            this.addPotionEffect(new PotionEffect(PotionEffectType.CONDUIT_POWER, (int)(20L * duration / 1000L), 0));
            this.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, (int)(20L * duration / 1000L), 0));
        }
    }
}
