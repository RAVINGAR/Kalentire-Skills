package com.herocraftonline.dev.heroes.skill.skills;

import java.util.List;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.api.SkillResult;
import com.herocraftonline.dev.heroes.effects.common.CombustEffect;
import com.herocraftonline.dev.heroes.hero.Hero;
import com.herocraftonline.dev.heroes.skill.ActiveSkill;
import com.herocraftonline.dev.heroes.skill.SkillConfigManager;
import com.herocraftonline.dev.heroes.skill.SkillType;
import com.herocraftonline.dev.heroes.util.Messaging;
import com.herocraftonline.dev.heroes.util.Setting;

public class SkillBlaze extends ActiveSkill {

    public SkillBlaze(Heroes plugin) {
        super(plugin, "Blaze");
        setDescription("You ignite all nearby enemies.");
        setUsage("/skill blaze");
        setArgumentRange(0, 0);
        setIdentifiers("skill blaze");
        setTypes(SkillType.FIRE, SkillType.DAMAGING, SkillType.HARMFUL);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(Setting.DURATION.node(), 30000);
        node.set(Setting.RADIUS.node(), 5);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        int range = SkillConfigManager.getUseSetting(hero, this, Setting.RADIUS, 5, false);
        List<Entity> entities = hero.getPlayer().getNearbyEntities(range, range, range);
        int fireTicks = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 30000, false) / 50;
        boolean damaged = false;
        for (Entity entity : entities) {
            if (!(entity instanceof LivingEntity)) {
                continue;
            }
            LivingEntity lEntity = (LivingEntity) entity;

            if (!damageCheck(player, lEntity)) {
                continue;
            }

            damaged = true;
            lEntity.setFireTicks(fireTicks);

            if (lEntity instanceof Player) {
                plugin.getHeroManager().getHero((Player) lEntity).addEffect(new CombustEffect(this, player));
            } else {
                plugin.getEffectManager().addEntityEffect(lEntity, new CombustEffect(this, player));
            }
        }

        if (!damaged) {
            Messaging.send(player, "No targets in range!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }
}
