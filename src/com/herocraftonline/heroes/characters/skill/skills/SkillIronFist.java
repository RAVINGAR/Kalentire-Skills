package com.herocraftonline.heroes.characters.skill.skills;

import java.util.List;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.Vector;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;

public class SkillIronFist extends ActiveSkill {

    public SkillIronFist(Heroes plugin) {
        super(plugin, "IronFist");
        setDescription("Strike the ground with an iron fist, dealing $1 damage and knocking nearby enemies within $2 blocks into the air.");
        setUsage("/skill ironfist");
        setArgumentRange(0, 0);
        setIdentifiers("skill ironfist");
        setTypes(SkillType.PHYSICAL, SkillType.FORCE, SkillType.DAMAGING, SkillType.HARMFUL);
    }

    @Override
    public String getDescription(Hero hero) {
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, Integer.valueOf(25), false);
        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, Integer.valueOf(5), false);

        return getDescription().replace("$1", damage + "").replace("$2", radius + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DAMAGE.node(), 4);
        node.set(SkillSetting.RADIUS.node(), 3);
        node.set("vertical-power", 0.8);
        node.set("horizontal-power", 0.1);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, Integer.valueOf(5), false);
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
            double damage = SkillConfigManager.getUseSetting(hero, this, "damage", Integer.valueOf(25), false);
            addSpellTarget(target, hero);
            damageEntity(target, player, damage, DamageCause.ENTITY_ATTACK);

            // Do our knockback
            Location playerLoc = player.getLocation();
            Location targetLoc = target.getLocation();

            double xDir = targetLoc.getX() - playerLoc.getX();
            double zDir = targetLoc.getZ() - playerLoc.getZ();
            double magnitude = Math.sqrt(xDir * xDir + zDir * zDir);
            double multiplier = SkillConfigManager.getUseSetting(hero, this, "horizontal-power", Double.valueOf(0.5), false);
            xDir = xDir / magnitude * multiplier;
            zDir = zDir / magnitude * multiplier;

            double verticalPower = SkillConfigManager.getUseSetting(hero, this, "vertical-power", Double.valueOf(0.25), false);
            target.setVelocity(new Vector(xDir, verticalPower, zDir));
        }
        player.getWorld().playEffect(player.getLocation(), Effect.MOBSPAWNER_FLAMES, 3);
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.EXPLODE, 0.5F, 1.0F);
        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }
}
