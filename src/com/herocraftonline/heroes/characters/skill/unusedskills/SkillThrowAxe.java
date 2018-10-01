package com.herocraftonline.heroes.characters.skill.unusedskills;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.heroes.util.CompatSound;
import com.herocraftonline.heroes.util.Util;

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
        node.set(SkillSetting.MAX_DISTANCE.node(), 10);
        node.set(SkillSetting.MAX_DISTANCE_INCREASE_PER_STRENGTH.node(), 0.5);
        node.set(SkillSetting.DAMAGE.node(), 50);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_STRENGTH.node(), 0.75);

        return node;
    }

    @Override
    public String getDescription(Hero hero) {
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 50, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 0.75, false);
        damage += (int) (damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH));

        return getDescription().replace("$1", damage + "");
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();
        ItemStack item = NMSHandler.getInterface().getItemInMainHand(player.getInventory());

        if (!SkillConfigManager.getUseSetting(hero, this, "weapons", Util.axes).contains(item.getType().name())) {
            hero.getPlayer().sendMessage("You cannot use this skill with that weapon!");
            return SkillResult.FAIL;
        }

        broadcastExecuteText(hero, target);

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 50, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 1.0, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH);

        Vector vector  = target.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();
        final Entity axe = player.getLocation().getWorld().dropItemNaturally(player.getLocation(), new ItemStack(Material.STONE_AXE, 1, (short) 1));
        axe.setVelocity(vector.multiply(3.5));
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            public void run() {
                axe.remove();
            }
        }, 10);

        addSpellTarget(target, hero);
        damageEntity(target, player, damage, EntityDamageEvent.DamageCause.ENTITY_ATTACK);

        broadcastExecuteText(hero, target);

        //player.getWorld().spigot().playEffect(target.getLocation().add(0, 0.5, 0), org.bukkit.Effect.CRIT, 0, 0, 0, 0, 0, 1, 25, 16);
        player.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 0.5, 0), 25, 0, 0, 0, 1);
        player.getWorld().playEffect(player.getLocation(), org.bukkit.Effect.BLAZE_SHOOT, 3);
        player.getWorld().playSound(player.getLocation(), CompatSound.ENTITY_ARROW_SHOOT.value(), 0.8F, 1.0F);

        return SkillResult.NORMAL;
    }
}
