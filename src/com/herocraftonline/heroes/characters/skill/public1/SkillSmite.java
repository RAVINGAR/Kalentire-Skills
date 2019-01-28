package com.herocraftonline.heroes.characters.skill.public1;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Util;

public class SkillSmite extends TargettedSkill {

    public SkillSmite(Heroes plugin) {
        super(plugin, "Smite");
        setDescription("You smite the target, dealing $1 light damage to the target. Will instead deal $2 damage if the target is undead.");
        setUsage("/skill smite");
        setArgumentRange(0, 0);
        setIdentifiers("skill smite");
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.DAMAGING, SkillType.ABILITY_PROPERTY_LIGHT, SkillType.SILENCEABLE, SkillType.AGGRESSIVE);
    }

    @Override
    public String getDescription(Hero hero) {

        int intellect = hero.getAttributeValue(AttributeType.INTELLECT);

        int undeadDamage = SkillConfigManager.getUseSetting(hero, this, "undead-damage", 80, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.0, false);
        undeadDamage += damageIncrease * intellect;

        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 40, false);
        damage += damageIncrease * intellect;

        String formattedUndeadDamage = Util.decFormat.format(undeadDamage);
        String formattedDamage = Util.decFormat.format(damage);

        return getDescription().replace("$1", formattedDamage).replace("$2", formattedUndeadDamage);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 6);
        node.set("undead-damage", 80);
        node.set(SkillSetting.DAMAGE.node(), (double) 40);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 1.0);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero, target);

        int intellect = hero.getAttributeValue(AttributeType.INTELLECT);

        double damage;
        if (Util.isUndead(plugin, target)) {
            damage = SkillConfigManager.getUseSetting(hero, this, "undead-damage", 80, false);
            double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.0, false);
            damage += (damageIncrease * intellect);
        }
        else {
            damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 40, false);
            double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.0, false);
            damage += (damageIncrease * intellect);
        }

        addSpellTarget(target, hero);
        damageEntity(target, player, damage, DamageCause.ENTITY_EXPLOSION);

        player.getWorld().playEffect(target.getLocation(), org.bukkit.Effect.BLAZE_SHOOT, 1);

//        player.getWorld().spigot().playEffect(target.getLocation().add(0, 0.5, 0), // location
//                org.bukkit.Effect.VILLAGER_THUNDERCLOUD, // effect
//                0, 0, // id,data: for block effect
//                0.5F, 0.5F, 0.5F, // offset
//                1.0f, // speed
//                50, // particle count
//                SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 6, false) + 1); // radius: player observable distance
        player.getWorld().spawnParticle(Particle.VILLAGER_ANGRY, target.getLocation().add(0, 0.5, 0), 50, 0.5, 0.5, 0.5, 1, true);

        return SkillResult.NORMAL;
    }
}
