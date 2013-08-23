package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
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
        setTypes(SkillType.HARMFUL, SkillType.FORCE, SkillType.PHYSICAL, SkillType.DAMAGING);
    }

    public String getDescription(Hero hero) {

        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 25, false);
        double duration = Util.formatDouble(SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 3000, false) / 1000.0);

        return getDescription().replace("$1", damage + "").replace("$2", duration + "");
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection defaultConfig = super.getDefaultConfig();

        defaultConfig.set(SkillSetting.MAX_DISTANCE.node(), 5);
        defaultConfig.set(SkillSetting.DAMAGE.node(), 25);
        defaultConfig.set(SkillSetting.DURATION.node(), 3000);
        defaultConfig.set("vertical-power", 0.25);
        defaultConfig.set("min-side-push", 0.4);
        defaultConfig.set("max-side-push", 1.0);

        return defaultConfig;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 25, false);
        addSpellTarget(target, hero);
        damageEntity(target, hero.getPlayer(), damage, EntityDamageEvent.DamageCause.ENTITY_ATTACK, false);

        double verticalPower = SkillConfigManager.getUseSetting(hero, this, "vertical-power", Double.valueOf(0.25), false);
        double minSidePush = SkillConfigManager.getUseSetting(hero, this, "min-side-push", Double.valueOf(0.4), false);
        double maxSidePush = SkillConfigManager.getUseSetting(hero, this, "max-side-push", Double.valueOf(1.0), false);
        target.setVelocity(new Vector(Math.random() * maxSidePush - minSidePush, verticalPower, Math.random() * maxSidePush - minSidePush));

        if (target instanceof Player) {
            Player targetPlayer = (Player) target;
            Hero targetHero = plugin.getCharacterManager().getHero(targetPlayer);

            int silenceDuration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 3000, false);
            if (silenceDuration > 0) {
                targetHero.addEffect(new SilenceEffect(this, silenceDuration));
            }
        }

        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.HURT_FLESH, 0.8F, 1.0F);

        broadcastExecuteText(hero, target);

        return SkillResult.NORMAL;
    }
}
