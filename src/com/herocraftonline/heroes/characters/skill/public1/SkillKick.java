package com.herocraftonline.heroes.characters.skill.public1;

import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.SilenceEffect;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.CompatSound;
import com.herocraftonline.heroes.util.Util;

public class SkillKick extends TargettedSkill {

    public SkillKick(Heroes plugin) {
        super(plugin, "Kick");
        setDescription("Kick your target, dealing $1 physical damage, interrupting their casting, and silencing them for $2 seconds.");
        setUsage("/skill kick");
        setArgumentRange(0, 0);
        setIdentifiers("skill kick");
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.DAMAGING, SkillType.AGGRESSIVE, SkillType.SILENCING, SkillType.INTERRUPTING);
    }

    @Override
    public String getDescription(Hero hero) {
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 40, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 0.75, false);
        damage += (int) (damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH));

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 2000, false);
        String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription().replace("$1", damage + "").replace("$2", formattedDuration);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 3);
        node.set(SkillSetting.DAMAGE.node(), 40);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_STRENGTH.node(), 0.75);
        node.set(SkillSetting.DURATION.node(), 2000);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero, target);

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 2000, false);

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 40, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 0.75, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH);

        addSpellTarget(target, hero);
        damageEntity(target, player, damage, DamageCause.ENTITY_ATTACK);

        if (target instanceof Player) {
            SilenceEffect sEffect = new SilenceEffect(this, player, duration);
            plugin.getCharacterManager().getHero((Player) target).addEffect(sEffect);
        }

        player.getWorld().playSound(player.getLocation(), CompatSound.ENTITY_PLAYER_HURT.value(), 0.8F, 1.0F);
        //player.getWorld().spigot().playEffect(target.getLocation().add(0, 0.5, 0), org.bukkit.Effect.CRIT, 0, 0, 0, 0, 0, 1, 25, 16);
        player.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 0.5, 0), 25, 0, 0, 0, 1);

        return SkillResult.NORMAL;
    }
}
