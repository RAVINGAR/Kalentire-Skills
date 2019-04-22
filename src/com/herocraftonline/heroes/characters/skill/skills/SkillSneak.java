package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.SneakEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Util;

public class SkillSneak extends ActiveSkill {

    private boolean damageCancels;
    private boolean attackCancels;

    public SkillSneak(Heroes plugin) {
        super(plugin, "Sneak");
        setDescription("You crouch into the shadows.");
        setUsage("/skill stealth");
        setArgumentRange(0, 0);
        setIdentifiers("skill sneak");
        setTypes(SkillType.BUFFING, SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.STEALTHY);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillEventListener(), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DURATION.node(), 600000); // 10 minutes in milliseconds
        node.set("damage-cancels", true);
        node.set("attacking-cancels", true);
        node.set("refresh-interval", 5000); // in milliseconds
        return node;
    }

    @Override
    public void init() {
        super.init();
        damageCancels = SkillConfigManager.getRaw(this, "damage-cancels", true);
        attackCancels = SkillConfigManager.getRaw(this, "attacking-cancels", true);
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        if (hero.hasEffect("Sneak")) {
            hero.removeEffect(hero.getEffect("Sneak"));
            return SkillResult.REMOVED_EFFECT;
        }
        else {
            final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 600000, false);
            final int period = SkillConfigManager.getUseSetting(hero, this, "refresh-interval", 5000, true);

            Player player = hero.getPlayer();
            if (player.isSneaking())
                hero.addEffect(new SneakEffect(this, player, period, duration, true));
            else
                hero.addEffect(new SneakEffect(this, player, period, duration, false));
        }
        return SkillResult.NORMAL;
    }

    public class SkillEventListener implements Listener {

        @EventHandler(priority = EventPriority.MONITOR)
        public void onEntityDamage(EntityDamageEvent event) {
            if (event.isCancelled() || !damageCancels || (event.getDamage() == 0)) {
                return;
            }

            Player player;
            if (event.getEntity() instanceof Player) {
                player = (Player) event.getEntity();
                final Hero hero = plugin.getCharacterManager().getHero(player);
                if (hero.hasEffect("Sneak")) {
                    player.setSneaking(false);
                    hero.removeEffect(hero.getEffect("Sneak"));
                }
            }

            player = null;
            if (attackCancels && (event instanceof EntityDamageByEntityEvent)) {
                final EntityDamageByEntityEvent subEvent = (EntityDamageByEntityEvent) event;
                if (subEvent.getDamager() instanceof Player) {
                    player = (Player) subEvent.getDamager();
                }
                else if (subEvent.getDamager() instanceof Projectile) {
                    if (((Projectile) subEvent.getDamager()).getShooter() instanceof Player) {
                        player = (Player) ((Projectile) subEvent.getDamager()).getShooter();
                    }
                }

                if (player != null) {
                    final Hero hero = plugin.getCharacterManager().getHero(player);
                    if (hero.hasEffect("Sneak")) {
                        player.setSneaking(false);
                        hero.removeEffect(hero.getEffect("Sneak"));
                    }
                }
            }
        }

        @EventHandler(priority = EventPriority.LOW)
        public void onPlayerInteract(PlayerInteractEvent event) {

            // Try to force a right click event to work when the player is sneaking.
            if (!(event.getAction() == Action.RIGHT_CLICK_BLOCK) || !(event.hasItem()))
                return;

            // If the clicked block is null, we don't need to check if the block is interactable.
            if (event.getClickedBlock() == null)
                return;

            ItemStack activatedItem = event.getItem();

            // Check to see if the player is right clicking a block with a sword.
            if (Util.swords.contains(activatedItem.getType().name())) {

                // He is. Check to see if the block is interactable.
                if (Util.interactableBlocks.contains(event.getClickedBlock().getType())) {

                    // The block is interactable. Check to see if he's actual sneaking.
                    Hero hero = plugin.getCharacterManager().getHero(event.getPlayer());
                    if (hero.hasEffect("Sneak")) {
                        // We need to cancel the "blocking" portion of the sword
                        // So that he interacts with the block as normal.
                        event.setUseItemInHand(Event.Result.DENY);
                    }
                }
            }
        }

        @EventHandler(priority = EventPriority.HIGHEST)
        public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
            final Hero hero = plugin.getCharacterManager().getHero(event.getPlayer());
            if (hero.hasEffect("Sneak")) {
                SneakEffect sEffect = (SneakEffect) hero.getEffect("Sneak");

                // Messaging.send(hero.getPlayer(), "Sneak Toggle Event. Switching to sneak == " + event.isSneaking());	// DEBUG
                if (!event.isSneaking()) {
                    // Messaging.send(hero.getPlayer(), "Player is leaving sneak. Setting vanilla to false.");	// DEBUG
                    sEffect.setVanillaSneaking(false);
                    event.setCancelled(true);
                }
                else {
                    // Messaging.send(hero.getPlayer(), "Player is entering sneak. Setting vanilla to true.");	// DEBUG
                    sEffect.setVanillaSneaking(true);
                }
            }
        }
    }
}
