package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Created by Matt 'The Yeti' Burnett on 4/27/2014.
 * Copyright 2014 HeroCraft Online
 */
public class SkillThrowAxe extends TargettedSkill {

    public SkillThrowAxe(Heroes plugin) {
        super(plugin, "ThrowAxe");
        setDescription("Throw an axe to strike your target dealing $1 physical damage.");
        setUsage("/skill throwaxe");
        setArgumentRange(0, 0);
        setIdentifiers("skill throwaxe");
        setTypes(SkillType.DAMAGING, SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.ARMOR_PIERCING, SkillType.AGGRESSIVE);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set("weapons", Util.axes);
        node.set(SkillSetting.MAX_DISTANCE.node(), Integer.valueOf(10));
        node.set(SkillSetting.DAMAGE.node(), Integer.valueOf(50));
        node.set(SkillSetting.DAMAGE_INCREASE_PER_STRENGTH.node(), Double.valueOf(0.75));

        return node;
    }

    @Override
    public String getDescription(Hero hero) {
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, Integer.valueOf(50), false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, Double.valueOf(0.75), false);
        damage += (int) (damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH));

        return getDescription().replace("$1", damage + "");
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();
        ItemStack item = player.getItemInHand();

        if (!SkillConfigManager.getUseSetting(hero, this, "weapons", Util.axes).contains(item.getType().name())) {
            Messaging.send(hero.getPlayer(), "You cannot use this skill with that weapon!");
            return SkillResult.FAIL;
        }

        if (target instanceof Player) {
            Player targetPlayer = (Player) target;
            if (!inLineOfSight(player, targetPlayer)) {
                hero.getPlayer().sendMessage("Your target is not visible!");
                return SkillResult.FAIL;
            }
        }

        broadcastExecuteText(hero);

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, Integer.valueOf(50), false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, Double.valueOf(1.0), false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH);

        addSpellTarget(target, hero);
        damageEntity(target, player, damage, EntityDamageEvent.DamageCause.ENTITY_ATTACK);

        player.getWorld().playEffect(player.getLocation(), org.bukkit.Effect.BLAZE_SHOOT, 3);
        player.getWorld().playSound(player.getLocation(), Sound.SHOOT_ARROW, 0.8F, 1.0F);

        return SkillResult.NORMAL;
    }
}
