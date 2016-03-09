package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;

public class SkillPotion extends PassiveSkill {

    private static final List<String> tierIIPotions;

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

        for (PotionEffectType type : PotionEffectType.values()) {
            if(type == null)
                continue; // Docs for .values() say can be any order and contain null, which it totally does

            String potionName = type.getName();

            node.set("allow." + potionName, false);
            node.set("cooldown." + potionName, 180000);

            node.set("allow.SPLASH-" + potionName, false);
            node.set("cooldown.SPLASH-" + potionName, 180000);
            
            if(tierIIPotions.contains(potionName)) {
                node.set("allow." + potionName + "-II", false);
                node.set("cooldown." + potionName + "-II", 180000);

                node.set("allow.SPLASH-" + potionName + "-II", false);
                node.set("cooldown.SPLASH-" + potionName + "-II", 180000);
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
        return getPotionDuration(effect.getDuration(),  intensity, millis);
    }

    // More overloads, int and no specified millis
    public static int getPotionDuration(int duration, double intensity) {
        return getPotionDuration(duration,  intensity, false);
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
                Potion potion = Potion.fromItemStack(item);
                Player player = event.getPlayer();

                if(!canUsePotion(player, item)) {
                    event.setCancelled(true);
                    return;
                }

                // Custom effects are separately added due to code for getEffects() showing it calculated by dmg value and thus customs in NBT are ignored.
                List<PotionEffect> effects = new ArrayList<>(potion.getEffects()); // Adds vanilla effects
                effects.addAll(((PotionMeta) item.getItemMeta()).getCustomEffects()); // Adds custom effects

                List<PotionEffect> normalEffects = new ArrayList<>(effects); // Just in case there are effects we don't handle, I'd rather not ignore them


                for(PotionEffect effect : effects) {
                    if(processPotionEffectEvent(player, effect))
                        normalEffects.remove(effect);
                }

                if(!effects.equals(normalEffects)) { // If we did stuff with the potion, apply any remaining normal effects and make sure it's a bottle now.
                    for(PotionEffect effect : normalEffects)
                        applyVanillaEffect(player, effect);

                    if (item.equals(player.getItemInHand()))
                        player.setItemInHand(new ItemStack(Material.GLASS_BOTTLE)); // Normal
                    else if (player.getInventory().contains(item)) { // Triggers in case of magic or code that makes the current potion invalid, but another one exists.
                        PlayerInventory inventory = player.getInventory();
                        inventory.setItem(inventory.first(item), new ItemStack(Material.GLASS_BOTTLE));
                        Heroes.log(Level.WARNING, "User had no potion in hand slot when used, another existed!");
                    }
                    else // Triggers in cases of magic or code that makes the current bottle invalid, but another one does not exist.
                       Heroes.log(Level.WARNING, "User had no potion in hand slot when used, another did NOT exist!");

                    event.setItem(new ItemStack(Material.GLASS_BOTTLE)); // In case something un-cancels it, though it'll glitch a tad.
                    event.setCancelled(true); // This prevents the aforementioned glitch.
                }
            }
            // Not a potion, no care was given that day
        }

        // Determines from all the effects on a potion, whether its use is permitted and checks/sets cooldowns.
        private boolean canUsePotion(Player player, ItemStack activatedItem) {
            Hero hero = plugin.getCharacterManager().getHero(player);

            // see if the player can use potions at all
            if (!hero.canUseSkill(skill) || (hero.isInCombat() && SkillConfigManager.getUseSetting(hero, skill, SkillSetting.NO_COMBAT_USE, false))) {
                Messaging.send(player, "You can't use this potion!");
                return false;
            }

            // get potion and its effects
            Potion potion = Potion.fromItemStack(activatedItem);
            // Custom effects are separately added due to code for getEffects() showing it calculated by dmg value and thus customs in NBT are ignored.
            List<PotionEffect> effects = new ArrayList<>(potion.getEffects()); // Adds vanilla effects
            effects.addAll(((PotionMeta) activatedItem.getItemMeta()).getCustomEffects()); // Adds custom effects

            // map to store cooldowns so we can ensure all potions are ok, and boolean for splash because it'a always the same
            Map<String, Long> cooldowns = new HashMap<>();
            boolean splash = potion.isSplash();

            // loop 1 to check every potion is allowed
            for(PotionEffect effect : effects) {
                // get the potion type info
                String potionType = effect.getType().getName();
                String potionName = potionType;

                if(splash) potionName = "SPLASH-" + potionName;
                if(tierIIPotions.contains(potionType) && effect.getAmplifier() > 0) potionName += "-II"; // III and higher count as II, and non-II potions always count as I, for sanity reasons.

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

        // PotionSplash listener to apply the effects from a Splash potion, since the entities it hit aren't known until it does.
        @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
        public void onPotionSplash(PotionSplashEvent event) {
            ThrownPotion potion = event.getPotion();
            // Custom effects are separately added due to code for getEffects() showing it calculated by dmg value and thus customs in NBT are ignored.
            List<PotionEffect> effects = new ArrayList<>(event.getPotion().getEffects()); // Adds vanilla effects
            effects.addAll(((PotionMeta) potion.getItem().getItemMeta()).getCustomEffects()); // Adds custom effects

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

            Player player = (Player) projectile.getShooter();
            ItemStack potionItem = ((ThrownPotion) projectile).getItem();

            if(!canUsePotion(player, potionItem)) {
                event.setCancelled(true);

                // In Creative, potions aren't consumed on use, so this just spams potion drops
                if(player.getGameMode() != GameMode.CREATIVE)
                    player.getWorld().dropItem(player.getLocation(), potionItem);
            }
        }

        // In the case of a non-splash potion, it's always full effect, so shortcut method.
        private boolean processPotionEffectEvent(LivingEntity entity, PotionEffect effect) {
            return processPotionEffectEvent(entity, effect, 1);
        }

        // Takes the potion use from ItemConsume or PotionSplash and broadcasts it as an event. Returns true if the potion effect is cancelled, false if it stays.
        private boolean processPotionEffectEvent(LivingEntity entity, PotionEffect effect, double intensity) {
            // Reflection used so that if SkillCustomPotion doesn't exist, SkillPotion will still work (albeit with Vanilla effects)
            try {
                Skill skill = plugin.getSkillManager().getSkill("CustomPotion");
                if (skill == null) {
                    return false;
                }
                Class<?> skillClass = skill.getClass();
                Method skillMethod = skillClass.getDeclaredMethod("applyPotionEffect", LivingEntity.class, PotionEffect.class, double.class);
                return (Boolean) skillMethod.invoke(skill, entity, effect, intensity);
            }
            catch(IllegalAccessException | NoSuchMethodException ex) {
                return false;
            }
            catch(InvocationTargetException ex) {
                ex.printStackTrace(); // An error in the execution of the method should probably still be printed
                return false;
            }
        }

        // More shortcuts for drank potions
        public void applyVanillaEffect(LivingEntity entity, PotionEffect effect) {
            applyVanillaEffect(entity, effect, 1);
        }

        // Apply a vanilla effect in the case that we cancel an event but there's still vanilla effects to be applied
        public void applyVanillaEffect(LivingEntity entity, PotionEffect effect, double intensity) { // splash var because dmg pots operate slightly different depending on it and PotionEffect doesn't include this
            PotionEffectType type = effect.getType();

            if(type.isInstant()) { // Code for instant pot detection from Spigot, though it assumes only HARM/HEAL it accounts for undead
                boolean undead = Util.isUndead(plugin, entity);

                if((!type.getName().equals("HEAL") || undead) && (!type.getName().equals("HARM") || !undead)) { // Can't do a type == PotionEffectType.HARM/HEAL for some odd reason
                    if(type.getName().equals("HARM") && !undead || type.getName().equals("HEAL") && undead) { // Damage potion effect
                        double damage = Math.floor(intensity * (double) (6 << effect.getAmplifier()) + 0.5D);
                        entity.damage(damage); // This fires an EntityDamageEvent on its own, not necessary to fire an event like it is with Healing
                    }
                }
                else { // Healing potion effect
                    if(entity.getHealth() <= 0.0) return; // Not good to run a RegainHealthEvent on a dead thing
                    EntityRegainHealthEvent event = new EntityRegainHealthEvent(entity, Math.floor(intensity * (double) (4 << effect.getAmplifier()) + 0.5D), EntityRegainHealthEvent.RegainReason.MAGIC); // Logic for health calculation taken from Spigot
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

    }

    static {
        tierIIPotions = new ArrayList<>();

        tierIIPotions.add("REGENERATION");
        tierIIPotions.add("SPEED");
        tierIIPotions.add("POISON");
        tierIIPotions.add("HEAL");
        tierIIPotions.add("INCREASE_DAMAGE");
        tierIIPotions.add("HARM");
        tierIIPotions.add("LEAPING");
    }
}
