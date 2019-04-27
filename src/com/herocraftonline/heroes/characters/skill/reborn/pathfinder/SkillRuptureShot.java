package com.herocraftonline.heroes.characters.skill.reborn.pathfinder;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicDamageEffect;
import com.herocraftonline.heroes.characters.effects.common.ImbueEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class SkillRuptureShot extends ActiveSkill {

    private static String toggleableEffectName = "RuptureShotArrow";

    private String applyText;
    private String expireText;

    public SkillRuptureShot(Heroes plugin) {
        super(plugin, "RuptureShot");
        setDescription("You concentrate on rupturing your target with your arrows for the next $1 seconds. " +
                "While active, your next successful shot will rupture the target, " +
                "draining $2 stamina, $3 mana, and dealing $4 damage every $5 second(s) over the next $6 second(s).");
        setUsage("/skill ruptureshot");
        setIdentifiers("skill ruptureshot", "skill ruptureshot");
        setArgumentRange(0, 0);
        setTypes(SkillType.BUFFING, SkillType.ABILITY_PROPERTY_PROJECTILE);
        setToggleableEffectName(toggleableEffectName);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillDamageListener(this), plugin);
    }

    public String getDescription(Hero hero) {
//        long duration = SkillConfigManager.getUseSetting(hero, this, "rupture-duration", 6000, false);
//        long period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 1000, true);
//        int tickDamage = SkillConfigManager.getUseSetting(hero, this, "tick-damage", 15, false);
//        int staminaDrain = SkillConfigManager.getUseSetting(hero, this, "stamina-drain-per-tick", 35, false);
//        int manaDrain = SkillConfigManager.getUseSetting(hero, this, "mana-drain-per-tick", 25, false);

        return getDescription();
//                .replace("$1", Util.decFormat.format())
//                .replace("$2", Util.decFormat.format())
//                .replace("$3", Util.decFormat.format())
//                .replace("$4", Util.decFormat.format())
//                .replace("$1", Util.decFormat.format());
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.DURATION.node(), 10000);
        config.set(SkillSetting.PERIOD.node(), 1000);
        config.set(SkillSetting.DAMAGE_TICK.node(), 15);
        config.set("rupture-duration", 4000);
        config.set("mana-drain-per-tick", 25);
        config.set("stamina-drain-per-tick", 35);
        config.set(SkillSetting.APPLY_TEXT.node(), "%target% is ruptured!");
        config.set(SkillSetting.EXPIRE_TEXT.node(), "%target% has recovered from the rupture!");
        return config;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, "%target% is ruptured!")
                .replace("%target%", "$1");

        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "%target% has recovered from the rupture!")
                .replace("%target%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        broadcastExecuteText(hero);

        hero.addEffect(new RuptureShotBuff(this));

        return SkillResult.NORMAL;
    }

    public class ArrowRuptureEffect extends PeriodicDamageEffect {
        private int staminaDrain;
        private int manaDrain;

        ArrowRuptureEffect(Skill skill, Player applier, long period, long duration, double tickDamage, int sDrain, int mDrain) {
            super(skill, "ArrowRuptured", applier, period, duration, tickDamage, applyText, expireText);
            this.staminaDrain = sDrain;
            this.manaDrain = mDrain;

            types.add(EffectType.BLEED);
            types.add(EffectType.STAMINA_DECREASING);
            types.add(EffectType.MANA_DECREASING);
        }

        @Override
        public void tickHero(Hero hero) {
            super.tickHero(hero);

            int currentStamina = hero.getStamina();
            if (currentStamina > staminaDrain) {
                hero.setStamina(currentStamina - staminaDrain);
            } else {
                hero.setStamina(0);
            }

            int currentMana = hero.getMana();
            if (currentMana > manaDrain) {
                hero.setMana(currentMana - manaDrain);
            } else {
                hero.setStamina(0);
            }
        }
    }

    public class RuptureShotBuff extends ImbueEffect {
        RuptureShotBuff(Skill skill) {
            super(skill, toggleableEffectName);
            types.add(EffectType.BENEFICIAL);
        }
    }

    public class SkillDamageListener implements Listener {
        private final Skill skill;

        public SkillDamageListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
        public void onEntityDamage(EntityDamageByEntityEvent event) {
            if (!(event.getDamager() instanceof Arrow) || !(event.getEntity() instanceof LivingEntity))
                return;

            Arrow arrow = (Arrow) event.getDamager();
            if (!(arrow.getShooter() instanceof Player))
                return;

            Player player = (Player) arrow.getShooter();
            Hero hero = plugin.getCharacterManager().getHero(player);
            if (!hero.hasEffect(toggleableEffectName))
                return;

            long duration = SkillConfigManager.getUseSetting(hero, skill, "rupture-duration", 6000, false);
            long period = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.PERIOD, 1000, true);
            int tickDamage = SkillConfigManager.getUseSetting(hero, skill, "tick-damage", 15, false);
            int staminaDrain = SkillConfigManager.getUseSetting(hero, skill, "stamina-drain-per-tick", 35, false);
            int manaDrain = SkillConfigManager.getUseSetting(hero, skill, "mana-drain-per-tick", 25, false);

            LivingEntity target = (LivingEntity) event.getEntity();
            CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
            targetCT.addEffect(new ArrowRuptureEffect(skill, player, period, duration, tickDamage, staminaDrain, manaDrain));
            hero.removeEffect(hero.getEffect(toggleableEffectName));
        }
    }
}
