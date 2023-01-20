package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.BloodUnionEffect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicDamageEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

public class SkillBoilBlood extends ActiveSkill {
    private final String bloodUnionEffectName = "BloodUnionEffect";
    private String applyText;
    private String expireText;

    public SkillBoilBlood(final Heroes plugin) {
        super(plugin, "BoilBlood");
        setDescription("Boil the blood of $6 enemies within $1 blocks, " +
                "dealing $2 instant damage, and doing an additional $3 damage over $4 second(s). " +
                "Requires $5 Blood Union to use. " +
                "Reduces Blood Union by $5.");
        setUsage("/skill boilblood");
        setIdentifiers("skill boilblood");
        setArgumentRange(0, 0);
        setTypes(SkillType.ABILITY_PROPERTY_DARK, SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.DAMAGING, SkillType.AREA_OF_EFFECT, SkillType.SILENCEABLE, SkillType.AGGRESSIVE);
    }

    @Override
    public String getDescription(final Hero hero) {
        final double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5.0, false);
        final int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 2000, false);
        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 7500, false);
        final int bloodUnionReq = SkillConfigManager.getUseSetting(hero, this, "blood-union-required-for-use", 3, false);

        final double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE, false);
        final double tickDamage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE_TICK, false);

        final int maxTargets = SkillConfigManager.getUseSetting(hero, this, "max-targets", 6, false);

        // Change description to either say "all" if there is no maximum target number specified.
        final String targetText;
        if (maxTargets > 0) {
            targetText = "up to " + maxTargets;
        } else {
            targetText = "all";
        }

        return getDescription()
                .replace("$6", targetText)
                .replace("$1", Util.decFormat.format(radius))
                .replace("$2", Util.decFormat.format(damage))
                .replace("$3", Util.decFormat.format((tickDamage * ((double) duration / (double) period))))
                .replace("$4", Util.decFormat.format(duration / 1000.0))
                .replace("$5", bloodUnionReq + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.DAMAGE.node(), 30.0);
        config.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 0.0);
        config.set(SkillSetting.RADIUS.node(), 5.0);
        config.set(SkillSetting.DAMAGE_TICK.node(), 5.0);
        config.set(SkillSetting.DAMAGE_TICK_INCREASE_PER_INTELLECT.node(), 0.0);
        config.set(SkillSetting.PERIOD.node(), 2000);
        config.set(SkillSetting.DURATION.node(), 10000);
        config.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target%'s blood begins to boil!");
        config.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target%'s blood is no longer boiling.");
        config.set("blood-union-required-for-use", 3);
        config.set("max-targets", 6);
        return config;
    }

    @Override
    public void init() {
        super.init();
        this.applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%target%'s blood begins to boil!").replace("%target%", "$1").replace("$target$", "$1");
        this.expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%target%'s blood is no longer boiling.").replace("%target%", "$1").replace("$target$", "$1");
    }

    @Override
    public SkillResult use(final Hero hero, final String[] args) {
        final Player player = hero.getPlayer();

        // Get Blood Union Level
        int bloodUnionLevel = 0;
        if (hero.hasEffect(bloodUnionEffectName)) {
            final BloodUnionEffect buEffect = (BloodUnionEffect) hero.getEffect(bloodUnionEffectName);
            bloodUnionLevel = buEffect.getBloodUnionLevel();
        }

        // Add DoT if blood union is high enough.
        final int bloodUnionReq = SkillConfigManager.getUseSetting(hero, this, "blood-union-required-for-use", 3, false);
        if (bloodUnionLevel < bloodUnionReq) {
            player.sendMessage("    " + ChatComponents.GENERIC_SKILL + "You must have at least " + bloodUnionReq + " Blood Union to use this ability!");
            return SkillResult.FAIL;
        }

        // Blood Union high enough, proceed.

        broadcastExecuteText(hero);

        final double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5.0, false);
        final double damage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE, false);
        final double tickDamage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE_TICK, false);

        final int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 2000, false);
        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 8000, false);
        final int maxTargets = SkillConfigManager.getUseSetting(hero, this, "max-targets", 0, false);

        int targetsHit = 0;
        for (final Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (maxTargets > 0 && targetsHit >= maxTargets) {
                break;
            }

            // Check to see if the entity can be damaged
            if (!(entity instanceof LivingEntity) || !damageCheck(player, (LivingEntity) entity)) {
                continue;
            }

            final LivingEntity target = (LivingEntity) entity;

            //player.getWorld().spigot().playEffect(target.getLocation(), Effect.LARGE_SMOKE, 0, 0, 0, 0, 0, 0.2F, 50, 16);
            player.getWorld().spawnParticle(Particle.SMOKE_LARGE, target.getLocation(), 50, 0, 0, 0, 0.2);
            //player.getWorld().spigot().playEffect(target.getEyeLocation(), Effect.TILE_BREAK, Material.NETHER_WARTS.getId(), 0, 0, 0.1F, 0, 0.1F, 16, 16);
            player.getWorld().spawnParticle(Particle.BLOCK_CRACK, target.getEyeLocation(), 16, 0, 0.1, 0, 0.1, Bukkit.createBlockData(Material.NETHER_WART_BLOCK));
            player.getWorld().playSound(target.getLocation(), Sound.BLOCK_LAVA_AMBIENT, 10.0F, 16);

            addSpellTarget(target, hero);
            damageEntity(target, player, damage, DamageCause.MAGIC);

            // Create and add DoT effect to target
            final CharacterTemplate targCT = plugin.getCharacterManager().getCharacter(target);
            targCT.addEffect(new BoilingBloodEffect(this, player, period, duration, tickDamage, applyText, expireText));

            // Increase counter
            targetsHit++;
        }

        // Decrease Blood Union
        final BloodUnionEffect buEffect = (BloodUnionEffect) hero.getEffect(bloodUnionEffectName);
        buEffect.decreaseBloodUnion(bloodUnionReq);

        return SkillResult.NORMAL;
    }

    public static class BoilingBloodEffect extends PeriodicDamageEffect {
        private final String applyText;
        private final String expireText;

        public BoilingBloodEffect(final Skill skill, final Player applier, final long period, final long duration, final double tickDamage, final String applyText, final String expireText) {
            super(skill, "BoilingBlood", applier, period, duration, tickDamage, false, applyText, expireText);

            types.add(EffectType.DARK);
            types.add(EffectType.HARMFUL);

            this.applyText = applyText;
            this.expireText = expireText;
        }
    }
}