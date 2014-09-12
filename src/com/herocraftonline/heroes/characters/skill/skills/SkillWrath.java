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
    // This is for Firework Effects
    //public VisualEffect fplayer = new VisualEffect();

    public SkillWrath(Heroes plugin) {
        super(plugin, "Wrath");
        setDescription("You instill wrath to the target, dealing $1 light damage to the target. Will instead deal $2 damage if the target is undead.");
        setUsage("/skill warth");
        setArgumentRange(0, 0);
        setIdentifiers("skill warth");
        setTypes(SkillType.DAMAGING, SkillType.ABILITY_PROPERTY_LIGHT, SkillType.SILENCEABLE, SkillType.AGGRESSIVE);
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
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 1.2);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero, target);

        int intellect = hero.getAttributeValue(AttributeType.INTELLECT);

        double damage;
        if (Util.isUndead(plugin, target)) {
            damage = SkillConfigManager.getUseSetting(hero, this, "undead-damage", 80, false);
            double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.0, false);
            damage += (damageIncrease * intellect);
        }
        else {
            damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 40, false);
            double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.0, false);
            damage += (damageIncrease * intellect);
        }

        addSpellTarget(target, hero);
        damageEntity(target, player, damage, DamageCause.ENTITY_EXPLOSION);

        /* this is our fireworks
        try {
            fplayer.playFirework(player.getWorld(), target.getLocation().add(0, 1.5, 0), FireworkEffect.builder().flicker(false)
                    .trail(false).with(FireworkEffect.Type.BALL).withColor(Color.SILVER).withFade(Color.NAVY).build());
        } catch (Exception e) {
            e.printStackTrace();
        }*/

        player.getWorld().spigot().playEffect(target.getLocation().add(0, 0.2, 0), Effect.MAGIC_CRIT, 0, 0, 0, 0, 0, 1, 25, 16);
        player.getWorld().spigot().playEffect(target.getLocation().add(0, 0.3, 0), Effect.MAGIC_CRIT, 0, 0, 0, 0, 0, 1, 25, 16);
        player.getWorld().spigot().playEffect(target.getLocation().add(0, 0.4, 0), Effect.MAGIC_CRIT, 0, 0, 0, 0, 0, 1, 25, 16);
        player.getWorld().spigot().playEffect(target.getLocation().add(0, 0.5, 0), Effect.MAGIC_CRIT, 0, 0, 0, 0, 0, 1, 25, 16);
        player.getWorld().spigot().playEffect(target.getLocation().add(0, 0.6, 0), Effect.MAGIC_CRIT, 0, 0, 0, 0, 0, 1, 25, 16);
        player.getWorld().spigot().playEffect(target.getLocation().add(0, 0.7, 0), Effect.MAGIC_CRIT, 0, 0, 0, 0, 0, 1, 25, 16);
        return SkillResult.NORMAL;
    }
}
