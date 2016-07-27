package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Effect;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

public class SkillWrath extends TargettedSkill {

    public SkillWrath(Heroes plugin) {
        super(plugin, "Wrath");
        setDescription("You instill wrath to the target, dealing $1 light damage to the target. Will instead deal $2 damage if the target is undead.");
        setUsage("/skill warth");
        setArgumentRange(0, 0);
        setIdentifiers("skill wrath");
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.DAMAGING, SkillType.ABILITY_PROPERTY_LIGHT, SkillType.SILENCEABLE, SkillType.AGGRESSIVE);
    }

    @Override
    public String getDescription(Hero hero) {

        int intellect = hero.getAttributeValue(AttributeType.INTELLECT);

        int undeadDamage = SkillConfigManager.getUseSetting(hero, this, "undead-damage", 80, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.0, false);
        undeadDamage += damageIncrease * intellect;

        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 40, false);
        damage += damageIncrease * intellect;

        String formattedUndeadDamage = Util.decFormat.format(undeadDamage);
        String formattedDamage = Util.decFormat.format(damage);

        return getDescription().replace("$1", formattedDamage).replace("$2", formattedUndeadDamage);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 6);
        node.set("undead-damage", 120);
        node.set(SkillSetting.DAMAGE.node(), (double) 60);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 1.15);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero, target);

        int intellect = hero.getAttributeValue(AttributeType.INTELLECT);

        double damage;
        if (Util.isUndead(plugin, target)) {
            damage = SkillConfigManager.getUseSetting(hero, this, "undead-damage", 120, false);
            double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 2.0, false);
            damage += (damageIncrease * intellect);
        }
        else {
            damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 60, false);
            double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.15, false);
            damage += (damageIncrease * intellect);
        }

        addSpellTarget(target, hero);
        damageEntity(target, player, damage, DamageCause.MAGIC);

        //public void playEffect(Location location, Effect effect, int_ id, int_ data, float_ offsetX, float_ offsetY, float_ offsetZ, float_ speed, int_ particleCount, int_ radius)
        //public void playEffect(Location location, Effect effect,  id,  data,  offsetX,  offsetY,  offsetZ,  speed,  particleCount,  radius)
        target.getWorld().spigot().playEffect(target.getLocation(), Effect.VILLAGER_THUNDERCLOUD, 1, 1, 1F, 1F, 1F, 1F, 30, 10);
        target.getWorld().spigot().playEffect(target.getLocation(), Effect.VILLAGER_THUNDERCLOUD, 1, 1, 10F, 1F, 1F, 50F, 30, 10);
        target.getWorld().spigot().playEffect(target.getLocation(), Effect.PORTAL, 1, 1, 0F, 0F, 0F, 10F, 200, 10);
        target.getWorld().spigot().playEffect(target.getLocation(), Effect.PORTAL, 1, 1, -5F, -5F, -5F, 10F, 100, 10);
        player.getWorld().playEffect(player.getLocation(), Effect.GHAST_SHOOT, 0);
        player.getWorld().spigot().playEffect(target.getLocation().add(5.0, 5.0, 0), Effect.POTION_SWIRL, 0, 0, 0.3F, 0.3F, 0.3F, 25, 100, 20);
        /*player.getWorld().spigot().playEffect(target.getLocation().add(5.0, 5.0, 0), Effect.LAVADRIP, 0, 0, 0, 0, 0, 1, 25, 16);
        player.getWorld().spigot().playEffect(target.getLocation().add(4.0, 5.0, 0), Effect.LAVADRIP, 0, 0, 0, 0, 0, 1, 25, 16);
        player.getWorld().spigot().playEffect(target.getLocation().add(3.0, 5.0, 0), Effect.LAVADRIP, 0, 0, 0, 0, 0, 1, 25, 16);
        player.getWorld().spigot().playEffect(target.getLocation().add(2.0, 5.0, 0), Effect.LAVADRIP, 0, 0, 0, 0, 0, 1, 25, 16);
        player.getWorld().spigot().playEffect(target.getLocation().add(1.0, 5.0, 0), Effect.LAVADRIP, 0, 0, 0, 0, 0, 1, 25, 16);
        player.getWorld().spigot().playEffect(target.getLocation().add(0, 5.0, 5), Effect.LAVADRIP, 0, 0, 0, 0, 0, 1, 25, 16);
        player.getWorld().spigot().playEffect(target.getLocation().add(0, 5.0, 4), Effect.LAVADRIP, 0, 0, 0, 0, 0, 1, 25, 16);
        player.getWorld().spigot().playEffect(target.getLocation().add(0, 5.0, 3), Effect.LAVADRIP, 0, 0, 0, 0, 0, 1, 25, 16);
        player.getWorld().spigot().playEffect(target.getLocation().add(0, 5.0, 2), Effect.LAVADRIP, 0, 0, 0, 0, 0, 1, 25, 16);
        player.getWorld().spigot().playEffect(target.getLocation().add(0, 5.0, 1), Effect.LAVADRIP, 0, 0, 0, 0, 0, 1, 25, 16);
        player.getWorld().spigot().playEffect(target.getLocation().add(0, 8.0, 0), Effect.LAVADRIP, 0, 0, 0, 0, 0, 1, 25, 16);
        player.getWorld().spigot().playEffect(target.getLocation().add(0, 7.0, 0), Effect.LAVADRIP, 0, 0, 0, 0, 0, 1, 25, 16);
        player.getWorld().spigot().playEffect(target.getLocation().add(0, 6.0, 0), Effect.LAVADRIP, 0, 0, 0, 0, 0, 1, 25, 16);
        player.getWorld().spigot().playEffect(target.getLocation().add(0, 5.0, 0), Effect.LAVADRIP, 0, 0, 0, 0, 0, 1, 25, 16);
        player.getWorld().spigot().playEffect(target.getLocation().add(0, 4.0, 0), Effect.LAVADRIP, 0, 0, 0, 0, 0, 1, 25, 16);
        player.getWorld().spigot().playEffect(target.getLocation().add(0, 3.0, 0), Effect.LAVADRIP, 0, 0, 0, 0, 0, 1, 25, 16);
        player.getWorld().spigot().playEffect(target.getLocation().add(0, 2.0, 0), Effect.LAVADRIP, 0, 0, 0, 0, 0, 1, 25, 16);
        player.getWorld().spigot().playEffect(target.getLocation().add(0, 1.0, 0), Effect.LAVADRIP, 0, 0, 0, 0, 0, 1, 25, 16);
        player.getWorld().spigot().playEffect(target.getLocation().add(0, 0.0, 0), Effect.LAVADRIP, 0, 0, 0, 0, 0, 1, 25, 16);*/
        return SkillResult.NORMAL;
    }
}
