package com.herocraftonline.heroes.characters.skill.reborn.chronomancer;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SkillDimensionalRift extends TargettedSkill {
    public SkillDimensionalRift(Heroes plugin) {
        super(plugin, "DimensionalRift");
        setDescription("Open a rift in space between you and your target, interrupting their spellcasting and causing both of you to switch places.");
        setUsage("/skill dimensionalrift");
        setArgumentRange(0, 0);
        setIdentifiers("skill dimensionalrift");
        setTypes(SkillType.SILENCEABLE, SkillType.TELEPORTING, SkillType.INTERRUPTING, SkillType.MULTI_GRESSIVE, SkillType.NO_SELF_TARGETTING);
    }

    public String getDescription(Hero hero) {
        return getDescription();
    }

    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();
        CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);

        if (targetCT.hasEffectType(EffectType.STUN) || targetCT.hasEffectType(EffectType.ROOT))
            return SkillResult.INVALID_TARGET;
        else if (hero.hasEffectType(EffectType.STUN) || hero.hasEffectType(EffectType.ROOT))
            return SkillResult.INVALID_TARGET;

        broadcastExecuteText(hero, target);

        Location pLocation = player.getLocation();
        pLocation.setYaw(target.getLocation().getYaw());
        pLocation.setPitch(target.getLocation().getPitch());

        Location tLocation = target.getLocation();
        tLocation.setYaw(player.getLocation().getYaw());
        tLocation.setPitch(player.getLocation().getPitch());

        player.teleport(tLocation);
        target.teleport(pLocation);

        // TODO: Damage and effects

        return SkillResult.NORMAL;
    }
}