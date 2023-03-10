package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.SlowEffect;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

public class SkillEmpathy extends TargettedSkill {

    public SkillEmpathy(Heroes plugin) {
        super(plugin, "Empathy");
        setDescription("You deal up to $1 dark damage equal to $2 base damage and $3% of your missing health and slow the target for $4 second(s).");
        setUsage("/skill empathy");
        setArgumentRange(0, 0);
        setIdentifiers("skill empathy");
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.ABILITY_PROPERTY_DARK, SkillType.DAMAGING, SkillType.SILENCEABLE, SkillType.AGGRESSIVE);
    }

    @Override
    public String getDescription(Hero hero) {
        double baseDamage = SkillConfigManager.getScaledUseSettingDouble(hero, this, "base-damage", false);
        double maxDamage = SkillConfigManager.getScaledUseSettingDouble(hero, this, "max-damage", false);
        double modifier = SkillConfigManager.getScaledUseSettingDouble(hero, this, "damage-modifier", 0.5, false);

        int slowDuration = SkillConfigManager.getUseSetting(hero, this, "slow-duration", 4000, false);

        return getDescription()
                .replace("$1", Util.decFormat.format(maxDamage))
                .replace("$2", Util.decFormat.format(baseDamage))
                .replace("$3", Util.decFormat.format(modifier * 100.0))
                .replace("$4", Util.decFormat.format(slowDuration / 1000.0));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.MAX_DISTANCE.node(), 10);
        config.set("base-damage", 40);
        config.set("base-damage-per-level", 0.0);
        config.set("max-damage", 152);
        config.set("max-damage-increase-per-intellect", 1.0);
        config.set("max-damage-per-level", 0.0);
        config.set("damage-modifier", 1.0);
        config.set("damage-modifier-increase-per-intellect", 0.0);
        config.set("damage-modifier-per-level", 0.0);
        config.set("slow-duration", 4000);
        config.set("slow-amplifier", 2);
        return config;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();
        if (target.equals(player)) {
            return SkillResult.INVALID_TARGET;
        }

        double baseDamage = SkillConfigManager.getScaledUseSettingDouble(hero, this, "base-damage", false);
        double maxDamage = SkillConfigManager.getScaledUseSettingDouble(hero, this, "max-damage", false);
        double modifier = SkillConfigManager.getScaledUseSettingDouble(hero, this, "damage-modifier", 0.5, false);

        double damage = baseDamage + (player.getMaxHealth() - player.getHealth()) * modifier;
        if (damage > maxDamage)
            damage = maxDamage;

        if (damage > 0) {
            addSpellTarget(target, hero);
            damageEntity(target, player, damage, DamageCause.MAGIC);
        }

        int slowDuration = SkillConfigManager.getUseSetting(hero, this, "slow-duration", 4000, false);
        int amplifier = SkillConfigManager.getUseSetting(hero, this, "amplifier", 2, false);

        CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
        targetCT.addEffect(new SlowEffect(this, player, slowDuration, amplifier, null, null));

        broadcastExecuteText(hero, target);
        
        //target.getWorld().spigot().playEffect(target.getLocation().add(0, 0.4, 0), Effect.WITCH_MAGIC, 0, 0, 0.3F, 0.3F, 0.3F, 0.5F, 45, 16);
        target.getWorld().spawnParticle(Particle.SPELL_WITCH, target.getLocation().add(0, 0.4, 0), 45, 0.3, 0.3, 0.3, 0.5);
        //target.getWorld().spigot().playEffect(target.getLocation().add(0, 0.4, 0), Effect.SPELL, 0, 0, 0.3F, 0.3F, 0.3F, 0.0F, 15, 16);
        target.getWorld().spawnParticle(Particle.SPELL, target.getLocation().add(0, 0.4, 0), 15, 0.3, 0.3, 0.3, 0);

        return SkillResult.NORMAL;
    }

}