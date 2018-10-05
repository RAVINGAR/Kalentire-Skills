package com.herocraftonline.heroes.characters.skill.public1;

import org.bukkit.Effect;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.Sound;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.CustomNameManager;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;

public class SkillScan extends TargettedSkill {
    private final Heroes plugin;

    public SkillScan(Heroes plugin) {
        super(plugin, "Scan");
        this.plugin = plugin;
        setDescription("Reports the target's health");
        setUsage("/skill scan <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill scan");
        setTypes(SkillType.STEALTHY);
    }

    public String getDescription(Hero hero) {
        return getDescription();
    }

    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();
        CharacterTemplate character = this.plugin.getCharacterManager().getCharacter(target);
        if ((character instanceof Hero)) {
            Hero tHero = (Hero) character;

            // Don't allow self targeting
            if (tHero == hero) {
                return SkillResult.FAIL;
            }

            // Send the message
            player.sendMessage(tHero.getPlayer().getName() + " is a level " + tHero.getHeroLevel(tHero.getHeroClass()) + " " + tHero.getHeroClass().getName() + " and has " + (int) target.getHealth() + " / " + (int) target.getMaxHealth() + " HP");
        }
        else {
            // Send the message
            player.sendMessage(CustomNameManager.getName(target) + " has " + (int) target.getHealth() + " / " + (int) target.getMaxHealth() + " HP");
        }

        player.getWorld().playEffect(player.getLocation(), Effect.ENDER_SIGNAL, 3);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8F, 1.0F);

        return SkillResult.NORMAL;
    }
}