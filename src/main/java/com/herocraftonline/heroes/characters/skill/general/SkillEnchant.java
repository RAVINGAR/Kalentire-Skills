package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.classes.HeroClass;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.util.Properties;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import to.hc.common.core.collect.Pair;

import java.util.*;
import java.util.Map.Entry;

public class SkillEnchant extends ActiveSkill implements Passive {
    private final static List<String> ALL_ENCHANTMENTS = getAllEnchantments();
    
    public SkillEnchant(Heroes plugin) {
        super(plugin, "Enchant");
        setDescription("You are able to enchant items.");
        setUsage("/skill enchant");
        setArgumentRange(0, 0);
        setIdentifiers("skill enchant");
        setTypes(SkillType.ITEM_MODIFYING);
        
        Bukkit.getServer().getPluginManager().registerEvents(new SkillEnchantListener(this), plugin);
    }

    public static List<String> getAllEnchantments() {
        List<String> enchants = new LinkedList<>();
        for(Enchantment e : Enchantment.values()) {
            enchants.add(e.getKey().getKey());
        }
        return enchants;
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection section = super.getDefaultConfig();
        ALL_ENCHANTMENTS.forEach(e -> {
            section.set(e, 1);
        });
        section.set("experience-cost-per-level", -1);
        section.set(SkillSetting.APPLY_TEXT.node(), "");
        section.set(SkillSetting.UNAPPLY_TEXT.node(), "");
        return section;
    }

    @Override
    public SkillResult use(Hero hero, String[] strings) {
        Player player = hero.getPlayer();

        player.sendMessage(ChatColor.DARK_PURPLE + "----------[ " + ChatColor.WHITE + "Enchanting " + ChatColor.DARK_PURPLE + "]----------");

        List<Pair<String, Integer>> requiredLevels = new LinkedList<>();
        ALL_ENCHANTMENTS.forEach(e -> requiredLevels.add(new Pair<>(e, SkillConfigManager.getUseSetting(hero, this, e, 1, true))));
        requiredLevels.sort(Comparator.comparing(Pair::getRight));

        List<String> notUnlocked = new LinkedList<>();
        StringBuilder unlocked = new StringBuilder();

        unlocked.append("Unlocked : ");
        for(Pair<String, Integer> entry : requiredLevels) {
            if(hero.getHeroLevel(hero.getEnchantingClass()) >= entry.getRight()) {
                unlocked.append(ChatColor.GREEN).append(classify(entry.getLeft())).append(ChatColor.WHITE).append(" | ");
            }
            else {
                notUnlocked.add(ChatColor.DARK_GRAY + classify(entry.getLeft()) + ChatColor.WHITE + " | Requires Level: " + ChatColor.GRAY + entry.getRight());
            }
        }
        notUnlocked.forEach(player::sendMessage);
        player.sendMessage("");
        player.sendMessage(unlocked.toString());

        return SkillResult.NORMAL;
    }

    private String classify(String key) {
        StringBuilder builder = new StringBuilder();
        String[] split = key.split("_");
        for(int i = 0; i < split.length; i++) {
            builder.append(split[i].substring(0,1).toUpperCase());
            builder.append(split[i].substring(1));
            if(i + 1 < split.length) {
                builder.append(" ");
            }
        }
        return builder.toString();
    }

    @Override
    public void tryApplying(Hero hero) {

    }

    @Override
    public void apply(Hero hero) {

    }

    @Override
    public void unapply(Hero hero) {

    }

    public class SkillEnchantListener implements Listener {

        private final List<UUID> playersInEnchantingTable;
        private final Skill skill;

        public SkillEnchantListener(Skill skill) {
            this.skill = skill;
            playersInEnchantingTable = new LinkedList<>();
        }

        @EventHandler(priority = EventPriority.LOWEST)
        public void onPrepareItemEnchant(PrepareItemEnchantEvent event) {
            if (event.isCancelled()) {
                return;
            }
            Hero hero = plugin.getCharacterManager().getHero(event.getEnchanter());
            if (!hero.canUseSkill(skill)) {
                // Don't offer enchants to players that don't meet the requirements
                hero.getPlayer().sendMessage("You aren't an enchanter!");
                event.setCancelled(true);
                return;
            }

            HeroClass hc = hero.getEnchantingClass();
            if(hc == null) {
                // if for some reason we don't have an enchanting class also cancel the event
                hero.getPlayer().sendMessage("You aren't an enchanter!");
                event.setCancelled(true);
            }
            else {
                hero.setSyncPrimary(hc.equals(hero.getHeroClass()));
                hero.syncExperience(hc);
                playersInEnchantingTable.add(hero.getUUID());
            }
        }

        @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
        public void onEnchantItem(EnchantItemEvent event) {
            Player player = event.getEnchanter();
            Hero hero = plugin.getCharacterManager().getHero(player);

            HeroClass enchanter = hero.getEnchantingClass();
            int level = hero.getHeroLevel(enchanter);

            double perLevel = SkillConfigManager.getUseSettingDouble(hero, skill, "experience-cost-per-level", true);

            Map<Enchantment, Integer> enchants = event.getEnchantsToAdd();
            Iterator<Entry<Enchantment, Integer>> iter = (new HashMap<>(enchants)).entrySet().iterator(); //copy to avoid concurrent modification
            double levelCost = 0;
            while (iter.hasNext()) {
                Entry<Enchantment, Integer> entry = iter.next();
                Enchantment ench = entry.getKey();
                double reqLevel = SkillConfigManager.getUseSetting(hero, skill, entry.getKey().getKey().getKey(), -1.0, true);
                if(reqLevel == -1) {
                    reqLevel = SkillConfigManager.getUseSetting(hero, skill, entry.getKey().getName(), 1.0, true);
                }

                //IF level of enchanter is less than the required level.
                if (level < reqLevel || (event.getItem().getType() != Material.BOOK && !ench.canEnchantItem(event.getItem()))) {
                    iter.remove();
                    enchants.remove(ench);
                } else if(perLevel >= 0) {
                    levelCost += perLevel * entry.getValue();
                }
            }
            if (event.getEnchantsToAdd().isEmpty()) {
                player.sendMessage("You are not skilled enough to apply such enchants to that item!");
                event.setCancelled(true);
                return;
            }
            ItemStack reagent = getReagentCost(hero);
            if (!hasReagentCost(player, reagent)) {
                player.sendMessage("You need " + reagent.getAmount() + " " + reagent.getType().name().toLowerCase().replace("_", " ") + " to enchant an item!");
                event.setCancelled(true);
            }

            if(perLevel == -1) {
                levelCost = event.whichButton() + 1;
            }

            event.setExpLevelCost((int) levelCost);
            if(Heroes.properties.enchantXPMultiplier > 0) {
                levelCost *= Heroes.properties.enchantXPMultiplier;
            }


            if (hero.getHeroLevel(enchanter) < levelCost) {
                player.sendMessage("You don't have enough experience to enchant that item!");
                event.setCancelled(true);
                return;
            }
            double exp = (Math.max(0, com.herocraftonline.heroes.util.Properties.getTotalExp(level) - Properties.getTotalExp((int) (level-levelCost)))) * -1;
            if(exp < 0) {
                hero.gainExp(exp, HeroClass.ExperienceType.ENCHANTING, player.getLocation());
            }
        }

        @EventHandler
        public void onEnchantingTableClose(InventoryCloseEvent event) {
            if(playersInEnchantingTable.remove(event.getPlayer().getUniqueId())) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        Hero hero = plugin.getCharacterManager().getHero((Player)event.getPlayer());
                        if(!(hero.getPlayer().getOpenInventory().getTopInventory().getType() == InventoryType.ENCHANTING)) {
                            hero.setSyncPrimary(true);
                            hero.syncExperience();
                        }

                    }
                }.runTaskLater(plugin, 5L);
            }
        }
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }
}
