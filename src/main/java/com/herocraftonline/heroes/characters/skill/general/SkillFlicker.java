package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.effects.common.InvisibleEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class SkillFlicker extends ActiveSkill {

    private String applyText;
    private String expireText;

    public SkillFlicker(final Heroes plugin) {
        super(plugin, "Flicker");
        setDescription("You appear to flicker in and out of sight for $1 second(s).");
        setUsage("/skill flicker");
        setArgumentRange(0, 0);
        setIdentifiers("skill flicker");
        setTypes(SkillType.ABILITY_PROPERTY_ILLUSION, SkillType.BUFFING, SkillType.SILENCEABLE);
    }

    @Override
    public String getDescription(final Hero hero) {
        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 7500, false);
        final String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription().replace("$1", formattedDuration);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DURATION.node(), 7500);
        node.set(SkillSetting.PERIOD.node(), 1500);
        node.set("invis-duration", 500);
        node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% begins to flicker!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% is no longer flickering.");

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%hero% begins to flicker!").replace("%hero%", "$1").replace("$hero$", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%hero% is no longer flickering.").replace("%hero%", "$1").replace("$hero$", "$1");
    }

    @Override
    public SkillResult use(final Hero hero, final String[] args) {
        broadcastExecuteText(hero);

        final long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 1500, false);
        final long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 7500, false);
        final long invisDuration = SkillConfigManager.getUseSetting(hero, this, "invis-duration", 500, false);
        hero.addEffect(new FlickerEffect(this, hero.getPlayer(), period, duration, invisDuration));

        return SkillResult.NORMAL;
    }

    private class FlickerEffect extends PeriodicExpirableEffect {

        final long invisDuration;

        public FlickerEffect(final Skill skill, final Player applier, final long period, final long duration, final long invisDuration) {
            super(skill, "Flicker", applier, period, duration, applyText, expireText);

            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.MAGIC);
            types.add(EffectType.DISPELLABLE);

            this.invisDuration = invisDuration;
        }

        @Override
        public void tickHero(final Hero hero) {
            final Player player = hero.getPlayer();
            final Location playerLoc = player.getLocation();
            player.getWorld().playEffect(playerLoc, Effect.ENDER_SIGNAL, 3);
            player.getWorld().playSound(playerLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8F, 1.0F);

            final InvisibleEffect customInvisEffect = new InvisibleEffect(skill, player, invisDuration, null, null);
            customInvisEffect.types.add(EffectType.UNBREAKABLE);
            hero.addEffect(customInvisEffect);
        }

        @Override
        public void tickMonster(final Monster monster) {
        }
    }
}
