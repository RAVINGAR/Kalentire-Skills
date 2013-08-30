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
import com.herocraftonline.heroes.util.Util;

public class SkillSmite extends TargettedSkill {
    // This is for Firework Effects
    public VisualEffect fplayer = new VisualEffect();

    public SkillSmite(Heroes plugin) {
        super(plugin, "Smite");
        setDescription("You smite the target, dealing $1 light damage to the target. Will instead deal $2 damage if the target is undead.");
        setUsage("/skill smite");
        setArgumentRange(0, 0);
        setIdentifiers("skill smite");
        setTypes(SkillType.DAMAGING, SkillType.ABILITY_PROPERTY_LIGHT, SkillType.SILENCABLE, SkillType.AGGRESSIVE);
    }

    @Override
    public String getDescription(Hero hero) {
        int undeadDamage = SkillConfigManager.getUseSetting(hero, this, "undead-damage", Integer.valueOf(80), false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_WISDOM, Double.valueOf(1.0), false);
        undeadDamage += (damageIncrease * hero.getAttributeValue(AttributeType.WISDOM));

        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, Integer.valueOf(40), false);
        damage += (damageIncrease * hero.getAttributeValue(AttributeType.WISDOM));
        damage += (damageIncrease * hero.getAttributeValue(AttributeType.WISDOM));

        String formattedUndeadDamage = Util.decFormat.format(undeadDamage);
        String formattedDamage = Util.decFormat.format(damage);

        return getDescription().replace("$1", formattedDamage).replace("$2", formattedUndeadDamage);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), Integer.valueOf(6));
        node.set("undead-damage", Integer.valueOf(80));
        node.set(SkillSetting.DAMAGE.node(), Double.valueOf(40));
        node.set(SkillSetting.DAMAGE_INCREASE_PER_WISDOM.node(), Double.valueOf(1.0));

        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero, target);

        double damage = 0;
        if (Util.isUndead(plugin, target)) {
            damage = SkillConfigManager.getUseSetting(hero, this, "undead-damage", Integer.valueOf(80), false);
            double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_WISDOM, Double.valueOf(1.0), false);
            damage += (damageIncrease * hero.getAttributeValue(AttributeType.WISDOM));
        }
        else {
            damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, Integer.valueOf(40), false);
            double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_WISDOM, Double.valueOf(1.0), false);
            damage += (damageIncrease * hero.getAttributeValue(AttributeType.WISDOM));
        }

        addSpellTarget(target, hero);
        damageEntity(target, player, damage, DamageCause.MAGIC);

        // this is our fireworks
        try {
            fplayer.playFirework(player.getWorld(), target.getLocation().add(0, 1.5, 0), FireworkEffect.builder().flicker(false).trail(false).with(FireworkEffect.Type.BALL).withColor(Color.SILVER).withFade(Color.NAVY).build());
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
