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
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.characters.skill.VisualEffect;
import com.herocraftonline.heroes.util.Util;

public class SkillBoastfulBellow extends TargettedSkill {
    // This is for Firework Effects
    public VisualEffect fplayer = new VisualEffect();

    public SkillBoastfulBellow(Heroes plugin) {
        super(plugin, "BoastfulBellow");
        setDescription("Unleash a Boastful Bellow on your target, dealing $1 damage and all enemies within $2 blocks of them. Enemies hit with the ability will also have their casting interrupted.");
        setUsage("/skill boastfulbellow");
        setArgumentRange(0, 0);
        setIdentifiers("skill boastfulbellow");
        setTypes(SkillType.DAMAGING, SkillType.INTERRUPTING, SkillType.AREA_OF_EFFECT, SkillType.SILENCABLE, SkillType.AGGRESSIVE);
    }

    @Override
    public String getDescription(Hero hero) {
        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5, false);

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 50, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.0, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT);

        String formattedDamage = Util.decFormat.format(damage);

        return getDescription().replace("$1", formattedDamage).replace("$2", radius + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), Integer.valueOf(4));
        node.set(SkillSetting.DAMAGE.node(), 60);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), Double.valueOf(1.5));
        node.set(SkillSetting.RADIUS.node(), 3);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5, false);
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 50, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.5, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT);

        broadcastExecuteText(hero, target);

        addSpellTarget(target, hero);
        damageEntity(target, player, damage, DamageCause.MAGIC);

        player.getWorld().playSound(player.getLocation(), Sound.ENDERDRAGON_GROWL, 1.5F, .5F);

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
        }
        catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        long currentTime = System.currentTimeMillis();
        List<Entity> entities = hero.getPlayer().getNearbyEntities(radius, radius, radius);
        for (Entity entity : entities) {
            if (!(entity instanceof LivingEntity)) {
                continue;
            }

            LivingEntity newTarget = (LivingEntity) entity;
            CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);

            if (!damageCheck(player, newTarget))
                continue;

            addSpellTarget(newTarget, hero);
            damageEntity(newTarget, player, damage, DamageCause.MAGIC);
            
            if (targetCT instanceof Hero) {
                Hero enemy = (Hero) targetCT;
                if (enemy.getDelayedSkill() != null) {
                    if (enemy.cancelDelayedSkill())
                        enemy.setCooldown("global", Heroes.properties.globalCooldown + currentTime);
                }
            }
        }

        return SkillResult.NORMAL;
    }
}
