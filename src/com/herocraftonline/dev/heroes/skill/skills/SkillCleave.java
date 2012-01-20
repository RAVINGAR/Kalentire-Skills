package com.herocraftonline.dev.heroes.skill.skills;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.api.SkillResult;
import com.herocraftonline.dev.heroes.hero.Hero;
import com.herocraftonline.dev.heroes.skill.SkillConfigManager;
import com.herocraftonline.dev.heroes.skill.SkillType;
import com.herocraftonline.dev.heroes.skill.TargettedSkill;
import com.herocraftonline.dev.heroes.util.Messaging;
import com.herocraftonline.dev.heroes.util.Setting;
import com.herocraftonline.dev.heroes.util.Util;

public class SkillCleave extends TargettedSkill {

    public SkillCleave(Heroes plugin) {
        super(plugin, "Cleave");
        setDescription("You cleave your target and nearby enemies for $1% weapon damage.");
        setUsage("/skill cleave <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill cleave");
        setTypes(SkillType.PHYSICAL, SkillType.DAMAGING, SkillType.HARMFUL);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("weapons", Util.axes);
        node.set(Setting.MAX_DISTANCE.node(), 2);
        node.set(Setting.RADIUS.node(), 3);
        node.set("damage-multiplier", 1.0);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();
        
        Material item = player.getItemInHand().getType();
        if (!SkillConfigManager.getUseSetting(hero, this, "weapons", Util.axes).contains(item.name())) {
            Messaging.send(player, "You can't cleave with that weapon!");
            return SkillResult.FAIL;
        }

        int damage = plugin.getDamageManager().getItemDamage(item, player);
        damage *= SkillConfigManager.getUseSetting(hero, this, "damage-multiplier", 1.0, false);
        damageEntity(target, player, damage, DamageCause.ENTITY_ATTACK);
        int radius = SkillConfigManager.getUseSetting(hero, this, Setting.RADIUS, 3, false);
        for (Entity entity : target.getNearbyEntities(radius, radius, radius)) {
            if (!(entity instanceof LivingEntity) || !damageCheck(player, (LivingEntity) entity)) {
                continue;
            }
            damageEntity((LivingEntity) entity, player, damage, DamageCause.ENTITY_ATTACK);
        }

        broadcastExecuteText(hero, target);
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(Hero hero) {
        double mult = SkillConfigManager.getUseSetting(hero, this, "damage-multiplier", 1.0, false);
        return getDescription().replace("$1", mult * 100 + "");
    }
}
