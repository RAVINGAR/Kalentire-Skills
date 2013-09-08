package com.herocraftonline.heroes.characters.skill.skills;

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

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicDamageEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.runeskills.Rune;
import com.herocraftonline.heroes.characters.skill.runeskills.RuneActivationEvent;
import com.herocraftonline.heroes.characters.skill.runeskills.RuneApplicationEvent;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

public class SkillToxicRune extends ActiveSkill {
    public SkillToxicRune(Heroes plugin) {
        // Heroes stuff
        super(plugin, "ToxicRune");
        setDescription("Imbue your blade with the Rune of Toxicity. Upon Rune application, this Rune will poison the target causing $1 damage over $2 seconds.");
        setUsage("/skill toxicrune");
        setIdentifiers("skill toxicrune");
        setTypes(SkillType.DEBUFFING, SkillType.AGGRESSIVE, SkillType.DAMAGING, SkillType.SILENCABLE);
        setArgumentRange(0, 0);

        // Start up the listener for runeword skill usage
        Bukkit.getServer().getPluginManager().registerEvents(new ToxicRuneListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 17500, false);
        int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 2500, false);

        double tickDamage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_TICK, 15, false);
        double tickDamageIncrease = hero.getAttributeValue(AttributeType.INTELLECT) * SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 0.4, false);
        tickDamage += tickDamageIncrease;

        String formattedDamage = Util.decFormat.format(tickDamage * ((double) duration / (double) period));
        String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription().replace("$1", formattedDamage).replace("$2", formattedDuration);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DAMAGE.node(), Integer.valueOf(15));
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), Double.valueOf(0.375));
        node.set(SkillSetting.PERIOD.node(), 3000);
        node.set(SkillSetting.DURATION.node(), 12000);
        node.set(SkillSetting.USE_TEXT.node(), Messaging.getSkillDenoter() + "%hero% imbues his blade with a Rune of " + ChatColor.DARK_GREEN + "Toxicity.");
        node.set(SkillSetting.APPLY_TEXT.node(), Messaging.getSkillDenoter() + "%target% has been poisoned by a Rune of Toxicity!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), Messaging.getSkillDenoter() + "%target% has recovered from the poison!");
        node.set("rune-chat-color", ChatColor.DARK_GREEN.toString());

        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        // Let the world know that the hero has activated a Rune.
        broadcastExecuteText(hero);

        // Create the Rune
        int manaCost = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MANA, 20, false);
        String runeChatColor = SkillConfigManager.getRaw(this, "rune-chat-color", ChatColor.DARK_GREEN.toString());
        Rune toxicRune = new Rune("ToxicRune", manaCost, runeChatColor);

        // Add the ToxicRune to the rune queue here
        Bukkit.getServer().getPluginManager().callEvent(new RuneActivationEvent(hero, toxicRune));

        // Play Sound
        player.getWorld().playSound(player.getLocation(), Sound.WITHER_IDLE, 0.5F, 1.0F);

        return SkillResult.NORMAL;
    }

    /*
     * This listener is the main controller for the ToxicRune ability. The primary function is to listen to the Rune Application event.
     * It could be used to listen to other things as well, but that won't typically be necessary.
     */
    private class ToxicRuneListener implements Listener {
        private final Skill skill;

        public ToxicRuneListener(Skill skill) {
            this.skill = skill;
        }

        // Listen for the toxic rune application
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onRuneApplication(RuneApplicationEvent event) {
            // Get Hero information
            final Hero hero = event.getHero();

            // Check to see if this is the correct rune to apply, and that the player actually has the rune applied.
            if (!(event.getRuneList().getHead().name == "ToxicRune"))
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

                    // Set the variables
                    long duration = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DURATION, 9000, false);
                    long period = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.PERIOD, 3000, false);

                    double damage = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE, Integer.valueOf(55), false);
                    double damageIncrease = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, Double.valueOf(0.875), false);
                    damage += (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

                    String applyText = SkillConfigManager.getRaw(skill, SkillSetting.APPLY_TEXT, Messaging.getSkillDenoter() + "%target% has been poisoned by a Rune of Toxicity!").replace("%target%", "$1");
                    String expireText = SkillConfigManager.getRaw(skill, SkillSetting.EXPIRE_TEXT, Messaging.getSkillDenoter() + "%target% has recovered from the poison!").replace("%target%", "$1");

                    // Create the effect
                    ToxicRunePoison pEffect = new ToxicRunePoison(skill, hero.getPlayer(), period, duration, damage, applyText, expireText);

                    // Add the effects to the target
                    CharacterTemplate targCT = skill.plugin.getCharacterManager().getCharacter((LivingEntity) targEnt);
                    targCT.addEffect(pEffect);

                    // Play sound
                    hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.FIZZ, 0.5F, 1.0F);
                }
            }, (long) (0.1 * 20));

            return;
        }
    }

    private class ToxicRunePoison extends PeriodicDamageEffect {
        private final String applyText;
        private final String expireText;
        private final Player applier;

        public ToxicRunePoison(Skill skill, Player applier, long period, long duration, double tickDamage, String applyText, String expireText) {
            super(skill, "ToxicRunePoison", applier, period, duration, tickDamage, false);

            this.applyText = applyText;
            this.expireText = expireText;
            this.applier = applier;

            types.add(EffectType.POISON);
            types.add(EffectType.DISPELLABLE);

            addMobEffect(19, (int) ((duration / 1000.0) * 20), 0, true);
        }

        @Override
        public void applyToMonster(Monster monster) {
            super.applyToMonster(monster);
            broadcast(monster.getEntity().getLocation(), applyText, Messaging.getLivingEntityName(monster), applier.getDisplayName());
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            final Player player = hero.getPlayer();
            broadcast(player.getLocation(), applyText, player.getDisplayName());
        }

        @Override
        public void removeFromMonster(Monster monster) {
            super.removeFromMonster(monster);
            broadcast(monster.getEntity().getLocation(), expireText, Messaging.getLivingEntityName(monster), applier.getDisplayName());
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            final Player player = hero.getPlayer();
            broadcast(player.getLocation(), expireText, player.getDisplayName());
        }
    }
}