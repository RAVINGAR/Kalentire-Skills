package com.herocraftonline.heroes.characters.skill.skills;

import java.util.List;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Sound;
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

public class SkillBoastfulBellow extends ActiveSkill {
    // This is for Firework Effects
    public VisualEffect fplayer = new VisualEffect();
    public SkillBoastfulBellow(Heroes plugin) {
        super(plugin, "BoastfulBellow");
        setDescription("Unleash a Boastful Bellow, dealing $1 damage to nearby enemies and interrupting their casting.");
        setUsage("/skill boastfulbellow");
        setArgumentRange(0, 0);
        setIdentifiers("skill boastfulbellow");
        setTypes(SkillType.DAMAGING, SkillType.INTERRUPTING, SkillType.SILENCABLE, SkillType.AGGRESSIVE);
    }

    @Override
    public String getDescription(Hero hero) {
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 50, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.0, false);
        damage += (int) (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        return getDescription().replace("$1", damage + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DAMAGE.node(), 60);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), Double.valueOf(1.5));
        node.set(SkillSetting.RADIUS.node(), 5);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5, false);
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 50, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.5, false);
        damage += (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.ENDERDRAGON_GROWL, 0.5F, 1.0F);
        broadcastExecuteText(hero);

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
                fplayer.playFirework(player.getWorld(), 
                                     target.getLocation().add(0, 1.5, 0),
                                     FireworkEffect.builder()
                                                   .flicker(false)
                                                   .trail(false)
                                                   .with(FireworkEffect.Type.BALL)
                                                   .withColor(Color.GRAY)
                                                   .withFade(Color.NAVY)
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
