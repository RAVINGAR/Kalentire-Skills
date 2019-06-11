package com.herocraftonline.heroes.characters.skill.reborn.bloodmage;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.BloodUnionEffect;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Util;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.LineEffect;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.Vector;

public class SkillSiphonBlood extends TargettedSkill {

    public SkillSiphonBlood(Heroes plugin) {
        super(plugin, "SiphonBlood");
        setDescription("Siphon blood from your target, dealing $1 dark damage and restoring your health for $2% of the damage dealt. " +
                "Life stolen is increased by $3% per level of Blood Union. " +
                "Increases Blood Union by $4.");
        setUsage("/skill siphonblood");
        setIdentifiers("skill siphonblood");
        setArgumentRange(0, 0);
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.DAMAGING, SkillType.SILENCEABLE, SkillType.AGGRESSIVE, SkillType.ABILITY_PROPERTY_DARK);
    }

    @Override
    public String getDescription(Hero hero) {
        double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE, false);
        double healMult = SkillConfigManager.getUseSetting(hero, this, "heal-mult", 1.1, false);
        double healMultIncrease = SkillConfigManager.getUseSetting(hero, this, "blood-union-heal-mult-increase", 0.04, false);
        int bloodUnionIncrease = SkillConfigManager.getUseSetting(hero, this, "blood-union-increase", 1, false);

        return getDescription()
                .replace("$1", Util.decFormat.format(damage))
                .replace("$2", Util.decFormat.format(healMult * 100))
                .replace("$3", Util.decFormat.format(healMultIncrease * 100))
                .replace("$4", bloodUnionIncrease + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.MAX_DISTANCE.node(), 12);
        config.set(SkillSetting.MAX_DISTANCE_INCREASE_PER_INTELLECT.node(), 0.0);
        config.set(SkillSetting.DAMAGE.node(), 28);
        config.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 0.0);
        config.set("heal-mult", 1.5);
        config.set("blood-union-heal-mult-increase", 0.06);
        config.set("blood-union-increase", 1);
        return config;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero, target);

        // Calculate damage
        double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE, false);
        double healMult = SkillConfigManager.getUseSetting(hero, this, "heal-mult", 1.1, false);

        // Get Blood Union Level
        int bloodUnionLevel = 0;

        boolean hasBloodUnion = hero.hasEffect(BloodUnionEffect.unionEffectName);
        BloodUnionEffect buEffect = null;
        if (hasBloodUnion) {
            buEffect = (BloodUnionEffect) hero.getEffect(BloodUnionEffect.unionEffectName);
            bloodUnionLevel = buEffect.getBloodUnionLevel();
        }

        // Damage target
        addSpellTarget(target, hero);
        damageEntity(target, player, damage, DamageCause.MAGIC);

        // Increase health multiplier by blood union level
        double healIncrease = SkillConfigManager.getUseSetting(hero, this, "blood-union-heal-mult-increase", 0.05, false);
        healIncrease *= bloodUnionLevel;
        healMult += healIncrease;

        hero.tryHeal(hero, this, damage * healMult, true);

        // Increase Blood Union
        if (hasBloodUnion) {
            int bloodUnionIncrease = SkillConfigManager.getUseSetting(hero, this, "blood-union-increase", 1, false);
            if (target instanceof Player)
                buEffect.addBloodUnion(bloodUnionIncrease, true);
            else
                buEffect.addBloodUnion(bloodUnionIncrease, false);
        }

        EffectManager effectManager = new EffectManager(plugin);
        LineEffect lineVisual = new LineEffect(effectManager);
        lineVisual.particle = Particle.REDSTONE;
        lineVisual.color = Color.RED;
        lineVisual.setLocation(player.getLocation().add(new Vector(0, 0.5, 0)));
        lineVisual.setTargetEntity(target);
        effectManager.start(lineVisual);

        player.getWorld().spigot().playEffect(target.getLocation().add(0, 0.5, 0), Effect.TILE_BREAK, Material.NETHER_STALK.getId(), 0, 0.3F, 0.3F, 0.3F, 0.1F, 50, 16);
//        player.getWorld().spawnParticle(Particle.BLOCK_CRACK, target.getLocation(), 50, 0.3, 0.3, 0.3, 0.1, Bukkit.createBlockData(Material.NETHER_WART_BLOCK));
        player.getWorld().playSound(target.getLocation(), Sound.ENTITY_GENERIC_DRINK, 0.5F, 1.0F);

        return SkillResult.NORMAL;
    }
}
