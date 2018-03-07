package com.herocraftonline.heroes.characters.skill.public1;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
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
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.nms.NMSHandler;
import com.herocraftonline.heroes.util.Util;

public class SkillEnderPearls extends PassiveSkill {

    public SkillEnderPearls(Heroes plugin) {
        super(plugin, "EnderPearls");
        setDescription("You can throw ender pearls! $1");
        setArgumentRange(0, 0);
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.ABILITY_PROPERTY_PROJECTILE, SkillType.TELEPORTING);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        boolean hasCombatCooldown = SkillConfigManager.getUseSetting(hero, this, "cooldown-during-combat", true);
        int cdDuration = SkillConfigManager.getUseSetting(hero, this, "combat-toss-cooldown", 60000, false);

        String combatCooldownString = "";
        if (hasCombatCooldown) {
            String formattedCooldown = Util.decFormat.format(cdDuration / 1000.0);
            combatCooldownString = "If you are in combat when throwing an ender pearl, you will not be able to throw another for " + formattedCooldown + " seconds.";
        }

        return getDescription().replace("$1", combatCooldownString);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.APPLY_TEXT.node(), "");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "");
        node.set("vertical-leniency", 2);
        node.set("velocity-multiplier", 0.85);
        node.set("cooldown-during-combat", true);
        node.set("combat-toss-cooldown", 60000);

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
            ItemStack itemInHand = NMSHandler.getInterface().getItemInMainHand(player.getInventory());

            if (itemInHand.getType() == Material.ENDER_PEARL) {
                if (!hero.canUseSkill(skill)) {
                    player.sendMessage("You are not trained to use Ender Pearls!");
                    event.setUseItemInHand(Result.DENY);
                    return;
                }

                if (hero.hasEffect("EnderPearlUsageCooldownEffect")) {
                    long remainingTime = ((ExpirableEffect) hero.getEffect("EnderPearlUsageCooldownEffect")).getRemainingTime();
                    player.sendMessage(ChatComponents.GENERIC_SKILL + "You must wait " + ChatColor.WHITE + Util.decFormatCDs.format(remainingTime / 1000.0) + ChatColor.GRAY + "s before you can throw another Ender Pearl.");
                    event.setUseItemInHand(Result.DENY);
                    return;
                }

                boolean hasCombatCooldown = SkillConfigManager.getUseSetting(hero, skill, "cooldown-during-combat", true);
                int cdDuration = SkillConfigManager.getUseSetting(hero, skill, "combat-toss-cooldown", 5000, false);
                boolean applyCooldown = false;
                if (hasCombatCooldown) {
                    if (hero.isInCombat())
                        applyCooldown = true;
                }

                CooldownEffect cdEffect = new CooldownEffect(skill, player, cdDuration);

                // If the clicked block is null, we are clicking air. Air is a valid block that we do not need to validate
                if (event.getClickedBlock() != null) {

                    // NON-AIR BLOCK, VALIDATE USAGE
                    if (Util.interactableBlocks.contains(event.getClickedBlock().getType())) {
                        // Dealing with an interactable block. Let them interact with that block instead of throwing the ender pearl.
                        event.setUseItemInHand(Result.DENY);
                    }
                    else {
                        if (applyCooldown) {
                            // The ender pearl will be used in this case. Let's add the cooldown effect to them.
                            hero.addEffect(cdEffect);
                        }
                    }
                }
                else {
                    // AIR BLOCK. NO BLOCK VALIDATION

                    if (applyCooldown) {
                        // The ender pearl will be used in this case. Let's add the cooldown effect to them.
                        hero.addEffect(cdEffect);
                    }
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

            double velocityMultiplier = SkillConfigManager.getUseSetting(hero, skill, "velocity-multiplier", 0.75, false);
            EnderPearl enderPearl = (EnderPearl) event.getEntity();
            double yVel = enderPearl.getVelocity().getY();
            enderPearl.setVelocity(enderPearl.getVelocity().multiply(velocityMultiplier).setY(yVel));
        }

        // Ender pearls are rather...exploity. This event will watch the teleports and moderate them.
        @EventHandler(priority = EventPriority.HIGHEST)
        public void onPlayerTeleport(PlayerTeleportEvent event) {
            if (event.getCause() == TeleportCause.ENDER_PEARL) {

                event.setCancelled(true);       // Cancel the event because we don't want players to be dealt "ender pearl damage"

                Hero hero = plugin.getCharacterManager().getHero(event.getPlayer());
                if (hero.hasEffectType(EffectType.ROOT)) {
                    event.getPlayer().sendMessage(ChatComponents.GENERIC_SKILL + "You cannot teleport while rooted!");
                    return;
                }

                // Store some of the original teleport location variables
                Location teleportLoc = event.getTo();
                Block teleportLocBlock = teleportLoc.getBlock();
                Material teleportLocBlockType = teleportLocBlock.getType();
                float pitch = teleportLoc.getPitch();
                float yaw = teleportLoc.getYaw();

                int verticalLeniency = SkillConfigManager.getUseSetting(hero, skill, "vertical-leniency", 2, false);

                boolean validLocation = false;
                int i = 0;
                do {
                    if (Util.transparentBlocks.contains(teleportLocBlockType)) {
                        if (Util.transparentBlocks.contains(teleportLocBlock.getRelative(BlockFace.UP).getType())) {
                            validLocation = true;
                            break;
                        }

                        // Give them a block of wiggle room if we've got an invalid location.
                        if (Util.transparentBlocks.contains(teleportLocBlock.getRelative(BlockFace.DOWN).getType())) {
                            teleportLoc = teleportLoc.subtract(0, 1, 0);
                            teleportLocBlock = teleportLoc.getBlock();
                            teleportLocBlockType = teleportLocBlock.getType();
                        }
                        else
                            break;
                    }
                    i++;
                }
                while (i <= verticalLeniency);

                if (!validLocation) {
                    // Going up, we only ever need 1 block of vertical leniency, so just do a single block check.
                    teleportLoc = event.getTo().add(0, 1, 0);
                    teleportLocBlock = teleportLoc.getBlock();
                    teleportLocBlockType = teleportLocBlock.getType();

                    if (Util.transparentBlocks.contains(teleportLocBlockType) && Util.transparentBlocks.contains(teleportLocBlock.getRelative(BlockFace.UP).getType()))
                        validLocation = true;
                }

                if (!validLocation) {
                    event.getPlayer().sendMessage(ChatComponents.GENERIC_SKILL + "A mysterious force prevents you from teleporting to your ender pearl location.");
                    return;
                }

                // Move our location to the block it landed on, and center it. This fixes several exploit issues.
                teleportLoc = teleportLoc.getBlock().getLocation().clone();
                teleportLoc.add(new Vector(.5, .5, .5));
                teleportLocBlock = teleportLoc.getBlock();

                teleportLoc.setPitch(pitch);
                teleportLoc.setYaw(yaw);

                event.getPlayer().teleport(teleportLoc);    // Manually teleport the player.
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
