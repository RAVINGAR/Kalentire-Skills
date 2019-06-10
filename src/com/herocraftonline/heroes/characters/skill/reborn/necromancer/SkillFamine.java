package com.herocraftonline.heroes.characters.skill.reborn.necromancer;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.effects.common.StaminaRegenPercentDecreaseEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SkillFamine extends TargettedSkill {
    private static String degenPartEffectName = "Famined-Degen-Slow-Part";

    private String applyText;
    private String expireText;

    public SkillFamine(Heroes plugin) {
        super(plugin, "Famine");
        setDescription("Cause a wave of famine to your target and all enemies within $1 blocks of that target. " +
                "Famine causes stamina regeneration to be reduced by $2%, and they will also lose $3 stamina over the next $4 second(s). " +
                "Monsters will be slowed instead.");
        setUsage("/skill famine");
        setIdentifiers("skill famine");
        setArgumentRange(0, 0);
        setTypes(SkillType.SILENCEABLE, SkillType.AGGRESSIVE, SkillType.ABILITY_PROPERTY_DARK, SkillType.STAMINA_DECREASING, SkillType.DEBUFFING, SkillType.AREA_OF_EFFECT);
    }

    public String getDescription(Hero hero) {
        double radius = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.RADIUS, false);
        int duration = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.DURATION, false);
        int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 1000, true);

        double degenPercent = SkillConfigManager.getUseSetting(hero, this, "stamina-degen-percent", 0.5, false);
        int staminaDrain = SkillConfigManager.getUseSetting(hero, this, "stamina-drain-per-tick", 60, false);
        double drainIncrease = SkillConfigManager.getUseSetting(hero, this, "stamina-drain-increase-intellect", 0.0, false);

        return getDescription()
                .replace("$1", Util.decFormat.format(radius))
                .replace("$2", Util.decFormat.format(degenPercent * 100))
                .replace("$3", Util.decFormat.format(staminaDrain * ((double) duration / (double) period)))
                .replace("$4", Util.decFormat.format(duration / 1000.0));
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.MAX_DISTANCE.node(), 10);
        config.set("stamina-degen-percent", 0.5);
        config.set("stamina-drain-per-tick", 25);
        config.set("stamina-drain-increase-intellect", 0.0);
        config.set(SkillSetting.RADIUS.node(), 6.0);
        config.set(SkillSetting.DURATION.node(), 5000);
        config.set(SkillSetting.PERIOD.node(), 1000);
        config.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target%'s has been overcome with famine!");
        config.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target%'s famine has ended.");
        return config;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT,
                ChatComponents.GENERIC_SKILL + "%target%'s has been overcome with famine!")
                .replace("%target%", "$1");

        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT,
                ChatComponents.GENERIC_SKILL + "%target%'s famine has ended.")
                .replace("%target%", "$1");
    }

    public SkillResult use(Hero hero, LivingEntity target, String[] args) {

        Player player = hero.getPlayer();

        broadcastExecuteText(hero, target);

        double radius = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.RADIUS, false);

        // Get Debuff values
        int duration = SkillConfigManager.getScaledUseSettingInt(hero, this, SkillSetting.DURATION, false);
        int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 1000, true);

        double degenPercent = SkillConfigManager.getUseSetting(hero, this, "stamina-degen-percent", 0.5, false);

        int staminaDrain = SkillConfigManager.getUseSetting(hero, this, "stamina-drain-per-tick", 60, false);
        double drainIncrease = SkillConfigManager.getUseSetting(hero, this, "stamina-drain-increase-intellect", 0.0, false);

        // Famine the first target
        FamineEffect effect = new FamineEffect(this, player, period, duration, degenPercent, staminaDrain);
        CharacterTemplate targCT = plugin.getCharacterManager().getCharacter(target);
        targCT.addEffect(effect);

        // Famine the rest
        for (Entity entity : target.getNearbyEntities(radius, radius / 2.0, radius)) {
            if (entity instanceof LivingEntity) {
                if (!damageCheck(player, (LivingEntity) entity)) {
                    continue;
                }

                CharacterTemplate newTargCT = plugin.getCharacterManager().getCharacter((LivingEntity) entity);
                newTargCT.addEffect(effect);
            }
        }

        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 0.7F, 2.0F);
        target.getWorld().spigot().playEffect(target.getLocation().add(0, 0.5, 0), Effect.TILE_BREAK, Material.SLIME_BLOCK.getId(), 0, 0.3F, 0.2F, 0.3F, 0.0F, 25, 16);
//        target.getWorld().spawnParticle(Particle.BLOCK_CRACK, target.getLocation().add(0, 0.5, 0), 25, 0.3, 0.2, 0.3, 0, Bukkit.createBlockData(Material.SLIME_BLOCK));

        return SkillResult.NORMAL;
    }

    class FamineEffect extends PeriodicExpirableEffect {
        private final int staminaDrain;
        private final double degenPercent;

        public FamineEffect(Skill skill, Player applier, int period, int duration, double degenPercent, int staminaDrain) {
            super(skill, "Famined", applier, period, duration, applyText, expireText);

            types.add(EffectType.MAGIC);
            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.HARMFUL);
            types.add(EffectType.HUNGER);
            types.add(EffectType.STAMINA_REGEN_FREEZING);
            types.add(EffectType.STAMINA_DECREASING);

            this.staminaDrain = staminaDrain;
            this.degenPercent = degenPercent;

            addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, duration / 1000 * 20, 0));
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            hero.addEffect(new FamineDegenPartEffect(skill, getApplier(), getDuration(), this.degenPercent));
        }

        @Override
        public void applyToMonster(Monster monster) {
            super.applyToMonster(monster);
            monster.addEffect(new FamineDegenPartEffect(skill, getApplier(), getDuration(), this.degenPercent));
        }

        @Override
        public void tickHero(Hero hero) {
            hero.setStamina(hero.getStamina() - staminaDrain);
        }

        @Override
        public void tickMonster(Monster monster) {}

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            if (hero.hasEffect(degenPartEffectName))
                hero.removeEffect(hero.getEffect(degenPartEffectName));
        }

        @Override
        public void removeFromMonster(Monster monster) {
            super.removeFromMonster(monster);
            if (monster.hasEffect(degenPartEffectName))
                monster.removeEffect(monster.getEffect(degenPartEffectName));
        }
    }

    class FamineDegenPartEffect extends StaminaRegenPercentDecreaseEffect {

        FamineDegenPartEffect(Skill skill, Player applier, long duration, double degenPercent) {
            super(skill, degenPartEffectName, applier, duration, degenPercent, null, null);
        }

        @Override
        public void applyToMonster(Monster monster) {
            addPotionEffect(new PotionEffect(PotionEffectType.SLOW, (int) (getDuration() / 50), 1));

            super.applyToMonster(monster);
        }
    }
}