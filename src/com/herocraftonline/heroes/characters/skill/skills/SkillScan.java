package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.characters.skill.VisualEffect;
import com.herocraftonline.heroes.util.Messaging;

public class SkillScan extends TargettedSkill {
    // This is for Firework Effects
    public VisualEffect fplayer = new VisualEffect();
    private final Heroes plugin;
    
    public SkillScan(Heroes plugin) {
        super(plugin, "Scan");
        this.plugin = plugin;
        setDescription("Reports the target's health");
        setUsage("/skill scan <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill scan");
        setTypes(SkillType.KNOWLEDGE, SkillType.STEALTHY);
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();
        CharacterTemplate character = plugin.getCharacterManager().getCharacter(target);
        if (character instanceof Hero) {
            Hero tHero = (Hero) character;
            Messaging.send(player, "$1 is a level $2 $3 and has $4 / $5 HP", tHero.getPlayer().getDisplayName(), tHero.getLevel(tHero.getHeroClass()), tHero.getHeroClass().getName(), tHero.getHealth(), tHero.getMaxHealth());
        } else {
            Messaging.send(player, "$1 has $2 / $3 HP", Messaging.getLivingEntityName(target), character.getHealth(), character.getMaxHealth());
        }
        
        // this is our fireworks shit
        try {
            fplayer.playFirework(player.getWorld(), target.getLocation(), FireworkEffect.builder().flicker(false).trail(true).with(FireworkEffect.Type.BALL).withColor(Color.FUCHSIA).withFade(Color.MAROON).build());
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

}
