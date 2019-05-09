package com.herocraftonline.heroes.characters.skill.reborn.necromancer;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.CustomNameManager;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SkillFamine extends TargettedSkill {
	
    private String applyText;
    private String expireText;

    public SkillFamine(Heroes plugin) {
        super(plugin, "Famine");
        setDescription("Cause a wave of famine to your target and all enemies within $1 blocks of that target. " +
                "Famine causes all affected targets to lose $2 stamina over $3 second(s).");
        setUsage("/skill famine");
        setIdentifiers("skill famine");
        setArgumentRange(0, 0);
        setTypes(SkillType.SILENCEABLE, SkillType.AGGRESSIVE, SkillType.ABILITY_PROPERTY_DARK, SkillType.STAMINA_DECREASING, SkillType.DEBUFFING, SkillType.AREA_OF_EFFECT);
    }

    public String getDescription(Hero hero) {
        double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 4.0, false);

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 3000, false);
        int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 1000, true);

        int staminaDrain = SkillConfigManager.getUseSetting(hero, this, "stamina-drain-per-tick", 60, false);
        double staminaIncrease = SkillConfigManager.getUseSetting(hero, this, "stamina-drain-increase-intellect", 0.0, false);
        staminaDrain += hero.getAttributeValue(AttributeType.INTELLECT) * staminaIncrease;

        return getDescription()
                .replace("$1", Util.decFormat.format(radius))
                .replace("$2", Util.decFormat.format(staminaDrain * ((double) duration / (double) period)))
                .replace("$3", Util.decFormat.format(duration / 1000.0));
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.MAX_DISTANCE.node(), 7);
        config.set("stamina-drain-per-tick", 60);
        config.set("stamina-drain-increase-intellect", 1);
        config.set(SkillSetting.RADIUS.node(), 4);
        config.set(SkillSetting.DURATION.node(), 6000);
        config.set(SkillSetting.PERIOD.node(), 1500);
        config.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target%'s has been overcome with famine!");
        config.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target%'s famine has ended.");
        return config;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%target%'s has been overcome with famine!").replace("%target%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%target%'s famine has ended.").replace("%target%", "$1");
    }

    public SkillResult use(Hero hero, LivingEntity target, String[] args) {

        if (!(target instanceof Player))
            return SkillResult.INVALID_TARGET;

        Player player = hero.getPlayer();

        broadcastExecuteText(hero, target);

        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 4, false);

        // Get Debuff values
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 6000, false);
        int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 1500, true);
        int staminaDrain = SkillConfigManager.getUseSetting(hero, this, "stamina-drain-per-tick", 60, false);
        int staminaIncrease = SkillConfigManager.getUseSetting(hero, this, "stamina-drain-increase-intellect", 1, false);
        staminaDrain += hero.getAttributeValue(AttributeType.INTELLECT) * staminaIncrease;

        // Famine the first target
        FamineEffect tbEffect = new FamineEffect(this, player, period, duration, staminaDrain);
        CharacterTemplate targCT = plugin.getCharacterManager().getCharacter(target);
        targCT.addEffect(tbEffect);

        // Famine the rest
        for (Entity entity : target.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof LivingEntity) {
                if (!damageCheck(player, (LivingEntity) entity)) {
                    continue;
                }

                CharacterTemplate newTargCT = plugin.getCharacterManager().getCharacter((LivingEntity) entity);
                newTargCT.addEffect(tbEffect);
            }
        }

        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 0.7F, 2.0F);
        target.getWorld().spigot().playEffect(target.getLocation().add(0, 0.5, 0), Effect.TILE_BREAK, Material.SLIME_BLOCK.getId(), 0, 0.3F, 0.2F, 0.3F, 0.0F, 25, 16);
//        target.getWorld().spawnParticle(Particle.BLOCK_CRACK, target.getLocation().add(0, 0.5, 0), 25, 0.3, 0.2, 0.3, 0, Bukkit.createBlockData(Material.SLIME_BLOCK));

        return SkillResult.NORMAL;
    }

    public class FamineEffect extends PeriodicExpirableEffect {
        private final int staminaDrain;

        public FamineEffect(Skill skill, Player applier, int period, int duration, int staminaDrain) {
            super(skill, "Famined", applier, period, duration, applyText, expireText);

            types.add(EffectType.MAGIC);
            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.HARMFUL);
            types.add(EffectType.HUNGER);
            types.add(EffectType.STAMINA_REGEN_FREEZING);
            types.add(EffectType.STAMINA_DECREASING);

            this.staminaDrain = staminaDrain;

            addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, duration / 1000 * 20, 0));
        }

        @Override
        public void tickHero(Hero hero) {
            hero.setStamina(hero.getStamina() - staminaDrain);
        }

        @Override
        public void tickMonster(Monster monster) {}
    }
}