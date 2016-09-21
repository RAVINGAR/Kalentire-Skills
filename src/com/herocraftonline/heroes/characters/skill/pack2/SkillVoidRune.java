package com.herocraftonline.heroes.characters.skill.pack2;

/*
 * Coded by: Delfofthebla - Last updated on 5 / 5 / 2013
 * 
 * OVERVIEW:
 * ------------------------------------------------
 * This is the "Void" Rune ability. It is one of the many Rune abilities available to the Runeblade.
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
 * Upon Rune activation, this ability damages the target by a specified amount and applies a silence effect for a set duration.
 */

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.Vector;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.SilenceEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.runes.Rune;
import com.herocraftonline.heroes.characters.skill.runes.RuneActivationEvent;
import com.herocraftonline.heroes.characters.skill.runes.RuneApplicationEvent;
import com.herocraftonline.heroes.util.CompatSound;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

public class SkillVoidRune extends ActiveSkill {
    public SkillVoidRune(Heroes plugin) {
        // Heroes stuff
        super(plugin, "VoidRune");
        setDescription("Imbue your blade with the Rune of Void. Upon Rune application, this Rune will damage the target for $1 damage and then silence them for $2 seconds.");
        setUsage("/skill voidrune");
        setIdentifiers("skill voidrune");
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.DEBUFFING, SkillType.AGGRESSIVE, SkillType.INTERRUPTING, SkillType.SILENCING, SkillType.DAMAGING, SkillType.SILENCEABLE);
        setArgumentRange(0, 0);

        Bukkit.getServer().getPluginManager().registerEvents(new VoidRuneListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 35, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 0.625, false);
        damage += (int) (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 2000, false);
        String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription().replace("$1", damage + "").replace("$2", formattedDuration);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DAMAGE.node(), 25);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 0.625);
        node.set(SkillSetting.DURATION.node(), 1500);
        node.set(SkillSetting.USE_TEXT.node(), Messaging.getSkillDenoter() + "%hero% imbues his blade with a Rune of " + ChatColor.DARK_PURPLE + "Void.");
        node.set(SkillSetting.APPLY_TEXT.node(), Messaging.getSkillDenoter() + "%target% has been silenced by a Rune of Void!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), Messaging.getSkillDenoter() + "%target% is no longer silenced!");
        node.set("rune-chat-color", ChatColor.DARK_PURPLE.toString());

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
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        // Let the world know that the hero has activated a Rune.
        broadcastExecuteText(hero);

        // Create the Rune
        int manaCost = (SkillConfigManager.getUseSetting(hero, this, SkillSetting.MANA, 30, false));
        String runeChatColor = SkillConfigManager.getRaw(this, "rune-chat-color", ChatColor.DARK_PURPLE.toString());
        Rune voidRune = new Rune("VoidRune", manaCost, runeChatColor);

        // Add the Rune to the RuneWord queue here
        Bukkit.getServer().getPluginManager().callEvent(new RuneActivationEvent(hero, voidRune));

        // Play Effects
        Util.playClientEffect(player, "enchantmenttable", new Vector(0, 0, 0), 1F, 10, true);
        player.getWorld().playSound(player.getLocation(), CompatSound.ENTITY_WITHER_AMBIENT.value(), 0.5F, 1.0F);
        
        for (int i = 0; i < circle(player.getLocation(), 36, 1.5).size(); i++)
		{
        	player.getWorld().spigot().playEffect(circle(player.getLocation().add(0, 1, 0), 36, 1.5).get(i), org.bukkit.Effect.ENDER_SIGNAL, 0, 0, 0.0F, 0.0F, 0.0F, 0.0F, 1, 16);
		}

        return SkillResult.NORMAL;
    }

    /*
     * This listener is the main controller for the VoidRune ability. The primary function is to listen to the Rune Application event.
     * It could be used to listen to other things as well, but that won't typically be necessary.
     */
    private class VoidRuneListener implements Listener {
        private final Skill skill;

        public VoidRuneListener(Skill skill) {
            this.skill = skill;
        }

        // Listen for the Void rune application
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onRuneApplication(RuneApplicationEvent event) {
            // Get Hero information
            final Hero hero = event.getHero();
            final Player player = hero.getPlayer();

            // Check to see if this is the correct rune to apply, and that the player actually has the rune applied.
            if (!(event.getRuneList().getHead().name.equals("VoidRune")))
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

                    long duration = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DURATION, 1500, false);

                    double damage = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE, 25, false);
                    double damageIncrease = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 0.625, false);
                    damage += (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

                    String applyText = SkillConfigManager.getRaw(skill, SkillSetting.APPLY_TEXT, Messaging.getSkillDenoter() + "%target% has been silenced by a Rune of Void!").replace("%target%", "$1");
                    String expireText = SkillConfigManager.getRaw(skill, SkillSetting.EXPIRE_TEXT, Messaging.getSkillDenoter() + "%target% is no longer silenced!").replace("%target%", "$1");

                    // Damage and silence the target
                    addSpellTarget(targEnt, hero);
                    damageEntity((LivingEntity) targEnt, player, damage, DamageCause.MAGIC, false);
                    targEnt.getWorld().spigot().playEffect(targEnt.getLocation(), Effect.PORTAL, 0, 0, 0, 0, 0, 0.5F, 35, 16);
                    
                    VoidRuneSilenceEffect voidRuneSilenceEffect = new VoidRuneSilenceEffect(skill, player, duration, applyText, expireText);

                    // Add the effect to the target
                    targCT.addEffect(voidRuneSilenceEffect);

                    // Play Effects
                    Util.playClientEffect(player, "enchantmenttable", new Vector(0, 0, 0), 1F, 10, true);
                    player.getWorld().playSound(player.getLocation(), CompatSound.ENTITY_GENERIC_BURN.value(), 0.5F, 1.0F);
                }
            }, (long) (0.1 * 20));

        }
    }

    private class VoidRuneSilenceEffect extends SilenceEffect {
        public VoidRuneSilenceEffect(Skill skill, Player applier, long duration, String applyText, String expireText) {
            super(skill, applier, duration, applyText, expireText);
        }
    }
}