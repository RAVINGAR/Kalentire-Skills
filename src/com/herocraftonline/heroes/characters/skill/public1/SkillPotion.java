package com.herocraftonline.heroes.characters.skill.public1;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
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
        this.setDescription("You are able to use potions!");
        this.setTypes(SkillType.KNOWLEDGE, SkillType.ITEM_MODIFYING);
        Bukkit.getPluginManager().registerEvents(new PotionListener(this), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection section = super.getDefaultConfig();
        section.set(SkillSetting.LEVEL.node(), 1);
        section.set(SkillSetting.NO_COMBAT_USE.node(), false);
        for (final String potion : regularPotions.values()) {
            section.set("allow." + potion, false);
            section.set("cooldown." + potion, 10 * 60000);
        }

        for (final String potion : splashPotions.values()) {
            section.set("allow." + potion, false);
            section.set("cooldown." + potion, 10 * 60000);
        }

        return section;
    }

    @Override
    public String getDescription(Hero hero) {
        return this.getDescription();
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
            if ((event.useItemInHand() == Event.Result.DENY) || !event.hasItem()) {
                return;
            }

            final ItemStack item = event.getItem();
            final Material material = item.getType();

            if (material != Material.POTION) {
                return;
            }

            final Player player = event.getPlayer();
            final Hero hero = SkillPotion.this.plugin.getCharacterManager().getHero(player);

            // see if the player can use potions at all
            if (!hero.canUseSkill(this.skill) || (hero.isInCombat() && SkillConfigManager.getUseSetting(hero, this.skill, SkillSetting.NO_COMBAT_USE, false))) {
                player.sendMessage(ChatColor.GRAY + "You can't use this potion!");
                event.setUseItemInHand(Event.Result.DENY);
                player.updateInventory();
                return;
            }

            // get the potion type info
            final byte potionId = item.getData().getData();
            String potionName;
            if (isPotion(potionId)) {
                potionName = regularPotions.get(potionId);
            } else if (isSplashPotion(potionId)) {
                potionName = splashPotions.get(potionId);
            } else {
                return;
            }

            // see if the player can use this type of potion
            if (!SkillConfigManager.getUseSetting(hero, this.skill, "allow." + potionName, false)) {
                player.sendMessage(ChatColor.GRAY + "You can't use this potion!");
                event.setUseItemInHand(Event.Result.DENY);
                player.updateInventory();
                return;
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
            final long time = System.currentTimeMillis();
            final Long readyTime = hero.getCooldown(potionType);
            if ((readyTime != null) && (time < readyTime)) {
                final int secRemaining = (int) Math.ceil((readyTime - time) / 1000.0);
                player.sendMessage(ChatColor.GRAY + "You can't use this potion for " + ChatColor.WHITE + secRemaining + ChatColor.GRAY + "!");
                event.setUseItemInHand(Event.Result.DENY);
                player.updateInventory();
                return;
            }


            // potion is okay to use, so trigger a cooldown
            final long cooldown = SkillConfigManager.getUseSetting(hero, this.skill, "cooldown." + potionName, 10 * 60000, true);
            hero.setCooldown(potionType, time + cooldown);
        }
    }

    static {
        final Map<Byte, String> regMap = new HashMap<Byte, String>();
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

        final Map<Byte, String> splashMap = new HashMap<Byte, String>();
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
