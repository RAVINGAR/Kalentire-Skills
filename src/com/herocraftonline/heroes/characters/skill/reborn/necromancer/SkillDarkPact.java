package com.herocraftonline.heroes.characters.skill.reborn.necromancer;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.effects.PeriodicHealEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.GeometryUtil;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.List;

public class SkillDarkPact extends TargettedSkill {

    private String expireText;
    private String applyText;

    public SkillDarkPact(Heroes plugin) {
        super(plugin, "DarkPact");
        setDescription("You form a dark pact with your target, sapping your own life and granting it to them. " +
                "The pact lasts for $1 second(s) and transfers $2 life every $3 second(s). " +
                "You cannot cancel this pact at will--be careful.");
        setUsage("/skill darkpact <target>");
        setIdentifiers("skill darkpact");
        setArgumentRange(0, 1);
        setTypes(SkillType.BUFFING, SkillType.HEALING, SkillType.SILENCEABLE, SkillType.NAME_TARGETTING_ENABLED, SkillType.NO_SELF_TARGETTING);
    }

    public String getDescription(Hero hero) {
        int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 2000, false);
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 10000, false);

        double drainTick = SkillConfigManager.getUseSetting(hero, this, "drain-tick", 40.0, false);

        return getDescription()
                .replace("$1", Util.decFormat.format(duration / 1000.0))
                .replace("$2", Util.decFormat.format(drainTick))
                .replace("$3", Util.decFormat.format(period / 1000.0));
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.MAX_DISTANCE.node(), 12);
        config.set(SkillSetting.DURATION.node(), 12000);
        config.set(SkillSetting.PERIOD.node(), 2000);
        config.set("drain-tick", 40.0);
        config.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% has opened a dark pact with %target%!");
        config.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero%'s pact has ended with %target%.");
        return config;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT,
                ChatComponents.GENERIC_SKILL + "%hero% has opened a dark pact with %target%!")
                .replace("%hero%", "$2")
                .replace("%target%", "$1");

        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT,
                ChatComponents.GENERIC_SKILL + "%hero%'s pact has ended with %target%.")
                .replace("%hero%", "$2")
                .replace("%target%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();
        if (!hero.isAlliedTo(target)) {
            return SkillResult.INVALID_TARGET;
        }

        broadcastExecuteText(hero, target);

        int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 2000, false);
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 10000, false);
        double drainTick = SkillConfigManager.getUseSetting(hero, this, "drain-tick", 40.0, false);

        CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
        targetCT.addEffect(new DarkPactHealEffect(this, player, period, duration, drainTick));
        hero.addEffect(new DarkPactDrainEffect(this, player, period, duration, drainTick));

        List<Location> particleLocations = GeometryUtil.helix(target.getLocation(), 1, 3.0D, 2.0D, 0.05D);
        for (Location l : particleLocations) {
//        	player.getWorld().spigot().playEffect(l, org.bukkit.Effect.HAPPY_VILLAGER, 0, 0, 0, 0, 0, 0, 1, 16);
            player.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, l, 1, 0, 0, 0, 0);
        }

        //player.getWorld().spigot().playEffect(player.getLocation(), Effect.HAPPY_VILLAGER, 0, 0, 0.5F, 1.0F, 0.5F, 0.1F, 35, 16);
        player.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, player.getLocation(), 35, 0.5, 1, 0.5, 1);

        return SkillResult.NORMAL;
    }

    public class DarkPactHealEffect extends PeriodicHealEffect {

        DarkPactHealEffect(Skill skill, Player applier, long period, long duration, double tickHealth) {
            super(skill, "DarkPactHealing" + applier.getName(), applier, period, duration, tickHealth, applyText, expireText);

            types.add(EffectType.DARK);
            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.DISPELLABLE);
        }
    }

    public class DarkPactDrainEffect extends PeriodicExpirableEffect {

        private final double healthDrainTick;

        DarkPactDrainEffect(Skill skill, Player applier, long period, long duration, double healthDrainTick) {
            super(skill, "DarkPactDrain", applier, period, duration, null, null);
            this.healthDrainTick = healthDrainTick;

            types.add(EffectType.HARMFUL);
            types.add(EffectType.DARK);
            types.add(EffectType.DISPELLABLE);
        }

        @Override
        public void tickHero(Hero hero) {
            Player player = hero.getPlayer();
            double newHealth = player.getHealth() - healthDrainTick;
            if (newHealth < 1) {
                player.setHealth(0);
            } else {
                player.setHealth(newHealth);
            }
        }

        @Override
        public void tickMonster(Monster monster) { }
    }
}