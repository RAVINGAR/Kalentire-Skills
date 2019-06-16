package com.herocraftonline.heroes.characters.skill.reborn.unused;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.events.SkillDamageEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.characters.skill.reborn.bloodmage.SkillFocusOfMind;
import com.herocraftonline.heroes.util.GeometryUtil;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.List;

public class SkillSymbioticBond extends PassiveSkill {
    private final String bondEffectName = "SymbioticBond";
    private String applyText;
    private String expireText;

    public SkillSymbioticBond(Heroes plugin) {
        super(plugin, "SymbioticBond");
        setDescription("When in the Sanguine Focus stance, you form a symbiotic bond with your allies." +
                "While bound, you convert $1% of your magic damage into health for up to $3 party members within a $2 block radius (excluding yourself).");
        setArgumentRange(0, 0);
        setTypes(SkillType.BUFFING, SkillType.AREA_OF_EFFECT, SkillType.HEALING, SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.ABILITY_PROPERTY_DARK);

        Bukkit.getServer().getPluginManager().registerEvents(new SymbioticBondListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        double healPercent = SkillConfigManager.getUseSetting(hero, this, "heal-percent", 0.25, false);
        double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS.node(), 12.0, false);

        return getDescription()
                .replace("$1", Util.decFormat.format((healPercent * 100)))
                .replace("$2", Util.decFormat.format(radius));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set("heal-percent", 0.5);
        config.set(SkillSetting.RADIUS.node(), 8.0);
        config.set("max-targets", 5);
        return config;
    }

    @Override
    public void apply(Hero hero) {
        hero.addEffect(new SymbioticBondEffect(this, hero.getPlayer()));
    }

    @Override
    public String getPassiveEffectName() {
        return bondEffectName;
    }

    public class SymbioticBondEffect extends Effect {
        SymbioticBondEffect(SkillSymbioticBond skill, Player applier) {
            super(skill, bondEffectName, applier, applyText, expireText);

            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.AREA_OF_EFFECT);
            types.add(EffectType.HEALING);
        }
    }

    public class SymbioticBondListener implements Listener {
        private final Skill skill;

        SymbioticBondListener(Skill skill) {
            this.skill = skill;
        }

        // Why was this here? Is it actually needed? We need to test.
//        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
//        public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
//            if (!event.getCause().equals(DamageCause.MAGIC) || !(event.getDamager() instanceof Player))
//                return;
//
//            Hero hero = plugin.getCharacterManager().getHero((Player) event.getDamager());
//            if (!hero.hasParty() || !hero.hasEffect(bondEffectName))
//                return;
//            if (!hero.hasEffect(SkillFocusOfMind.stanceEffectName))
//                return;
//
//            SkillFocusOfMind.FocusEffect stanceEffect = (SkillFocusOfMind.FocusEffect) hero.getEffect(SkillFocusOfMind.stanceEffectName);
//            if (stanceEffect == null || stanceEffect.getCurrentStance() != SkillFocusOfMind.StanceType.SANGUINE)
//                return;
//
//            healHeroParty(hero, event.getDamage());
//        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onSkillDamage(SkillDamageEvent event) {
            if ((!(event.getDamager() instanceof Player)))
                return;

            Hero hero = plugin.getCharacterManager().getHero((Player) event.getDamager());
            if (!hero.hasParty() || !hero.hasEffect(bondEffectName))
                return;
            if (!hero.hasEffect(SkillFocusOfMind.stanceEffectName))
                return;

            SkillFocusOfMind.FocusEffect focusEffect = (SkillFocusOfMind.FocusEffect) hero.getEffect(SkillFocusOfMind.stanceEffectName);
            if (focusEffect == null || focusEffect.getCurrentStance() != SkillFocusOfMind.StanceType.SANGUINE)
                return;

            healHeroParty(hero, event.getDamage());
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