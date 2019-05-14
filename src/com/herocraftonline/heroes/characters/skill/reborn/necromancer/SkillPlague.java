package com.herocraftonline.heroes.characters.skill.reborn.necromancer;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicDamageEffect;
import com.herocraftonline.heroes.characters.effects.common.interfaces.HealthRegainReduction;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SkillPlague extends TargettedSkill {
    private static String effectName = "Plagued";

    private String applyText;
    private String expireText;

    public SkillPlague(Heroes plugin) {
        super(plugin, "Plague");
        setDescription("You cast a plague upon your target for $1 seconds. " +
                "While afflicted, the target will have their healing reduced by $2%. " +
                "They will also be dealt $3 damage every $4 second(s), at which time they will spread the plague to all other enemies within $5 blocks.");
        setUsage("/skill plague");
        setIdentifiers("skill plague");
        setArgumentRange(0, 0);
        setTypes(SkillType.ABILITY_PROPERTY_DISEASE, SkillType.DAMAGING, SkillType.SILENCEABLE, SkillType.AGGRESSIVE, SkillType.AREA_OF_EFFECT);
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.DURATION, false);
        int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 2500, false);

        double radius = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.RADIUS, false);
        double healingReduction = SkillConfigManager.getUseSetting(hero, this, "healing-reduction-percent", 0.15, false);

        double tickDamage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE_TICK, false);

        return getDescription()
                .replace("$1", Util.decFormat.format(duration / 1000.0))
                .replace("$2", Util.decFormat.format(healingReduction * 100))
                .replace("$3", Util.decFormat.format(tickDamage))
                .replace("$4", Util.decFormat.format(period / 1000.0))
                .replace("$5", Util.decFormat.format(radius));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.MAX_DISTANCE.node(), 12);
        config.set(SkillSetting.DURATION.node(), 15000);
        config.set(SkillSetting.PERIOD.node(), 1500);
        config.set(SkillSetting.DAMAGE_TICK.node(), 10.0);
        config.set(SkillSetting.DAMAGE_TICK_INCREASE_PER_INTELLECT.node(), 0.0);
        config.set(SkillSetting.RADIUS.node(), 4.0);
        config.set("healing-reduction-percent", 0.15);
        config.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% is infected with the plague!");
        config.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% is no longer infected with the plague!");
        return config;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT,
                ChatComponents.GENERIC_SKILL + "%target% is infected with the plague!")
                .replace("%target%", "$1");

        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT,
                ChatComponents.GENERIC_SKILL + "%target% is no longer infected with the plague!")
                .replace("%target%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero, target);

        long duration = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.DURATION, false);
        long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 1500, true);
        double radius = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.RADIUS, false);
        double healingReduction = SkillConfigManager.getUseSetting(hero, this, "healing-reduction-percent", 0.15, false);

        double tickDamage = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.DAMAGE_TICK, false);

        plugin.getCharacterManager().getCharacter(target).addEffect(new PlagueEffect(this, player, duration, period, radius, tickDamage, healingReduction));

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BAT_HURT, 0.8F, 1.0F);

        return SkillResult.NORMAL;
    }

    class PlagueEffect extends PeriodicDamageEffect implements HealthRegainReduction {
        private final double radius;
        private double healingReduction;

        private boolean jumped = false;

        PlagueEffect(Skill skill, Player applier, long duration, long period, double radius, double tickDamage, double healingReduction) {
            super(skill, effectName, applier, period, duration, tickDamage, applyText, expireText);
            this.radius = radius;
            this.healingReduction = healingReduction;

            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.DISEASE);
            types.add(EffectType.HARMFUL);

            addPotionEffect(new PotionEffect(PotionEffectType.POISON, (int) duration / 50, 0));
        }

        // Clone Constructor
        private PlagueEffect(PlagueEffect pEffect) {
            super(pEffect.getSkill(), pEffect.getName(), pEffect.getApplier(), pEffect.getPeriod(),
                    pEffect.getRemainingTime(), pEffect.tickDamage, applyText, expireText);

            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.DISEASE);
            types.add(EffectType.HARMFUL);

            this.jumped = true;
            this.radius = pEffect.radius;
            this.healingReduction = pEffect.healingReduction;
            addPotionEffect(new PotionEffect(PotionEffectType.POISON, (int) pEffect.getRemainingTime() / 50, 0));
        }

        @Override
        public void tickMonster(Monster monster) {
            super.tickMonster(monster);
            spreadToNearbyEntities(monster.getEntity());
        }

        @Override
        public void tickHero(Hero hero) {
            super.tickHero(hero);
            spreadToNearbyEntities(hero.getPlayer());
        }

        /**
         * Attempts to spread the effect to all nearby entities
         * Will not target non-pvpable targets
         *
         * @param lEntity
         */
        private void spreadToNearbyEntities(LivingEntity lEntity) {
            if (jumped)
                return;

            Hero applyHero = plugin.getCharacterManager().getHero(getApplier());
            for (Entity target : lEntity.getNearbyEntities(radius, radius, radius)) {
                if (!(target instanceof LivingEntity))
                    continue;

                if (!damageCheck(getApplier(), (LivingEntity) target))
                    continue;

                CharacterTemplate character = plugin.getCharacterManager().getCharacter((LivingEntity) target);
                if (!character.hasEffect(effectName)) {
                    character.addEffect(new PlagueEffect(this));
                }
            }
        }

        @Override
        public Double getDelta() {
            return healingReduction;
        }

        @Override
        public void setDelta(Double newReduction) {
            this.healingReduction = newReduction;
        }
    }
}
