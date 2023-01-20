package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.CustomNameManager;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SkillFamine extends TargettedSkill {

    private String applyText;
    private String expireText;

    public SkillFamine(final Heroes plugin) {
        super(plugin, "Famine");
        setDescription("Cause a wave of famine to your target and all enemies within $1 blocks of that target. Famine causes all affected targets to lose $2 stamina over $3 second(s).");
        setUsage("/skill famine");
        setArgumentRange(0, 0);
        setIdentifiers("skill famine");
        setTypes(SkillType.SILENCEABLE, SkillType.AGGRESSIVE, SkillType.ABILITY_PROPERTY_DARK, SkillType.STAMINA_DECREASING, SkillType.DEBUFFING, SkillType.AREA_OF_EFFECT);
    }

    @Override
    public String getDescription(final Hero hero) {

        final int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 4, false);

        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 3000, false);
        final int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 1500, true);

        int staminaDrain = SkillConfigManager.getUseSetting(hero, this, "stamina-drain-per-tick", 60, false);
        final int staminaIncrease = SkillConfigManager.getUseSetting(hero, this, "stamina-drain-increase-intellect", 1, false);
        staminaDrain += hero.getAttributeValue(AttributeType.INTELLECT) * staminaIncrease;

        final String formattedStaminaDrain = Util.decFormat.format(staminaDrain * ((double) duration / (double) period));
        final String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription().replace("$1", radius + "").replace("$2", formattedStaminaDrain).replace("$3", formattedDuration);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 7);
        node.set("stamina-drain-per-tick", 60);
        node.set("stamina-drain-increase-intellect", 1);
        node.set(SkillSetting.RADIUS.node(), 4);
        node.set(SkillSetting.DURATION.node(), 6000);
        node.set(SkillSetting.PERIOD.node(), 1500);
        node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target%'s has been overcome with famine!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target%'s famine has ended.");

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%target%'s has been overcome with famine!").replace("%target%", "$1").replace("$target$", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%target%'s famine has ended.").replace("%target%", "$1").replace("$target$", "$1");
    }

    @Override
    public SkillResult use(final Hero hero, final LivingEntity target, final String[] args) {

        if (!(target instanceof Player)) {
            return SkillResult.INVALID_TARGET;
        }

        final Player player = hero.getPlayer();

        broadcastExecuteText(hero, target);

        final int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 4, false);

        // Get Debuff values
        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 6000, false);
        final int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 1500, true);
        int staminaDrain = SkillConfigManager.getUseSetting(hero, this, "stamina-drain-per-tick", 60, false);
        final int staminaIncrease = SkillConfigManager.getUseSetting(hero, this, "stamina-drain-increase-intellect", 1, false);
        staminaDrain += hero.getAttributeValue(AttributeType.INTELLECT) * staminaIncrease;

        // Famine the first target
        final FamineEffect tbEffect = new FamineEffect(this, player, period, duration, staminaDrain);
        final CharacterTemplate targCT = plugin.getCharacterManager().getCharacter(target);
        targCT.addEffect(tbEffect);

        // Famine the rest
        for (final Entity entity : target.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof LivingEntity) {
                if (!damageCheck(player, (LivingEntity) entity)) {
                    continue;
                }

                final CharacterTemplate newTargCT = plugin.getCharacterManager().getCharacter((LivingEntity) entity);
                newTargCT.addEffect(tbEffect);
            }
        }

        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 0.7F, 2.0F);
        //target.getWorld().spigot().playEffect(target.getLocation().add(0, 0.5, 0), Effect.TILE_BREAK, Material.SLIME_BLOCK.getId(), 0, 0.3F, 0.2F, 0.3F, 0.0F, 25, 16);
        target.getWorld().spawnParticle(Particle.BLOCK_CRACK, target.getLocation().add(0, 0.5, 0), 25, 0.3, 0.2, 0.3, 0, Bukkit.createBlockData(Material.SLIME_BLOCK));

        return SkillResult.NORMAL;
    }

    public class FamineEffect extends PeriodicExpirableEffect {
        private final int staminaDrain;

        public FamineEffect(final Skill skill, final Player applier, final int period, final int duration, final int staminaDrain) {
            super(skill, "FamineEffect", applier, period, duration);

            types.add(EffectType.MAGIC);
            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.HARMFUL);
            types.add(EffectType.HUNGER);
            types.add(EffectType.STAMINA_REGEN_FREEZING);
            types.add(EffectType.STAMINA_DECREASING);

            this.staminaDrain = staminaDrain;

            addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, duration / 1000 * 20, 0), false);
        }

        @Override
        public void applyToMonster(final Monster monster) {
            super.applyToMonster(monster);
            broadcast(monster.getEntity().getLocation(), "    " + applyText, CustomNameManager.getName(monster), applier.getName());
        }

        @Override
        public void applyToHero(final Hero hero) {
            super.applyToHero(hero);
            final Player player = hero.getPlayer();
            broadcast(player.getLocation(), "    " + applyText, player.getName());
        }

        @Override
        public void removeFromMonster(final Monster monster) {
            super.removeFromMonster(monster);
            broadcast(monster.getEntity().getLocation(), "    " + expireText, CustomNameManager.getName(monster), applier.getName());
        }

        @Override
        public void removeFromHero(final Hero hero) {
            super.removeFromHero(hero);
            final Player player = hero.getPlayer();
            broadcast(player.getLocation(), "    " + expireText, player.getName());
        }

        @Override
        public void tickHero(final Hero hero) {
            hero.setStamina(hero.getStamina() - staminaDrain);
        }

        @Override
        public void tickMonster(final Monster monster) {
        }
    }
}