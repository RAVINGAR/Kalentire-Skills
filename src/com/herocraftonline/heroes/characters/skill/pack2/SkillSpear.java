package com.herocraftonline.heroes.characters.skill.pack2;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.Vector;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.heroes.util.CompatSound;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

public class SkillSpear extends TargettedSkill {

    public SkillSpear(Heroes plugin) {
        super(plugin, "Spear");
        setDescription("Spear your target, pulling him back towards you and dealing $1 physical damage");
        setUsage("/skill spear");
        setArgumentRange(0, 0);
        setIdentifiers("skill spear");
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.DAMAGING, SkillType.AGGRESSIVE);
    }

    @Override
    public String getDescription(Hero hero) {
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 30, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 0.7, false);
        damage += (int) (damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH));

        return getDescription().replace("$1", damage + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 12);
        node.set(SkillSetting.DAMAGE.node(), 45);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_STRENGTH.node(), 1.375);
        node.set("weapons", Util.shovels);
        node.set("ncp-exemption-duration", 1000);
        node.set(SkillSetting.DELAY.node(), 500);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, final LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        Material item = NMSHandler.getInterface().getItemInMainHand(player.getInventory()).getType();
        if (!SkillConfigManager.getUseSetting(hero, this, "weapons", Util.shovels).contains(item.name())) {
            Messaging.send(player, "You can't use spear with that weapon!");
            return SkillResult.FAIL;
        }

        broadcastExecuteText(hero, target);

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 50, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 1.6, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH);

        if (damage > 0) {
            addSpellTarget(target, hero);
            damageEntity(target, player, damage, DamageCause.ENTITY_ATTACK, false);
        }

        Location playerLoc = player.getLocation();
        Location targetLoc = target.getLocation();

        final double vPower = SkillConfigManager.getUseSetting(hero, this, "vertical-power", 0.4, false);

        Vector pushUpVector = new Vector(0, vPower, 0);
        target.setVelocity(pushUpVector);

        final double xDir = (playerLoc.getX() - targetLoc.getX()) / 3;
        final double zDir = (playerLoc.getZ() - targetLoc.getZ()) / 3;

        final double hPower = SkillConfigManager.getUseSetting(hero, this, "horizontal-power", 0.5, false);

        // push them "up" first. THEN we can pull them to us.
        double delay = SkillConfigManager.getUseSetting(hero, this, "pull-delay", 0.2, false);
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            public void run() {
                // Push them away
                //double yDir = player.getVelocity().getY();
                Vector pushVector = new Vector(xDir, 0, zDir).normalize().multiply(hPower).setY(vPower);
                target.setVelocity(pushVector);
            }
        }, (long) (delay * 20));

        player.getWorld().spigot().playEffect(target.getLocation().add(0, 0.5, 0), org.bukkit.Effect.CRIT, 0, 0, 0, 0, 0, 1, 25, 16);
        player.getWorld().playSound(player.getLocation(), CompatSound.ENTITY_PLAYER_HURT.value(), 0.8F, 1.0F);

        return SkillResult.NORMAL;
    }

}
