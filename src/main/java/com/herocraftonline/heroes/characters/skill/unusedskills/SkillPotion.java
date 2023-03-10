package com.herocraftonline.heroes.characters.skill.unusedskills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SkillPotion extends PassiveSkill {
    private static final Map<Byte, String> regularPotions;
    private static final Map<Byte, String> splashPotions;

    public SkillPotion(Heroes plugin) {
        super(plugin, "Potion");
        setDescription("You are able to use potions!");
        setTypes(SkillType.ITEM_MODIFYING);

        Bukkit.getPluginManager().registerEvents(new PotionListener(this), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.APPLY_TEXT.node(), "");
        node.set(SkillSetting.UNAPPLY_TEXT.node(), "");
        node.set(SkillSetting.LEVEL.node(), 1);
        node.set(SkillSetting.NO_COMBAT_USE.node(), false);

        for (String potion : regularPotions.values()) {
            node.set("allow." + potion, false);
            node.set("cooldown." + potion, 10 * 60000);
        }

        for (String potion : splashPotions.values()) {
            node.set("allow." + potion, false);
            node.set("cooldown." + potion, 10 * 60000);
        }

        return node;
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

    private static boolean isPotion(byte id) {
        return regularPotions.containsKey(id);
    }

    private static boolean isSplashPotion(byte id) {
        return splashPotions.containsKey(id);
    }

    public class PotionListener implements Listener {
        private final Skill skill;

        public PotionListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.LOW)
        public void onPlayerInteract(PlayerInteractEvent event) {
            if (event.useItemInHand() == Event.Result.DENY) {
                return;
            }

            // Make sure the player is right clicking.
            if (!(event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK))
                return;

            if (!event.hasItem())
                return;

            ItemStack activatedItem = event.getItem();

            if (activatedItem.getType() == Material.POTION) {

                Player player = event.getPlayer();

                // If the clicked block is null, we are clicking air. Air is a valid block that we do not need to validate
                if (event.getClickedBlock() != null) {

                    // VALIDATE NON-AIR BLOCK
                    if ((Util.interactableBlocks.contains(event.getClickedBlock().getType()))) {
                        // Prevent us from using the item when clicking an interactable block.
                        event.setUseItemInHand(Event.Result.DENY);
                        return;
                    }
                }

                if (!(canUsePotion(player, activatedItem)))
                    event.setUseItemInHand(Event.Result.DENY);
            }
        }

        public boolean canUsePotion(Player player, ItemStack activatedItem) {
            Hero hero = plugin.getCharacterManager().getHero(player);

            // see if the player can use potions at all
            if (!hero.canUseSkill(skill) || (hero.isInCombat() && SkillConfigManager.getUseSetting(hero, skill, SkillSetting.NO_COMBAT_USE, false))) {
                player.sendMessage("You can't use this potion!");
                return false;
            }

            // get the potion type info
            byte potionId = activatedItem.getData().getData();
            String potionName;
            if (isPotion(potionId)) {
                potionName = regularPotions.get(potionId);
            }
            else if (isSplashPotion(potionId)) {
                potionName = splashPotions.get(potionId);
            }
            else {
                return true;        // Not a proper potion, so don't block them from using it.
            }

            // see if the player can use this type of potion
            if (!SkillConfigManager.getUseSetting(hero, skill, "allow." + potionName, false)) {
                player.sendMessage("You can't use this potion!");
                return false;
            }

            // trim the rank and splash from the potion name
            String potionType = potionName;
            if (potionName.endsWith("-II")) {
                potionType = potionType.substring(0, potionName.length() - 3);
            }
            if (potionName.startsWith("splash-")) {
                potionType = potionType.substring(7);
            }

            // see if this potion is on cooldown
            long time = System.currentTimeMillis();
            Long readyTime = hero.getCooldown(potionType);
            if (readyTime != null && time < readyTime) {
                int secRemaining = (int) Math.ceil((readyTime - time) / 1000.0);
                player.sendMessage("You can't use this potion for " + secRemaining + "s!");
                return false;
            }

            // potion is okay to use, so trigger a cooldown
            long cooldown = SkillConfigManager.getUseSetting(hero, skill, "cooldown." + potionName, 10 * 60000, true);
            hero.setCooldown(potionType, time + cooldown);

            return true;
        }
    }

    static {
        Map<Byte, String> regMap = new HashMap<>();
        regMap.put((byte) 8193, "regeneration");
        regMap.put((byte) 8257, "regeneration");
        regMap.put((byte) 8225, "regeneration-II");
        regMap.put((byte) 8194, "swiftness");
        regMap.put((byte) 8258, "swiftness");
        regMap.put((byte) 8226, "swiftness-II");
        regMap.put((byte) 8195, "fire-resistance");
        regMap.put((byte) 8259, "fire-resistance");
        regMap.put((byte) 8227, "fire-resistance");
        regMap.put((byte) 8197, "healing");
        regMap.put((byte) 8261, "healing");
        regMap.put((byte) 8229, "healing-II");
        regMap.put((byte) 8201, "strength");
        regMap.put((byte) 8265, "strength");
        regMap.put((byte) 8233, "strength-II");
        regMap.put((byte) 8196, "poison");
        regMap.put((byte) 8260, "poison");
        regMap.put((byte) 8228, "poison-II");
        regMap.put((byte) 8200, "weakness");
        regMap.put((byte) 8264, "weakness");
        regMap.put((byte) 8232, "weakness");
        regMap.put((byte) 8202, "slowness");
        regMap.put((byte) 8266, "slowness");
        regMap.put((byte) 8234, "slowness");
        regMap.put((byte) 8204, "harming");
        regMap.put((byte) 8268, "harming");
        regMap.put((byte) 8236, "harming-II");
        regularPotions = Collections.unmodifiableMap(regMap);

        Map<Byte, String> splashMap = new HashMap<>();
        splashMap.put((byte) 16385, "splash-regeneration");
        splashMap.put((byte) 16449, "splash-regeneration");
        splashMap.put((byte) 16417, "splash-regeneration-II");
        splashMap.put((byte) 16386, "splash-swiftness");
        splashMap.put((byte) 16450, "splash-swiftness");
        splashMap.put((byte) 16418, "splash-swiftness-II");
        splashMap.put((byte) 16387, "splash-fire-resistance");
        splashMap.put((byte) 16451, "splash-fire-resistance");
        splashMap.put((byte) 16419, "splash-fire-resistance");
        splashMap.put((byte) 16389, "splash-healing");
        splashMap.put((byte) 16453, "splash-healing");
        splashMap.put((byte) 16421, "splash-healing-II");
        splashMap.put((byte) 16393, "splash-strength");
        splashMap.put((byte) 16457, "splash-strength");
        splashMap.put((byte) 16425, "splash-strength-II");
        splashMap.put((byte) 16388, "splash-poison");
        splashMap.put((byte) 16452, "splash-poison");
        splashMap.put((byte) 16420, "splash-poison-II");
        splashMap.put((byte) 16392, "splash-weakness");
        splashMap.put((byte) 16456, "splash-weakness");
        splashMap.put((byte) 16424, "splash-weakness");
        splashMap.put((byte) 16394, "splash-slowness");
        splashMap.put((byte) 16458, "splash-slowness");
        splashMap.put((byte) 16426, "splash-slowness");
        splashMap.put((byte) 16396, "splash-harming");
        splashMap.put((byte) 16460, "splash-harming");
        splashMap.put((byte) 16428, "splash-harming-II");
        splashPotions = Collections.unmodifiableMap(splashMap);
    }
}
