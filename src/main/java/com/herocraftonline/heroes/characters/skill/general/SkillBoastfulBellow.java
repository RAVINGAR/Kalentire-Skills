package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.util.List;

public class SkillBoastfulBellow extends TargettedSkill {

    public SkillBoastfulBellow(Heroes plugin) {
        super(plugin, "BoastfulBellow");
        setDescription("Unleash a Boastful Bellow on your target, dealing $1 damage and all enemies within $2 blocks of them. Enemies hit with the ability will also have their casting interrupted.");
        setUsage("/skill boastfulbellow");
        setArgumentRange(0, 0);
        setIdentifiers("skill boastfulbellow");
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.DAMAGING, SkillType.INTERRUPTING, SkillType.AREA_OF_EFFECT, SkillType.SILENCEABLE, SkillType.AGGRESSIVE);
    }

    @Override
    public String getDescription(Hero hero) {
        double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5.0, false);
        double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE, false);

        return getDescription()
                .replace("$1", Util.decFormat.format(damage))
                .replace("$2", Util.decFormat.format(radius));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.MAX_DISTANCE.node(), 4.0);
        node.set(SkillSetting.DAMAGE.node(), 60.0);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 0.0);
        node.set(SkillSetting.RADIUS.node(), 3.0);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5.0, false);
        double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE, false);

        broadcastExecuteText(hero, target);

        addSpellTarget(target, hero);
        damageEntity(target, player, damage, DamageCause.MAGIC);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.5F, .5F);

        long currentTime = System.currentTimeMillis();
        List<Entity> entities = target.getNearbyEntities(radius, radius, radius);
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

        //player.getWorld().spigot().playEffect(target.getLocation().add(0, 0.5, 0), org.bukkit.Effect.MAGIC_CRIT, 0, 0, 0, 0, 0, 1, 25, 16);
        player.getWorld().spawnParticle(Particle.CRIT_MAGIC, target.getLocation().add(0, 0.5, 0), 25, 0, 0, 0, 1);
        //target.getWorld().spigot().playEffect(target.getLocation(), Effect.NOTE, 1, 1, 0F, 1F, 0F, 50F, 30, 10);
        target.getWorld().spawnParticle(Particle.NOTE, target.getLocation(), 30, 0, 1, 0, 50);
        //target.getWorld().spigot().playEffect(target.getLocation(), Effect.EXPLOSION_HUGE, 0, 0, 0F, 0F, 0F, 0F, 1, 12);
        target.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, target.getLocation(), 1, 0, 0, 0, 0);

        return SkillResult.NORMAL;
    }
}