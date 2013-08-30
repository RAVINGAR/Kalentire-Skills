package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.WaterBreatheEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;

public class SkillGills extends ActiveSkill {

    private String applyText;
    private String expireText;

    public SkillGills(Heroes plugin) {
        super(plugin, "Gills");
        setDescription("You are able to breath water for $1 seconds.");
        setUsage("/skill gills");
        setArgumentRange(0, 0);
        setIdentifiers("skill gills");
        setTypes(SkillType.SILENCABLE, SkillType.BUFFING);
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 60000, false);
        return getDescription().replace("$1", duration / 1000 + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DURATION.node(), 60000);
        node.set(SkillSetting.APPLY_TEXT.node(), Messaging.getSkillDenoter() + "%hero% has grown a set of gills!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), Messaging.getSkillDenoter() + "%hero% lost his gills!");

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, Messaging.getSkillDenoter() + "%hero% has grown a set of gills!").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, Messaging.getSkillDenoter() + "%hero% lost his gills!").replace("%hero%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        broadcastExecuteText(hero);

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 60000, false);
        hero.addEffect(new WaterBreatheEffect(this, player, duration, applyText, expireText));

        player.getWorld().playSound(player.getLocation(), Sound.SPLASH, 0.5F, 1.0F);

        return SkillResult.NORMAL;
    }
}
