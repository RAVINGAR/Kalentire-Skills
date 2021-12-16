package com.herocraftonline.heroes.characters.skill.remastered;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.BloodUnionEffect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicDamageEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

public class SkillCombustBlood extends TargettedSkill {
    private final String bloodUnionEffectName = "BloodUnionEffect";
    private String applyText;
    private String expireText;

    public SkillCombustBlood(Heroes plugin) {
        super(plugin, "CombustBlood");
        setDescription("Combust the blood of your target, dealing $1 dark damage. " +
                "If you have Blood Union $2 or greater, the target will bleed, taking an additional $3 damage over $4 second(s). " +
                "Increases Blood Union by 1.");
        setUsage("/skill combustblood");
        setIdentifiers("skill combustblood");
        setArgumentRange(0, 0);
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.DAMAGING, SkillType.SILENCEABLE, SkillType.AGGRESSIVE, SkillType.ABILITY_PROPERTY_DARK);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.MAX_DISTANCE.node(), 12.0);
        config.set(SkillSetting.MAX_DISTANCE_INCREASE_PER_INTELLECT.node(), 0.0);
        config.set(SkillSetting.DAMAGE.node(), 25.0);
        config.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 0.0);
        config.set("blood-union-increase", 1);
        config.set("blood-union-required-for-dot", 3);
        config.set(SkillSetting.HEALTH_COST.node(), 40.0);
        config.set(SkillSetting.DAMAGE_TICK.node(), 7.5);
        config.set(SkillSetting.DAMAGE_TICK_INCREASE_PER_INTELLECT.node(), 0.0);
        config.set(SkillSetting.PERIOD.node(), 2000);
        config.set(SkillSetting.DURATION.node(), 8000);
        config.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% is bleeding from the effects of their Combusted Blood!");
        config.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% is no longer bleeding.");
        return config;
    }

    @Override
    public String getDescription(Hero hero) {
        int bloodUnionReq = SkillConfigManager.getUseSetting(hero, this, "blood-union-required-for-dot", 3, false);
        int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 2000, false);
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 8000, false);
        double healthCost = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALTH_COST, 40.0, false);
        double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE, false);
        double tickDamage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE_TICK, false);

        return getDescription()
                .replace("$1", Util.decFormat.format(damage))
                .replace("$2", bloodUnionReq + "")
                .replace("$3", Util.decFormat.format((tickDamage * ((double) duration / (double) period))))
                .replace("$4", Util.decFormat.format(duration / 1000.0));
    }

    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT,
                ChatComponents.GENERIC_SKILL + "%target% is bleeding from the effects of their Combusted Blood!")
                .replace("%target%", "$1");

        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT,
                ChatComponents.GENERIC_SKILL + "%target% is no longer bleeding.")
                .replace("%target%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE, false);
        addSpellTarget(target, hero);
        damageEntity(target, player, damage, DamageCause.MAGIC);

        broadcastExecuteText(hero, target);

        // Get Blood Union Level
        int bloodUnionLevel = 0;
        if (hero.hasEffect(bloodUnionEffectName)) {
            BloodUnionEffect buEffect = (BloodUnionEffect) hero.getEffect(bloodUnionEffectName);
            bloodUnionLevel = buEffect.getBloodUnionLevel();
        }

        // Add DoT if blood union is high enough.
        int bloodUnionRequirement = SkillConfigManager.getUseSetting(hero, this, "blood-union-required-for-dot", 3, false);
        if (bloodUnionLevel >= bloodUnionRequirement) {
            double tickDamage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE_TICK, false);

            int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 2500, false);
            int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 7500, false);

            // Add DoT effect
            CombustingBloodEffect cbEffect = new CombustingBloodEffect(this, player, period, duration, tickDamage);
            CharacterTemplate targCT = plugin.getCharacterManager().getCharacter(target);
            targCT.addEffect(cbEffect);
        }

        // Increase Blood Union
        if (hero.hasEffect(bloodUnionEffectName)) {
            int bloodUnionIncrease = SkillConfigManager.getUseSetting(hero, this, "blood-union-increase", 1, false);
            BloodUnionEffect buEffect = (BloodUnionEffect) hero.getEffect(bloodUnionEffectName);

            if (target instanceof Player)
                buEffect.addBloodUnion(bloodUnionIncrease, true);
            else
                buEffect.addBloodUnion(bloodUnionIncrease, false);
        }

        //player.getWorld().spigot().playEffect(target.getLocation(), Effect.LAVA_POP, 0, 0, 0, 0, 0, 1, 75, 16);
        player.getWorld().spawnParticle(Particle.LAVA, target.getLocation(), 75, 0, 0, 0, 1);
        //player.getWorld().spigot().playEffect(target.getEyeLocation(), Effect.EXPLOSION_LARGE, 0, 0, 0, 0, 0, 0, 10, 16);
        player.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, target.getEyeLocation(), 10, 0, 0, 0, 0);
        //FIXME Explore replacement for `TILE_BREAK`
        //player.getWorld().spigot().playEffect(target.getEyeLocation(), Effect.TILE_BREAK, Material.NETHER_WARTS.getId(), 0, 0, 0.1F, 0, 0.1F, 16, 16);
        player.getWorld().spawnParticle(Particle.BLOCK_CRACK, target.getEyeLocation(), 16, 0, 0.1, 0, 0.1, Bukkit.createBlockData(Material.NETHER_WART_BLOCK));
        player.getWorld().playSound(target.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 10.0F, 16);
        player.getWorld().playSound(target.getLocation(), Sound.BLOCK_FIRE_AMBIENT, 10.0F, 16);

        return SkillResult.NORMAL;
    }

    public class CombustingBloodEffect extends PeriodicDamageEffect {
        public CombustingBloodEffect(Skill skill, Player applier, long period, long duration, double tickDamage) {
            super(skill, "CombustingBlood", applier, period, duration, tickDamage, applyText, expireText);

            types.add(EffectType.BLEED);
            types.add(EffectType.DARK);
            types.add(EffectType.HARMFUL);
        }
    }
}
