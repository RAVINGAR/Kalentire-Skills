package com.herocraftonline.heroes.characters.skill.skills;

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

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainHealthEvent;
import com.herocraftonline.heroes.api.events.SkillDamageEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

public class SkillBloodBond extends ActiveSkill {
    public SkillBloodBond(Heroes plugin) {
        super(plugin, "BloodBond");
        setDescription("Form a Blood Bond with your party. While bound, you convert $1% of your magic damage into healh for you and all party members within a $2 block radius. Costs $4 health to use, and $3 mana per second to maintain the effect.");
        setUsage("/skill bloodbond");
        setArgumentRange(0, 0);
        setIdentifiers("skill bloodbond");
        setTypes(SkillType.BUFFING, SkillType.SILENCABLE, SkillType.HEALING, SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.ABILITY_PROPERTY_DARK);

        Bukkit.getServer().getPluginManager().registerEvents(new BloodBondListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        double healPercent = SkillConfigManager.getUseSetting(hero, this, "heal-percent", 0.25, false);
        int manaTick = SkillConfigManager.getUseSetting(hero, this, "mana-tick", 40, false);
        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS.node(), 12, false);
        int healthCost = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALTH_COST.node(), 25, false);

        String formattedHealPercent = Util.decFormat.format((healPercent * 100));

        return getDescription().replace("$1", formattedHealPercent).replace("$2", radius + "").replace("$3", manaTick + "").replace("$4", healthCost + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set("heal-percent", 0.15);
        node.set(SkillSetting.RADIUS.node(), 12);
        node.set("mana-tick", 13);
        node.set("mana-tick-period", 1000);
        node.set("toggle-on-text", Messaging.getSkillDeonoter() + "%hero% has formed a " + ChatColor.BOLD + "BloodBond" + ChatColor.RESET + "!");
        node.set("toggle-off-text", Messaging.getSkillDeonoter() + "%hero% has broken his " + ChatColor.BOLD + "BloodBond" + ChatColor.RESET + "!");

        return node;
    }

    @Override
    public SkillResult use(Hero hero, String args[]) {
        if (hero.hasEffect("BloodBond")) {
            hero.removeEffect(hero.getEffect("BloodBond"));
            return SkillResult.REMOVED_EFFECT;
        }

        // Get config values for the effect
        int manaTick = SkillConfigManager.getUseSetting(hero, this, "mana-tick", 13, false);
        int manaTickPeriod = SkillConfigManager.getUseSetting(hero, this, "mana-tick-period", 1000, false);

        // Get config values for text values
        String applyText = SkillConfigManager.getRaw(this, "toggle-on-text", Messaging.getSkillDeonoter() + "%hero% has formed a " + ChatColor.BOLD + "BloodBond" + ChatColor.RESET + "!").replace("%hero%", "$1");
        String expireText = SkillConfigManager.getRaw(this, "toggle-off-text", Messaging.getSkillDeonoter() + "%hero% has broken his " + ChatColor.BOLD + "BloodBond" + ChatColor.RESET + "!").replace("%hero%", "$1");

        hero.addEffect(new BloodBondEffect(this, manaTick, manaTickPeriod, applyText, expireText));

        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.WITHER_SPAWN, 0.5F, 1.0F);
        return SkillResult.NORMAL;
    }

    // Primary listener for bloodbond healing
    public class BloodBondListener implements Listener {
        private final Skill skill;

        public BloodBondListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
            // Pre-checks
            if (event.getCause().equals(DamageCause.MAGIC) && event.getDamager() instanceof Player) {
                // Make sure the hero has the bloodbond effect
                Hero hero = plugin.getCharacterManager().getHero((Player) event.getDamager());
                if (hero.hasEffect("BloodBond")) {
                    healHeroParty(hero, event.getDamage());
                }
            }
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onSkillDamage(SkillDamageEvent event) {
            // Pre-checks
            if (!(event.isCancelled()) && (event.getDamager() instanceof Player)) {
                // Make sure the hero has the bloodbond effect
                Hero hero = plugin.getCharacterManager().getHero((Player) event.getDamager());
                if (hero.hasEffect("BloodBond")) {
                    healHeroParty(hero, event.getDamage());
                }
            }
        }

        // Heals the hero and his party based on the specified damage
        private void healHeroParty(Hero hero, double d) {
            // Set the healing amount
            double healPercent = SkillConfigManager.getUseSetting(hero, skill, "heal-percent", 0.15, false);
            double healAmount = healPercent * d;

            // Set the distance variables 
            int radius = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.RADIUS, 12, false);
            int radiusSquared = radius * radius;

            // Check if the hero has a party
            if (hero.hasParty()) {
                Location playerLocation = hero.getPlayer().getLocation();
                // Loop through the player's party members and heal as necessary
                for (Hero member : hero.getParty().getMembers()) {
                    Location memberLocation = member.getPlayer().getLocation();

                    // Ensure the party member is in the same world.
                    if (memberLocation.getWorld().equals(playerLocation.getWorld())) {

                        if (memberLocation.distanceSquared(playerLocation) <= radiusSquared) {
                            // Check to see if they are close enough to the player to receive healing

                            HeroRegainHealthEvent healEvent = new HeroRegainHealthEvent(member, healAmount, skill, hero);
                            Bukkit.getPluginManager().callEvent(healEvent);
                            if (!healEvent.isCancelled())
                                member.heal(healEvent.getAmount());
                        }
                    }
                }
            }
            else {
                HeroRegainHealthEvent healEvent = new HeroRegainHealthEvent(hero, healAmount, skill, hero);
                Bukkit.getPluginManager().callEvent(healEvent);
                if (!healEvent.isCancelled())
                    hero.heal(healEvent.getAmount());
            }
        }
    }

    // Bloodbond effect
    public class BloodBondEffect extends PeriodicEffect {
        private String applyText = "";
        private String expireText = "";

        private final int manaTick;
        private boolean firstTime = true;

        public BloodBondEffect(SkillBloodBond skill, int manaTick, int period, String applyText, String expireText) {
            super(skill, "BloodBond", period);

            this.manaTick = manaTick;
            this.applyText = applyText;
            this.expireText = expireText;

            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.BENEFICIAL);
            types.add(EffectType.MANA_DECREASING);
            types.add(EffectType.HEALING);
        }

        @Override
        public void applyToHero(Hero hero) {
            firstTime = true;
            super.applyToHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), applyText, player.getDisplayName(), "BloodBond");
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), expireText, player.getDisplayName(), "BloodBond");
        }

        @Override
        public void tickHero(Hero hero) {
            super.tickHero(hero);

            if (firstTime)		// Don't drain mana on first tick
                firstTime = false;
            else {
                // Remove the effect if they don't have enough mana
                if (hero.getMana() < manaTick) {
                    hero.removeEffect(this);
                }
                else    // They have enough mana--continue
                {
                    // Drain the player's mana
                    hero.setMana(hero.getMana() - manaTick);
                }
            }
        }
    }
}