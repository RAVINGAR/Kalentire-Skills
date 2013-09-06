package com.herocraftonline.heroes.characters.skill.skills;

/*
 * Coded by: Delfofthebla - Last updated on 5 / 5 / 2013
 * 
 * OVERVIEW:
 * ------------------------------------------------
 * This is a MULTI PURPOSE ABILITY. It is absolutely //REQUIRED// for a player to properly use other "Rune" abilities.
 * Without this skill, the Rune abilities will NOT function, and possibly cause some java exceptions.
 * 
 * PASSIVELY, it maintains a hashmap of every player's RuneQueue, which contains a list of their applied Runes.
 * It is also a hosting listener for all "Rune" related skills or abilities.
 * It determines when Runes are applied, who they are applied by, and who they are applied to.
 * 
 * ACTIVELY, it clears all Runes within a specified heroes hashmap, and then returns a portion of mana spent back to the player.
 * The amount of mana returned is determined by the "conversion-rate" config setting.
 * 
 * 
 * Here are the REQUIRED imports for this skill to function.
 * //YOU MUST INCLUDE THESE WITH WITH THIS SKILL//
 * 
 * com.herocraftonline.heroes.characters.skill.skills.absorbrunes.Rune;
 * com.herocraftonline.heroes.characters.skill.skills.absorbrunes.RuneActivationEvent;
 * com.herocraftonline.heroes.characters.skill.skills.absorbrunes.RuneApplicationEvent;
 * com.herocraftonline.heroes.characters.skill.skills.absorbrunes.RuneExpireEvent;
 * com.herocraftonline.heroes.characters.skill.skills.absorbrunes.RuneQueue;
 * 
 * Import Descriptions:
 * 
 * Rune.java					// The actual Rune object. It is a key component of the RuneQueue Data Structure and keeps track
 * 								//		of Rune names and the next Runes in queue.
 * RuneActivationEvent.java		// The Rune activation event class. Needed for detecting when a Rune skill has been activated.
 * RuneExpireEvent.java			// The Rune expiration event class. Needed for detecting when a Rune has been applied and needs to be expired.
 * RuneQueue.java				// The actual Rune Queue List. A unique object of this kind is attached to every player that has this skill.
 */

import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.ClassChangeEvent;
import com.herocraftonline.heroes.api.events.HeroChangeLevelEvent;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.classes.HeroClass;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.runeskills.Rune;
import com.herocraftonline.heroes.characters.skill.runeskills.RuneActivationEvent;
import com.herocraftonline.heroes.characters.skill.runeskills.RuneApplicationEvent;
import com.herocraftonline.heroes.characters.skill.runeskills.RuneExpireEvent;
import com.herocraftonline.heroes.characters.skill.runeskills.RuneQueue;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

public class SkillAbsorbRunes extends ActiveSkill {
    // Runequeue Hashmap for holding all player RuneQueue tables
    HashMap<Hero, RuneQueue> heroRunes;

    public SkillAbsorbRunes(Heroes plugin) {
        // Heroes stuff
        super(plugin, "AbsorbRunes");
        setDescription("Activate to absorb your weapon's imbued Runes and regain a portion of the mana spent at a $1% rate.");
        setUsage("/skill absorbrunes");
        setIdentifiers("skill absorbrunes");
        setTypes(SkillType.MANA_INCREASING, SkillType.ABILITY_PROPERTY_MAGICAL);
        setArgumentRange(0, 0);

        // Start up the listener for Rune events
        Bukkit.getPluginManager().registerEvents(new AbsorbRunesListener(this), plugin);

        // Create a new hashmap for all hero Rune queues.
        heroRunes = new HashMap<Hero, RuneQueue>();
    }

    @Override
    public String getDescription(Hero hero) {
        double conversionRate = SkillConfigManager.getUseSetting(hero, this, "conversion-rate", 0.35, false);
        return getDescription().replace("$1", (int) (conversionRate * 100) + "");
    }

    // Tell heroes to populate skills.yml with our default values if they do not exist
    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        // Skill usage configs
        node.set(SkillSetting.USE_TEXT.node(), ChatColor.GRAY + "[" + ChatColor.DARK_GREEN + "Skill" + ChatColor.GRAY + "] %hero% absorbs his Runes!");
        node.set("weapons", Util.swords);
        node.set("conversion-rate", 0.35);
        node.set("fail-text-no-runes", ChatColor.GRAY + "[" + ChatColor.DARK_GREEN + "Skill" + ChatColor.GRAY + "] " + ChatColor.WHITE + "You have no Runes to absorb!");

        // Rune usage configs
        node.set("rune-application-cooldown", Integer.valueOf(1000));
        node.set("imbued-runes-text-empty", ChatColor.GRAY + "[" + ChatColor.DARK_GREEN + "Skill" + ChatColor.GRAY + "] " + ChatColor.WHITE + "Your weapon is no longer imbued with Runes!");
        node.set("imbued-runes-text-start", ChatColor.GRAY + "[" + ChatColor.DARK_GREEN + "Skill" + ChatColor.GRAY + "] " + ChatColor.WHITE + "Imbued Runes: <");
        node.set("imbued-runes-text-delimiter", ChatColor.WHITE + "|");
        node.set("imbued-runes-text-end", ChatColor.WHITE + ">");

        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        // Check to see if the hero is contained within the hashmap and is actually allowed to use runeList
        if (!heroRunes.containsKey(hero)) {
            return SkillResult.FAIL;
        }

        Player player = hero.getPlayer();

        // Check to see if they actually have Runes to absorb
        RuneQueue runeList = heroRunes.get(hero);
        if (runeList.isEmpty()) {
            String failText = SkillConfigManager.getUseSetting(hero, this, "fail-text-no-runes", ChatColor.GRAY + "[" + ChatColor.DARK_GREEN + "Skill" + ChatColor.GRAY + "] " + ChatColor.WHITE + "You have no Runes to absorb!");
            Messaging.send(player, failText, new Object[0]);
            return SkillResult.FAIL;
        }

        // We have Runes to absorb, continue on!

        // Delete the whole Rune list and total an absorption value
        int absorbValue = 0;
        double conversionRate = SkillConfigManager.getUseSetting(hero, this, "conversion-rate", 0.35, false);
        do {
            // Increase the absorb value
            absorbValue += (runeList.getHead().manaCost * conversionRate);

            // Pop the Rune off the list
            runeList.pop();
        }
        while (!runeList.isEmpty());

        // Return the Mana to the player
        hero.setMana(hero.getMana() + absorbValue);

        // Let the world know that the hero has absorbed his Runes
        broadcastExecuteText(hero);

        // Play firework effect
        // CODE HERE

        // Play sound
        player.getWorld().playSound(player.getLocation(), Sound.LEVEL_UP, 0.5F, 1.0F);

        // Save the altered list to the hashmap
        heroRunes.put(hero, runeList);

        return SkillResult.NORMAL;
    }

    private class AbsorbRunesListener implements Listener {
        private final Skill skill;

        public AbsorbRunesListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onWeaponDamage(WeaponDamageEvent event) {
            if (!(event.getDamager() instanceof Hero) || !(event.getEntity() instanceof LivingEntity)) {
                return;
            }

            Hero hero = (Hero) event.getDamager();
            Player player = hero.getPlayer();
            LivingEntity target = (LivingEntity) event.getEntity();

            // Check to see if we actually have a Runelist bound to this player
            if (!heroRunes.containsKey(hero))
                return;		// Player isn't on the hashmap. Do not continue

            // Make sure they are actually dealing damage to the target.
            if (!damageCheck(player, target)) {
                return;
            }

            if (!(event.getAttackerEntity() instanceof Arrow)) {
                Material item = player.getItemInHand().getType();
                if (!SkillConfigManager.getUseSetting(hero, skill, "weapons", Util.swords).contains(item.name())) {
                    return;
                }
            }
            else
                return;     // Don't allow arrows.

            // Check to see if the hero can apply runes to his target right now
            CharacterTemplate targetCT = plugin.getCharacterManager().getCharacter(target);
            String cdEffectName = hero.getName() + "_RuneApplicationCooldownEffect";
            if (targetCT.hasEffect(cdEffectName))
                return;		// Rune application is on cooldown. Do not continue.

            // We have a runelist bound to the player, check to see if it actually has any Runes in it
            RuneQueue runeList = heroRunes.get(hero);

            // Make sure the rune list isn't empty
            if (runeList.isEmpty())
                return;

            // We have Runes, continue

            // Trigger the rune application event so that the appropriate Rune activates.
            Bukkit.getServer().getPluginManager().callEvent(new RuneApplicationEvent(hero, event.getEntity(), runeList));

            // Only expire if they don't have the RuneExpirationImmunityEffect
            if (!hero.hasEffect("RuneExpirationImmunityEffect")) {
                // Expire the rune from the player's rune list
                Bukkit.getServer().getPluginManager().callEvent(new RuneExpireEvent(hero, 1));

                // Add the "cooldown" effect to the hero so he can't immediately trigger another Rune Application Event here.
                int cdDuration = SkillConfigManager.getUseSetting(hero, skill, "rune-application-cooldown", 1000, false);
                targetCT.addEffect(new RuneApplicationCooldownEffect(skill, player, cdEffectName, cdDuration));
            }

            return;
        }

        // Listen for Rune activations, and then queue them to the player's Rune list.
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onRuneActivation(RuneActivationEvent event) {
            Hero hero = event.getHero();
            Rune rune = event.getRune();

            // Check to see if the hero is contained within the hashmap and is actually allowed to use runeList
            if (!heroRunes.containsKey(hero))
                return;			// Not on the hashmap. Do not continue

            RuneQueue runeList = heroRunes.get(hero);

            // Ensure that there is an actual Runelist
            if (runeList.isEmpty())
                runeList = new RuneQueue();		// There is no Runelist. Create one

            // Push the new Rune to the list
            runeList.push(rune);

            // Save the list to the hashmap
            heroRunes.put(hero, runeList);

            // Display the Runequeue to the player
            displayRuneQueue(hero, runeList);
        }

        // Listen for rune expirations, and then remove them from the player's rune list
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onRuneExpiration(RuneExpireEvent event) {
            Hero hero = event.getHero();

            // Check to see if the hero is contained within the hashmap and is actually allowed to use runeList
            if (heroRunes.containsKey(hero)) {
                RuneQueue runeList = heroRunes.get(hero);

                // Ensure that there is an actual Runelist
                if (runeList.isEmpty())
                    return;		// There is no list. Invalid expire event. Do not continue

                for (int i = 0; i < event.getRunesToExpire(); i++) {
                    // Pop the Rune off the list
                    runeList.pop();
                }

                // Save the list to the hashmap
                heroRunes.put(hero, runeList);

                // Display the Runequeue to the player
                displayRuneQueue(hero, runeList);
            }

            return;
        }

        // Manipulate the RuneList hashmap on player death
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onPlayerDeath(PlayerDeathEvent event) {
            Hero hero = skill.plugin.getCharacterManager().getHero(event.getEntity());

            // Check to see if the player is on the hash map
            if (!heroRunes.containsKey(hero))
                return;

            clearRuneList(hero);

            return;
        }

        // Manipulate the Rune hashmap on player world change
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onPlayerWorldChange(PlayerChangedWorldEvent event) {
            // Prep variables
            Hero hero = skill.plugin.getCharacterManager().getHero(event.getPlayer());

            // Check to see if the player is on the hash map
            if (!heroRunes.containsKey(hero))
                return;

            clearRuneList(hero);

            return;
        }

        // Manipulate the RuneList hashmap on player join
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onPlayerJoin(PlayerJoinEvent event) {
            // Prep variables
            Hero hero = skill.plugin.getCharacterManager().getHero(event.getPlayer());
            HeroClass heroClass = hero.getHeroClass();
            int level = hero.getLevel(heroClass);

            // Check if the player's class actually has the skill available
            if (heroClass.hasSkill(skill.getName())) {
                // The class does have the skill. Check to see if the hero is high enough level to use it.
                int levelReq = SkillConfigManager.getSetting(heroClass, skill, SkillSetting.LEVEL.node(), 1);
                if (level >= levelReq) {
                    // They are high enough level, add them to the hashmap
                    heroRunes.put(hero, new RuneQueue());
                }
            }

            return;
        }

        // Manipulate the HashMap upon player logout
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onPlayerQuit(PlayerQuitEvent event) {
            Hero hero = skill.plugin.getCharacterManager().getHero(event.getPlayer());

            // If the player is on the hashmap, remove him from it.
            if (heroRunes.containsKey(hero))
                heroRunes.remove(hero);

            return;
        }

        // Manipulate the HashMap on hero level changes
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onHeroChangeLevel(HeroChangeLevelEvent event) {
            // Prep variables
            Hero hero = event.getHero();
            HeroClass heroClass = hero.getHeroClass();

            // Check if the player's class actually has the skill available
            if (!heroClass.hasSkill(skill.getName()))
                return;					// Class does not have the skill. Do nothing.

            // Check to see if the player is on the hash map or not (He could be if he is being de-leveled)
            if (!heroRunes.containsKey(hero)) {
                // Player is not on the hashmap. Check to see if he should be.

                int toLevel = event.getTo();

                // Check to see if the hero is high enough level to get the skill
                int levelReq = SkillConfigManager.getSetting(event.getHeroClass(), skill, SkillSetting.LEVEL.node(), 1);
                if (toLevel >= levelReq) {
                    // Player is high enough level to use the skill, put him on the hashmap.
                    heroRunes.put(hero, new RuneQueue());
                }

                return;
            }
            else {
                // Player is on the hashmap. Check to see if we should remove him.

                int toLevel = event.getTo();

                // Check to see if the hero is high enough level to get the skill
                int levelReq = SkillConfigManager.getSetting(event.getHeroClass(), skill, SkillSetting.LEVEL.node(), 1);
                if (toLevel < levelReq) {
                    // Player is not high enough level to use the skill, remove him from the hashmap.
                    heroRunes.remove(hero);
                }

                return;
            }
        }

        // Determine hash map manipulation on class switch
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onClassChange(ClassChangeEvent event) {
            // Prep heroes variables
            Hero hero = event.getHero();
            HeroClass to = event.getTo();

            // This appears to be getting a null result at times. Check it first to avoid exceptions
            if (to != null) {
                // Check if the class actually has the skill available
                if (!to.hasSkill(skill.getName())) {
                    // If they don't have the skill but are on the hashmap, remove them from it.
                    if (heroRunes.containsKey(hero)) {
                        heroRunes.remove(hero);
                    }

                    return;
                }
                else {
                    // The class does have the skill. Check to see if the hero is allowed to have it yet.
                    int toLevel = hero.getLevel(to);
                    int levelReq = SkillConfigManager.getSetting(to, skill, SkillSetting.LEVEL.node(), 1);
                    if (toLevel < levelReq) {
                        // They aren't high enough level

                        // Check to see if they're already on the hashmap
                        if (heroRunes.containsKey(hero)) {
                            // Remove them until they are high enough
                            heroRunes.remove(hero);
                        }

                        return;
                    }
                    else {
                        // They are high enough level

                        // Check to see if they're already on the hashmap
                        if (!heroRunes.containsKey(hero)) {
                            // They aren't on the map for some reason. Put them on it.
                            heroRunes.put(hero, new RuneQueue());
                        }

                        return;
                    }
                }
            }
        }
    }

    // Clears the hero's rune list
    private void clearRuneList(Hero hero) {
        // If the player is on the hashmap, empty his Rune list.
        RuneQueue runeList = heroRunes.get(hero);

        if (runeList != null) {
            while (!runeList.isEmpty()) {
                // Pop the Rune off the list
                runeList.pop();
            }

            // Save the new list to the hashmap
            heroRunes.put(hero, runeList);
        }
    }

    // Displays the player's Rune Queue to the player
    private void displayRuneQueue(Hero hero, RuneQueue runeList) {
        // Start the rune queue string
        String[] runeListStr = runeList.getColoredRuneNameList();
        String currentRuneQueueStr = "";

        // Check to see if the list is empty before attempting to build a string
        if (runeListStr == null) {
            currentRuneQueueStr = SkillConfigManager.getRaw(this, "imbued-runes-text-empty", ChatColor.GRAY + "[" + ChatColor.DARK_GREEN + "Skill" + ChatColor.GRAY + "] " + ChatColor.WHITE + "Your weapon is no longer imbued with Runes!");
        }
        else {
            String runeQueueStrStart = SkillConfigManager.getRaw(this, "imbued-runes-text-start", ChatColor.GRAY + "[" + ChatColor.DARK_GREEN + "Skill" + ChatColor.GRAY + "] " + ChatColor.WHITE + "Imbued Runes: <");
            String runeQueueStrDelimiter = SkillConfigManager.getRaw(this, "imbued-runes-text-delimiter", ChatColor.WHITE + "|");

            currentRuneQueueStr = runeQueueStrStart + runeQueueStrDelimiter;

            // List is not empty. Build the string
            for (int i = 0; i < runeListStr.length; i++) {
                currentRuneQueueStr += (" " + runeListStr[i] + " " + runeQueueStrDelimiter);
            }

            currentRuneQueueStr += SkillConfigManager.getRaw(this, "imbued-runes-text-end", ChatColor.WHITE + ">");
        }

        // Show the player his the message
        Messaging.send(hero.getPlayer(), currentRuneQueueStr, new Object[0]);
    }

    // Effect required for implementing an internal cooldown on rune application
    private class RuneApplicationCooldownEffect extends ExpirableEffect {
        public RuneApplicationCooldownEffect(Skill skill, Player applier, String effectName, long duration) {
            super(skill, effectName, applier, duration);
        }
    }
}
