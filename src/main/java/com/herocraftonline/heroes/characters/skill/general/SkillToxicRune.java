package com.herocraftonline.heroes.characters.skill.general;

/*
 * Coded by: Delfofthebla - Last updated on 5 / 5 / 2013
 *
 * OVERVIEW:
 * ------------------------------------------------
 * This is the "Toxic" Rune ability. It is one of the many Rune abilities available to the Runeblade.
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
 * Upon Rune activation, this ability applies a debuff to the target that damages them every few seconds for a specified duration.
 */

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.CustomNameManager;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicDamageEffect;
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
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

public class SkillToxicRune extends ActiveSkill {
    public SkillToxicRune(final Heroes plugin) {
        // Heroes stuff
        super(plugin, "ToxicRune");
        setDescription("Imbue your blade with the Rune of Toxicity. Upon Rune application, this Rune will poison the target causing $1 damage over $2 second(s).");
        setUsage("/skill toxicrune");
        setIdentifiers("skill toxicrune");
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.DEBUFFING, SkillType.AGGRESSIVE, SkillType.DAMAGING, SkillType.SILENCEABLE);
        setArgumentRange(0, 0);

        // Start up the listener for runeword skill usage
        Bukkit.getServer().getPluginManager().registerEvents(new ToxicRuneListener(this), plugin);
    }

    @Override
    public String getDescription(final Hero hero) {
        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 17500, false);
        final int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 2500, false);

        double tickDamage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK, 15, false);
        final double tickDamageIncrease = hero.getAttributeValue(AttributeType.INTELLECT) * SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 0.4, false);
        tickDamage += tickDamageIncrease;

        final String formattedDamage = Util.decFormat.format(tickDamage * ((double) duration / (double) period));
        final String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription().replace("$1", formattedDamage).replace("$2", formattedDuration);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DAMAGE.node(), 15);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 0.375);
        node.set(SkillSetting.PERIOD.node(), 3000);
        node.set(SkillSetting.DURATION.node(), 12000);
        node.set(SkillSetting.USE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%hero% imbues his blade with a Rune of " + ChatColor.DARK_GREEN + "Toxicity.");
        node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% has been poisoned by a Rune of Toxicity!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% has recovered from the poison!");
        node.set("rune-chat-color", ChatColor.DARK_GREEN.toString());

        return node;
    }

    @Override
    public SkillResult use(final Hero hero, final String[] args) {
        final Player player = hero.getPlayer();

        // Let the world know that the hero has activated a Rune.
        broadcastExecuteText(hero);

        // Create the Rune
        final int manaCost = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MANA, 20, false);
        final String runeChatColor = SkillConfigManager.getRaw(this, "rune-chat-color", ChatColor.DARK_GREEN.toString());
        final Rune toxicRune = new Rune("ToxicRune", manaCost, runeChatColor);

        // Add the ToxicRune to the rune queue here
        Bukkit.getServer().getPluginManager().callEvent(new RuneActivationEvent(hero, toxicRune));

        // Play Effects
        //Util.playClientEffect(player, "enchantmenttable", new Vector(0, 0, 0), 1F, 10, true);
        player.getWorld().spawnParticle(Particle.ENCHANTMENT_TABLE, player.getEyeLocation(), 10, 0.5, 0.3, 0.5, 1F);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 0.5F, 1.0F);

        final List<Location> circle = GeometryUtil.circle(player.getLocation(), 36, 1.5);
        for (final Location location : circle) {
            //player.getWorld().spigot().playEffect(circle(player.getLocation().add(0, 1, 0), 36, 1.5).get(i), org.bukkit.Effect.HAPPY_VILLAGER, 0, 0, 0.0F, 0.0F, 0.0F, 0.0F, 1, 16);
            player.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, location, 0, 0, 0, 0);
        }

        return SkillResult.NORMAL;
    }

    private static class ToxicRunePoison extends PeriodicDamageEffect {
        private final String applyText;
        private final String expireText;
        private final Player applier;

        public ToxicRunePoison(final Skill skill, final Player applier, final long period, final long duration, final double tickDamage, final String applyText, final String expireText) {
            super(skill, "ToxicRunePoison", applier, period, duration, tickDamage, false);

            this.applyText = applyText;
            this.expireText = expireText;
            this.applier = applier;

            types.add(EffectType.POISON);
            types.add(EffectType.DISPELLABLE);

            addPotionEffect(new PotionEffect(PotionEffectType.POISON, (int) ((duration / 1000.0) * 20), 0), true);
        }

        @Override
        public void applyToMonster(final Monster monster) {
            super.applyToMonster(monster);
            broadcast(monster.getEntity().getLocation(), "    " + applyText, CustomNameManager.getName(monster), applier.getName());
        }

        @Override
        public void applyToHero(final Hero hero) {
            super.applyToHero(hero);
            final Player player = hero.getPlayer();
            broadcast(player.getLocation(), "    " + applyText, player.getName());
        }

        @Override
        public void removeFromMonster(final Monster monster) {
            super.removeFromMonster(monster);
            broadcast(monster.getEntity().getLocation(), "    " + expireText, CustomNameManager.getName(monster), applier.getName());
        }

        @Override
        public void removeFromHero(final Hero hero) {
            super.removeFromHero(hero);
            final Player player = hero.getPlayer();
            broadcast(player.getLocation(), "    " + expireText, player.getName());
        }
    }

    /*
     * This listener is the main controller for the ToxicRune ability. The primary function is to listen to the Rune Application event.
     * It could be used to listen to other things as well, but that won't typically be necessary.
     */
    private class ToxicRuneListener implements Listener {
        private final Skill skill;

        public ToxicRuneListener(final Skill skill) {
            this.skill = skill;
        }

        // Listen for the toxic rune application
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onRuneApplication(final RuneApplicationEvent event) {
            // Get Hero information
            final Hero hero = event.getHero();

            // Check to see if this is the correct rune to apply, and that the player actually has the rune applied.
            if (!(event.getRuneList().getHead().name == "ToxicRune")) {
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

                // Set the variables
                final long duration = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DURATION, 9000, false);
                final long period = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.PERIOD, 3000, false);

                double damage = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE, 55, false);
                final double damageIncrease = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 0.875, false);
                damage += (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

                final String applyText = SkillConfigManager.getRaw(skill, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%target% has been poisoned by a Rune of Toxicity!").replace("%target%", "$1").replace("$target$", "$1");
                final String expireText = SkillConfigManager.getRaw(skill, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%target% has recovered from the poison!").replace("%target%", "$1").replace("$target$", "$1");

                // Create the effect
                final ToxicRunePoison pEffect = new ToxicRunePoison(skill, hero.getPlayer(), period, duration, damage, applyText, expireText);

                // Add the effects to the target
                final CharacterTemplate targCT = skill.plugin.getCharacterManager().getCharacter((LivingEntity) targEnt);
                targCT.addEffect(pEffect);

                // Play Effects
                //Util.playClientEffect(player, "enchantmenttable", new Vector(0, 0, 0), 1F, 10, true);
                player.getWorld().spawnParticle(Particle.ENCHANTMENT_TABLE, player.getEyeLocation(), 10, 0.5, 0.3, 0.5, 1F);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_BURN, 0.5F, 1.0F);
                //targCT.getEntity().getWorld().spigot().playEffect(targCT.getEntity().getLocation().add(0, 1, 0), org.bukkit.Effect.HAPPY_VILLAGER, 0, 0, 0.5F, 0.0F, 0.5F, 0.2F, 35, 16);
                targCT.getEntity().getWorld().spawnParticle(Particle.VILLAGER_HAPPY, targCT.getEntity().getLocation(), 35, 0.5, 0, 0.5, 0.2);
            }, (long) (0.1 * 20));

            return;
        }
    }
}