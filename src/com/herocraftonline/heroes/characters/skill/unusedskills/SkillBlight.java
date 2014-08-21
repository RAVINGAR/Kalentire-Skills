package com.herocraftonline.heroes.characters.skill.unusedskills;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
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
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

public class SkillBlight extends TargettedSkill {
    // This is for Firework Effects
    public VisualEffect fplayer = new VisualEffect();

    private String applyText;
    private String expireText;

    public SkillBlight(Heroes plugin) {
        super(plugin, "Blight");
        setDescription("You disease your target, dealing $1 dark damage over $2 seconds, enemies that get too close will also be damaged.");
        setUsage("/skill blight");
        setArgumentRange(0, 0);
        setTypes(SkillType.ABILITY_PROPERTY_DARK, SkillType.AREA_OF_EFFECT, SkillType.SILENCEABLE, SkillType.DAMAGING, SkillType.AGGRESSIVE);
        setIdentifiers("skill blight");
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 17500, false);
        int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 2500, false);

        double tickDamage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK, 15, false);
        double tickDamageIncrease = hero.getAttributeValue(AttributeType.INTELLECT) * SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK_INCREASE_PER_INTELLECT, 0.4, false);
        tickDamage += tickDamageIncrease;

        String formattedDamage = Util.decFormat.format(tickDamage * ((double) duration / (double) period));
        String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription().replace("$1", formattedDamage).replace("$2", formattedDuration);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), Integer.valueOf(12));
        node.set(SkillSetting.DURATION.node(), Integer.valueOf(17500));
        node.set(SkillSetting.PERIOD.node(), Integer.valueOf(2500));
        node.set(SkillSetting.DAMAGE_TICK.node(), Double.valueOf(15));
        node.set(SkillSetting.DAMAGE_TICK_INCREASE_PER_INTELLECT.node(), Double.valueOf(0.4));
        node.set(SkillSetting.RADIUS.node(), 4);
        node.set(SkillSetting.APPLY_TEXT.node(), Messaging.getSkillDenoter() + "%target% begins to radiate a cloud of disease!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), Messaging.getSkillDenoter() + "%target% is no longer diseased!");

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, Messaging.getSkillDenoter() + "%target% begins to radiate a cloud of disease!").replace("%target%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, Messaging.getSkillDenoter() + "%target% is no longer diseased!").replace("%target%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero, target);

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 17500, false);
        int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 2500, true);

        double tickDamage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK, 15, false);
        double tickDamageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK_INCREASE_PER_INTELLECT, 0.4, false);
        tickDamage += (tickDamageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
        targetCT.addEffect(new BlightEffect(this, player, duration, period, tickDamage));

        // this is our fireworks shit
        try {
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
        }

        return SkillResult.NORMAL;
    }

    public class BlightEffect extends PeriodicDamageEffect {

        public BlightEffect(Skill skill, Player applier, long duration, long period, double tickDamage) {
            super(skill, "Blight", applier, period, duration, tickDamage);

            types.add(EffectType.DISEASE);
            types.add(EffectType.HARMFUL);
            types.add(EffectType.AREA_OF_EFFECT);
            types.add(EffectType.DISPELLABLE);

            addMobEffect(19, (int) (duration / 1000) * 20, 0, true);
        }

        @Override
        public void applyToMonster(Monster monster) {
            super.applyToMonster(monster);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), "    " + applyText, player.getName(), applier.getName());
        }

        @Override
        public void removeFromMonster(Monster monster) {
            super.removeFromMonster(monster);
            broadcast(monster.getEntity().getLocation(), "    " + expireText, Messaging.getLivingEntityName(monster), applier.getName());
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), "    " + expireText, player.getName(), applier.getName());
        }

        @Override
        public void tickMonster(Monster monster) {
            super.tickMonster(monster);
            damageNearby(monster.getEntity());
        }

        @Override
        public void tickHero(Hero hero) {
            super.tickHero(hero);
            damageNearby(hero.getPlayer());
        }

        private void damageNearby(LivingEntity lEntity) {
            Hero applyHero = plugin.getCharacterManager().getHero(getApplier());
            int radius = SkillConfigManager.getUseSetting(applyHero, skill, SkillSetting.RADIUS, 4, false);
            for (Entity target : lEntity.getNearbyEntities(radius, radius, radius)) {
                if (!(target instanceof LivingEntity)) {
                    continue;
                }

                LivingEntity lTarget = (LivingEntity) target;

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
