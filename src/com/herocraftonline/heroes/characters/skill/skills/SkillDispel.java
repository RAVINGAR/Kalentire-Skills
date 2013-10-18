package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Messaging;

public class SkillDispel extends TargettedSkill {

    public SkillDispel(Heroes plugin) {
        super(plugin, "Dispel");
        setDescription("You remove up to $1 harmful magic effects from your target.");
        setUsage("/skill dispel <Target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill dispel");
        setTypes(SkillType.SILENCABLE, SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.BUFFING, SkillType.DISPELLING);
    }

    @Override
    public String getDescription(Hero hero) {
        int removals = SkillConfigManager.getUseSetting(hero, this, "max-removals", 1, false);

        return getDescription().replace("$1", removals + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), Integer.valueOf(12));
        node.set("max-removals", Integer.valueOf(1));

        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        if (!(target instanceof Player))
            return SkillResult.INVALID_TARGET;

        boolean removed = false;
        int maxRemovals = SkillConfigManager.getUseSetting(hero, this, "max-removals", 1, false);

        Hero targetHero = plugin.getCharacterManager().getHero((Player) target);
        for (Effect effect : targetHero.getEffects()) {
            if (effect.isType(EffectType.DISPELLABLE) && effect.isType(EffectType.HARMFUL)) {
                hero.removeEffect(effect);
                removed = true;
                maxRemovals--;
                if (maxRemovals == 0) {
                    break;
                }
            }
        }

        if (removed) {
            broadcastExecuteText(hero, target);
            return SkillResult.NORMAL;
        }
        else {
            Messaging.send(player, "Your target has nothing to dispel!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }
    }
}
