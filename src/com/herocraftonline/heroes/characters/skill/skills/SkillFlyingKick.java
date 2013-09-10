package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.SilenceEffect;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Util;

public class SkillFlyingKick extends TargettedSkill {

    public SkillFlyingKick(Heroes plugin) {
        super(plugin, "FlyingKick");
        setDescription("Deliver a Flying Kick to your target, knocking them upwards, dealing $1 damage, and silencing them for $2 seconds.");
        setUsage("/skill flyingkick");
        setArgumentRange(0, 0);
        setIdentifiers("skill flyingkick");
        setTypes(SkillType.AGGRESSIVE, SkillType.FORCE, SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.SILENCING, SkillType.DAMAGING);
    }

    public String getDescription(Hero hero) {

        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 50, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 0.75, false);
        damage += (int) (damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH));

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
        String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription().replace("$1", damage + "").replace("$2", formattedDuration);
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection defaultConfig = super.getDefaultConfig();

        defaultConfig.set(SkillSetting.MAX_DISTANCE.node(), 4);
        defaultConfig.set(SkillSetting.DAMAGE.node(), 50);
        defaultConfig.set(SkillSetting.DAMAGE_INCREASE_PER_STRENGTH.node(), Double.valueOf(0.625));
        defaultConfig.set(SkillSetting.DURATION.node(), 3000);
        defaultConfig.set("vertical-power", Double.valueOf(0.8));
        defaultConfig.set("vertical-power-increase-per-intellect", Double.valueOf(0.005));
        defaultConfig.set("min-side-push", Double.valueOf(0.4));
        defaultConfig.set("max-side-push", Double.valueOf(1.0));

        return defaultConfig;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {

        Player player = hero.getPlayer();

        broadcastExecuteText(hero, target);

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 50, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 0.75, false);
        damage += (damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH));

        addSpellTarget(target, hero);
        damageEntity(target, hero.getPlayer(), damage, EntityDamageEvent.DamageCause.ENTITY_ATTACK, false);

        double verticalPower = SkillConfigManager.getUseSetting(hero, this, "vertical-power", Double.valueOf(0.9), false);
        double verticalPowerIncrease = SkillConfigManager.getUseSetting(hero, this, "vertical-power-increase-per-intellect", Double.valueOf(0.9), false);
        verticalPower += hero.getAttributeValue(AttributeType.INTELLECT) * verticalPowerIncrease;

        double minSidePush = SkillConfigManager.getUseSetting(hero, this, "min-side-push", Double.valueOf(0.4), false);
        double maxSidePush = SkillConfigManager.getUseSetting(hero, this, "max-side-push", Double.valueOf(1.0), false);
        target.setVelocity(new Vector(Math.random() * maxSidePush - minSidePush, verticalPower, Math.random() * maxSidePush - minSidePush));

        if (target instanceof Player) {
            Player targetPlayer = (Player) target;
            Hero targetHero = plugin.getCharacterManager().getHero(targetPlayer);

            int silenceDuration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 3000, false);
            if (silenceDuration > 0) {
                targetHero.addEffect(new SilenceEffect(this, player, silenceDuration));
            }
        }

        player.getWorld().playSound(player.getLocation(), Sound.HURT_FLESH, 0.8F, 1.0F);

        return SkillResult.NORMAL;
    }
}
