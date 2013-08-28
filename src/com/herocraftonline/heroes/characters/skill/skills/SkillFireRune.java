package com.herocraftonline.heroes.characters.skill.skills;

/*
 * Coded by: Delfofthebla - Last updated on 5 / 5 / 2013
 * 
 * OVERVIEW:
 * ------------------------------------------------
 * This is the "Fire" Rune ability. It is one of the many Rune abilities available to the Runeblade.
 * It functions similar to a Rogue's Envenom, applying a buff to the player's weapon that will activate upon left clicking an enemy.
 * However, unlike Envenom, the RuneWord system allows for multiple effects to be applied to their weapon at once.
 * These are added in a queue, up to a maximum of 3 (determined by the SkillRuneAbsoprtipn Heroes Skill.)
 * 
 * Due to the unique nature of the Rune system, this ability, and all other Runeblade abilities, speak to a hosting skill called "SkillAbsorbRunes".
 * This is necessary in order to keep track of the Rune Queue for each individual player.
 * However, no programmer should ever have to worry about this unless they are changing the system itself.
 * Each of these Rune Abilities are stand-alone and are very clean to code.
 * 
 * 
 * ACTUAL SKILL INFORMATION:
 * ------------------------------------------------
 * Upon Rune activation, this ability causes a specified amount of fire damage to the target.
 */

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.runeskills.Rune;
import com.herocraftonline.heroes.characters.skill.runeskills.RuneActivationEvent;
import com.herocraftonline.heroes.characters.skill.runeskills.RuneApplicationEvent;
import com.herocraftonline.heroes.util.Messaging;

public class SkillFireRune extends ActiveSkill {
    public SkillFireRune(Heroes plugin) {
        // Heroes stuff
        super(plugin, "FireRune");
        setDescription("Imbue your blade with the Rune of Fire. Upon Rune application, this Rune will deal $1 fire damage to the target.");
        setUsage("/skill firerune");
        setIdentifiers("skill firerune");
        setTypes(SkillType.ABILITY_PROPERTY_FIRE, SkillType.AGGRESSIVE, SkillType.DAMAGING, SkillType.SILENCABLE);
        setArgumentRange(0, 0);

        Bukkit.getServer().getPluginManager().registerEvents(new FireRuneListener(this), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DAMAGE.node(), Integer.valueOf(55));
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), Double.valueOf(0.875));
        node.set(SkillSetting.USE_TEXT.node(), Messaging.getSkillDenoter() + "%hero% imbues his blade with a Rune of " + ChatColor.RED + "Fire.");
        node.set(SkillSetting.APPLY_TEXT.node(), Messaging.getSkillDenoter() + "%target% has been burned by a Rune of Fire!");
        node.set("rune-chat-color", ChatColor.RED.toString());

        return node;
    }

    @Override
    public String getDescription(Hero hero) {
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, Integer.valueOf(55), false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, Double.valueOf(0.875), false);
        damage += (int) (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        return getDescription().replace("$1", damage + "");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        // Create the Rune
        int manaCost = (SkillConfigManager.getUseSetting(hero, this, SkillSetting.MANA, 15, false));
        String runeChatColor = SkillConfigManager.getRaw(this, "rune-chat-color", ChatColor.RED.toString());
        Rune fireRune = new Rune("FireRune", manaCost, runeChatColor);

        // Add the Rune to the RuneWord queue here
        Bukkit.getServer().getPluginManager().callEvent(new RuneActivationEvent(hero, fireRune));

        // Play Firework
        // CODE HERE

        // Play Sound
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.WITHER_IDLE, 0.5F, 1.0F);

        // Let the world know that the hero has activated a Rune.
        broadcastExecuteText(hero);

        return SkillResult.NORMAL;
    }

    private class FireRuneListener implements Listener {
        private final Skill skill;

        public FireRuneListener(Skill skill) {
            this.skill = skill;
        }

        // Listen for the Fire rune application
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onRuneApplication(RuneApplicationEvent event) {
            // Get Hero information
            final Hero hero = event.getHero();

            // Check to see if this is the correct rune to apply, and that the player actually has the rune applied.
            if (!(event.getRuneList().getHead().name == "FireRune"))
                return;

            // Ensure that the target is a living entity
            final Entity targEnt = event.getTarget();
            if (!(targEnt instanceof LivingEntity))
                return;

            Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable()
            {
                public void run()
                {
                    if (!(damageCheck(hero.getPlayer(), (LivingEntity) targEnt)))
                        return;

                    // Prep variables
                    CharacterTemplate targCT = skill.plugin.getCharacterManager().getCharacter((LivingEntity) targEnt);

                    double damage = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE, Integer.valueOf(55), false);
                    double damageIncrease = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, Double.valueOf(0.875), false);
                    damage += (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

                    String applyText = SkillConfigManager.getRaw(skill, SkillSetting.APPLY_TEXT, "%target% has been burned by a Rune of Fire!").replace("%target%", "$1");

                    // Damage the target
                    addSpellTarget((LivingEntity) targEnt, hero);
                    damageEntity((LivingEntity) targEnt, hero.getPlayer(), damage, EntityDamageEvent.DamageCause.MAGIC, false);

                    // Announce that the player has been hit with the skill 
                    broadcast(targEnt.getLocation(), applyText, targCT.getName());

                    // Play sound
                    hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.FIZZ, 0.5F, 1.0F);
                }
            }, (long) (0.1 * 20));

            return;
        }
    }
}