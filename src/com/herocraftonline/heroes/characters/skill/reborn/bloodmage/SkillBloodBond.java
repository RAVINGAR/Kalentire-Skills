package com.herocraftonline.heroes.characters.skill.reborn.bloodmage;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.SkillDamageEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.GeometryUtil;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.util.List;

public class SkillBloodBond extends ActiveSkill {
    private final String effectName = "BloodBond";
    //    private static final Particle.DustOptions skillEffectDustOptions = new Particle.DustOptions(Color.RED, 1);
    private String applyText;
    private String expireText;

    public SkillBloodBond(Heroes plugin) {
        super(plugin, "BloodBond");
        setDescription("Form a Blood Bond with your party. " +
                "While bound, you convert $1% of your magic damage into health for you and all party members within a $2 block radius. " +
                "Costs $4 health to use, and $3 mana per second to maintain the effect.");
        setUsage("/skill bloodbond");
        setIdentifiers("skill bloodbond");
        setArgumentRange(0, 0);
        setTypes(SkillType.BUFFING, SkillType.SILENCEABLE, SkillType.AREA_OF_EFFECT, SkillType.HEALING, SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.ABILITY_PROPERTY_DARK);

        setToggleableEffectName(effectName);
        Bukkit.getServer().getPluginManager().registerEvents(new BloodBondListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        double healPercent = SkillConfigManager.getUseSetting(hero, this, "heal-percent", 0.25, false);
        int manaTick = SkillConfigManager.getUseSetting(hero, this, "mana-tick", 40, false);
        double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS.node(), 12.0, false);
        double healthCost = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALTH_COST.node(), 25.0, false);

        return getDescription()
                .replace("$1", Util.decFormat.format((healPercent * 100)))
                .replace("$2", Util.decFormat.format(radius))
                .replace("$3", manaTick + "")
                .replace("$4", Util.decFormat.format(healthCost));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set("heal-percent", 0.15);
        config.set(SkillSetting.RADIUS.node(), 8.0);
        config.set("mana-tick", 22);
        config.set("mana-tick-period", 1000);
        config.set("toggle-on-text", ChatComponents.GENERIC_SKILL + "%hero% has formed a " + ChatColor.BOLD + "Blood Bond" + ChatColor.RESET + "!");
        config.set("toggle-off-text", ChatComponents.GENERIC_SKILL + "%hero% has broken his " + ChatColor.BOLD + "Blood Bond" + ChatColor.RESET + "!");
        return config;
    }

    @Override
    public void init() {
        super.init();

        this.applyText = SkillConfigManager.getRaw(this,
                "toggle-on-text", ChatComponents.GENERIC_SKILL + "%hero% has formed a " + ChatColor.BOLD + "Blood Bond" + ChatColor.RESET + "!")
                .replace("%hero%", "$1");

        this.expireText = SkillConfigManager.getRaw(this,
                "toggle-off-text", ChatComponents.GENERIC_SKILL + "%hero% has broken his " + ChatColor.BOLD + "Blood Bond" + ChatColor.RESET + "!")
                .replace("%hero%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        // Get config values for the effect
        int manaTick = SkillConfigManager.getUseSetting(hero, this, "mana-tick", 13, false);
        int manaTickPeriod = SkillConfigManager.getUseSetting(hero, this, "mana-tick-period", 1000, false);

        hero.addEffect(new BloodBondEffect(this, player, manaTick, manaTickPeriod));

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5F, 1.0F);

        List<Location> circle = GeometryUtil.circle(player.getLocation(), 36, 1.5);
        for (int i = 0; i < circle.size(); i++) {
//            hero.getPlayer().getWorld().spawnParticle(Particle.REDSTONE, circle.get(i), 4, 0.2F, 1.5F, 0.2F, 0, skillEffectDustOptions);
            player.getWorld().spigot().playEffect(circle.get(i), org.bukkit.Effect.COLOURED_DUST, 0, 0, 0.2F, 1.5F, 0.2F, 0, 4, 16);
        }
        return SkillResult.NORMAL;
    }

    public class BloodBondEffect extends PeriodicEffect {
        private final int manaTick;
        private boolean firstTime = true;

        BloodBondEffect(SkillBloodBond skill, Player applier, int manaTick, int period) {
            super(skill, effectName, applier, period, applyText, expireText);

            this.manaTick = manaTick;

            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.MANA_DECREASING);
            types.add(EffectType.AREA_OF_EFFECT);
            types.add(EffectType.HEALING);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            firstTime = true;
        }

        @Override
        public void tickHero(Hero hero) {
            super.tickHero(hero);

            if (firstTime) {        // Don't drain mana on first tick
                firstTime = false;
            } else {
                // Remove the effect if they don't have enough mana
                if (hero.getMana() < manaTick) {
                    hero.removeEffect(this);
                } else {
                    hero.setMana(hero.getMana() - manaTick);
                }
            }
        }
    }

    public class BloodBondListener implements Listener {
        private final Skill skill;

        BloodBondListener(Skill skill) {
            this.skill = skill;
        }

        // Why was this here? Is it actually needed? We need to test.
//        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
//        public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
//            if (!event.getCause().equals(DamageCause.MAGIC) || !(event.getDamager() instanceof Player))
//                return;
//
//            // Make sure the hero has the bloodbond effect
//            Hero hero = plugin.getCharacterManager().getHero((Player) event.getDamager());
//            if (hero.hasEffect(effectName)) {
//                healHeroParty(hero, event.getDamage());
//            }
//        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onSkillDamage(SkillDamageEvent event) {
            if ((!(event.getDamager() instanceof Player)))
                return;

            // Make sure the hero has the bloodbond effect
            Hero hero = plugin.getCharacterManager().getHero((Player) event.getDamager());
            if (hero.hasEffect(effectName)) {
                healHeroParty(hero, event.getDamage());
            }
        }

        // Heals the hero and his party based on the specified damage
        private void healHeroParty(Hero hero, double d) {
            // Set the healing amount
            double healPercent = SkillConfigManager.getUseSetting(hero, skill, "heal-percent", 0.15, false);
            double healAmount = healPercent * d;

            // Set the distance variables
            double radius = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.RADIUS, 12.0, false);
            double radiusSquared = radius * radius;

            List<Location> circle = GeometryUtil.circle(hero.getPlayer().getLocation(), 36, 1.5);
            for (int i = 0; i < circle.size(); i++) {
                hero.getPlayer().getWorld().spigot().playEffect(circle.get(i), org.bukkit.Effect.COLOURED_DUST, 0, 0, 0.2F, 1.5F, 0.2F, 0, 4, 16);
//                hero.getPlayer().getWorld().spawnParticle(Particle.REDSTONE, circle.get(i), 4, 0.2F, 1.5F, 0.2F, 0, skillEffectDustOptions);
            }

            // Check if the hero has a party
            if (!hero.hasParty()) {
                hero.tryHeal(hero, skill, healAmount);
            } else {
                Location playerLocation = hero.getPlayer().getLocation();

                for (Hero member : hero.getParty().getMembers()) {
                    Location memberLocation = member.getPlayer().getLocation();
                    if (!memberLocation.getWorld().equals(playerLocation.getWorld()))
                        continue;
                    if (!(memberLocation.distanceSquared(playerLocation) <= radiusSquared))
                        continue;

                    member.tryHeal(hero, skill, healAmount);
                }
            }
        }
    }
}