package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.util.MovingParticle;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;

/** Port of Ardorlon's Dash Skill */
public class SkillDash extends ActiveSkill
{
    public SkillDash(Heroes plugin)
    {
        super(plugin, "Dash");
        setUsage("/skill dash");
        setIdentifiers("skill dash");
        setArgumentRange(0, 0);
        setDescription("You dash forward, dealing $1 damage to all enemies in your path and knocking them back. Deals an extra $2 damage to airborne enemies and knocks them back further. Dash speed increases with Dexterity.");
        setTypes(SkillType.DAMAGING, SkillType.MOVEMENT_INCREASING, SkillType.ABILITY_PROPERTY_PHYSICAL);
    }

    public String getDescription(Hero hero)
    {
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 35, true) +
                SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 0.4, true) * hero.getAttributeValue(AttributeType.STRENGTH);
        double bonusDmg = SkillConfigManager.getUseSetting(hero, this, "bonus-damage", 15, true) +
                SkillConfigManager.getUseSetting(hero, this, "bonus-damage-per-dexterity", 0.5, true) * hero.getAttributeValue(AttributeType.DEXTERITY);

        return getDescription().replace("$1", damage + "")
                .replace("$2", bonusDmg + "");
    }

    public ConfigurationSection getDefaultConfig()
    {
        ConfigurationSection cs = super.getDefaultConfig();

        cs.set(SkillSetting.DAMAGE.node(), 35);
        cs.set(SkillSetting.DAMAGE_INCREASE_PER_STRENGTH.node(), 0.4);
        cs.set("bonus-damage", 15);
        cs.set("bonus-damage-per-dexterity", 0.5);
        cs.set("speed-boost-per-dexterity", 0.0125);
        cs.set("speed-mult", 1.0001);

        return cs;
    }

    public SkillResult use(Hero h, String[] args)
    {
        final Hero hero = h;
        final Player player = hero.getPlayer();

        final double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 35, true) +
                SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 0.4, true) * hero.getAttributeValue(AttributeType.STRENGTH);
        final double bonusDmg = SkillConfigManager.getUseSetting(hero, this, "bonus-damage", 15, true) +
                SkillConfigManager.getUseSetting(hero, this, "bonus-damage-per-dexterity", 0.5, true) * hero.getAttributeValue(AttributeType.DEXTERITY);

        final ArrayList<LivingEntity> hit = new ArrayList<>();

        final double speedMult = SkillConfigManager.getUseSetting(hero, this, "speed-mult", 1.0001, true);
        double boost = SkillConfigManager.getUseSetting(hero, this, "speed-boost-per-dexterity", 0.0125, true) * hero.getAttributeValue(AttributeType.DEXTERITY);

        final Vector velocity = player.getLocation().getDirection().clone().setY(0.0D).multiply(speedMult + boost);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0F, 1.0F);

        new BukkitRunnable() {
            int ticks = 0;
            int maxTicks = (1 * 5);
            public void run() {
                if (ticks == maxTicks) cancel();
                player.setVelocity(velocity);
                float minX = (float) (velocity.getX() * -1);
                float minY = (float) (velocity.getY() * -1);
                float minZ = (float) (velocity.getZ() * -1);
                //FIXME Alter method source
//                MovingParticle.createMovingParticle(player.getLocation().add(0, 1.5, 0), Effect.CLOUD, 0, 0,
//                        0.6F, 1.0F, 0.6F, minX, minY, minZ, 10, 128, true);
                for (Entity e : player.getNearbyEntities(3.0D, 3.0D, 3.0D))
                {
                    if (!(e instanceof LivingEntity)) continue;
                    LivingEntity le = (LivingEntity) e;
                    if (!damageCheck(player, le)) continue;
                    if (hit.contains(le)) continue;
                    boolean inAir = le.getLocation().getBlock().getType() == Material.AIR;
                    double finalDmg = damage + (inAir ? bonusDmg : 0);
                    addSpellTarget(le, hero);
                    damageEntity(le, player, finalDmg, EntityDamageEvent.DamageCause.ENTITY_ATTACK, false);
                    le.setVelocity(velocity.clone().divide(new Vector(3, 3, 3)).setY(0.4).add(inAir ? velocity.clone().divide(new Vector(5, 5, 5)) : new Vector(0, 0, 0)));
                    le.getWorld().playSound(le.getLocation(), Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 2.0F, 1.0F);
                    hit.add(le);
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);

        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }
}
