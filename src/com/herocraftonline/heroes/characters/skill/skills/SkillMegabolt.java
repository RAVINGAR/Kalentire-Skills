package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
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

public class SkillMegabolt extends TargettedSkill {

    public SkillMegabolt(Heroes plugin) {
        super(plugin, "Megabolt");
        setDescription("Calls down multiple bolts of lightning on your target, dealing $1 damage to them and all enemies within $2 blocks.");
        setUsage("/skill megabolt");
        setArgumentRange(0, 0);
        setIdentifiers("skill megabolt");
        setTypes(SkillType.ABILITY_PROPERTY_LIGHTNING, SkillType.AREA_OF_EFFECT, SkillType.DAMAGING, SkillType.SILENCABLE, SkillType.AGGRESSIVE);
    }

    @Override
    public String getDescription(Hero hero) {
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, Integer.valueOf(100), false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, Double.valueOf(1.75), false);
        damage += (int) (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, Integer.valueOf(5), false);

        return getDescription().replace("$1", damage + "").replace("$2", radius + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), Integer.valueOf(7));
        node.set(SkillSetting.MAX_DISTANCE_INCREASE_PER_INTELLECT.node(), Double.valueOf(0.125));
        node.set(SkillSetting.DAMAGE.node(), Integer.valueOf(100));
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), Double.valueOf(1.75));
        node.set(SkillSetting.RADIUS.node(), Integer.valueOf(5));
        node.set(SkillSetting.REAGENT.node(), Integer.valueOf(289));
        node.set(SkillSetting.REAGENT_COST.node(), Integer.valueOf(2));

        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, Integer.valueOf(5), false);

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, Integer.valueOf(100), false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, Double.valueOf(1.75), false);
        damage += (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        // Damage the first target
        target.getWorld().strikeLightningEffect(target.getLocation());
        addSpellTarget(target, hero);
        damageEntity(target, player, damage, DamageCause.MAGIC, false);

        // Damage the rest
        for (Entity entity : target.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof LivingEntity) {
                if (!damageCheck(player, (LivingEntity) entity)) {
                    continue;
                }
                
                target.getWorld().strikeLightningEffect(target.getLocation());
                addSpellTarget(target, hero);
                damageEntity((LivingEntity) entity, player, damage, DamageCause.MAGIC, false);
            }
        }

        broadcastExecuteText(hero, target);
        return SkillResult.NORMAL;
    }
}
