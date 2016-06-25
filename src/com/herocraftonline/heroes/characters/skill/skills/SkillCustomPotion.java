package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.events.HeroEnterCombatEvent;
import com.herocraftonline.heroes.api.events.HeroRegainHealthEvent;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.PeriodicHealEffect;
import com.herocraftonline.heroes.characters.effects.common.SpeedEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.util.Messaging;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SkillCustomPotion extends PassiveSkill implements Listener {

    public SkillCustomPotion(Heroes plugin) {
        super(plugin, "CustomPotion");
        setDescription("You can utilise the effect of Custom Potions, which modify the effect of standard Potions.");
        setTypes(SkillType.ITEM_MODIFYING);

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.APPLY_TEXT.node(), "");
        node.set(SkillSetting.UNAPPLY_TEXT.node(), "");
        node.set(SkillSetting.LEVEL.node(), 1);

        node.set("HEAL.percent-heal", true);
        node.set("HEAL.heal-amount", .175);

        node.set("REGENERATION.percent-heal", false);
        node.set("REGENERATION.heal-amount", 4.0);

        return node;
    }

    public boolean applyPotionEffect(LivingEntity entity, PotionEffect effect, double intensity, boolean lingering) {
        boolean isCustomPotion = false;

        CharacterTemplate ct = plugin.getCharacterManager().getCharacter(entity);
        boolean isHero = ct instanceof Hero;
        Hero hero = isHero ? (Hero) ct : null;

        String potionType = effect.getType().getName();

        if(isHero && hero.isInCombat() && SkillConfigManager.getRaw(this, potionType + "." + SkillSetting.NO_COMBAT_USE.node(), false)) {
            Messaging.send(hero.getPlayer(), "You may not use " + potionType  + " Potions in combat!");
            return true; // Self explanatory
        }

        switch(effect.getType().getName()) {
            case "SPEED":
                isCustomPotion = applySpeedEffect(entity, isHero, effect, intensity, lingering);
                break;
            case "HEAL":
                isCustomPotion = applyHealingEffect(entity, isHero, effect, intensity, lingering);
                break;
            case "REGENERATION":
                isCustomPotion = applyRegenerationEffect(entity, isHero, effect, intensity, lingering);
                break;
        }

        return isCustomPotion;
    }

    private boolean applySpeedEffect(LivingEntity entity, boolean isHero, PotionEffect effect, double intensity, boolean lingering) {
        if(!isHero)
            return false; // Monsters aren't our concern with speed, so the normal effect is sufficient with them.

        Hero hero = plugin.getCharacterManager().getHero((Player) entity);
        if(!SkillConfigManager.getRaw(this, "SPEED." + SkillSetting.NO_COMBAT_USE.node(), false))
            return false; // As SpeedEffect just ends up with a potion effect anyway, if we aren't checking combat we don't care.

        int amplifier = effect.getAmplifier();
        int duration = SkillPotion.getPotionDuration(effect.getDuration(), intensity, true);
        if(lingering)
            duration *= SkillConfigManager.getUseSetting(hero, plugin.getSkillManager().getSkill("Potion"), "lingering-multiplier.duration", 0.25, true);

        if (hero.hasEffect("SpeedPotionEffect")) {

            Effect oldEffect = hero.getEffect("SpeedPotionEffect");
            if(!(oldEffect instanceof SpeedPotionEffect))
                return true;

            SpeedPotionEffect oldSpeedEffect = (SpeedPotionEffect) oldEffect;
            long oldDuration = oldSpeedEffect.getRemainingTime();
            int oldAmplifier = oldSpeedEffect.getAmplifier();

            if(oldAmplifier > amplifier || (oldAmplifier == amplifier && oldDuration > duration))
                return true; // Stronger effect, or same strength with longer duration, cannot be overridden
            // At this point we either have stronger new effect or longer new duration, which can continue
        }

        // Apply the speed effect, with proper duration for Splash potions
        hero.addEffect(new SpeedPotionEffect(this, duration, amplifier));
        return true;
    }

    private boolean applyHealingEffect(LivingEntity entity, boolean isHero, PotionEffect effect, double intensity, boolean lingering) {
        boolean percentHeal;
        double healAmount;

        // Check they aren't at full health, no point in continuing if they are
        if (entity.getHealth() >= entity.getMaxHealth()) {
            return true; // Self explanatory
        }

        // If it's a Hero, check for combat, get settings normally and use provided events/methods to heal a Hero
        if (isHero) {
            Hero hero = plugin.getCharacterManager().getHero((Player) entity);

            percentHeal = SkillConfigManager.getRaw( this, "HEAL.percent-heal", true);
            healAmount = getRaw(this, "HEAL.heal-amount", .175);
            if(lingering)
                healAmount *= SkillConfigManager.getUseSetting(hero, plugin.getSkillManager().getSkill("Potion"), "lingering-multiplier.instant", 0.5, true);

            HeroRegainHealthEvent hrhEvent = new HeroRegainHealthEvent(hero, calculateHealing(entity, percentHeal, healAmount, effect.getAmplifier(), intensity), this);

            Bukkit.getPluginManager().callEvent(hrhEvent);

            if (!hrhEvent.isCancelled())
                hero.heal(hrhEvent.getAmount());
        }
        // If it's not a Hero, it's a Monster, but for the sake of consistency we'll heal it too (albeit with a different event and calculations)
        // This doesn't harm undead, but that'd create lore issues with BecomeDeath and we can just say these are chemical rather than magical... right?
        else {
            percentHeal = SkillConfigManager.getRaw(this, "HEAL.percent-heal", true);
            healAmount = getRaw(this, "HEAL.heal-amount", .175);
            if(lingering)
                healAmount *= getRaw(plugin.getSkillManager().getSkill("Potion"), "lingering-multiplier.instant", 0.5);

            // Heroes doesn't seem to use this event for Monsters, but it feels wrong to not include it.
            EntityRegainHealthEvent erhEvent = new EntityRegainHealthEvent(entity, calculateHealing(entity, percentHeal, healAmount, effect.getAmplifier(), intensity), EntityRegainHealthEvent.RegainReason.MAGIC);

            Bukkit.getPluginManager().callEvent(erhEvent);

            if (!erhEvent.isCancelled()) {
                double healed = entity.getHealth() + erhEvent.getAmount();
                double max = entity.getMaxHealth();
                entity.setHealth(healed <= max ? healed : max);
            }
        }
        return true;
    }

    private boolean applyRegenerationEffect(LivingEntity entity, boolean isHero, PotionEffect effect, double intensity, boolean lingering) {
        CharacterTemplate ct = plugin.getCharacterManager().getCharacter(entity);

        boolean noCombat;
        boolean percentHeal;
        double healAmount;

        // Check they aren't at full health, no point in continuing if they are
        if (entity.getHealth() >= entity.getMaxHealth()) {
            return true; // Self explanatory
        }
        // If it's a Hero, check for combat, get settings normally
        if (isHero) {
            Hero hero = (Hero) ct;
            noCombat = SkillConfigManager.getRaw(this, "REGENERATION." + SkillSetting.NO_COMBAT_USE.node(), false);

            percentHeal = SkillConfigManager.getRaw(this, "REGENERATION.percent-heal", false);
            healAmount = getRaw(this, "REGENERATION.heal-amount", 4.0);

            if(lingering)
                healAmount *= SkillConfigManager.getUseSetting(hero, plugin.getSkillManager().getSkill("Potion"), "lingering-multiplier.instant", 0.5, true);
        }
        // If it's not a Hero, it's a Monster, but for the sake of consistency we'll heal it too.
        else {
            noCombat = false; // Monsters gotta stay strong
            percentHeal = SkillConfigManager.getRaw(this, "REGENERATION.percent-heal", false);
            healAmount = getRaw(this, "REGENERATION.heal-amount", 4.0);

            if(lingering)
                healAmount *= getRaw(plugin.getSkillManager().getSkill("Potion"), "lingering-multiplier.instant", 0.5);

        }

        double totalHealing = (percentHeal ? healAmount * entity.getMaxHealth() : healAmount) * (effect.getAmplifier() + 1);
        ct.addEffect(new RegenerationPotionEffect(this, 2500, SkillPotion.getPotionDuration(effect, intensity, true), totalHealing, noCombat));
        return true;
    }

    // Removes effects if the Hero enters Combat
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHeroEnterCombat(HeroEnterCombatEvent event) {
        Hero hero = event.getHero();
        if (hero.hasEffect("SpeedPotionEffect"))
            hero.removeEffect(hero.getEffect("SpeedPotionEffect"));

        if (hero.hasEffect("RegenerationPotionEffect")) {
            Effect effect = hero.getEffect("RegenerationPotionEffect");
            if (effect instanceof RegenerationPotionEffect && ((RegenerationPotionEffect) effect).isNoCombat())
                hero.removeEffect(hero.getEffect("RegenerationPotionEffect"));
        }
    }

    // Extend PeriodicHeal but add a boolean to determine no-combat or not.
    public class RegenerationPotionEffect extends PeriodicHealEffect {
        boolean expireOnCombat;

        public RegenerationPotionEffect(Skill skill, long period, long duration, double tickHealth, boolean noCombat) {
            super(skill, "RegenerationPotionEffect", null, period, duration, tickHealth, null, null);

            expireOnCombat = noCombat;
        }

        public boolean isNoCombat() {
            return expireOnCombat;
        }

        @Override
        public void tickHero(Hero hero) {
            // PeriodicHealEffect expects the applier to be non-null, so we have to reimplement this ourselves...
            // super.tickHero(hero);
            HeroRegainHealthEvent event = new HeroRegainHealthEvent(hero, super.getTickHealth(), skill);
            plugin.getServer().getPluginManager().callEvent(event);
            if(!event.isCancelled())
                hero.heal(event.getAmount());

            Player player = hero.getPlayer();
            player.getWorld().spigot().playEffect(player.getLocation(), org.bukkit.Effect.HAPPY_VILLAGER, 0, 0, 0.5F, 1.0F, 0.5F, 0.1F, 25, 16);
        }

        @Override
        public void tickMonster(Monster monster) {
            super.tickMonster(monster);
            LivingEntity entity = monster.getEntity();
            entity.getWorld().spigot().playEffect(entity.getLocation(), org.bukkit.Effect.HAPPY_VILLAGER, 0, 0, 0.5F, 1.0F, 0.5F, 0.1F, 25, 16);
        }
    }

    // Speed effect taken from Accelerando just in case its delayed removal is necessary. Not sure why it's there, so keeping it for now.
    public class SpeedPotionEffect extends SpeedEffect {

        int amplifier;

        public SpeedPotionEffect(Skill skill, int duration, int multiplier) {
            super(skill, "SpeedPotionEffect", null, duration, multiplier, null, null);
            amplifier = multiplier;
        }

        public int getAmplifier() {
            return amplifier;
        }

        @Override
        public void removeFromHero(final Hero hero) {
            Player player = hero.getPlayer();

            if (player.hasPotionEffect(PotionEffectType.POISON) || player.hasPotionEffect(PotionEffectType.WITHER)
                    || player.hasPotionEffect(PotionEffectType.HARM)) {
                // If they have a harmful effect present when removing the ability, delay effect removal by a bit.
                Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                    @Override
                    public void run() {
                        SpeedPotionEffect.super.removeFromHero(hero);
                    }
                }, 2L);
            }
            else {
                super.removeFromHero(hero);
            }
        }
    }

    // Get heal amount, either a % of health or straight amount, multiply by amplifier and multiply again by intensity
    private double calculateHealing(LivingEntity entity, boolean percentHeal, double healAmount, int amplifier, double intensity) {
        return (percentHeal ? healAmount * entity.getMaxHealth() : healAmount) * (amplifier + 1) * intensity + 0.5 ;
    }

    // Convenience method to get raw doubles, as getRaw only allows String and Boolean
    private double getRaw(Skill skill, String node, double def) {
        if (skill == null) {
            return def;
        }

        String num = SkillConfigManager.getRaw(skill, node, null);

        if (num == null) {
            return def;
        }

        try {
            return Double.parseDouble(num);
        }
        catch (NumberFormatException ex) {
            return def;
        }
    }

}
