package com.herocraftonline.dev.heroes.skill.skills;

import java.util.List;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.config.ConfigurationNode;

import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.hero.Hero;
import com.herocraftonline.dev.heroes.skill.ActiveSkill;
import com.herocraftonline.dev.heroes.skill.SkillType;
import com.herocraftonline.dev.heroes.skill.ActiveSkill.SkillResult;
import com.herocraftonline.dev.heroes.util.Setting;

public class SkillPulse extends ActiveSkill {

    public SkillPulse(Heroes plugin) {
        super(plugin, "Pulse");
        setDescription("Damages everyone around you");
        setUsage("/skill pulse");
        setArgumentRange(0, 0);
        setIdentifiers("skill pulse");
        setTypes(SkillType.DAMAGING, SkillType.FORCE, SkillType.SILENCABLE, SkillType.HARMFUL);
    }

    @Override
    public ConfigurationNode getDefaultConfig() {
        ConfigurationNode node = super.getDefaultConfig();
        node.setProperty(Setting.DAMAGE.node(), 1);
        node.setProperty(Setting.RADIUS.node(), 5);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        int radius = getSetting(hero, Setting.RADIUS.node(), 5, false);
        List<Entity> entities = hero.getPlayer().getNearbyEntities(radius, radius, radius);
        for (Entity entity : entities) {
            if (!(entity instanceof LivingEntity)) {
                continue;
            }
            LivingEntity target = (LivingEntity) entity;

            if (!damageCheck(player, target))
                continue;

            int damage = getSetting(hero, "damage", 1, false);
            addSpellTarget(target, hero);
            target.damage(damage, player);
        }
        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }
}
