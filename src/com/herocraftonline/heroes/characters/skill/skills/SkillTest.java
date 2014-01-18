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

public class SkillTest extends TargettedSkill {
    // This is for Firework Effects
    public VisualEffect fplayer = new VisualEffect();

    public SkillTest(Heroes plugin) {
        super(plugin, "Test");
        setDescription("You test the target, dealing $1 light damage to the target. Will instead deal $2 damage if the target is undead.");
        setUsage("/skill test");
        setArgumentRange(0, 0);
        setIdentifiers("skill test");
        setTypes(SkillType.DAMAGING, SkillType.ABILITY_PROPERTY_LIGHT, SkillType.SILENCABLE, SkillType.AGGRESSIVE);
    }

    @Override
    public String getDescription(Hero hero) {

        int intellect = hero.getAttributeValue(AttributeType.INTELLECT);

        int undeadDamage = SkillConfigManager.getUseSetting(hero, this, "undead-damage", Integer.valueOf(80), false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, Double.valueOf(1.0), false);
        undeadDamage += damageIncrease * intellect;

        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, Integer.valueOf(40), false);
        damage += damageIncrease * intellect;

        String formattedUndeadDamage = Util.decFormat.format(undeadDamage);
        String formattedDamage = Util.decFormat.format(damage);

        return getDescription().replace("$1", formattedDamage).replace("$2", formattedUndeadDamage);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), Integer.valueOf(6));
        node.set("undead-damage", Integer.valueOf(80));
        node.set(SkillSetting.DAMAGE.node(), Double.valueOf(40));
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), Double.valueOf(1.0));

        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero, target);

        int intellect = hero.getAttributeValue(AttributeType.INTELLECT);

        double damage = 0;
        if (Util.isUndead(plugin, target)) {
            damage = SkillConfigManager.getUseSetting(hero, this, "undead-damage", Integer.valueOf(80), false);
            double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, Double.valueOf(1.0), false);
            damage += (damageIncrease * intellect);
        }
        else {
            damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, Integer.valueOf(40), false);
            double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, Double.valueOf(1.0), false);
            damage += (damageIncrease * intellect);
        }

        addSpellTarget(target, hero);
        damageEntity(target, player, damage, DamageCause.MAGIC);
        /* This is the new Particle API system for Spigot - the first few int = id, data, offsetX/Y/Z, speed, count, radius)
        * offset controls how spread out the particles are
        * id and data only work for two particles: ITEM_BREAK and TILE_BREAK
        * */
        player.getWorld().spigot().playEffect(target.getLocation().add(0, 1.5, 0), Effect.MAGIC_CRIT, 0, 0, 0, 0, 0, 1, 25, 16);

        /* this is our fireworks
        try {
            fplayer.playFirework(player.getWorld(), target.getLocation().add(0, 1.5, 0), FireworkEffect.builder().flicker(false).trail(false).with(FireworkEffect.Type.BALL).withColor(Color.SILVER).withFade(Color.NAVY).build());
        }
        catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        catch (Exception e) {
            e.printStackTrace();
        }*/

        return SkillResult.NORMAL;
    }
}
