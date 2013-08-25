package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.characters.skill.VisualEffect;

public class SkillArcaneblast extends TargettedSkill {

    public VisualEffect fplayer = new VisualEffect();
    public SkillArcaneblast(Heroes plugin) {
        super(plugin, "Arcaneblast");
        setDescription("You arcaneblast the target for $1 light damage.");
        setUsage("/skill arcaneblast");
        setArgumentRange(0, 0);
        setIdentifiers("skill arcaneblast");
        setTypes(SkillType.DAMAGING, SkillType.ABILITY_PROPERTY_LIGHT, SkillType.SILENCABLE, SkillType.AGGRESSIVE);
    }

    public String getDescription(Hero hero) {
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 150, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 3.8, false);
        damage += (int) (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        return getDescription().replace("$1", damage + "");
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DAMAGE.node(), Integer.valueOf(200));
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), Double.valueOf(2.5));
        node.set(SkillSetting.MAX_DISTANCE.node(), Integer.valueOf(10));
        node.set(SkillSetting.MAX_DISTANCE_INCREASE_PER_INTELLECT.node(), Double.valueOf(0.2));

        return node;
    }

    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 200, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 2.5, false);
        damage += (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        addSpellTarget(target, hero);
        damageEntity(target, player, damage, DamageCause.MAGIC);

        broadcastExecuteText(hero, target);

        // this is our fireworks shit
        try {
            fplayer.playFirework(target.getWorld(),
                                 target.getLocation().add(0, 3, 0),
                                 FireworkEffect.builder()
                                               .flicker(false).trail(true)
                                               .with(FireworkEffect.Type.BALL_LARGE)
                                               .withColor(Color.AQUA)
                                               .withFade(Color.PURPLE)
                                               .build());
        }
        catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        
        return SkillResult.NORMAL;
    }
}
