package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

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
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

public class SkillFlicker extends ActiveSkill {

    private String applyText;
    private String expireText;

    public SkillFlicker(Heroes plugin) {
        super(plugin, "Flicker");
        setDescription("You appear to flicker in and out of sight for $1 seconds.");
        setUsage("/skill flicker");
        setArgumentRange(0, 0);
        setIdentifiers("skill flicker");
        setTypes(SkillType.ABILITY_PROPERTY_ILLUSION, SkillType.BUFFING, SkillType.SILENCABLE);
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 7500, false);
        String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription().replace("$1", formattedDuration);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DURATION.node(), 7500);
        node.set(SkillSetting.PERIOD.node(), 1500);
        node.set("invis-duration", 500);
        node.set(SkillSetting.APPLY_TEXT.node(), Messaging.getSkillDeonoter() + "%hero% begins to flicker!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), Messaging.getSkillDeonoter() + "%hero% is no longer flickering.");

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, Messaging.getSkillDeonoter() + "%hero% begins to flicker!").replace("%hero%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, Messaging.getSkillDeonoter() + "%hero% is no longer flickering.").replace("%hero%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        broadcastExecuteText(hero);

        long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 1500, false);
        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 7500, false);
        long invisDuration = SkillConfigManager.getUseSetting(hero, this, "invis-duration", 500, false);
        hero.addEffect(new FlickerEffect(this, period, duration, invisDuration));

        return SkillResult.NORMAL;
    }
    
    private class FlickerEffect extends PeriodicExpirableEffect {

        long invisDuration;
        
        public FlickerEffect(Skill skill, long period, long duration, long invisDuration) {
            super(skill, "Flicker", period, duration);

            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.MAGIC);
            types.add(EffectType.DISPELLABLE);
            
            this.invisDuration = invisDuration;
        }
        
        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);

            Player player = hero.getPlayer();

            broadcast(player.getLocation(), applyText, player.getDisplayName());
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);

            Player player = hero.getPlayer();
            broadcast(player.getLocation(), expireText, player.getDisplayName());
        }

        @Override
        public void tickHero(Hero hero) {
            Player player = hero.getPlayer();
            Location playerLoc = player.getLocation();
            player.getWorld().playEffect(playerLoc, Effect.ENDER_SIGNAL, 3);
            player.getWorld().playSound(playerLoc, Sound.ENDERMAN_TELEPORT, 0.8F, 1.0F);

            InvisibleEffect customInvisEffect = new InvisibleEffect(skill, invisDuration, null, null);
            customInvisEffect.types.add(EffectType.UNBREAKABLE);
            hero.addEffect(customInvisEffect);
        }

        @Override
        public void tickMonster(Monster monster) {}
    }
}
