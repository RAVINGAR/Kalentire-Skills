package com.herocraftonline.heroes.characters.skill.skills;

import java.util.List;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.VisualEffect;

public class SkillPulse extends ActiveSkill {
    // This is for Firework Effects
    public VisualEffect fplayer = new VisualEffect();
    public SkillPulse(Heroes plugin) {
        super(plugin, "Pulse");
        setDescription("You deal $1 damage to all enemies within $2 blocks.");
        setUsage("/skill pulse");
        setArgumentRange(0, 0);
        setIdentifiers("skill pulse");
        setTypes(SkillType.DAMAGING, SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.SILENCEABLE, SkillType.AGGRESSIVE);
    }

    @Override
    public String getDescription(Hero hero) {
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 60, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.0, false);
        damage += (int) (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5, false);

        return getDescription().replace("$1", damage + "").replace("$2", radius + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DAMAGE.node(), 60);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), Double.valueOf(1.0));
        node.set(SkillSetting.RADIUS.node(), 5);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero);

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, Integer.valueOf(60), false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, Double.valueOf(1.0), false);
        damage += (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5, false);

        List<Entity> entities = hero.getPlayer().getNearbyEntities(radius, radius, radius);
        for (Entity entity : entities) {
            if (!(entity instanceof LivingEntity)) {
                continue;
            }

            LivingEntity target = (LivingEntity) entity;
            if (!damageCheck(player, target))
                continue;

            addSpellTarget(target, hero);
            damageEntity(target, player, damage, DamageCause.MAGIC);

            // this is our fireworks shit
            try {
                fplayer.playFirework(player.getWorld(), target.getLocation().add(0,1.5,0), 
                		FireworkEffect.builder().flicker(false).trail(false)
                		.with(FireworkEffect.Type.BALL)
                		.withColor(Color.YELLOW)
                		.withFade(Color.SILVER)
                		.build());
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        return SkillResult.NORMAL;
    }
}
