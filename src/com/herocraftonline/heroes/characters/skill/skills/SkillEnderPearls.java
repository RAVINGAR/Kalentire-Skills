package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

public class SkillEnderPearls extends PassiveSkill {

    public SkillEnderPearls(Heroes plugin) {
        super(plugin, "EnderPearls");
        setDescription("You can throw ender pearls once every $1 seconds.");
        setArgumentRange(0, 0);
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.ABILITY_PROPERTY_PROJECTILE, SkillType.TELEPORTING);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        int cdDuration = SkillConfigManager.getUseSetting(hero, this, "toss-cooldown", Integer.valueOf(5000), false);

        String formattedCooldown = Util.decFormatCDs.format(cdDuration / 1000.0);

        return getDescription().replace("$1", formattedCooldown);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.APPLY_TEXT.node(), "");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "");
        node.set("velocity-multiplier", Double.valueOf(0.85));
        node.set("toss-cooldown", Integer.valueOf(0));

        return node;
    }

    public class SkillEntityListener implements Listener {

        private Skill skill;

        public SkillEntityListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler
        public void onPlayerInteract(PlayerInteractEvent event) {

            // Make sure the player is right clicking.
            if (!(event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK))
                return;

            // Make sure we're dealing with an item use event
            if (!event.hasItem())
                return;

            Player player = event.getPlayer();
            Hero hero = plugin.getCharacterManager().getHero(player);
            ItemStack itemInHand = player.getItemInHand();

            if (itemInHand.getType() == Material.ENDER_PEARL) {
                if (!hero.canUseSkill(skill)) {
                    Messaging.send(player, "You are not able to use Ender Pearls!");
                    event.setUseItemInHand(Result.DENY);
                    return;
                }

                if (hero.hasEffect("EnderPearlUsageCooldownEffect")) {
                    long remainingTime = ((ExpirableEffect) hero.getEffect("EnderPearlUsageCooldownEffect")).getRemainingTime();
                    Messaging.send(player, Messaging.getSkillDenoter() + "You must wait " + ChatColor.WHITE + Util.decFormat.format(remainingTime / 1000.0) + ChatColor.GRAY + "s before you can throw another Ender Pearl.");
                    event.setUseItemInHand(Result.DENY);
                    return;
                }

                int cdDuration = SkillConfigManager.getUseSetting(hero, skill, "toss-cooldown", Integer.valueOf(5000), false);

                // If the clicked block is null, we are clicking air. Air is a valid block that we do not need to validate
                if (event.getClickedBlock() != null) {

                    // NON-AIR BLOCK, VALIDATE USAGE
                    if (Util.interactableBlocks.contains(event.getClickedBlock().getType())) {
                        // Dealing with an interactable block. Let them interact with that block instead of throwing the ender pearl.
                        event.setUseItemInHand(Result.DENY);
                        return;
                    }
                    else {
                        // The ender pearl will be used in this case. Let's add the cooldown effect to them.
                        CooldownEffect cdEffect = new CooldownEffect(skill, player, cdDuration);
                        hero.addEffect(cdEffect);
                    }
                }
                else {
                    // AIR BLOCK. NO BLOCK VALIDATION

                    // The ender pearl will be used in this case. Let's add the cooldown effect to them.
                    CooldownEffect cdEffect = new CooldownEffect(skill, player, cdDuration);
                    hero.addEffect(cdEffect);
                }
            }
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onProjectileLaunch(ProjectileLaunchEvent event) {
            if (!(event.getEntity() instanceof EnderPearl))
                return;

            if (!(event.getEntity().getShooter() instanceof Player))
                return;

            Player player = (Player) event.getEntity().getShooter();
            Hero hero = plugin.getCharacterManager().getHero(player);

            double velocityMultiplier = SkillConfigManager.getUseSetting(hero, skill, "velocity-multiplier", Double.valueOf(0.75), false);
            EnderPearl enderPearl = (EnderPearl) event.getEntity();
            enderPearl.setVelocity(enderPearl.getVelocity().multiply(velocityMultiplier));
        }

        // Ender pearls are rather...exploity. This event will watch the teleports and moderate them.
        @EventHandler(priority = EventPriority.HIGHEST)
        public void onPlayerTeleport(PlayerTeleportEvent event) {
            if (event.getCause() == TeleportCause.ENDER_PEARL) {

                Hero hero = plugin.getCharacterManager().getHero(event.getPlayer());
                if (hero.hasEffect("Root")) {
                    event.setCancelled(true);
                    Messaging.send(event.getPlayer(), Messaging.getSkillDenoter() + "You cannot teleport while rooted!");
                }

                // Store some of the original teleport location variables
                Location teleportLoc = event.getTo();
                float pitch = teleportLoc.getPitch();
                float yaw = teleportLoc.getYaw();

                // Move our location to the block it landed on, and center it. This fixes most exploit issues.
                teleportLoc = teleportLoc.getBlock().getLocation().clone();
                teleportLoc.add(new Vector(.5, 0, .5));

                teleportLoc.setPitch(pitch);
                teleportLoc.setYaw(yaw);

                Block teleportLocBlock = teleportLoc.getBlock();

                // Some block types are still relatively exploitable however, so we should check against those.
                switch (teleportLocBlock.getType()) {
                    case IRON_DOOR:
                    case IRON_DOOR_BLOCK:
                    case WOODEN_DOOR:
                    case IRON_FENCE:
                    case FENCE:
                    case THIN_GLASS:
                    case GLASS:
                        // Cancel immediately when dealing with exploitable blocks.
                        event.setCancelled(true);
                        Messaging.send(event.getPlayer(), Messaging.getSkillDenoter() + "A mysterious force prevents you from teleporting to your ender pearl location.");
                        break;
                    default:
                        event.setCancelled(true);       // Cancel the event because we don't want players to be dealt "ender pearl damage"
                        event.getPlayer().teleport(teleportLoc);    // Manually teleport the player.
                        break;
                }
                return;
            }
        }
    }

    // Effect required for implementing an internal cooldown on healing
    private class CooldownEffect extends ExpirableEffect {
        public CooldownEffect(Skill skill, Player applier, long duration) {
            super(skill, "EnderPearlUsageCooldownEffect", applier, duration);
        }
    }
}
