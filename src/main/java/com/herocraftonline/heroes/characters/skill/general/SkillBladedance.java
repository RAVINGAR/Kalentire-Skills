package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

public class SkillBladedance extends TargettedSkill {

    public SkillBladedance(Heroes plugin) {
        super(plugin, "Bladedance");
        setDescription("You execute bladedance on your target and nearby enemies for $1% weapon damage.");
        setUsage("/skill bladedance");
        setArgumentRange(0, 0);
        setIdentifiers("skill bladedance");
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.DAMAGING, SkillType.AGGRESSIVE);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("weapons", Util.swords);
        node.set(SkillSetting.MAX_DISTANCE.node(), 2);
        node.set(SkillSetting.RADIUS.node(), 3);
        node.set("damage-multiplier", 1.0);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();
        
        Material item = NMSHandler.getInterface().getItemInMainHand(player.getInventory()).getType();
        if (!SkillConfigManager.getUseSetting(hero, this, "weapons", Util.axes).contains(item.name())) {
            player.sendMessage("You can't bladedance with that weapon!");
            return SkillResult.FAIL;
        }

        World world = target.getWorld();

        double damage = plugin.getDamageManager().getFlatItemDamage(hero);
        damage *= SkillConfigManager.getUseSetting(hero, this, "damage-multiplier", 1.0, false);
        addSpellTarget(target, hero);
        world.spawnParticle(Particle.DRIPPING_OBSIDIAN_TEAR, player.getLocation(), 50, 0.5, 0.5, 0.5, 0);
        world.playSound(player.getLocation(), Sound.ITEM_TRIDENT_THUNDER, 0.8F, 0.7F);
        damageEntity(target, player, damage, DamageCause.ENTITY_ATTACK);
        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 3, false);

        for (Entity entity : target.getNearbyEntities(radius, radius, radius)) {
            if (!(entity instanceof LivingEntity) || !damageCheck(player, (LivingEntity) entity)) {
                continue;
            }
            addSpellTarget(target, hero);
            damageEntity((LivingEntity) entity, player, damage, DamageCause.ENTITY_ATTACK);
            world.spawnParticle(Particle.GLOW, entity.getLocation(), 30, 1, 1, 1, 0);
        }
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.ENTITY_PLAYER_HURT , 0.8F, 1.0F);
        broadcastExecuteText(hero, target);
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(Hero hero) {
        double mult = SkillConfigManager.getUseSetting(hero, this, "damage-multiplier", 1.0, false);
        return getDescription().replace("$1", mult * 100 + "");
    }
}
