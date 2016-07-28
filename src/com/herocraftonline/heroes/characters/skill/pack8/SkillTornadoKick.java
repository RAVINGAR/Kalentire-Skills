package com.herocraftonline.heroes.characters.skill.pack8;

import com.google.common.collect.Lists;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.ncp.NCPFunction;
import com.herocraftonline.heroes.characters.skill.ncp.NCPUtils;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.Vector;

import java.util.List;

public class SkillTornadoKick extends ActiveSkill {

    public SkillTornadoKick(Heroes plugin) {
        super(plugin, "TornadoKick");
        setDescription("Unleash a Tornado Kick, dealing $1 physical damage and knocking nearby enemies within $2 blocks away from you.");
        setUsage("/skill tornadokick");
        setArgumentRange(0, 0);
        setIdentifiers("skill tornadokick");
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.FORCE, SkillType.DAMAGING, SkillType.AGGRESSIVE);
    }

    @Override
    public String getDescription(Hero hero) {
        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5, false);

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 50, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 1.6, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH);

        String formattedDamage = Util.decFormat.format(damage);
        
        return getDescription().replace("$2", radius + "").replace("$1", formattedDamage);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DAMAGE.node(), 50);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_STRENGTH.node(), 1.125);
        node.set(SkillSetting.RADIUS.node(), 5);
        node.set(SkillSetting.DELAY.node(), 500);
        node.set("horizontal-power", 0.4);
        node.set("horizontal-power-increase-per-intellect", 0.015);
        node.set("vertical-power", 0.2);
        node.set("vertical-power-increase-per-intellect", 0.0075);
        node.set("ncp-exemption-duration", 1000);

        return node;
    }

    @Override
    public void onWarmup(Hero hero) {
        Player player = hero.getPlayer();
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.7F, 0.4F);
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5, false);

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 50, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 1.125, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH);

        double hPower = SkillConfigManager.getUseSetting(hero, this, "horizontal-power", 0.4, false);
        double hPowerIncrease = SkillConfigManager.getUseSetting(hero, this, "horizontal-power-increase-per-intellect", 0.015, false);
        hPower += (hPowerIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        double vPower = SkillConfigManager.getUseSetting(hero, this, "vertical-power", 0.0, false);
        double vPowerIncrease = SkillConfigManager.getUseSetting(hero, this, "vertical-power-increase-per-intellect", 0.0, false);
        vPower += (vPowerIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        broadcastExecuteText(hero);

        List<Entity> entities = hero.getPlayer().getNearbyEntities(radius, radius, radius);
        for (Entity entity : entities) {
            if (!(entity instanceof LivingEntity)) {
                continue;
            }

            // Check if the target is damagable
            if (!damageCheck(player, (LivingEntity) entity)) {
                continue;
            }

            final LivingEntity target = (LivingEntity) entity;

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

            // Let's bypass the nocheat issues...
            final Vector velocity = new Vector(xDir, vPower, zDir);
            NCPUtils.applyExemptions(target, new NCPFunction() {
                
                @Override
                public void execute()
                {
                    target.setVelocity(velocity);                    
                }
            }, Lists.newArrayList("MOVING"), SkillConfigManager.getUseSetting(hero, this, "ncp-exemption-duration", 500, false));
        }

        player.getWorld().playEffect(player.getLocation(), Effect.MOBSPAWNER_FLAMES, 3);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.5F, 1.0F);

        return SkillResult.NORMAL;
    }
}
