package com.herocraftonline.heroes.characters.skill.reborn.enderbeast;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.SkillDamageEvent;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.effects.common.WalkSpeedDecreaseEffect;
import com.herocraftonline.heroes.characters.effects.common.WalkSpeedPercentDecreaseEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class SkillHardenScales extends ActiveSkill {

    private static String effectName = "HardenedScales";

    private String applyText;
    private String expireText;

    public SkillHardenScales(Heroes plugin) {
        super(plugin, "HardenScales");
        setDescription("Harden your scales, increasing your weight and defense for the next $1 second(s). " +
                "While active, you reduce all incoming damage by $2%.");
        setArgumentRange(0, 0);
        setUsage("/skill hardenscales");
        setIdentifiers("skill hardenscales");
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.BUFFING);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillHeroListener(), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 4000, false);
        double damageReduction = SkillConfigManager.getUseSetting(hero, this, "damage-reduction-percent", 0.35, false);

        return getDescription()
                .replace("$1", Util.decFormat.format((double) duration / 1000.0))
                .replace("$2", Util.decFormat.format(damageReduction * 100));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set("damage-reduction-percent", 0.5);
        config.set("movespeed-reduction-percent", 0.35);
        config.set(SkillSetting.DURATION.node(), 6000);
        config.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% has hardened their scales!");
        config.set(SkillSetting.USE_TEXT.node(), "");
        config.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero%'s skin returns to normal.");
        return config;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT,
                ChatComponents.GENERIC_SKILL + "%hero% has hardened their scales!")
                .replace("%hero%", "$1");

        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT,
                ChatComponents.GENERIC_SKILL + "%hero%'s skin returns to normal.")
                .replace("%hero%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 4000, false);
        double damageReduction = SkillConfigManager.getUseSetting(hero, this, "damage-reduction-percent", 0.5, false);
        double movementPercentDecrease = SkillConfigManager.getUseSetting(hero, this, "movespeed-reduction-percent", 0.35, false);

        hero.addEffect(new HardenScalesEffect(this, player, duration, movementPercentDecrease, damageReduction));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 0.8F, 1.0F);  //TODO: Better sound

        broadcastExecuteText(hero);

        return SkillResult.NORMAL;
    }

    public class SkillHeroListener implements Listener {

        @EventHandler
        public void onSkillDamage(SkillDamageEvent event) {
            if (!(event.getEntity() instanceof Player))
                return;

            Hero defenderHero = plugin.getCharacterManager().getHero((Player) event.getEntity());
            Player defenderPlayer = defenderHero.getPlayer();
            if (!defenderHero.hasEffect(effectName))
                return;

            HardenScalesEffect effect = (HardenScalesEffect) defenderHero.getEffect(effectName);
            if (effect == null)
                return;

            double damageReduction = 1.0 - effect.damageReduction;
            event.setDamage(event.getDamage() * damageReduction);
        }

        @EventHandler
        public void onWeaponDamage(WeaponDamageEvent event) {
            if (!(event.getEntity() instanceof Player))
                return;

            Hero defenderHero = plugin.getCharacterManager().getHero((Player) event.getEntity());
            Player defenderPlayer = defenderHero.getPlayer();
            if (!defenderHero.hasEffect(effectName))
                return;

            HardenScalesEffect effect = (HardenScalesEffect) defenderHero.getEffect(effectName);
            if (effect == null)
                return;

            double damageReduction = 1.0 - effect.damageReduction;
            event.setDamage(event.getDamage() * damageReduction);
        }
    }

    public class HardenScalesEffect extends WalkSpeedPercentDecreaseEffect {

        private final double damageReduction;

        HardenScalesEffect(Skill skill, Player applier, long duration, double speedReductionPercent, double damageReduction) {
            super(skill, effectName, applier, duration, speedReductionPercent, applyText, expireText);

            types.add(EffectType.PHYSICAL);
            types.add(EffectType.BENEFICIAL);

            this.damageReduction = damageReduction;
        }

        public double getDamageReduction() {
            return damageReduction;
        }
    }
}
