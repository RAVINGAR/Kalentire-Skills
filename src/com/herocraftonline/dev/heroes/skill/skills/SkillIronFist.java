package com.herocraftonline.dev.heroes.skill.skills;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.Vector;

import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.api.SkillResult;
import com.herocraftonline.dev.heroes.hero.Hero;
import com.herocraftonline.dev.heroes.skill.ActiveSkill;
import com.herocraftonline.dev.heroes.skill.SkillConfigManager;
import com.herocraftonline.dev.heroes.skill.SkillType;
import com.herocraftonline.dev.heroes.util.Setting;

public class SkillIronFist extends ActiveSkill {

    public SkillIronFist(Heroes plugin) {
        super(plugin, "IronFist");
        setDescription("Damages and knocks back nearby enemies");
        setUsage("/skill ironfist");
        setArgumentRange(0, 0);
        setIdentifiers("skill ironfist", "skill ifist");
        setTypes(SkillType.PHYSICAL, SkillType.DAMAGING, SkillType.HARMFUL);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(Setting.DAMAGE.node(), 4);
        node.set(Setting.RADIUS.node(), 3);
        node.set("vertical-power", .25);
        node.set("horizontal-power", .5);
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
            if (target.equals(player)) {
                continue;
            }

            // Check if the target is damagable
            if (!damageCheck(player, target)) {
                continue;
            }

            // Damage the target
            int damage = SkillConfigManager.getUseSetting(hero, this, "damage", 1, false);
            addSpellTarget(target, hero);
            damageEntity(target, player, damage, DamageCause.ENTITY_ATTACK);

            // Do our knockback
            Location playerLoc = player.getLocation();
            Location targetLoc = target.getLocation();
            
            double xDir =  targetLoc.getX() - playerLoc.getX();
            double zDir =  targetLoc.getZ() - playerLoc.getZ();
            double magnitude = Math.sqrt(xDir * xDir + zDir * zDir);
            double multiplier = SkillConfigManager.getUseSetting(hero, this, "horizontal-power", .5, false);
            xDir = xDir / magnitude * multiplier;
            zDir = zDir / magnitude * multiplier;
            
            target.setVelocity(new Vector(xDir, SkillConfigManager.getUseSetting(hero, this, "vertical-power", .25, false), zDir));
        }

        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }
}
