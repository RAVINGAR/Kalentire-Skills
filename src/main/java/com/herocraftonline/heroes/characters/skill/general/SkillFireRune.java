package com.herocraftonline.heroes.characters.skill.general;

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

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.CustomNameManager;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.characters.skill.runes.Rune;
import com.herocraftonline.heroes.characters.skill.runes.RuneActivationEvent;
import com.herocraftonline.heroes.characters.skill.runes.RuneApplicationEvent;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.GeometryUtil;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.util.List;

public class SkillFireRune extends ActiveSkill {
    public SkillFireRune(Heroes plugin) {
        // Heroes stuff
        super(plugin, "FireRune");
        setDescription("Imbue your blade with the Rune of Fire. Upon Rune application, this Rune will deal $1 fire damage to the target.");
        setUsage("/skill firerune");
        setIdentifiers("skill firerune");
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.ABILITY_PROPERTY_FIRE, SkillType.AGGRESSIVE, SkillType.DAMAGING, SkillType.SILENCEABLE);
        setArgumentRange(0, 0);

        Bukkit.getServer().getPluginManager().registerEvents(new FireRuneListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 55, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 0.875, false);
        damage += (int) (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        return getDescription().replace("$1", damage + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DAMAGE.node(), 55);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 1.375);
        node.set(SkillSetting.USE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% imbues his blade with a Rune of " + ChatColor.RED + "Fire.");
        node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% has been burned by a Rune of Fire!");
        node.set("rune-chat-color", ChatColor.RED.toString());

        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        // Let the world know that the hero has activated a Rune.
        broadcastExecuteText(hero);

        // Create the Rune
        int manaCost = (SkillConfigManager.getUseSetting(hero, this, SkillSetting.MANA, 15, false));
        String runeChatColor = SkillConfigManager.getRaw(this, "rune-chat-color", ChatColor.RED.toString());
        Rune fireRune = new Rune("FireRune", manaCost, runeChatColor);

        // Add the Rune to the RuneWord queue here
        Bukkit.getServer().getPluginManager().callEvent(new RuneActivationEvent(hero, fireRune));

        // Play Effects
        //Util.playClientEffect(player, "enchantmenttable", new Vector(0, 0, 0), 1F, 10, true);
        player.getWorld().spawnParticle(Particle.ENCHANTMENT_TABLE, player.getEyeLocation(), 10, 0.5, 0.3, 0.5, 1F);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 0.5F, 1.0F);

        List<Location> circle = GeometryUtil.circle(player.getLocation(), 36, 1.5);
        for (int i = 0; i < circle.size(); i++)
		{
        	//player.getWorld().spigot().playEffect(circle(player.getLocation().add(0, 1, 0), 36, 1.5).get(i), org.bukkit.Effect.FLAME, 0, 0, 0.0F, 0.0F, 0.0F, 0.0F, 1, 16);
            player.getWorld().spawnParticle(Particle.FLAME, circle.get(i), 1, 0, 0, 0, 0);
		}

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
            if (!(event.getRuneList().getHead().name.equals("FireRune")))
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

                    Player player = hero.getPlayer();

                    // Prep variables
                    CharacterTemplate targCT = skill.plugin.getCharacterManager().getCharacter((LivingEntity) targEnt);

                    double damage = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE, 55, false);
                    double damageIncrease = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 0.875, false);
                    damage += (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

                    String applyText = SkillConfigManager.getRaw(skill, SkillSetting.APPLY_TEXT, "%target% has been burned by a Rune of Fire!");

                    // Damage the target
                    addSpellTarget((LivingEntity) targEnt, hero);
                    damageEntity((LivingEntity) targEnt, hero.getPlayer(), damage, DamageCause.MAGIC, 0.0F);
                    
                    //targEnt.getWorld().spigot().playEffect(targEnt.getLocation().add(0, 0.5, 0), Effect.FLAME, 0, 0, 0, 0, 0, 1.5F, 45, 16);
                    targEnt.getWorld().spawnParticle(Particle.FLAME, targEnt.getLocation(), 45, 0, 0, 0, 1.5);

                    // Announce that the player has been hit with the skill
                    broadcast(targEnt.getLocation(), "    " + applyText.replace("%target%", CustomNameManager.getName(targCT)));

                    // Play Effects
                    //Util.playClientEffect(player, "enchantmenttable", new Vector(0, 0, 0), 1F, 10, true);
                    player.getWorld().spawnParticle(Particle.ENCHANTMENT_TABLE, player.getEyeLocation(), 10, 0.5, 0.3, 0.5, 1F);
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_BURN, 0.5F, 1.0F);
                }
            }, (long) (0.1 * 20));

        }
    }
}