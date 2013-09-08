package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicDamageEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

public class SkillStrike extends TargettedSkill {

    private String applyText;
    private String expireText;

    public SkillStrike(Heroes plugin) {
        super(plugin, "Strike");
        setDescription("You violently strike the target for $1 physical damage, and causing them to bleed out for $2 physical damage over $3 seconds!");
        setUsage("/skill strike");
        setArgumentRange(0, 0);
        setIdentifiers("skill strike");
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.DAMAGING, SkillType.AGGRESSIVE);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.USE_TEXT.node(), Messaging.getSkillDenoter() + "%target% is struct greivously by %hero%!");
        node.set(SkillSetting.MAX_DISTANCE.node(), Integer.valueOf(4));
        node.set(SkillSetting.DAMAGE.node(), Integer.valueOf(40));
        node.set(SkillSetting.DAMAGE_INCREASE_PER_STRENGTH.node(), Double.valueOf(1.0));
        node.set(SkillSetting.DURATION.node(), Integer.valueOf(3000));
        node.set(SkillSetting.PERIOD.node(), Integer.valueOf(1500));
        node.set(SkillSetting.DAMAGE_TICK.node(), Integer.valueOf(10));
        node.set(SkillSetting.DAMAGE_TICK_INCREASE_PER_STRENGTH.node(), Double.valueOf(0.25));
        node.set(SkillSetting.APPLY_TEXT.node(), "");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "");

        return node;
    }

    @Override
    public String getDescription(Hero hero) {
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, Integer.valueOf(30), false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, Double.valueOf(0.7), false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH);

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 17500, false);
        int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 2500, false);

        double tickDamage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK, 15, false);
        double tickDamageIncrease = hero.getAttributeValue(AttributeType.STRENGTH) * SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 0.4, false);
        tickDamage += tickDamageIncrease;

        String formattedDamage = Util.decFormat.format(damage);
        String formattedDoTDamage = Util.decFormat.format(tickDamage * ((double) duration / (double) period));
        String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription().replace("$1", formattedDamage).replace("$2", formattedDoTDamage).replace("$3", formattedDuration);
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, "%target% is bleeding from a grievous wound!").replace("%target%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "%target% has stopped bleeding!").replace("%target%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero, target);

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, Integer.valueOf(30), false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, Double.valueOf(0.7), false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH);

        // Damage the target
        addSpellTarget(target, hero);
        damageEntity(target, player, damage, DamageCause.ENTITY_ATTACK);

        // Apply our effect
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 17500, false);
        int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 2500, false);

        double tickDamage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK, 15, false);
        double tickDamageIncrease = hero.getAttributeValue(AttributeType.STRENGTH) * SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 0.4, false);
        tickDamage += tickDamageIncrease;

        plugin.getCharacterManager().getCharacter(target).addEffect(new StrikeBleedEffect(this, player, period, duration, tickDamage));
        player.getWorld().playSound(player.getLocation(), Sound.HURT_FLESH, 0.8F, 1.0F);

        return SkillResult.NORMAL;
    }

    public class StrikeBleedEffect extends PeriodicDamageEffect {

        public StrikeBleedEffect(Skill skill, Player applier, long period, long duration, double tickDamage) {
            super(skill, "StrikeBleed", applier, period, duration, tickDamage, applyText, expireText);

            types.add(EffectType.BLEED);
            types.add(EffectType.HARMFUL);
            types.add(EffectType.PHYSICAL);
        }
    }
}
