package com.herocraftonline.heroes.characters.skill.remastered.profs;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.util.MaterialUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;

/**
 * Created By MysticMight 2021
 */

public class SkillCraftNetheriteGear extends PassiveSkill {

    private final Set<Material> netheriteGear;

    public SkillCraftNetheriteGear(Heroes plugin) {
        super(plugin, "CraftNetheriteGear");
        setDescription("You are able to craft netherite items.");
        setArgumentRange(0, 0);
        setEffectTypes(EffectType.BENEFICIAL);

        netheriteGear = new HashSet<>();
        netheriteGear.add(Material.NETHERITE_SWORD);
        netheriteGear.add(Material.NETHERITE_PICKAXE);
        netheriteGear.add(Material.NETHERITE_AXE);
        netheriteGear.add(Material.NETHERITE_SHOVEL);
        netheriteGear.add(Material.NETHERITE_HOE);
        netheriteGear.add(Material.NETHERITE_HELMET);
        netheriteGear.add(Material.NETHERITE_CHESTPLATE);
        netheriteGear.add(Material.NETHERITE_LEGGINGS);
        netheriteGear.add(Material.NETHERITE_BOOTS);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillCraftingListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(Material.NETHERITE_SWORD.toString(), 1);
        config.set(Material.NETHERITE_PICKAXE.toString(), 1);
        config.set(Material.NETHERITE_AXE.toString(), 1);
        config.set(Material.NETHERITE_SHOVEL.toString(), 1);
        config.set(Material.NETHERITE_HOE.toString(), 1);
        config.set(Material.NETHERITE_HELMET.toString(), 1);
        config.set(Material.NETHERITE_CHESTPLATE.toString(), 1);
        config.set(Material.NETHERITE_LEGGINGS.toString(), 1);
        config.set(Material.NETHERITE_BOOTS.toString(), 1);
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
        public void onPrepareUpgradingItem(PrepareSmithingEvent event) {
            final ItemStack result = event.getResult();
            if (result == null || !netheriteGear.contains(result.getType()))
                return; // Skip handled or non netherite items

            final Material resultType = result.getType();
            for (HumanEntity humanEntity : event.getViewers()) {
                // Should only be one, still...
                Hero hero = plugin.getCharacterManager().getHero((Player) humanEntity);

                // Don't allow crafting to players that don't have this skill
                if (!skill.hasPassive(hero)) {
                    hero.getPlayer().sendMessage(ChatColor.RED + "You must be a Blacksmith to create netherite gear!");
                    event.setResult(null);
                    continue;
                }

                int levelRequired = SkillConfigManager.getUseSetting(hero, skill, resultType.toString(), 1, true);
                int level = hero.getHeroLevel(skill);

                // Don't allow crafting to players that don't have the level required for it
                if (level <= 0 || level < levelRequired) {
                    hero.getPlayer().sendMessage(ChatColor.RED + "You must be level " + levelRequired + " to create "
                            + MaterialUtil.getFriendlyName(resultType) + "!");
                    event.setResult(null);
                }
            }
        }
    }
//
//    public HeroClass getClassWithSkill(Hero hero, Skill skill) {
//        HeroClass heroClass = null;
//
//        HeroClass prim = hero.getHeroClass();
//        HeroClass sec = hero.getSecondaryClass();
//        HeroClass race = hero.getRaceClass();
//        if (prim.hasSkill(skill.getName())){
//            heroClass = prim;
//        }
//
//        if (sec.hasSkill(skill.getName())){
//            heroClass = sec;
//        }
//
//        if (race.hasSkill(skill.getName())){
//            heroClass = race;
//        }
//        return heroClass;
//    }
}
