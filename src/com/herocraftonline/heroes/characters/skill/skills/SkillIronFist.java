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
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;

public class SkillIronFist extends ActiveSkill {

    public SkillIronFist(Heroes plugin) {
        super(plugin, "IronFist");
        setDescription("Strike the ground with an iron fist, striking all targets within $1 blocks, dealing $2 damage and knocking them into the air.");
        setUsage("/skill ironfist");
        setArgumentRange(0, 0);
        setIdentifiers("skill ironfist");
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.FORCE, SkillType.DAMAGING, SkillType.AGGRESSIVE);
    }

    @Override
    public String getDescription(Hero hero) {
        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, Integer.valueOf(5), false);

        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, Integer.valueOf(50), false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, Double.valueOf(1.6), false);
        damage += (int) (damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH));

        return getDescription().replace("$1", radius + "").replace("$2", damage + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DAMAGE.node(), Integer.valueOf(50));
        node.set(SkillSetting.DAMAGE_INCREASE_PER_STRENGTH.node(), Double.valueOf(1.125));
        node.set(SkillSetting.RADIUS.node(), Integer.valueOf(5));
        node.set(SkillSetting.DELAY.node(), Integer.valueOf(500));
        node.set("horizontal-power", Double.valueOf(1.5));
        node.set("horizontal-power-increase-per-intellect", Double.valueOf(0.0375));
        node.set("vertical-power", Double.valueOf(0.25));
        node.set("vertical-power-increase-per-intellect", Double.valueOf(0.0075));

        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, Integer.valueOf(5), false);

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 50, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, Double.valueOf(1.125), false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH);

        double hPower = SkillConfigManager.getUseSetting(hero, this, "horizontal-power", Double.valueOf(0.0), false);
        double hPowerIncrease = SkillConfigManager.getUseSetting(hero, this, "horizontal-power-increase-per-intellect", Double.valueOf(0.0), false);
        hPower += (hPowerIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        double vPower = SkillConfigManager.getUseSetting(hero, this, "vertical-power", Double.valueOf(0.4), false);
        double vPowerIncrease = SkillConfigManager.getUseSetting(hero, this, "vertical-power-increase-per-intellect", Double.valueOf(0.015), false);
        vPower += (vPowerIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

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
            addSpellTarget(target, hero);
            damageEntity(target, player, damage, DamageCause.ENTITY_ATTACK, false);

            // Do our knockback
            Location playerLoc = player.getLocation();
            Location targetLoc = target.getLocation();

            double xDir = targetLoc.getX() - playerLoc.getX();
            double zDir = targetLoc.getZ() - playerLoc.getZ();
            double magnitude = Math.sqrt(xDir * xDir + zDir * zDir);

            xDir = xDir / magnitude * hPower;
            zDir = zDir / magnitude * hPower;

            target.setVelocity(new Vector(xDir, vPower, zDir));
        }

        player.getWorld().playEffect(player.getLocation(), Effect.MOBSPAWNER_FLAMES, 3);
        player.getWorld().playSound(player.getLocation(), Sound.EXPLODE, 0.5F, 1.0F);

        broadcastExecuteText(hero);

        return SkillResult.NORMAL;
    }
}
