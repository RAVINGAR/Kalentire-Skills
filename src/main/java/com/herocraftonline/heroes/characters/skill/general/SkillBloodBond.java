package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Listenable;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.GeometryUtil;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.util.List;

public class SkillBloodBond extends ActiveSkill implements Listenable {
    private static final Particle.DustOptions skillEffectDustOptions = new Particle.DustOptions(Color.RED, 1);
    private final String effectName = "BloodBond";
    private final Listener listener;
    private String applyText;
    private String expireText;

    public SkillBloodBond(final Heroes plugin) {
        super(plugin, "BloodBond");
        setDescription("Form a Blood Bond with your party. " +
                "While bound, you convert $1% of your magic damage into health for you and all party members within a $2 block radius. " +
                "Costs $3 health to use, and $4 mana per $5 second(s) to maintain the effect.");
        setUsage("/skill bloodbond");
        setIdentifiers("skill bloodbond");
        setArgumentRange(0, 0);
        setTypes(SkillType.BUFFING, SkillType.SILENCEABLE, SkillType.AREA_OF_EFFECT, SkillType.HEALING, SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.ABILITY_PROPERTY_DARK);

        setToggleableEffectName(effectName);
        listener = new BloodBondListener(this);
    }

    @Override
    public String getDescription(final Hero hero) {
        final double healPercent = SkillConfigManager.getUseSetting(hero, this, "heal-percent", 0.2, false);
        final int manaTick = SkillConfigManager.getUseSetting(hero, this, "mana-tick", 22, false);
        final int manaTickPeriod = SkillConfigManager.getUseSetting(hero, this, "mana-tick-period", 1000, false);
        final double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS.node(), 10.0, false);
        final double healthCost = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALTH_COST.node(), 25.0, false);


        return getDescription()
                .replace("$1", Util.decFormat.format((healPercent * 100.0)))
                .replace("$2", Util.decFormat.format(radius))
                .replace("$3", Util.decFormat.format(healthCost))
                .replace("$4", manaTick + "")
                .replace("$5", Util.decFormat.format(manaTickPeriod / 1000.0));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection config = super.getDefaultConfig();
        config.set("heal-percent", 0.2);
        config.set(SkillSetting.RADIUS.node(), 12.0);
        config.set("mana-tick", 22);
        config.set("mana-tick-period", 1000);
        config.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% has formed a Blood Bond!");
        config.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% has broken their Bond of Blood.");
        return config;
    }

    @Override
    public void init() {
        super.init();

        this.applyText = SkillConfigManager.getRaw(this,
                        SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%hero% has formed a Blood Bond!")
                .replace("%hero%", "$1").replace("$hero$", "$1");

        this.expireText = SkillConfigManager.getRaw(this,
                        SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%hero% has broken their Bond of Blood.")
                .replace("%hero%", "$1").replace("$hero$", "$1");
    }

    @Override
    public SkillResult use(final Hero hero, final String[] args) {
        final Player player = hero.getPlayer();

        final int manaTickPeriod = SkillConfigManager.getUseSetting(hero, this, "mana-tick-period", 1000, false);
        hero.addEffect(new BloodBondEffect(this, player, manaTickPeriod));

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5F, 1.0F);

        final List<Location> circle = GeometryUtil.circle(player.getLocation(), 36, 1.5);
        for (final Location location : circle) {
            //hero.getPlayer().getWorld().spigot().playEffect(circle.get(i), org.bukkit.Effect.COLOURED_DUST, 0, 0, 0.2F, 1.5F, 0.2F, 0, 4, 16);
            player.getWorld().spawnParticle(Particle.REDSTONE, location, 4, 0.2F, 1.5F, 0.2F, 0, skillEffectDustOptions);
        }
        return SkillResult.NORMAL;
    }

    @Override
    public Listener getListener() {
        return listener;
    }

    public class BloodBondEffect extends PeriodicEffect {
        private int manaTick;
        private boolean firstTime = true;
        private double healPercent;
        private double radius;
        private double radiusSquared;

        BloodBondEffect(final SkillBloodBond skill, final Player applier, final int period) {
            super(skill, effectName, applier, period, applyText, expireText);

            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.MANA_DECREASING);
            types.add(EffectType.AREA_OF_EFFECT);
            types.add(EffectType.HEALING);
        }

        @Override
        public void applyToHero(final Hero hero) {
            super.applyToHero(hero);
            firstTime = true;

            this.manaTick = SkillConfigManager.getUseSetting(hero, skill, "mana-tick", 13, false);
            this.healPercent = SkillConfigManager.getUseSetting(hero, skill, "heal-percent", 0.15, false);
            this.radius = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.RADIUS, 12.0, false);
            this.radiusSquared = radius * radius;
        }

        @Override
        public void tickHero(final Hero hero) {
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

        public double getHealPercent() {
            return healPercent;
        }

        public double getRadiusSquared() {
            return radiusSquared;
        }
    }

    public class BloodBondListener implements Listener {
        private final Skill skill;

        BloodBondListener(final Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onSkillDamage(final EntityDamageByEntityEvent event) {
            if (!event.getCause().equals(DamageCause.MAGIC) || !(event.getDamager() instanceof Player)) {
                return;
            }

            // Make sure the hero has the bloodbond effect
            final Hero hero = plugin.getCharacterManager().getHero((Player) event.getDamager());
            if (hero.hasEffect(effectName)) {
                final BloodBondEffect effect = (BloodBondEffect) hero.getEffect(effectName);
                healHeroParty(hero, event.getDamage(), effect);
            }
        }

        // Heals the hero and his party based on the specified damage
        private void healHeroParty(final Hero hero, final double damage, final BloodBondEffect effect) {
            final double healAmount = effect.getHealPercent() * damage;

            // Check if the hero has a party
            if (!hero.hasParty()) {
                hero.tryHeal(hero, skill, healAmount);
            } else {
                final Location playerLocation = hero.getPlayer().getLocation();

                for (final Hero member : hero.getParty().getMembers()) {
                    final Location memberLocation = member.getPlayer().getLocation();
                    if (!memberLocation.getWorld().equals(playerLocation.getWorld())) {
                        continue;
                    }
                    if (memberLocation.distanceSquared(playerLocation) > effect.getRadiusSquared()) {
                        continue;
                    }

                    member.tryHeal(hero, skill, healAmount);
                }
            }

            final List<Location> circle = GeometryUtil.circle(hero.getPlayer().getLocation(), 36, 1.5);
            for (final Location location : circle) {
                //hero.getPlayer().getWorld().spigot().playEffect(circle.get(i), org.bukkit.Effect.COLOURED_DUST, 0, 0, 0.2F, 1.5F, 0.2F, 0, 4, 16);
                hero.getPlayer().getWorld().spawnParticle(Particle.REDSTONE, location, 4, 0.2F, 1.5F, 0.2F, 0, skillEffectDustOptions);
            }
        }
    }
}