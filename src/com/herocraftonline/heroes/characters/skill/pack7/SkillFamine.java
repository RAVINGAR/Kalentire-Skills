package com.herocraftonline.heroes.characters.skill.pack7;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.util.CompatSound;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SkillFamine extends TargettedSkill {
	
    private String applyText;
    private String expireText;

    public SkillFamine(Heroes plugin) {
        super(plugin, "Famine");
        setDescription("Cause a wave of famine to your target and all enemies within $1 blocks of that target. Famine causes all affected targets to lose $2 stamina over $3 seconds.");
        setUsage("/skill famine");
        setArgumentRange(0, 0);
        setIdentifiers("skill famine");
        setTypes(SkillType.SILENCEABLE, SkillType.AGGRESSIVE, SkillType.ABILITY_PROPERTY_DARK, SkillType.STAMINA_DECREASING, SkillType.DEBUFFING, SkillType.AREA_OF_EFFECT);
    }

    public String getDescription(Hero hero) {

        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 4, false);

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 3000, false);
        int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 1500, true);

        int staminaDrain = SkillConfigManager.getUseSetting(hero, this, "stamina-drain-per-tick", 60, false);
        int staminaIncrease = SkillConfigManager.getUseSetting(hero, this, "stamina-drain-increase-intellect", 1, false);
        staminaDrain += hero.getAttributeValue(AttributeType.INTELLECT) * staminaIncrease;

        String formattedStaminaDrain = Util.decFormat.format(staminaDrain * ((double) duration / (double) period));
        String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription().replace("$1", radius + "").replace("$2", formattedStaminaDrain).replace("$3", formattedDuration);
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 7);
        node.set("stamina-drain-per-tick", 60);
        node.set("stamina-drain-increase-intellect", 1);
        node.set(SkillSetting.RADIUS.node(), 4);
        node.set(SkillSetting.DURATION.node(), 6000);
        node.set(SkillSetting.PERIOD.node(), 1500);
        node.set(SkillSetting.APPLY_TEXT.node(), Messaging.getSkillDenoter() + "%target%'s has been overcome with famine!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), Messaging.getSkillDenoter() + "%target%'s famine has ended.");

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, Messaging.getSkillDenoter() + "%target%'s has been overcome with famine!").replace("%target%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, Messaging.getSkillDenoter() + "%target%'s famine has ended.").replace("%target%", "$1");
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

        target.getWorld().playSound(target.getLocation(), CompatSound.ENTITY_BLAZE_AMBIENT.value(), 0.7F, 2.0F);
        target.getWorld().spigot().playEffect(target.getLocation().add(0, 0.5, 0), Effect.TILE_BREAK, Material.SLIME_BLOCK.getId(), 0, 0.3F, 0.2F, 0.3F, 0.0F, 25, 16);

        return SkillResult.NORMAL;
    }

    public class FamineEffect extends PeriodicExpirableEffect {
        private final int staminaDrain;

        public FamineEffect(Skill skill, Player applier, int period, int duration, int staminaDrain) {
            super(skill, "FamineEffect", applier, period, duration);

            types.add(EffectType.MAGIC);
            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.HARMFUL);
            types.add(EffectType.HUNGER);
            types.add(EffectType.STAMINA_REGEN_FREEZING);
            types.add(EffectType.STAMINA_DECREASING);

            this.staminaDrain = staminaDrain;

            addMobEffect(17, duration / 1000 * 20, 0, false);
        }

        @Override
        public void applyToMonster(Monster monster) {
            super.applyToMonster(monster);
            broadcast(monster.getEntity().getLocation(), "    " + applyText, Messaging.getLivingEntityName(monster), applier.getName());
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            final Player player = hero.getPlayer();
            broadcast(player.getLocation(), "    " + applyText, player.getName());
        }

        @Override
        public void removeFromMonster(Monster monster) {
            super.removeFromMonster(monster);
            broadcast(monster.getEntity().getLocation(), "    " + expireText, Messaging.getLivingEntityName(monster), applier.getName());
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            final Player player = hero.getPlayer();
            broadcast(player.getLocation(), "    " + expireText, player.getName());
        }

        @Override
        public void tickHero(Hero hero) {
            hero.setStamina(hero.getStamina() - staminaDrain);
        }

        @Override
        public void tickMonster(Monster monster) {}
    }
}