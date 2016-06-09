package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;

public class SkillPotion extends PassiveSkill {

    private static final List<String> tierIIPotions;
    private static final Map<String, Integer> potionDurations; // Tick values

    private static Method customPotionMethod = null; // reflection caching

    public SkillPotion(Heroes plugin) {
        super(plugin, "Potion");
        setDescription("You are able to use potions!");
        setTypes(SkillType.ITEM_MODIFYING);

        Bukkit.getPluginManager().registerEvents(new PotionListener(this), plugin);
    }

    // Configuration automatically determines potions from PotionEffectType.values() and sources valid tier II potions from the static tierIIPotions list.
    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.APPLY_TEXT.node(), "");
        node.set(SkillSetting.UNAPPLY_TEXT.node(), "");
        node.set(SkillSetting.LEVEL.node(), 1);
        node.set(SkillSetting.NO_COMBAT_USE.node(), false);

        // Configurability of the multipliers used when Lingering potions are appled
        node.set("lingering-multiplier.instant", 0.5);
        node.set("lingering-multiplier.duration", 0.25);


        for (PotionEffectType type : PotionEffectType.values()) {
            if(type == null)
                continue; // Docs for .values() say can be any order and contain null, which it totally does

            String potionName = type.getName();

            node.set("allow." + potionName, false);
            node.set("cooldown." + potionName, 180000);

            node.set("allow.SPLASH-" + potionName, false);
            node.set("cooldown.SPLASH-" + potionName, 180000);

            node.set("allow.LINGERING-" + potionName, false);
            node.set("cooldown.LINGERING-" + potionName, 180000);

            if(tierIIPotions.contains(potionName)) {
                node.set("allow." + potionName + "-II", false);
                node.set("cooldown." + potionName + "-II", 180000);

                node.set("allow.SPLASH-" + potionName + "-II", false);
                node.set("cooldown.SPLASH-" + potionName + "-II", 180000);

                node.set("allow.LINGERING-" + potionName + "-II", false);
                node.set("cooldown.LINGERING-" + potionName + "-II", 180000);
            }
        }

        return node;
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

    // Convenience method to pass an effect rather than the duration
    public static int getPotionDuration(PotionEffect effect, double intensity) {
        return getPotionDuration(effect.getDuration(),  intensity, false);
    }

    // More overloads, effect and specified millis
    public static int getPotionDuration(PotionEffect effect, double intensity, boolean millis) {
        return getPotionDuration(effect.getDuration(), intensity, millis);
    }

    // More overloads, int and no specified millis
    public static int getPotionDuration(int duration, double intensity) {
        return getPotionDuration(duration, intensity, false);
    }
    // Calculates the duration of a Splash potion based on its intensity
    // returns ticks if millis is false, milliseconds if true
    public static int getPotionDuration(int duration, double intensity, boolean millis) {
        int calcDuration = (int)(intensity * duration + 0.5D); // Code for this from Spigot
        return millis ? calcDuration * 50 : calcDuration; // Divide by 20 for seconds then multiply by 1000 for millis. This comes out to *50
    }


    // Actual potion logic
    public class PotionListener implements Listener {
        private final Skill skill;

        public PotionListener(Skill skill) {
            this.skill = skill;
        }



        // PlayerItemConsume listener for accurate knowledge of when a potion is drank, rather than just when it's clicked.
        @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
        public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
            if(event.getItem().getType() == Material.POTION) {
                ItemStack item = event.getItem();
                Player player = event.getPlayer();

                if(!(item.getItemMeta() instanceof PotionMeta)) {
                    event.setCancelled(true);
                    Messaging.send(player, "This Potion has no PotionMeta - please report this for investigation!");
                    return; // I don't trust this not to happen
                }

                PotionMeta pMeta = (PotionMeta) item.getItemMeta();

                // Add custom effects, then add the effect as defined by the Base PotionData
                List<PotionEffect> effects = new ArrayList<>(pMeta.getCustomEffects());

                PotionEffect vanillaEffect = getPotionEffect(pMeta.getBasePotionData()); // Get vanilla effect
                if(vanillaEffect != null)
                    effects.add(vanillaEffect); // Vanilla effect can be null; apply if not null

                if(!canUsePotion(player, effects, 0)) {
                    event.setCancelled(true);
                    return;
                }

                List<PotionEffect> normalEffects = new ArrayList<>(effects); // Just in case there are effects we don't handle, I'd rather not ignore them


                for(PotionEffect effect : effects) {
                    if(processPotionEffectEvent(player, effect))
                        normalEffects.remove(effect);
                }

                if(!effects.equals(normalEffects)) { // If we did stuff with the potion, apply any remaining normal effects and make sure it's a bottle now.
                    for(PotionEffect effect : normalEffects)
                        applyVanillaEffect(player, effect);

                    PlayerInventory pInventory = player.getInventory();

                    // We need to check the hands for the potion we just drank in order to change it; the event just gives us the item.
                    // Potion drinking goes primary hand, then secondary. Check them in that order, too.
                    if (item.equals(pInventory.getItemInMainHand()))
                        pInventory.setItemInMainHand(new ItemStack(Material.GLASS_BOTTLE));
                    else if (item.equals(pInventory.getItemInOffHand()))
                        pInventory.setItemInOffHand(new ItemStack(Material.GLASS_BOTTLE));
                    else if (player.getInventory().contains(item)) { // Triggers in case of magic or code that makes the current potion invalid, but another one exists.
                        PlayerInventory inventory = player.getInventory();
                        inventory.setItem(inventory.first(item), new ItemStack(Material.GLASS_BOTTLE));
                        Heroes.log(Level.WARNING, "User had no or different potion in hand slots when used, another existed!");
                    }
                    else // Triggers in cases of magic or code that makes the current bottle invalid, but another one does not exist.
                        Heroes.log(Level.WARNING, "User had no or different potion in hand slots when used, another did NOT exist!");

                    event.setItem(new ItemStack(Material.GLASS_BOTTLE)); // In case something un-cancels it, though it'll glitch a tad.
                    event.setCancelled(true); // This prevents the aforementioned glitch.
                }
            }
            // Not a potion, no care was given that day
        }

        // Determines from all the effects on a potion, whether its use is permitted and checks/sets cooldowns.
        // Style is 0-2, 0 is normal, 1 is splash, 2 is lingering. Values not 1/2 are treated as 0.
        private boolean canUsePotion(Player player, List<PotionEffect> effects, int potionStyle) {
            Hero hero = plugin.getCharacterManager().getHero(player);

            // see if the player can use potions at all
            if (!hero.canUseSkill(skill) || (hero.isInCombat() && SkillConfigManager.getUseSetting(hero, skill, SkillSetting.NO_COMBAT_USE, false))) {
                Messaging.send(player, "You can't use this potion!");
                return false;
            }

            // map to store cooldowns so we can ensure all potions are ok, and boolean for splash because it'a always the same
            Map<String, Long> cooldowns = new HashMap<>();

            // loop 1 to check every potion is allowed
            for(PotionEffect effect : effects) {
                // get the potion type info
                String potionType = effect.getType().getName();
                String potionName = potionType;

                // splash and lingering are similar, but mutually exclusive technically
                if(potionStyle == 1)
                    potionName = "SPLASH-" + potionName;
                else if(potionStyle == 2)
                    potionName = "LINGERING-" + potionName;

                if(tierIIPotions.contains(potionType) && effect.getAmplifier() > 0)
                    potionName += "-II"; // III and higher count as II, and non-II potions always count as I, for sanity reasons.

                // see if the player can use this type of potion
                if (!SkillConfigManager.getUseSetting(hero, skill, "allow." + potionName, false)) {
                    Messaging.send(player, "You can't use this potion!");
                    return false;
                }

                // see if this potion is on cooldown
                long time = System.currentTimeMillis();
                Long readyTime = hero.getCooldown(potionType);
                if (readyTime != null && time < readyTime) {
                    int secRemaining = (int) Math.ceil((readyTime - time) / 1000.0);
                    Messaging.send(player, "You can't use this potion for $1s!", secRemaining);
                    return false;
                }

                // store cooldowns to a map so we're sure all of them are allowed before setting any
                long cooldown = SkillConfigManager.getUseSetting(hero, skill, "cooldown." + potionName, 180000, true);
                if(!cooldowns.containsKey(potionType) || cooldowns.get(potionType) < cooldown) {
                    cooldowns.put(potionType, cooldown);
                }

            }

            // loop 2 to set potion cooldowns, since they're okay to use
            for(String potionType : cooldowns.keySet()) {
                long time = System.currentTimeMillis();
                hero.setCooldown(potionType, time + cooldowns.get(potionType));
            }

            return true;
        }

        // CloudApply listener to apply the effects from a Lingering potion, since the entities it hit aren't known until it does.
        // Since the multiplier of duration/effect for lingering is customizable, we manually apply vanilla effects always.
        @EventHandler(priority = EventPriority.LOWEST)
        public void AreaEffectCloudApplyEvent(AreaEffectCloudApplyEvent event) {
            AreaEffectCloud cloud = event.getEntity();

            // Clouds don't have a getEffects(), just base and custom, so use them to our advantage
            List<PotionEffect> effects = new ArrayList<>(cloud.getCustomEffects()); // Get custom effects

            PotionEffect vanillaEffect = getPotionEffect(cloud.getBasePotionData()); // Get vanilla effect
            if(vanillaEffect != null)
                effects.add(vanillaEffect); // Vanilla effect can be null; apply if not null

            List<PotionEffect> normalEffects = new ArrayList<>(effects); // Just in case there are effects we don't handle, I'd rather not ignore them.

            boolean firstLoop = true; // The easiest way to determine first loop without duplicating the entire loop code.

            Float cloudRadiusOnUse = cloud.getRadiusOnUse();
            int cloudDurationOnUse = cloud.getDurationOnUse();


            for(LivingEntity entity : event.getAffectedEntities()) {

                for(PotionEffect effect : effects) {
                    if(processPotionEffectEvent(entity, effect, true) && firstLoop) {
                        normalEffects.remove(effect); // On first run, remove all handled effects from this list for comparison.
                    }
                }

                for (PotionEffect effect : normalEffects) {
                    applyVanillaEffect(entity, effect, true);
                }

                // Radius/duration code stolen from Spigot because otherwise the usages would never count.
                // The usages of .remove() are part of said code, but I'm not sure if they'll work as intended when used partway through.
                if (cloudRadiusOnUse != 0.0F) {
                    cloud.setRadius(cloud.getRadius() + cloudRadiusOnUse);
                    if(cloud.getRadius() < 0/5F) {
                        cloud.remove();
                        break;
                    }
                }
                if (cloudDurationOnUse != 0) {
                    cloud.setDuration(cloud.getDuration() + cloudDurationOnUse);
                    if (cloud.getDuration() <= 0) {
                        cloud.remove();
                        break;
                    }
                }

                firstLoop = false; // This will get called redundantly, but it's better than calling equals() redundantly.
            }
            event.getAffectedEntities().clear(); // Event isn't cancellable, and the list is mutable... makes no sense, and will break multiple plugins using this event, but it's all we got.
        }

        // PotionSplash listener to apply the effects from a Splash potion, since the entities it hit aren't known until it does.
        @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
        public void onPotionSplash(PotionSplashEvent event) {
            ThrownPotion potion = event.getPotion();

            // 1.9 now uses NBT for all potion stuff, but the base effect is separated. Sad trombone.
            PotionMeta pMeta = (PotionMeta) potion.getItem().getItemMeta();
            List<PotionEffect> effects = new ArrayList<>(pMeta.getCustomEffects()); // Get custom effects

            PotionEffect vanillaEffect = getPotionEffect(pMeta.getBasePotionData()); // Get vanilla effect
            if(vanillaEffect != null)
                effects.add(vanillaEffect); // Vanilla effect can be null; apply if not null


            List<PotionEffect> normalEffects = new ArrayList<>(effects); // Just in case there are effects we don't handle, I'd rather not ignore them.

            boolean firstLoop = true; // The easiest way to determine first loop without duplicating the entire loop code.

            for(LivingEntity entity : event.getAffectedEntities()) {
                double intensity = event.getIntensity(entity);

                for(PotionEffect effect : effects) {
                    if(processPotionEffectEvent(entity, effect, intensity) && firstLoop) {
                        normalEffects.remove(effect); // On first run, remove all handled effects from this list for comparison.
                    }
                }

                if(firstLoop && effects.equals(normalEffects)) // On first run, check if we did anything and if not, stop.
                    return;

                for (PotionEffect effect : normalEffects) {
                    applyVanillaEffect(entity, effect, intensity);
                }

                firstLoop = false; // This will get called redundantly, but it's better than calling equals() redundantly.
            }
            event.setCancelled(true);
        }

        // PlayerInteractEvent doesn't fire if they're not looking at a block, so we have to stop the launch
        @EventHandler
        public void onProjectileLaunch(ProjectileLaunchEvent event) {
            Projectile projectile = event.getEntity();

            if(!(projectile instanceof ThrownPotion) || !(projectile.getShooter() instanceof Player))
                return;

            ThrownPotion thrownPotion = (ThrownPotion) projectile;
            Player player = (Player) projectile.getShooter();
            ItemStack potionItem = thrownPotion.getItem();

            if(!(potionItem.getItemMeta() instanceof PotionMeta)) {
                event.setCancelled(true);
                Messaging.send(player, "This Potion has no PotionMeta - please report this for investigation!");
                return; // I don't trust this not to happen
            }

            PotionMeta pMeta = (PotionMeta) potionItem.getItemMeta();

            // Add custom effects, then add the effect as defined by the Base PotionData
            List<PotionEffect> effects = pMeta.getCustomEffects();

            PotionEffect vanillaEffect = getPotionEffect(pMeta.getBasePotionData()); // Get vanilla effect
            if(vanillaEffect != null)
                effects.add(vanillaEffect); // Vanilla effect can be null; apply if not null

            // ThrownPotion only includes Splash and Lingering, as of 1.9.
            // Checking Lingering is arbitrary, could check splash instead, but assume the other since there's just two.
            if(!canUsePotion(player, effects, thrownPotion instanceof LingeringPotion ? 2 : 1)) {
                event.setCancelled(true);

                // In Creative, potions aren't consumed on use, so this just spams potion drops
                if(player.getGameMode() != GameMode.CREATIVE)
                    player.getWorld().dropItem(player.getLocation(), potionItem);
            }
        }

        // In the case of a non-splash potion, it's always full effect, so shortcut method.
        private boolean processPotionEffectEvent(LivingEntity entity, PotionEffect effect) {
            return processPotionEffectEvent(entity, effect, 1, false);
        }

        // In the case of a splash potion, it's not lingering, so shortcut method.
        private boolean processPotionEffectEvent(LivingEntity entity, PotionEffect effect, double intensity) {
            return processPotionEffectEvent(entity, effect, intensity, false);
        }

        // In the case of a lingering potion, it's always full effect, so shortcut method.
        private boolean processPotionEffectEvent(LivingEntity entity, PotionEffect effect, boolean lingering) {
            return processPotionEffectEvent(entity, effect, 1, lingering);
        }

        // Takes the potion use from ItemConsume or PotionSplash and broadcasts it as an event. Returns true if the potion effect is cancelled, false if it stays.
        private boolean processPotionEffectEvent(LivingEntity entity, PotionEffect effect, double intensity, boolean lingering) {
            // Reflection used so that if SkillCustomPotion doesn't exist, SkillPotion will still work (albeit with Vanilla effects)
            try {
                Skill skill = plugin.getSkillManager().getSkill("CustomPotion");
                if (skill == null || customPotionMethod == null) {
                    return false;
                }

                return (Boolean) customPotionMethod.invoke(skill, entity, effect, intensity, lingering);
            }
            catch(IllegalAccessException ex) {
                return false;
            }
            catch(InvocationTargetException ex) {
                ex.printStackTrace(); // An error in the execution of the method should probably still be printed
                return false;
            }
        }

        // Shortcut for drank potions
        public void applyVanillaEffect(LivingEntity entity, PotionEffect effect) {
            applyVanillaEffect(entity, effect, 1, false);
        }

        // Shortcut for splash potions
        public void applyVanillaEffect(LivingEntity entity, PotionEffect effect, double intensity) {
            applyVanillaEffect(entity, effect, intensity, false);
        }

        // Shortcut for lingering potions
        // Lingering is 1/4 duration of normal potion length, or 1/2 of effect if instant
        public void applyVanillaEffect(LivingEntity entity, PotionEffect effect, boolean lingering) {
            applyVanillaEffect(entity, effect, 1, lingering);
        }

        // Apply a vanilla effect in the case that we cancel an event but there's still vanilla effects to be applied
        public void applyVanillaEffect(LivingEntity entity, PotionEffect effect, double intensity, boolean lingering) { // splash var because dmg pots operate slightly different depending on it and PotionEffect doesn't include this
            PotionEffectType type = effect.getType();

            double instantLingerMult = 0.5;
            double durationLingerMult = 0.25;

            if(lingering) {
                CharacterTemplate ct = plugin.getCharacterManager().getCharacter(entity);
                if (ct instanceof Hero) {
                    Hero hero = (Hero) ct;
                    instantLingerMult = SkillConfigManager.getUseSetting(hero, skill, "lingering-multiplier.instant", 0.5, true);
                    durationLingerMult = SkillConfigManager.getUseSetting(hero, skill, "lingering-multiplier.duration", 0.25, true);
                } else {
                    // Converting numbers from String because getRaw only supports String and Boolean
                    instantLingerMult = Double.parseDouble(SkillConfigManager.getRaw(plugin.getSkillManager().getSkill("Potion"), "lingering-multiplier.instant", "0.5"));
                    durationLingerMult = Double.parseDouble(SkillConfigManager.getRaw(plugin.getSkillManager().getSkill("Potion"), "lingering-multiplier.duration", "0.25"));
                }
            }

            if(type.isInstant()) { // Code for instant pot detection from Spigot, though it assumes only HARM/HEAL it accounts for undead
                boolean undead = Util.isUndead(plugin, entity);

                if((!type.getName().equals("HEAL") || undead) && (!type.getName().equals("HARM") || !undead)) { // Can't do a type == PotionEffectType.HARM/HEAL for some odd reason
                    if(type.getName().equals("HARM") && !undead || type.getName().equals("HEAL") && undead) { // Damage potion effect
                        double damage = Math.floor(intensity * (double) (6 << effect.getAmplifier()) + 0.5D);
                        if(lingering)
                            damage *= instantLingerMult;

                        entity.damage(damage); // This fires an EntityDamageEvent on its own, not necessary to fire an event like it is with Healing
                    }
                }
                else { // Healing potion effect
                    if(entity.getHealth() <= 0.0) return; // Not good to run a RegainHealthEvent on a dead thing
                    double healing = Math.floor(intensity * (double) (4 << effect.getAmplifier()) + 0.5D);
                    if(lingering)
                        healing *= instantLingerMult;

                    EntityRegainHealthEvent event = new EntityRegainHealthEvent(entity, healing, EntityRegainHealthEvent.RegainReason.MAGIC); // Logic for health calculation taken from Spigot
                    Bukkit.getPluginManager().callEvent(event);

                    if(!event.isCancelled()) {
                        double healed = entity.getHealth() + event.getAmount();
                        double max = entity.getMaxHealth();
                        entity.setHealth(healed <= max ? healed : max);
                    }
                }
            }
            else { // Not instant, just apply the effect with a properly trimmed duration
                int duration = getPotionDuration(effect, intensity);
                if (lingering)
                    duration *= durationLingerMult;

                entity.addPotionEffect(new PotionEffect(type, duration, effect.getAmplifier(), effect.isAmbient(), effect.hasParticles()), calculateForce(entity, effect, duration));
            }
        }

        // Checks whether to force a vanilla effect or not, to mimic normal logic.
        private boolean calculateForce(LivingEntity entity, PotionEffect effect, int newDuration) {
            PotionEffectType type = effect.getType();
            if (!entity.hasPotionEffect(type)) return false;

            for (PotionEffect curEffect : entity.getActivePotionEffects()) { // Bukkit API does not seem to include a way to grab an effect of a type, just check if there is one
                if (!curEffect.getType().getName().equals(type.getName())) continue;

                if (curEffect.getAmplifier() > effect.getAmplifier()) return false; // The effect refreshes if it's the same or higher amplifier

                if (curEffect.getDuration() > newDuration) return false;
            }

            return true;
        }

        private PotionEffect getPotionEffect(PotionData data) {
            PotionType type = data.getType();
            PotionEffectType effectType = type.getEffectType();

            if(effectType == null)
                return null; // Type can be null, so forward that null onward

            boolean upgraded = type.isUpgradeable() && data.isUpgraded();
            boolean extended = type.isExtendable() && data.isExtended();
            int duration = potionDurations.containsKey(effectType.getName()) ? potionDurations.get(effectType.getName()) : 90000; // 4500 seconds

            if(upgraded)
                duration *= 0.5; // 1/2 duration
            if(extended)
                duration *= (8.0/3.0); // 8/3 duration

            // According to Vanilla commands, it's 0 and 1 for intensity
            return new PotionEffect(effectType, duration, upgraded ? 1 : 0);
        }

    }

    static {
        tierIIPotions = new ArrayList<>();

        tierIIPotions.add("SPEED");
        tierIIPotions.add("SLOW");
        tierIIPotions.add("FAST_DIGGING");
        tierIIPotions.add("SLOW_DIGGING");
        tierIIPotions.add("INCREASE_DAMAGE");
        tierIIPotions.add("HEAL");
        tierIIPotions.add("HARM");
        tierIIPotions.add("JUMP");
        tierIIPotions.add("REGENERATION");
        tierIIPotions.add("DAMAGE_RESISTANCE");
        tierIIPotions.add("HUNGER");
        tierIIPotions.add("WEAKNESS");
        tierIIPotions.add("POISON");
        tierIIPotions.add("WITHER");
        tierIIPotions.add("HEALTH_BOOST");
        tierIIPotions.add("ABSORPTION");
        tierIIPotions.add("SATURATION");
        tierIIPotions.add("LEVITATION");
        tierIIPotions.add("LUCK");
        tierIIPotions.add("UNLUCK");

        // Spigot 1.9 API doesn't actually let me get a duration for a potion's base effect; just if it's extended or tier II.
        // Thus, a table of all known default durations will be stored here, until (hopefully) it's not stupid any more.
        potionDurations = new HashMap<>(); // Tick values

        potionDurations.put("SPEED", 3600);
        potionDurations.put("SLOW", 1800);

        potionDurations.put("INCREASE_DAMAGE", 3600);
        potionDurations.put("HEAL", 1); // Instant
        potionDurations.put("HARM", 1); // Instant
        potionDurations.put("JUMP", 3600);
        potionDurations.put("REGENERATION", 900);
        potionDurations.put("WEAKNESS", 1800);
        potionDurations.put("POISON", 900);
        potionDurations.put("LUCK", 6000);

        potionDurations.put("FIRE_RESISTANCE", 3600);
        potionDurations.put("INVISIBILITY", 3600);
        potionDurations.put("NIGHT_VISION", 3600);

        // These remaining effects have no default, as they aren't on potions. Only applies on the base effect of a potion.
        // Using 0:45 as a default for negative effects, 3:00 as a default for positive effects.
        potionDurations.put("FAST_DIGGING", 3600);
        potionDurations.put("SLOW_DIGGING", 900);
        potionDurations.put("DAMAGE_RESISTANCE", 3600);
        potionDurations.put("HUNGER", 900);
        potionDurations.put("HEALTH_BOOST", 3600);
        potionDurations.put("ABSORPTION", 3600);
        potionDurations.put("SATURATION", 3600);
        potionDurations.put("LEVITATION", 900);
        potionDurations.put("WITHER", 900);
        potionDurations.put("UNLUCK", 900);

        potionDurations.put("BLINDNESS", 900);
        potionDurations.put("CONFUSION", 900);
        potionDurations.put("GLOWING", 900);
        potionDurations.put("WATER_BREATHING", 3600);

        // Cache the reflection for later efficiency since it'll get called a large amount. Blame soren if this is bad.
        Skill skill = Heroes.getInstance().getSkillManager().getSkill("CustomPotion");
        if (skill != null) {
            Class<?> skillClass = skill.getClass();
            try {
                customPotionMethod = skillClass.getDeclaredMethod("applyPotionEffect", LivingEntity.class, PotionEffect.class, double.class, boolean.class);
            }
            catch (NoSuchMethodException ex) {
                // This space intentionally left blank.
            }
        }
    }
}
