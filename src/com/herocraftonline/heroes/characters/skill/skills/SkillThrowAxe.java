package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;
import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.hooks.NCPExemptionManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

/**
 * Created by Matt 'The Yeti' Burnett on 4/27/2014.
 * Copyright 2014 HeroCraft Online
 */
public class SkillThrowAxe extends TargettedSkill {

    private boolean ncpEnabled = false;

    public SkillThrowAxe(Heroes plugin) {
        super(plugin, "ThrowAxe");
        setDescription("Throw an axe to strike your target dealing $1 physical damage.");
        setUsage("/skill throwaxe");
        setArgumentRange(0, 0);
        setIdentifiers("skill throwaxe");
        setTypes(SkillType.DAMAGING, SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.ARMOR_PIERCING, SkillType.AGGRESSIVE);
        if (Bukkit.getServer().getPluginManager().getPlugin("NoCheatPlus") != null) {
            ncpEnabled = true;
        }

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
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, Integer.valueOf(50), false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 0.75, false);
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

        broadcastExecuteText(hero, target);

        if (ncpEnabled) {
            if (!player.isOp()) {
                NCPExemptionEffect ncpExemptEffect = new NCPExemptionEffect(this);
                hero.addEffect(ncpExemptEffect);
            }
        }
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, Integer.valueOf(50), false);
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

        player.getWorld().spigot().playEffect(target.getLocation().add(0, 0.5, 0), org.bukkit.Effect.CRIT, 0, 0, 0, 0, 0, 1, 25, 16);
        player.getWorld().playEffect(player.getLocation(), org.bukkit.Effect.BLAZE_SHOOT, 3);
        player.getWorld().playSound(player.getLocation(), Sound.SHOOT_ARROW, 0.8F, 1.0F);

        if (ncpEnabled) {
            if (!player.isOp()) {
                if (hero.hasEffect("NCPExemptionEffect_FIGHT"))
                    hero.removeEffect(hero.getEffect("NCPExemptionEffect_FIGHT"));
            }
        }
        return SkillResult.NORMAL;
    }

    private class NCPExemptionEffect extends Effect {

        public NCPExemptionEffect(Skill skill) {
            super(skill, "NCPExemptionEffect_FIGHT");
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            final Player player = hero.getPlayer();

            NCPExemptionManager.exemptPermanently(player, CheckType.FIGHT);
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            final Player player = hero.getPlayer();

            NCPExemptionManager.unexempt(player, CheckType.FIGHT);
        }
    }
}
