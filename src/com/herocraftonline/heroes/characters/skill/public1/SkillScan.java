package com.herocraftonline.heroes.characters.skill.public1;

import org.bukkit.Effect;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.CustomNameManager;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.CompatSound;
import com.herocraftonline.heroes.util.Messaging;

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

            // Create the message variables
            Object[] messageVariables = new Object[] {
                    tHero.getPlayer().getName(),
                    tHero.getLevel(tHero.getHeroClass()),
                    tHero.getHeroClass().getName(),
                    (int) target.getHealth(),
                    (int) target.getMaxHealth()
            };

            // Send the message
            Messaging.send(player, "$1 is a level $2 $3 and has $4 / $5 HP", messageVariables);
        }
        else {
            // Create the message variables
            Object[] messageVariables = new Object[] {
                    CustomNameManager.getName(target),
                    (int) target.getHealth(),
                    (int) target.getMaxHealth()
            };

            // Send the message
            Messaging.send(player, "$1 has $2 / $3 HP", messageVariables);
        }

        player.getWorld().playEffect(player.getLocation(), Effect.ENDER_SIGNAL, 3);
        player.getWorld().playSound(player.getLocation(), CompatSound.ENTITY_EXPERIENCE_ORB_PICKUP.value(), 0.8F, 1.0F);

        return SkillResult.NORMAL;
    }
}