package com.herocraftonline.heroes.characters.skill.general;

import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.Sound;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.SafeFallEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;

public class SkillSafefall extends ActiveSkill {

    public SkillSafefall(Heroes plugin) {
        super(plugin, "Safefall");
        setDescription("You float safely to the ground for $1 second(s).");
        setUsage("/skill safefall");
        setArgumentRange(0, 0);
        setIdentifiers("skill safefall");
        setTypes(SkillType.ABILITY_PROPERTY_AIR, SkillType.BUFFING, SkillType.SILENCEABLE);
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 6000, false);
        return getDescription().replace("$1", duration / 1000 + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DURATION.node(), 6000);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        broadcastExecuteText(hero);

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 6000, false);
        hero.addEffect(new SafeFallEffect(this, player, duration));

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BAT_LOOP, 1.0F, 1.0F);

        return SkillResult.NORMAL;
    }
}
