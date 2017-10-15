package com.herocraftonline.heroes.characters.skill.unusedskills;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.heroes.util.CompatSound;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

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
            Messaging.send(player, "You can't bladedance with that weapon!");
            return SkillResult.FAIL;
        }

        double damage = plugin.getDamageManager().getHighestItemDamage(hero, item);
        damage *= SkillConfigManager.getUseSetting(hero, this, "damage-multiplier", 1.0, false);
        addSpellTarget(target, hero);
        damageEntity(target, player, damage, DamageCause.ENTITY_ATTACK);
        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 3, false);
        for (Entity entity : target.getNearbyEntities(radius, radius, radius)) {
            if (!(entity instanceof LivingEntity) || !damageCheck(player, (LivingEntity) entity)) {
                continue;
            }
            addSpellTarget(target, hero);
            damageEntity((LivingEntity) entity, player, damage, DamageCause.ENTITY_ATTACK);
        }
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), CompatSound.ENTITY_PLAYER_HURT.value() , 0.8F, 1.0F);
        broadcastExecuteText(hero, target);
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(Hero hero) {
        double mult = SkillConfigManager.getUseSetting(hero, this, "damage-multiplier", 1.0, false);
        return getDescription().replace("$1", mult * 100 + "");
    }
}
