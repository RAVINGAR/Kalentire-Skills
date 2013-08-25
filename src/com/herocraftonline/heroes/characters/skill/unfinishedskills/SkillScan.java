package com.herocraftonline.heroes.characters.skill.unfinishedskills;

import org.bukkit.Effect;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Messaging;

public class SkillScan extends TargettedSkill {
	private final Heroes plugin;

	public SkillScan(Heroes plugin) {
		super(plugin, "Scan");
		this.plugin = plugin;
		setDescription("Reports the target's health");
		setUsage("/skill scan <target>");
		setArgumentRange(0, 1);
		setIdentifiers(new String[] { "skill scan" });
		setTypes(new SkillType[] { SkillType.KNOWLEDGE, SkillType.STEALTHY });
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
					tHero.getPlayer().getDisplayName(),
					Integer.valueOf(tHero.getLevel(tHero.getHeroClass())),
					tHero.getHeroClass().getName(),
                    Integer.valueOf((int) target.getHealth()),
                    Integer.valueOf((int) target.getMaxHealth())
				};
			
			// Send the message
			Messaging.send(player, "$1 is a level $2 $3 and has $4 / $5 HP", messageVariables);
		}
		else {
			// Create the message variables
			Object[] messageVariables = new Object[] {
					Messaging.getLivingEntityName(target),
                    Integer.valueOf((int) target.getHealth()),
                    Integer.valueOf((int) target.getMaxHealth())
				};
			
			// Send the message
			Messaging.send(player, "$1 has $2 / $3 HP", messageVariables);
		}
		player.getWorld().playEffect(player.getLocation(), Effect.ENDER_SIGNAL, 3);
		hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.ORB_PICKUP, 0.8F, 1.0F);
		return SkillResult.NORMAL;
	}

	public String getDescription(Hero hero) {
		return getDescription();
	}
}