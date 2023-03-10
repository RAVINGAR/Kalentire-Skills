package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.CustomNameManager;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicDamageEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.characters.skill.VisualEffect;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SkillBlight extends TargettedSkill {
    // This is for Firework Effects
    public VisualEffect fplayer = new VisualEffect();

    private String applyText;
    private String expireText;

    public SkillBlight(final Heroes plugin) {
        super(plugin, "Blight");
        setDescription("You disease your target, dealing $1 dark damage over $2 seconds, enemies that get too close will also be damaged.");
        setUsage("/skill blight");
        setArgumentRange(0, 0);
        setTypes(SkillType.ABILITY_PROPERTY_DARK, SkillType.AREA_OF_EFFECT, SkillType.SILENCEABLE, SkillType.DAMAGING, SkillType.AGGRESSIVE);
        setIdentifiers("skill blight");
    }

    @Override
    public String getDescription(final Hero hero) {
        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 17500, false);
        final int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 2500, false);

        double tickDamage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK, 15, false);
        final double tickDamageIncrease = hero.getAttributeValue(AttributeType.INTELLECT) * SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK_INCREASE_PER_INTELLECT, 0.4, false);
        tickDamage += tickDamageIncrease;

        final String formattedDamage = Util.decFormat.format(tickDamage * ((double) duration / (double) period));
        final String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription().replace("$1", formattedDamage).replace("$2", formattedDuration);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 12);
        node.set(SkillSetting.DURATION.node(), 17500);
        node.set(SkillSetting.PERIOD.node(), 2500);
        node.set(SkillSetting.DAMAGE_TICK.node(), 15);
        node.set(SkillSetting.DAMAGE_TICK_INCREASE_PER_INTELLECT.node(), 0.4);
        node.set(SkillSetting.RADIUS.node(), 4);
        node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% begins to radiate a cloud of disease!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% is no longer diseased!");

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%target% begins to radiate a cloud of disease!").replace("%target%", "$1").replace("$target$", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%target% is no longer diseased!").replace("%target%", "$1").replace("$target$", "$1");
    }

    @Override
    public SkillResult use(final Hero hero, final LivingEntity target, final String[] args) {
        final Player player = hero.getPlayer();

        broadcastExecuteText(hero, target);

        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 17500, false);
        final int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 2500, true);

        double tickDamage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK, 15, false);
        final double tickDamageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK_INCREASE_PER_INTELLECT, 0.4, false);
        tickDamage += (tickDamageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        final CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
        targetCT.addEffect(new BlightEffect(this, player, duration, period, tickDamage));

        // this is our fireworks shit
        /*try {
            fplayer.playFirework(player.getWorld(),
                                 target.getLocation().add(0, 1.5, 0),
                                 FireworkEffect.builder()
                                               .flicker(false)
                                               .trail(false)
                                               .with(FireworkEffect.Type.BALL)
                                               .withColor(Color.GRAY)
                                               .withFade(Color.GREEN)
                                               .build());
        }
        catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        catch (Exception e) {
            e.printStackTrace();
        }*/

        return SkillResult.NORMAL;
    }

    public class BlightEffect extends PeriodicDamageEffect {

        public BlightEffect(final Skill skill, final Player applier, final long duration, final long period, final double tickDamage) {
            super(skill, "Blight", applier, period, duration, tickDamage);

            types.add(EffectType.DISEASE);
            types.add(EffectType.HARMFUL);
            types.add(EffectType.AREA_OF_EFFECT);
            types.add(EffectType.DISPELLABLE);

            addPotionEffect(new PotionEffect(PotionEffectType.POISON, (int) (duration / 1000) * 20, 0), true);
        }

        @Override
        public void applyToMonster(final Monster monster) {
            super.applyToMonster(monster);
        }

        @Override
        public void applyToHero(final Hero hero) {
            super.applyToHero(hero);
            final Player player = hero.getPlayer();
            broadcast(player.getLocation(), "    " + applyText, player.getName(), applier.getName());
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
            broadcast(player.getLocation(), "    " + expireText, player.getName(), applier.getName());
        }

        @Override
        public void tickMonster(final Monster monster) {
            super.tickMonster(monster);
            damageNearby(monster.getEntity());
        }

        @Override
        public void tickHero(final Hero hero) {
            super.tickHero(hero);
            damageNearby(hero.getPlayer());
        }

        private void damageNearby(final LivingEntity lEntity) {
            final Hero applyHero = plugin.getCharacterManager().getHero(getApplier());
            final int radius = SkillConfigManager.getUseSetting(applyHero, skill, SkillSetting.RADIUS, 4, false);
            for (final Entity target : lEntity.getNearbyEntities(radius, radius, radius)) {
                if (!(target instanceof LivingEntity)) {
                    continue;
                }

                final LivingEntity lTarget = (LivingEntity) target;

                if (!damageCheck(applier, lTarget)) {
                    continue;
                }

                if (plugin.getCharacterManager().getCharacter(lTarget).hasEffect("Blight")) {
                    continue;
                }

                addSpellTarget(lTarget, applyHero);
                damageEntity(lTarget, applier, tickDamage, DamageCause.MAGIC);
            }
        }
    }
}
