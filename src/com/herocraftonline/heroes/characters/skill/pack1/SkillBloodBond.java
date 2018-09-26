package com.herocraftonline.heroes.characters.skill.pack1;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.*;
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
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.CompatSound;
import com.herocraftonline.heroes.util.Util;

public class SkillBloodBond extends ActiveSkill {
    public SkillBloodBond(Heroes plugin) {
        super(plugin, "BloodBond");
        setDescription("Form a Blood Bond with your party. While bound, you convert $1% of your magic damage into health for you and all party members within a $2 block radius. Costs $4 health to use, and $3 mana per second to maintain the effect.");
        setUsage("/skill bloodbond");
        setArgumentRange(0, 0);
        setIdentifiers("skill bloodbond");
        setTypes(SkillType.BUFFING, SkillType.SILENCEABLE, SkillType.AREA_OF_EFFECT, SkillType.HEALING, SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.ABILITY_PROPERTY_DARK);

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
        node.set("toggle-on-text", ChatComponents.GENERIC_SKILL + "%hero% has formed a " + ChatColor.BOLD + "BloodBond" + ChatColor.RESET + "!");
        node.set("toggle-off-text", ChatComponents.GENERIC_SKILL + "%hero% has broken his " + ChatColor.BOLD + "BloodBond" + ChatColor.RESET + "!");

        return node;
    }
    
    public ArrayList<Location> circle(Location centerPoint, int particleAmount, double circleRadius)
	{
		World world = centerPoint.getWorld();

		double increment = (2 * Math.PI) / particleAmount;

		ArrayList<Location> locations = new ArrayList<Location>();

		for (int i = 0; i < particleAmount; i++)
		{
			double angle = i * increment;
			double x = centerPoint.getX() + (circleRadius * Math.cos(angle));
			double z = centerPoint.getZ() + (circleRadius * Math.sin(angle));
			locations.add(new Location(world, x, centerPoint.getY(), z));
		}
		return locations;
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
        String applyText = SkillConfigManager.getRaw(this, "toggle-on-text", ChatComponents.GENERIC_SKILL + "%hero% has formed a " + ChatColor.BOLD + "BloodBond" + ChatColor.RESET + "!").replace("%hero%", "$1");
        String expireText = SkillConfigManager.getRaw(this, "toggle-off-text", ChatComponents.GENERIC_SKILL + "%hero% has broken his " + ChatColor.BOLD + "BloodBond" + ChatColor.RESET + "!").replace("%hero%", "$1");

        hero.addEffect(new BloodBondEffect(this, manaTick, manaTickPeriod, applyText, expireText));

        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), CompatSound.ENTITY_WITHER_SPAWN.value(), 0.5F, 1.0F);
        List<Location> circle = circle(hero.getPlayer().getLocation(), 36, 1.5);
        for (int i = 0; i < circle.size(); i++)
		{
			//hero.getPlayer().getWorld().spigot().playEffect(circle.get(i), org.bukkit.Effect.COLOURED_DUST, 0, 0, 0.2F, 1.5F, 0.2F, 0, 4, 16);
            hero.getPlayer().getWorld().spawnParticle(Particle.REDSTONE, circle.get(i), 4, 0.2F, 1.5F, 0.2F, 0, new Particle.DustOptions(Color.RED, 1), true);
		}
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

            List<Location> circle = circle(hero.getPlayer().getLocation(), 36, 1.5);
            for (int i = 0; i < circle.size(); i++)
    		{
            	//hero.getPlayer().getWorld().spigot().playEffect(circle.get(i), org.bukkit.Effect.COLOURED_DUST, 0, 0, 0.2F, 1.5F, 0.2F, 0, 4, 16);
                hero.getPlayer().getWorld().spawnParticle(Particle.REDSTONE, circle.get(i), 4, 0.2F, 1.5F, 0.2F, 0, new Particle.DustOptions(Color.RED, 1), true);
    		}

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
                                member.heal(healEvent.getDelta());
                        }
                    }
                }
            }
            else {
                HeroRegainHealthEvent healEvent = new HeroRegainHealthEvent(hero, healAmount, skill, hero);
                Bukkit.getPluginManager().callEvent(healEvent);
                if (!healEvent.isCancelled())
                    hero.heal(healEvent.getDelta());
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
            types.add(EffectType.AREA_OF_EFFECT);
            types.add(EffectType.HEALING);
        }

        @Override
        public void applyToHero(Hero hero) {
            firstTime = true;
            super.applyToHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), "    " + applyText, player.getName(), "BloodBond");
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), "    " + expireText, player.getName(), "BloodBond");
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
                else {      // They have enough mana--continue
                    // Drain the player's mana
                    hero.setMana(hero.getMana() - manaTick);
                }
            }
        }
    }
}