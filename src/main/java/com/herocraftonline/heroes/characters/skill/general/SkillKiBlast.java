package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.skills.SkillBaseSphere;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

public class SkillKiBlast extends SkillBaseSphere {

    public SkillKiBlast(final Heroes plugin) {
        super(plugin, "KiBlast");
        setDescription("Call upon your ki to damage and knock back enemies within $1 blocks for $2 every $3 second(s) for $4 second(s).");
        setUsage("/skill kiblast");
        setIdentifiers("skill kiblast");
        setArgumentRange(0, 0);
        setTypes(SkillType.MULTI_GRESSIVE, SkillType.AREA_OF_EFFECT, SkillType.DAMAGING, SkillType.FORCE, SkillType.NO_SELF_TARGETTING, SkillType.SILENCEABLE);
    }

    @Override
    public String getDescription(final Hero hero) {
        final double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5d, false);
        final long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 6000, false);
        final long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 1000, false);

        final double damageTick = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK, 100d, false)
                + SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK_INCREASE_PER_INTELLECT, 2d, false) * hero.getAttributeValue(AttributeType.INTELLECT);

        return getDescription()
                .replace("$1", Util.decFormat.format(radius))
                .replace("$2", Util.decFormat.format(damageTick))
                .replace("$3", Util.decFormat.format((double) period / 1000))
                .replace("$4", Util.decFormat.format((double) duration / 1000));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.RADIUS.node(), 5d);
        node.set(SkillSetting.DURATION.node(), 6000);
        node.set(SkillSetting.PERIOD.node(), 1000);
        node.set(SkillSetting.DAMAGE_TICK.node(), 100d);
        node.set(SkillSetting.DAMAGE_TICK_INCREASE_PER_INTELLECT.node(), 1d);

        return node;
    }

    @Override
    public SkillResult use(final Hero hero, final String[] strings) {
        if (isAreaSphereApplied(hero)) {
            return SkillResult.INVALID_TARGET_NO_MSG;
        } else {
            broadcastExecuteText(hero);

            final double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5d, false);
            final long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 6000, false);
            final long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 1000, false);

            final double damageTick = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK, 100d, false)
                    + SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK_INCREASE_PER_INTELLECT, 2d, false) * hero.getAttributeValue(AttributeType.INTELLECT);

            applyAreaSphereEffect(hero, period, duration, radius, new SphereActions() {

                @Override
                public void sphereTickAction(final Hero hero, final AreaSphereEffect effect) {
                    renderSphere(hero.getPlayer().getEyeLocation(), radius, Particle.SPELL_MOB, Color.BLACK);
                    hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.ENTITY_ENDERMAN_DEATH, 0.5f, 0.000001f);
                }

                @Override
                public void sphereTargetAction(final Hero hero, final Entity target) {
                    final Player player = hero.getPlayer();
                    if (target instanceof LivingEntity && !target.equals(player)) {
                        addSpellTarget(target, hero);
                        damageEntity((LivingEntity) target, player, damageTick, EntityDamageEvent.DamageCause.MAGIC, 0.5f);
                    }
                }
            }, EffectType.DISPELLABLE);

            return SkillResult.NORMAL;
        }
    }
}
