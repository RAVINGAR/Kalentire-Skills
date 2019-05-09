package com.herocraftonline.heroes.characters.skill.reborn.ninja;

import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicEffect;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.heroes.nms.scoreboard.TeamScoreboard;
import com.herocraftonline.heroes.nms.scoreboard.TeamScoreboardPacket;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
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
import com.herocraftonline.heroes.util.Util;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SkillSneak extends ActiveSkill {
    private static String toggleableEffectName = toggleableEffectName;
    private boolean damageCancels;
    private boolean attackCancels;

    public SkillSneak(Heroes plugin) {
        super(plugin, "Sneak");
        setDescription("You crouch into the shadows.");
        setUsage("/skill sneak");
        setIdentifiers("skill sneak", "skill stealth");
        setArgumentRange(0, 0);
        setTypes(SkillType.BUFFING, SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.STEALTHY);

        setToggleableEffectName(toggleableEffectName);
        //GhostManager ghostManager = new GhostManager(plugin);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillEventListener(), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection config = super.getDefaultConfig();
        config.set("damage-cancels", true);
        config.set("attacking-cancels", true);
        config.set("refresh-interval", 5000);
        return config;
    }

    @Override
    public void init() {
        super.init();
        damageCancels = SkillConfigManager.getRaw(this, "damage-cancels", true);
        attackCancels = SkillConfigManager.getRaw(this, "attacking-cancels", true);
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        if (hero.hasEffect(toggleableEffectName)) {
            hero.removeEffect(hero.getEffect(toggleableEffectName));
            return SkillResult.REMOVED_EFFECT;
        }

        final int period = SkillConfigManager.getUseSetting(hero, this, "refresh-interval", 5000, true);
        Player player = hero.getPlayer();
        hero.addEffect(new SneakEffect(this, player, period));
        return SkillResult.NORMAL;
    }

    public class SneakEffect extends PeriodicEffect {

        private String applyText;
        private String expireText;

        private boolean vanillaSneaking;

        public SneakEffect(Skill skill, Player applier, int period) {
            this(skill, toggleableEffectName, applier, period, ChatComponents.GENERIC_SKILL + "You are now sneaking", ChatComponents.GENERIC_SKILL + "You are no longer sneaking");
        }

        public SneakEffect(Skill skill, String name, Player applier, int period) {
            this(skill, name, applier, period, ChatComponents.GENERIC_SKILL + "You are now sneaking", ChatComponents.GENERIC_SKILL + "You are no longer sneaking");
        }

        public SneakEffect(Skill skill, Player applier, int period, String applyText, String expireText) {
            this(skill, toggleableEffectName, applier, period, applyText, expireText);
        }

        public SneakEffect(Skill skill, String name, Player applier, long period, String applyText, String expireText) {
            super(skill, name, applier, period, null, null);      // Don't use standard apply/expire text. We'll use our own here.

            this.types.add(EffectType.BENEFICIAL);
            this.types.add(EffectType.PHYSICAL);
            this.types.add(EffectType.SNEAK);
            this.types.add(EffectType.SILENT_ACTIONS);

            this.applyText = applyText;
            this.expireText = expireText;

            this.setVanillaSneaking(applier.isSneaking());
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            final Player player = hero.getPlayer();
            player.setSneaking(true);

            if (this.applyText != null && this.applyText.length() > 0) {
                player.sendMessage(ChatColor.GRAY + "    " + this.applyText);
            }
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            final Player player = hero.getPlayer();
            player.setSneaking(this.vanillaSneaking);

            if (this.expireText != null && this.expireText.length() > 0) {
                player.sendMessage(ChatColor.GRAY + "    " + this.expireText);
            }
        }

        @Override
        public void tickHero(Hero hero) {
            Player player = hero.getPlayer();

            player.setSneaking(false);
            player.setSneaking(true);
        }

        @Override
        public void tickMonster(Monster monster) {
        }

        public boolean isVanillaSneaking() {
            return this.vanillaSneaking;
        }

        public void setVanillaSneaking(boolean vanillaSneaking) {
            this.vanillaSneaking = vanillaSneaking;
        }
    }

    public class SkillEventListener implements Listener {

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onEntityDamage(EntityDamageEvent event) {
            if (!damageCancels || (event.getDamage() == 0)) {
                return;
            }

            Player player;
            if (event.getEntity() instanceof Player) {
                player = (Player) event.getEntity();
                final Hero hero = plugin.getCharacterManager().getHero(player);
                if (hero.hasEffect(toggleableEffectName)) {
                    player.setSneaking(false);
                    hero.removeEffect(hero.getEffect(toggleableEffectName));
                }
            }

            player = null;
            if (attackCancels && (event instanceof EntityDamageByEntityEvent)) {
                final EntityDamageByEntityEvent subEvent = (EntityDamageByEntityEvent) event;
                if (subEvent.getDamager() instanceof Player) {
                    player = (Player) subEvent.getDamager();
                } else if (subEvent.getDamager() instanceof Projectile) {
                    if (((Projectile) subEvent.getDamager()).getShooter() instanceof Player) {
                        player = (Player) ((Projectile) subEvent.getDamager()).getShooter();
                    }
                }

                if (player != null) {
                    final Hero hero = plugin.getCharacterManager().getHero(player);
                    if (hero.hasEffect(toggleableEffectName)) {
                        player.setSneaking(false);
                        hero.removeEffect(hero.getEffect(toggleableEffectName));
                    }
                }
            }
        }

        @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
        public void onPlayerInteract(PlayerInteractEvent event) {

            // Try to force a right click event to work when the player is sneaking.
            if (!(event.getAction() == Action.RIGHT_CLICK_BLOCK) || !(event.hasItem()))
                return;
            if (event.getClickedBlock() == null)
                return;

            ItemStack activatedItem = event.getItem();

            // Check to see if the player is right clicking a block with a sword.
            if (!Util.swords.contains(activatedItem.getType().name()))
                return;

            if (!Util.interactableBlocks.contains(event.getClickedBlock().getType()))
                return;

            Hero hero = plugin.getCharacterManager().getHero(event.getPlayer());
            if (hero.hasEffect(toggleableEffectName)) {
                // We need to cancel the "blocking" portion of the sword
                // So that he interacts with the block as normal.
                event.setUseItemInHand(Event.Result.DENY);
            }
        }

        @EventHandler(priority = EventPriority.HIGHEST)
        public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
            final Hero hero = plugin.getCharacterManager().getHero(event.getPlayer());
            if (hero.hasEffect(toggleableEffectName)) {
                SneakEffect sEffect = (SneakEffect) hero.getEffect(toggleableEffectName);

                // Messaging.send(hero.getPlayer(), "Sneak Toggle Event. Switching to sneak == " + event.isSneaking());	// DEBUG
                if (!event.isSneaking()) {
                    // Messaging.send(hero.getPlayer(), "Player is leaving sneak. Setting vanilla to false.");	// DEBUG
                    sEffect.setVanillaSneaking(false);
                    event.setCancelled(true);
                } else {
                    // Messaging.send(hero.getPlayer(), "Player is entering sneak. Setting vanilla to true.");	// DEBUG
                    sEffect.setVanillaSneaking(true);
                }
            }
        }
    }
}
