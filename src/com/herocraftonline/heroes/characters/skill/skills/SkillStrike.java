package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicDamageEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;
import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.hooks.NCPExemptionManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

public class SkillStrike extends TargettedSkill {

    private boolean ncpEnabled = false;
    private String applyText;
    private String expireText;

    public SkillStrike(Heroes plugin) {
        super(plugin, "Strike");
        setDescription("You violently strike the target for $1 physical damage, and causing them to bleed out for $2 physical damage over $3 seconds!");
        setUsage("/skill strike");
        setArgumentRange(0, 0);
        setIdentifiers("skill strike");
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.DAMAGING, SkillType.AGGRESSIVE);

        if (Bukkit.getServer().getPluginManager().getPlugin("NoCheatPlus") != null) {
            ncpEnabled = true;
        }

    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.USE_TEXT.node(), Messaging.getSkillDenoter() + "%target% is struct greivously by %hero%!");
        node.set(SkillSetting.MAX_DISTANCE.node(), 4);
        node.set(SkillSetting.DAMAGE.node(), 40);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_STRENGTH.node(), 1.0);
        node.set(SkillSetting.DURATION.node(), 3000);
        node.set(SkillSetting.PERIOD.node(), 1500);
        node.set(SkillSetting.DAMAGE_TICK.node(), 10);
        node.set(SkillSetting.DAMAGE_TICK_INCREASE_PER_STRENGTH.node(), 0.25);
        node.set(SkillSetting.APPLY_TEXT.node(), "");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "");

        return node;
    }

    @Override
    public String getDescription(Hero hero) {
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, Integer.valueOf(30), false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 0.7, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH);

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 17500, false);
        int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 2500, false);

        double tickDamage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK, 15, false);
        double tickDamageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 0.4, false);
        tickDamage += hero.getAttributeValue(AttributeType.STRENGTH) * tickDamageIncrease;

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

        if (ncpEnabled) {
            if (!player.isOp()) {
                NCPExemptionEffect ncpExemptEffect = new NCPExemptionEffect(this);
                hero.addEffect(ncpExemptEffect);
            }
        }

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, Integer.valueOf(30), false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 0.7, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH);

        // Damage the target
        addSpellTarget(target, hero);
        damageEntity(target, player, damage, DamageCause.ENTITY_ATTACK);

        // Apply our effect
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 17500, false);
        int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 2500, false);

        double tickDamage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK, 15, false);
        double tickDamageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 0.4, false);
        tickDamage += hero.getAttributeValue(AttributeType.STRENGTH) * tickDamageIncrease;

        plugin.getCharacterManager().getCharacter(target).addEffect(new StrikeBleedEffect(this, player, period, duration, tickDamage));
        player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.HURT_FLESH, 0.8F, 1.0F);
        player.getWorld().spigot().playEffect(target.getLocation().add(0, 0.5, 0), org.bukkit.Effect.CRIT, 0, 0, 0, 0, 0, 1, 25, 16);

        if (ncpEnabled) {
            if (!player.isOp()) {
                if (hero.hasEffect("NCPExemptionEffect_FIGHT"))
                    hero.removeEffect(hero.getEffect("NCPExemptionEffect_FIGHT"));
            }
        }
        return SkillResult.NORMAL;
    }

    public class StrikeBleedEffect extends PeriodicDamageEffect {

        public StrikeBleedEffect(Skill skill, Player applier, long period, long duration, double tickDamage) {
            super(skill, "StrikeBleed", applier, period, duration, tickDamage, applyText, expireText);

            types.add(EffectType.BLEED);
            types.add(EffectType.HARMFUL);
        }
    }

    private class NCPExemptionEffect extends Effect {

        public NCPExemptionEffect(Skill skill) {
            super(skill, "NCPExemptionEffect_FIGHT");
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            final Player player = hero.getPlayer();

            NCPExemptionManager.exemptPermanently(player, CheckType.FIGHT);
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            final Player player = hero.getPlayer();

            NCPExemptionManager.unexempt(player, CheckType.FIGHT);
        }
    }
}
