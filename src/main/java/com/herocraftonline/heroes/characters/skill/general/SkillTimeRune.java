package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.skills.SkillBaseMarkedTeleport;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;

public class SkillTimeRune extends SkillBaseMarkedTeleport {

    private static final String HEALING_PERCENTAGE_NODE = "healing-percentage";
    private static final String HEALING_PERCENTAGE_PER_WISDOM_NODE = "healing-percentage-per-wisdom";

    public SkillTimeRune(final Heroes plugin) {
        super(plugin, "TimeRune", true, new EffectType[]{
                EffectType.HEALING,
                EffectType.BENEFICIAL,
        }, Particle.REDSTONE, new Color[]{
                //TODO Color change
                Color.MAROON,
                Color.WHITE
        });
        // TODO Dscription change
        setDescription("Mark your current position in time for the next $1 second(s). At any point during that time you may re activate the skill to teleport" +
                " your self back to that location healing you for an amount starting at $3 ($2% of max health) and decaying towards 0 as the skills duration reaches end. If you do not" +
                " re activate the skill within the duration no healing is applied and no teleport occurs.");
        setUsage("/skill timerune");
        setIdentifiers("skill timerune");

        // TODO type edit
        setTypes(SkillType.HEALING, SkillType.TELEPORTING);
    }

    @Override
    public String getDescription(final Hero hero) {
        double healing = SkillConfigManager.getUseSetting(hero, this, HEALING_PERCENTAGE_NODE, 0.25d, false);
        final double healingIncrease = SkillConfigManager.getUseSetting(hero, this, HEALING_PERCENTAGE_PER_WISDOM_NODE, 0.005d, false);
        healing += hero.getAttributeValue(AttributeType.WISDOM) * healingIncrease;

        final double totalDuration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);

        return getDescription()
                .replace("$1", Util.decFormat.format(totalDuration / 1000))
                .replace("$2", Util.largeDecFormat.format(healing * 100))
                .replace("$3", Util.decFormat.format(hero.getPlayer().getMaxHealth() * healing));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DURATION.node(), 10000);
        node.set(HEALING_PERCENTAGE_NODE, 0.25d);
        node.set(HEALING_PERCENTAGE_PER_WISDOM_NODE, 0.005d);

        node.set(PRESERVE_LOOK_DIRECTION_NODE, true);
        node.set(PRESERVE_VELOCITY_NODE, true);

        return node;
    }

    @Override
    protected void onMarkerActivate(final SkillBaseMarkedTeleport.Marker marker, final long activateTime) {
        final double healing = SkillConfigManager.getScaledUseSettingDouble(marker.getHero(), this, HEALING_PERCENTAGE_NODE, 0.25d, false);

        final double maxHeal = marker.getTarget().getEntity().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() * healing;
        final long reCastDelay = SkillConfigManager.getUseSetting(marker.getHero(), this, RE_CAST_DELAY_NODE, 0, false);

        final double totalDuration = SkillConfigManager.getUseSetting(marker.getHero(), this, SkillSetting.DURATION, 10000, false);
        double healScale = ((activateTime - marker.getCreateTime() + reCastDelay) / (totalDuration - reCastDelay));
        if (healScale < 0) {
            healScale = 0;
        }

        marker.getTarget().heal(maxHeal * healScale);
    }
}
