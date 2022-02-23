package com.herocraftonline.heroes.characters.skill.remastered;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainHealthEvent;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.BloodUnionEffect;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

public class SkillSiphonBlood extends TargettedSkill {

    public SkillSiphonBlood(Heroes plugin) {
        super(plugin, "SiphonBlood");
        setDescription("Siphon blood from your target, dealing $1 dark damage and restoring your health for $2% of " +
                "the damage dealt. Life stolen is increased by $3% per level of Blood Union. Increases Blood Union by $4.");
        setUsage("/skill siphonblood");
        setArgumentRange(0, 0);
        setIdentifiers("skill siphonblood");
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.DAMAGING, SkillType.SILENCEABLE, SkillType.AGGRESSIVE,
                SkillType.ABILITY_PROPERTY_DARK);
    }

    @Override
    public String getDescription(Hero hero) {
        // Damage stuff
        double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE, false);

        // Heal mult stuff
        double healMult = SkillConfigManager.getUseSettingDouble(hero, this, "heal-mult", false);
        double healMultIncrease = SkillConfigManager.getUseSettingDouble(hero, this, "blood-union-heal-mult-increase", false);

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

        config.set(SkillSetting.MAX_DISTANCE.node(), 8);
        config.set(SkillSetting.MAX_DISTANCE_INCREASE_PER_INTELLECT.node(), 0.1);
        config.set(SkillSetting.DAMAGE.node(), 80);
        config.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 1.0);
        config.set("heal-mult", 1.1);
        config.set("blood-union-heal-mult-increase", 0.06);
        config.set("blood-union-increase", 1);

        return config;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero, target);

        double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE, false);
        double healMult = SkillConfigManager.getUseSettingDouble(hero, this, "heal-mult", false);

        // Get Blood Union Level
        int bloodUnionLevel = 0;
        if (hero.hasEffect("BloodUnionEffect")) {
            BloodUnionEffect buEffect = (BloodUnionEffect) hero.getEffect("BloodUnionEffect");
            assert buEffect != null;
            bloodUnionLevel = buEffect.getBloodUnionLevel();
        }

        // Damage target
        addSpellTarget(target, hero);
        damageEntity(target, player, damage, DamageCause.MAGIC);

        // Increase health multiplier by blood union level
        double healIncrease = SkillConfigManager.getUseSettingDouble(hero, this, "blood-union-heal-mult-increase", false);
        healMult += healIncrease * bloodUnionLevel;

        HeroRegainHealthEvent hrEvent = new HeroRegainHealthEvent(hero, damage * healMult, this, hero);
        plugin.getServer().getPluginManager().callEvent(hrEvent);
        if (!hrEvent.isCancelled()) {
            hero.heal(hrEvent.getDelta());
        }

        // Increase Blood Union
        if (hero.hasEffect("BloodUnionEffect")) {
            int bloodUnionIncrease = SkillConfigManager.getUseSetting(hero, this, "blood-union-increase", 1, false);
            BloodUnionEffect buEffect = (BloodUnionEffect) hero.getEffect("BloodUnionEffect");
            assert buEffect != null;
            buEffect.addBloodUnion(bloodUnionIncrease, target instanceof Player);
        }
        
        //player.getWorld().spigot().playEffect(target.getEyeLocation(), Effect.LAVADRIP, 0, 0, 0.3F, 0.3F, 0.3F, 0.1F, 50, 16);
        player.getWorld().spawnParticle(Particle.DRIP_LAVA, target.getEyeLocation(), 50, 0.3, 0.3, 0.3, 0.1);
        //player.getWorld().spigot().playEffect(target.getLocation().add(0, 0.5, 0), Effect.TILE_BREAK, Material.NETHER_STALK.getId(), 0, 0.3F, 0.3F, 0.3F, 0.1F, 50, 16);
        player.getWorld().spawnParticle(Particle.BLOCK_CRACK, target.getLocation(), 50, 0.3, 0.3, 0.3, 0.1, Bukkit.createBlockData(Material.NETHER_WART_BLOCK));
        player.getWorld().playSound(target.getLocation(), Sound.ENTITY_GENERIC_DRINK, 4.0F, 1);

        return SkillResult.NORMAL;
    }
}
