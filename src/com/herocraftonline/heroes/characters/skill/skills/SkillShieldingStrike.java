/*package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.GeometryUtil;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector; */
/*
public class SkillShieldingStrike extends ActiveSkill {

    public SkillShieldingStrike(Heroes plugin) {
        super(plugin, "ShieldingStrike");
        setDescription("Slam your shield into the ground in front of you, dealing $1 damage to enemies in a cone up to $2 meters ahead of you. "
                + "Applies an absorption shield with $3 health to all allies within $4 meters. The shield lasts $5 second(s).");
        setUsage("/sklll shieldingstrike");
        setIdentifiers("skill shieldingstrike");
        setArgumentRange(0, 0);
        setTypes(SkillType.AGGRESSIVE, SkillType.DAMAGING, SkillType.BUFFING);
    }

    public String getDescription(Hero hero) {
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 40, true) +
                (SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 1, true) * hero.getAttributeValue(AttributeType.STRENGTH));
        double range = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 6, true);
        double shieldHealth = SkillConfigManager.getUseSetting(hero, this, "shield-health", 50, true) +
                (SkillConfigManager.getUseSetting(hero, this, "shield-health-per-wis", 1, true) * hero.getAttributeValue(AttributeType.WISDOM));
        double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 8, true);
        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 8000, true);
        String formattedDuration = String.valueOf((double) duration / 1000);

        return getDescription().replace("$1", damage + "")
                .replace("$2", range + "")
                .replace("$3", shieldHealth + "")
                .replace("$4", radius + "")
                .replace("$5", formattedDuration);
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection cs = super.getDefaultConfig();

        cs.set(SkillSetting.DAMAGE.node(), 40);
        cs.set(SkillSetting.DAMAGE_INCREASE_PER_STRENGTH.node(), 1);
        cs.set(SkillSetting.MAX_DISTANCE.node(), 6);
        cs.set("shield-health", 50);
        cs.set("shield-health-per-wis", 1);
        cs.set(SkillSetting.RADIUS.node(), 8);
        cs.set(SkillSetting.DURATION.node(), 8000);

        return cs;
    }

    public void onWarmup(Hero hero) {
        final Player player = hero.getPlayer();
        if (player.getInventory().getItemInOffHand().getType() != Material.SHIELD) {
            Messaging.sendSkillMessage(player, "You must be using a shield in your off hand to use this.");
            return;
        }
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0f, 0.5f);
        Messaging.broadcastSkillMessage(player, hero.getName() + " raises their shield...");
    }

    public SkillResult use(Hero hero, String[] args) {
        final Player player = hero.getPlayer();

        if (player.getInventory().getItemInOffHand().getType() != Material.SHIELD) {
            Messaging.sendSkillMessage(player, "You must be using a shield in your off hand to use this.");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 40, true) +
                (SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 1, true) * hero.getAttributeValue(AttributeType.STRENGTH));
        double range = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 6, true);
        double shieldHealth = SkillConfigManager.getUseSetting(hero, this, "shield-health", 50, true) +
                (SkillConfigManager.getUseSetting(hero, this, "shield-health-per-wis", 1, true) * hero.getAttributeValue(AttributeType.WISDOM));
        double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 8, true);
        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 8000, true);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.5f);
        player.getWorld().spigot().playEffect(player.getLocation().add(0, 0.2, 0), Effect.TILE_BREAK, Material.WOOD.getId(), 0,
                0.4F, 0.1F, 0.4F, 0.3F, 105, 128);
        broadcastExecuteText(hero);

        Location noPitch = player.getLocation().clone();
        noPitch.setPitch(0.0f);
        Vector look = noPitch.getDirection().normalize();
        double distTraveled = 0.0D;
        Location to = player.getLocation().clone().add(0, 0.2, 0);
        while (distTraveled < range) {
            to.getWorld().spigot().playEffect(to, Effect.CRIT, 0, 0, (float) distTraveled * 0.6F, 0.1F, (float) distTraveled * 0.6F, 0.1F, 45, 128);
            to.add(look);
            distTraveled = to.distance(player.getEyeLocation());
        }

        for (LivingEntity le : GeometryUtil.getEntitiesInFrontOf(noPitch, range)) {
            if (!damageCheck(player, le)) continue;
            damageEntity(le, player, damage, EntityDamageEvent.DamageCause.ENTITY_ATTACK, true);
            le.getWorld().playSound(le.getLocation(), Sound.ITEM_SHIELD_BREAK, 1.0F, 1.0F);
            le.getWorld().spigot().playEffect(le.getLocation().add(0, 1, 0), Effect.CRIT, 0, 0,
                    0.3F, 0.3F, 0.3F, 0.1F, 75, 128);
        }

        // hero.addEffect(new ShieldStatus(plugin, player, duration, shieldHealth));

        for (Entity e: player.getNearbyEntities(range, range, range)) {
            if (!(e instanceof Player)) continue;
            //plugin.getCharacterManager().getHero((Player) e).addEffect(new ShieldStatus(plugin, player, duration, shieldHealth));
        }
        return SkillResult.NORMAL;
    }
}
*/