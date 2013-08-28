package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.BlindEffect;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

public class SkillBlind extends TargettedSkill {
    private String applyText;
    private String expireText;

    public SkillBlind(Heroes plugin) {
        super(plugin, "Blind");
        setDescription("You blind the target for $1 seconds.");
        setUsage("/skill blind");
        setArgumentRange(0, 0);
        setIdentifiers("skill blind");
        setTypes(SkillType.DEBUFFING, SkillType.AGGRESSIVE, SkillType.DAMAGING);
    }

    public String getDescription(Hero hero) {
        double duration = Util.formatDouble(SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 3000, false) / 1000.0);

        return getDescription().replace("$1", duration + "");
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 8);
        node.set(SkillSetting.DURATION.node(), Integer.valueOf(3000));
        node.set(SkillSetting.APPLY_TEXT.node(), Messaging.getSkillDenoter() + "%target% has been blinded!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), Messaging.getSkillDenoter() + "%target% can see again!");

        return node;
    }

    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT.node(), Messaging.getSkillDenoter() + "%target% has been blinded!").replace("%target%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT.node(), Messaging.getSkillDenoter() + "%target% can see again!").replace("%target%", "$1");
    }

    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        if (!(target instanceof Player)) {
            Messaging.send(player, "You must target a player!", new Object[0]);
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 3000, false);
        BlindEffect effect = new BlindEffect(this, duration, applyText, expireText);

        plugin.getCharacterManager().getHero((Player) target).addEffect(effect);

        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.ENDERMAN_IDLE, 0.8F, 1.0F);

        return SkillResult.NORMAL;
    }
}