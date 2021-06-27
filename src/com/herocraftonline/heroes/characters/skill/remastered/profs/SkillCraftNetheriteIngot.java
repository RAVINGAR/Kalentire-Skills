package com.herocraftonline.heroes.characters.skill.remastered.profs;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Created By MysticMight 2021
 */

public class SkillCraftNetheriteIngot extends PassiveSkill {

    public SkillCraftNetheriteIngot(Heroes plugin) {
        super(plugin, "CraftNetheriteIngot");
        setDescription("You are able to craft netherite ingots.");
        setArgumentRange(0, 0);
        setEffectTypes(EffectType.BENEFICIAL);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillCraftingListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.APPLY_TEXT.node(), "");
        config.set(SkillSetting.UNAPPLY_TEXT.node(), "");
        return config;
    }

    public class SkillCraftingListener implements Listener {
        private final PassiveSkill skill;

        public SkillCraftingListener(PassiveSkill skill) {
            this.skill = skill;
        }

        // Note Using the prepare event allows no result to appear when attempting to craft
        @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
        public void onPrepareUpgradingItem(PrepareItemCraftEvent event) {
            final ItemStack result = event.getInventory().getResult();
            if (result == null || result.getType() != Material.NETHERITE_INGOT)
                return; // Skip handled smithing or not netherite ingots

            for (HumanEntity humanEntity : event.getViewers()) {
                // Should only be one, still...
                Hero hero = plugin.getCharacterManager().getHero((Player) humanEntity);

                // Don't allow crafting to players that don't have this skill (or level required for it)
                if (!skill.hasPassive(hero)) {
                    if (!hero.hasAccessToSkill(skill)) {
                        hero.getPlayer().sendMessage(ChatColor.RED + "You must be a Blacksmith to create Netherite Ingots!");
                    } else {
                        // Could have access to the skill (has right class), but doesn't meet level requirements
                        int level = SkillConfigManager.getLevel(hero, skill, -1);
                        hero.getPlayer().sendMessage(ChatColor.RED + "You must be a level " + level + " Blacksmith to create Netherite Ingots!");
                    }
                    event.getInventory().setResult(null); // effectively 'cancel' crafting (doesn't close inventory though), showing it like its not a valid recipe

                    // Hopefully this wont cause issues for other plugins that may be using 'event.getRecipe().getResult()'
                    // which is supposed to be non-null. If this becomes a issue we can try going with a itemstack of
                    // Material.Air instead. It's not like this currently because I'm not sure on the difference
                }
            }
        }
    }
}
