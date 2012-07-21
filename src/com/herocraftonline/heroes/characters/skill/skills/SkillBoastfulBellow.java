package com.herocraftonline.heroes.characters.skill.skills;

import java.util.List;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Setting;

public class SkillBoastfulBellow extends ActiveSkill {

    public SkillBoastfulBellow(Heroes plugin) {
        super(plugin, "BoastfulBellow");
        setDescription("Your bellow deals $1 damage to all nearby enemies.");
        setUsage("/skill boastfulbellow");
        setArgumentRange(0, 0);
        setIdentifiers("skill boastfulbellow");
        setTypes(SkillType.DAMAGING, SkillType.FORCE, SkillType.SILENCABLE, SkillType.HARMFUL);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(Setting.DAMAGE.node(), 1);
        node.set(Setting.RADIUS.node(), 5);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        int radius = SkillConfigManager.getUseSetting(hero, this, Setting.RADIUS, 5, false);
        List<Entity> entities = hero.getPlayer().getNearbyEntities(radius, radius, radius);
        for (Entity entity : entities) {
            if (!(entity instanceof LivingEntity)) {
                continue;
            }
            LivingEntity target = (LivingEntity) entity;

            if (!damageCheck(player, target))
                continue;

            int damage = SkillConfigManager.getUseSetting(hero, this, "damage", 1, false);
            addSpellTarget(target, hero);
            damageEntity(target, player, damage, DamageCause.MAGIC);
        }
        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(Hero hero) {
        int damage = SkillConfigManager.getUseSetting(hero, this, Setting.DAMAGE, 1, false);
        return getDescription().replace("$1", damage + "");
    }
}
