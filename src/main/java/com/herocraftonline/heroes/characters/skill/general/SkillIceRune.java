package com.herocraftonline.heroes.characters.skill.general;

/*
 * Coded by: Delfofthebla - Last updated on 5 / 5 / 2013
 *
 * OVERVIEW:
 * ------------------------------------------------
 * This is the "Ice" Rune ability. It is one of the many Rune abilities available to the Runeblade.
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
 * Upon Rune activation, this ability damages the target by a specified amount and applies a slow effect for a set duration.
 */

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.common.SlowEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.runes.Rune;
import com.herocraftonline.heroes.characters.skill.runes.RuneActivationEvent;
import com.herocraftonline.heroes.characters.skill.runes.RuneApplicationEvent;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.GeometryUtil;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.List;

public class SkillIceRune extends ActiveSkill {
    public SkillIceRune(final Heroes plugin) {
        // Heroes stuff
        super(plugin, "IceRune");
        setDescription("Imbue your blade with the Rune of Ice. Upon Rune application, this Rune will deal $1 magic damage and slow the target for $2 second(s).");
        setUsage("/skill icerune");
        setIdentifiers("skill icerune");
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.AGGRESSIVE, SkillType.DAMAGING, SkillType.DEBUFFING, SkillType.MOVEMENT_SLOWING, SkillType.INTERRUPTING, SkillType.SILENCEABLE);
        setArgumentRange(0, 0);

        Bukkit.getServer().getPluginManager().registerEvents(new IceRuneListener(this), plugin);
    }

    @Override
    public String getDescription(final Hero hero) {
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 35, false);
        final double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 0.625, false);
        damage += (int) (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 2000, false);
        final String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription().replace("$1", damage + "").replace("$2", formattedDuration);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DAMAGE.node(), 35);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 0.75);
        node.set("speed-multiplier", 2);
        node.set(SkillSetting.DURATION.node(), 2000);
        node.set(SkillSetting.USE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% imbues his blade with a Rune of " + ChatColor.AQUA + "Ice.");
        node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% has been slowed by a Rune of Ice!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% is no longer slowed!");
        node.set("rune-chat-color", ChatColor.AQUA.toString());

        return node;
    }

    @Override
    public SkillResult use(final Hero hero, final String[] args) {
        final Player player = hero.getPlayer();

        // Let the world know that the hero has activated a Rune.
        broadcastExecuteText(hero);

        // Create the Rune
        final int manaCost = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MANA, 30, false);
        final String runeChatColor = SkillConfigManager.getRaw(this, "rune-chat-color", ChatColor.AQUA.toString());
        final Rune iceRune = new Rune("IceRune", manaCost, runeChatColor);

        // Add the Rune to the RuneWord queue here
        Bukkit.getServer().getPluginManager().callEvent(new RuneActivationEvent(hero, iceRune));

        // Play Effects
        //Util.playClientEffect(player, "enchantmenttable", new Vector(0, 0, 0), 1F, 10, true);
        player.getWorld().spawnParticle(Particle.ENCHANTMENT_TABLE, player.getEyeLocation(), 10, 0.5, 0.3, 0.5, 1F);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 0.5F, 1.0F);

        final List<Location> circle = GeometryUtil.circle(player.getLocation(), 36, 1.5);
        for (final Location location : circle) {
            //player.getWorld().spigot().playEffect(circle(player.getLocation().add(0, 1, 0), 36, 1.5).get(i), org.bukkit.Effect.TILE_BREAK, Material.ICE.getId(), 0, 0.0F, 0.0F, 0.0F, 0.0F, 1, 16);
            player.getWorld().spawnParticle(Particle.BLOCK_CRACK, location.add(0, 1, 0), 1, 0, 0, 0, 0, Bukkit.createBlockData(Material.ICE));
        }

        return SkillResult.NORMAL;
    }

    /*
     * This listener is the main controller for the IceRune ability. The primary function is to listen to the Rune Application event.
     * It could be used to listen to other things as well, but that won't typically be necessary.
     */
    private class IceRuneListener implements Listener {
        private final Skill skill;

        public IceRuneListener(final Skill skill) {
            this.skill = skill;
        }

        // Listen for the Ice rune application
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onRuneApplication(final RuneApplicationEvent event) {
            // Get Hero information
            final Hero hero = event.getHero();

            // Check to see if this is the correct rune to apply, and that the player actually has the rune applied.
            if (!(event.getRuneList().getHead().name.equals("IceRune"))) {
                return;
            }

            // Ensure that the target is a living entity
            final Entity targEnt = event.getTarget();
            if (!(targEnt instanceof LivingEntity)) {
                return;
            }

            Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                if (!(damageCheck(hero.getPlayer(), (LivingEntity) targEnt))) {
                    return;
                }

                final Player player = hero.getPlayer();

                // Prep variables
                final CharacterTemplate targCT = skill.plugin.getCharacterManager().getCharacter((LivingEntity) targEnt);

                final int amplifier = SkillConfigManager.getUseSetting(hero, skill, "speed-multiplier", 2, false);
                final long duration = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DURATION, 2000, false);

                double damage = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE, 35, false);
                final double damageIncrease = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 0.625, false);
                damage += (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

                final String applyText = SkillConfigManager.getRaw(skill, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%target% has been slowed by a Rune of Ice!").replace("%target%", "$1").replace("$target$", "$1");
                final String expireText = SkillConfigManager.getRaw(skill, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%target% is no longer slowed!").replace("%target%", "$1").replace("$target$", "$1");

                // Create the effect and slow the target
                final SlowEffect sEffect = new SlowEffect(skill, player, duration, amplifier, applyText, expireText);
                sEffect.types.add(EffectType.DISPELLABLE);
                sEffect.types.add(EffectType.ICE);

                // Damage and slow the target
                addSpellTarget((LivingEntity) targEnt, hero);
                damageEntity((LivingEntity) targEnt, player, damage, EntityDamageEvent.DamageCause.MAGIC, false);
                //targEnt.getWorld().spigot().playEffect(targEnt.getLocation(), Effect.TILE_BREAK, Material.ICE.getId(), 0, 0.5F, 1.0F, 0.5F, 0.0F, 35, 16);
                targEnt.getWorld().spawnParticle(Particle.BLOCK_CRACK, targEnt.getLocation(), 35, 0.5, 1, 0.5, 0, Bukkit.createBlockData(Material.ICE));
                targCT.addEffect(sEffect);

                // Play Effects
                //Util.playClientEffect(player, "enchantmenttable", new Vector(0, 0, 0), 1F, 10, true);
                player.getWorld().spawnParticle(Particle.ENCHANTMENT_TABLE, player.getEyeLocation(), 10, 0.5, 0.3, 0.5, 1F);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_BURN, 0.5F, 1.0F);
            }, 2L);

        }
    }
}