package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.CustomNameManager;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Effect;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SkillScan extends TargettedSkill {

    public SkillScan(Heroes plugin) {
        super(plugin, "Scan");
        setDescription("Reports the target's health");
        setUsage("/skill scan <target>");
        setIdentifiers("skill scan");
        setArgumentRange(0, 1);
        setTypes(SkillType.STEALTHY, SkillType.NO_SELF_TARGETTING);
    }

    public String getDescription(Hero hero) {
        return getDescription();
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.MAX_DISTANCE.node(), 16.0);
        config.set(SkillSetting.TARGET_HIT_TOLERANCE.node(), 0.5);
        return config;
    }

    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();
        CharacterTemplate character = this.plugin.getCharacterManager().getCharacter(target);
        if (character instanceof Hero) {
            Hero tHero = (Hero) character;

            player.sendMessage(tHero.getPlayer().getName() + " is a level "
                    + tHero.getHeroLevel(tHero.getHeroClass()) + " " + tHero.getHeroClass().getName()
                    + " and has " + (int) target.getHealth()
                    + " / " + (int) target.getMaxHealth() + " HP");
        } else {
            player.sendMessage(CustomNameManager.getName(target) + " has "
                    + Util.decFormat.format(target.getHealth())
                    + " / "
                    + Util.decFormat.format(target.getMaxHealth()) + " HP");
        }

        player.getWorld().playEffect(player.getLocation(), Effect.ENDER_SIGNAL, 3);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8F, 1.0F);

        return SkillResult.NORMAL;
    }
}