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
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.skills.absorbrunes.Rune;
import com.herocraftonline.heroes.characters.skill.skills.absorbrunes.RuneActivationEvent;
import com.herocraftonline.heroes.characters.skill.skills.absorbrunes.RuneApplicationEvent;

public class SkillFireRune extends ActiveSkill {
    public SkillFireRune(Heroes plugin) {
        // Heroes stuff
        super(plugin, "FireRune");
        setDescription("Imbue your blade with the Rune of Fire. Upon Rune application, this Rune will deal $1 fire damage to the target.");
        setUsage("/skill firerune");
        setIdentifiers("skill firerune");
        setTypes(SkillType.FIRE, SkillType.HARMFUL, SkillType.DAMAGING, SkillType.SILENCABLE);
        setArgumentRange(0, 0);

        // Start up the listener for Runeword skill usage
        Bukkit.getServer().getPluginManager().registerEvents(new FireRuneListener(this), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DAMAGE.node(), 105);
        node.set(SkillSetting.USE_TEXT.node(), ChatColor.GRAY + "[" + ChatColor.DARK_GREEN + "Skill" + ChatColor.GRAY + "] %hero% imbues his blade with a Rune of " + ChatColor.RED + "Fire.");
        node.set(SkillSetting.APPLY_TEXT.node(), ChatColor.GRAY + "[" + ChatColor.DARK_GREEN + "Skill" + ChatColor.GRAY + "] %target% has been burned by a Rune of Fire!");
        node.set("rune-chat-color", ChatColor.RED.toString());

        return node;
    }

    @Override
    public String getDescription(Hero hero) {
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 105, false);
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

    /*
     * This listener is the main controller for the FireRune ability. The primary function is to listen to the Rune Application event.
     * It could be used to listen to other things as well, but that won't typically be necessary.
     */
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

                    double damage = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE, 105, false);
                    String applyText = SkillConfigManager.getRaw(skill, SkillSetting.APPLY_TEXT, "%target% has been burned by a Rune of Fire!").replace("%target%", "$1");

                    // Damage and silence the target
                    skill.plugin.getDamageManager().addSpellTarget(targEnt, hero, skill);
                    damageEntity((LivingEntity) targEnt, hero.getPlayer(), damage, EntityDamageEvent.DamageCause.FIRE, false);

                    // Announce that the player has been hit with the skill 
                    broadcast(targEnt.getLocation(), applyText, targCT.getName());

                    // Play Firework effect
                    // CODE HERE

                    // Play sound
                    hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.FIZZ, 0.5F, 1.0F);
                }
            }, (long) (0.1 * 20));

            return;
        }
    }

    //    public class WitheringEffect extends ExpirableEffect {
    //
    //        private double finishDamage;
    //
    //        public WitheringEffect(Skill skill, double damage) {
    //            super(skill, "Withering", 100);
    //
    //            this.finishDamage = finishDamage;
    //
    //            types.add(EffectType.DISPELLABLE);
    //            types.add(EffectType.DARK);
    //            types.add(EffectType.WITHER);
    //            types.add(EffectType.HARMFUL);
    //
    //            addMobEffect(9, (int) ((duration + 4000) / 1000) * 20, 3, false);
    //            addMobEffect(20, (int) (duration / 1000) * 20, 1, false);
    //        }
    //
    //        @Override
    //        public void applyToMonster(Monster monster) {
    //            super.applyToMonster(monster);
    //
    //            broadcast(monster.getEntity().getLocation(), applyText, Messaging.getLivingEntityName(monster));
    //        }
    //
    //        @Override
    //        public void applyToHero(Hero hero) {
    //            super.applyToHero(hero);
    //
    //            Player player = hero.getPlayer();
    //            broadcast(player.getLocation(), applyText, player.getDisplayName());
    //        }
    //
    //        @Override
    //        public void removeFromMonster(Monster monster) {
    //            super.removeFromMonster(monster);
    //
    //            if (monster.getEntity().isDead())
    //                return;
    //
    //            skill.addSpellTarget(monster.getEntity(), getApplierHero());
    //            damageEntity(monster.getEntity(), getApplier(), finishDamage, DamageCause.MAGIC);
    //
    //            broadcast(monster.getEntity().getLocation(), expireText, Messaging.getLivingEntityName(monster));
    //        }
    //
    //        @Override
    //        public void removeFromHero(Hero hero) {
    //            super.removeFromHero(hero);
    //
    //            Player player = hero.getPlayer();
    //            if (player.isDead())
    //                return;
    //
    //            skill.addSpellTarget(hero.getEntity(), getApplierHero());
    //            damageEntity(player, getApplier(), finishDamage, DamageCause.MAGIC);
    //
    //            broadcast(player.getLocation(), expireText, player.getDisplayName());
    //        }
    //    }
}