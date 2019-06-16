package com.herocraftonline.heroes.characters.skill.reborn.bard;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicHealEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

//import org.bukkit.Particle; 1.13

public class SkillHealingChorus extends ActiveSkill {

    private String applyText;
    private String expireText;

    public SkillHealingChorus(Heroes plugin) {
        super(plugin, "HealingChorus");
        setDescription("You sing a chorus of healing, affecting party members within $1 blocks. " +
                "The chorus heals them for $2 health over $3 second(s). " +
                "You are only healed for $4 health from this effect.");
        setUsage("/skill healingchorus");
        setIdentifiers("skill healingchorus");
        setTypes(SkillType.AREA_OF_EFFECT, SkillType.BUFFING, SkillType.HEALING, SkillType.ABILITY_PROPERTY_SONG);
        setArgumentRange(0, 0);
    }

    public String getDescription(Hero hero) {
        int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 1500, false);
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 3000, false);

        double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 15.0, false);
        double healing = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.HEALING_TICK, false);

        return getDescription()
                .replace("$1", Util.decFormat.format(radius))
                .replace("$2", Util.decFormat.format(healing * ((double) duration / (double) period)))
                .replace("$3", Util.decFormat.format(duration / 1000.0))
                .replace("$4", Util.decFormat.format((healing * ((double) duration / (double) period)) * Heroes.properties.selfHeal));
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.DURATION.node(), 3000);
        config.set(SkillSetting.RADIUS.node(), 12.0);
        config.set(SkillSetting.PERIOD.node(), 1500);
        config.set(SkillSetting.HEALING_TICK.node(), 17.0);
        config.set(SkillSetting.HEALING_INCREASE_PER_CHARISMA.node(), 0.0);
        config.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "You are gifted with %hero%'s chorus of healing.");
        config.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero%'s chorus of healing has ended.");
        config.set(SkillSetting.DELAY.node(), 1000);
        return config;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT.node(), "");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT.node(), "");
    }

    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 15.0, false);
        double radiusSquared = radius * radius;
        double healing = SkillConfigManager.getScaledUseSettingDouble(hero, this, SkillSetting.HEALING_TICK, false);

        int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 1500, false);
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 3000, false);

        broadcastExecuteText(hero);

        // Check if the hero has a party
        if (hero.hasParty()) {
            Location playerLocation = player.getLocation();
            // Loop through the player's party members and add the effect as necessary
            for (Hero member : hero.getParty().getMembers()) {
                // Ensure the party member is in the same world.
                if (member.getPlayer().getLocation().getWorld().equals(playerLocation.getWorld())) {
                    // Check to see if they are close enough to the player to receive the buff
                    if (member.getPlayer().getLocation().distanceSquared(playerLocation) <= radiusSquared) {
                        // Add the effect
                        member.addEffect(new HealingChorusEffect(this, player, period, duration, healing));
                    }
                }
            }
        } else {
            // Add the effect to just the player
            hero.addEffect(new HealingChorusEffect(this, player, period, duration, healing));
        }

        //1.13 needs this block GONE
        player.getWorld().playEffect(player.getLocation().add(0, 2.5, 0), org.bukkit.Effect.NOTE, 3);
        player.getWorld().playEffect(player.getLocation().add(0, 2.5, 0), org.bukkit.Effect.NOTE, 3);
        player.getWorld().playEffect(player.getLocation().add(0, 2.5, 0), org.bukkit.Effect.NOTE, 3);

        return SkillResult.NORMAL;
    }

    public class HealingChorusEffect extends PeriodicHealEffect {

        public HealingChorusEffect(Skill skill, Player applier, long period, long duration, double healing) {
            super(skill, "HealingChorus", applier, period, duration, healing, applyText, expireText);

            types.add(EffectType.SILENT_ACTIONS);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);

            Player player = hero.getPlayer();
            final Player p = player;

            if (player == this.getApplier()) {
                new BukkitRunnable() {
                    private double time = 0;

                    @Override
                    public void run() {
                        Location location = p.getLocation();
                        if (time < 0.5) {
                            p.getWorld().spigot().playEffect(location, Effect.NOTE, 0, 0, 6.3F, 1.0F, 6.3F, 0.0F, 1, 16);
                            //p.getWorld().spawnParticle(Particle.NOTE, location, 1, 6.3, 1, 6.3, 1); 1.13
                        } else {
                            cancel();
                        }
                        time += 0.01;
                    }
                }.runTaskTimer(plugin, 1, 4);
            }
        }
    }
}