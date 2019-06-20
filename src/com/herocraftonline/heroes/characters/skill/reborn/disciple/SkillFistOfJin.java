package com.herocraftonline.heroes.characters.skill.reborn.disciple;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainHealthEvent;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.classes.scaling.ExpressionScaling;
import com.herocraftonline.heroes.characters.classes.scaling.Scaling;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.effects.PeriodicEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.heroes.util.GeometryUtil;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.ArrayList;
import java.util.List;

import static org.bukkit.event.entity.EntityDamageEvent.*;

public class SkillFistOfJin extends ActiveSkill {

    private static final String effectName = "FistsOfJin";
    private final String cooldownEffectName = "FistOfJinCooldownEffect";
    private String applyText;
    private String expireText;

    public SkillFistOfJin(Heroes plugin) {
        super(plugin, "FistOfJin");
        setDescription("Enhance your fists with Jin. Each of your melee strikes restore $1 health to you, and $2 health to party members within $3 blocks. " +
                "You cannot heal more than once per $4 second(s). " +
                "Costs $5 mana per $6 second(s) to maintain the effect.");
        setUsage("/skill fistofjin");
        setIdentifiers("skill fistofjin");
        setArgumentRange(0, 0);
        setTypes(SkillType.BUFFING, SkillType.SILENCEABLE, SkillType.AREA_OF_EFFECT, SkillType.HEALING, SkillType.ABILITY_PROPERTY_PHYSICAL);

        Bukkit.getPluginManager().registerEvents(new SkillHeroListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        double cdDuration = SkillConfigManager.getUseSetting(hero, this, "healing-internal-cooldown", 1000, false);
        int manaTick = SkillConfigManager.getUseSetting(hero, this, "mana-tick", 22, false);
        int manaTickPeriod = SkillConfigManager.getUseSetting(hero, this, "mana-tick-period", 1000, false);

        double selfHeal = SkillConfigManager.getScaledUseSettingDouble(hero, this, "heal-per-hit-self", false);
        double partyHeal = SkillConfigManager.getScaledUseSettingDouble(hero, this, "heal-per-hit-party", false);

        double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 8.0, false);

        return getDescription()
                .replace("$1", Util.decFormat.format(selfHeal))
                .replace("$2", Util.decFormat.format(partyHeal))
                .replace("$3", Util.decFormat.format(radius))
                .replace("$4", Util.decFormat.format(cdDuration / 1000.0))
                .replace("$5", manaTick + "")
                .replace("$6", Util.decFormat.format(manaTickPeriod / 1000.0));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.APPLY_TEXT.node(), "");
        config.set(SkillSetting.UNAPPLY_TEXT.node(), "");
        config.set(SkillSetting.RADIUS.node(), 8.0);
        config.set("mana-tick", 22);
        config.set("mana-tick-period", 1000);
        config.set("healing-internal-cooldown", 1000);
        config.set("heal-per-hit-self", 8);
        config.set("heal-per-hit-party", 3);
        return config;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this,
                "toggle-on-text", ChatComponents.GENERIC_SKILL + "%hero% has Fists of Jin!")
                .replace("%hero%", "$1");

        expireText = SkillConfigManager.getRaw(this,
                "toggle-off-text", ChatComponents.GENERIC_SKILL + "%hero% no longer has Fists of Jin.")
                .replace("%hero%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, String[] strings) {
        Player player = hero.getPlayer();

        int manaTickPeriod = SkillConfigManager.getUseSetting(hero, this, "mana-tick-period", 1000, false);
        hero.addEffect(new FistOfJinEffect(this, player, manaTickPeriod));

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5F, 1.0F);

        List<Location> circle = GeometryUtil.circle(player.getLocation(), 36, 1.5);
        for (Location location : circle) {
            player.getWorld().spigot().playEffect(location, Effect.SPELL, 0, 0, 0.2F, 1.5F, 0.2F, 0, 4, 16);
        }
        return SkillResult.NORMAL;
    }

    public class FistOfJinEffect extends PeriodicEffect {
        private int manaTick;
        private boolean firstTime = true;
        private double radius;
        private double radiusSquared;
        private double selfHeal;
        private double partyHeal;
        private int cdDuration;

        FistOfJinEffect(Skill skill, Player applier, int period) {
            super(skill, effectName, applier, period, applyText, expireText);

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

            this.cdDuration = SkillConfigManager.getUseSetting(hero, skill, "healing-internal-cooldown", 1000, false);
            this.selfHeal = SkillConfigManager.getScaledUseSettingDouble(hero, skill, "heal-per-hit-self", false);
            this.partyHeal = SkillConfigManager.getScaledUseSettingDouble(hero, skill, "heal-per-hit-party", false);
            this.manaTick = SkillConfigManager.getUseSetting(hero, skill, "mana-tick", 13, false);
            this.radius = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.RADIUS, 12.0, false);
            this.radiusSquared = radius * radius;
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

        public double getRadiusSquared() {
            return radiusSquared;
        }

        public double getSelfHeal() {
            return selfHeal;
        }

        public double getPartyHeal() {
            return partyHeal;
        }

        public int getCdDuration() {
            return cdDuration;
        }
    }

    private class SkillHeroListener implements Listener {
        private Skill skill;

        SkillHeroListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onWeaponDamage(EntityDamageByEntityEvent event) {
            if (!(event.getDamager() instanceof Player) || !(event.getEntity() instanceof LivingEntity) || event.getCause() != DamageCause.ENTITY_ATTACK) {
                return;
            }

            Player player = (Player) event.getDamager();
            Hero hero = plugin.getCharacterManager().getHero(player);
            if (hero.hasEffect(cooldownEffectName))
                return;
            if (!hero.hasEffect(effectName))
                return;

            FistOfJinEffect effect = (FistOfJinEffect) hero.getEffect(effectName);
            double radiusSquared = effect.getRadiusSquared();

            if (!hero.hasParty()) {
                hero.tryHeal(hero, skill, effect.getSelfHeal(), true);
            } else {
                Location playerLocation = player.getLocation();
                // Loop through the player's party members and heal as necessary
                for (Hero member : hero.getParty().getMembers()) {
                    Location memberLocation = member.getPlayer().getLocation();

                    // Ensure the party member is in the same world.
                    if (!memberLocation.getWorld().equals(playerLocation.getWorld())) {
                        continue;
                    }

                    // Check to see if we're dealing with the hero himself.
                    if (member.equals(hero)) {
                        hero.tryHeal(hero, skill, effect.getSelfHeal(), true);
                    } else if (memberLocation.distanceSquared(playerLocation) <= radiusSquared) {
                        // Check to see if they are close enough to the player to receive healing
                        member.tryHeal(hero, skill, effect.getPartyHeal());
                    }
                }
            }

            CooldownEffect cdEffect = new CooldownEffect(skill, player, effect.getCdDuration());
            hero.addEffect(cdEffect);
        }
    }

    // Effect for implementing an internal cooldown on healing
    private class CooldownEffect extends ExpirableEffect {
        CooldownEffect(Skill skill, Player applier, long duration) {
            super(skill, cooldownEffectName, applier, duration);
        }
    }
}