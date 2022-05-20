package com.herocraftonline.heroes.characters.skill.remastered;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.classes.HeroClass;
import com.herocraftonline.heroes.characters.classes.HeroClass.ExperienceType;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.util.Properties;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class SkillEnchant extends PassiveSkill {

    public SkillEnchant(Heroes plugin) {
        super(plugin, "Enchant");
        setDescription("You are able to enchant items.");
        setArgumentRange(0, 0);
        setTypes(SkillType.ITEM_MODIFYING);
        setEffectTypes(EffectType.BENEFICIAL);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillEnchantListener(this), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection section = super.getDefaultConfig();
        section.set("protection", 1);
        section.set("fire_protection", 1);
        section.set("feather_falling", 1);
        section.set("blast_protection", 1);
        section.set("projectile_protection", 1);
        section.set("respiration", 1);
        section.set("aqua_affinity", 1);
        section.set("sharpness", 1);
        section.set("smite", 1);
        section.set("bane_of_athropods", 1);
        section.set("knockback", 1);
        section.set("fire_aspect", 1);
        section.set("looting", 1);
        section.set("efficiency", 1);
        section.set("silk_touch", 1);
        section.set("unbreaking", 1);
        section.set("fortune", 1);
        section.set("power", 1);
        section.set("punch", 1);
        section.set("flame", 1);
        section.set("infinity", 1);
        section.set("frost_walker", 1);
        section.set("mending", 1);
        section.set("depth_strider", 1);
        section.set("sweeping_edge", 1);
        section.set("experience-cost-per-level", -1);
        section.set(SkillSetting.APPLY_TEXT.node(), "");
        section.set(SkillSetting.UNAPPLY_TEXT.node(), "");
        return section;
    }

    public class SkillEnchantListener implements Listener {

        private final Skill skill;

        public SkillEnchantListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.LOWEST)
        public void onPrepareItemEnchant(PrepareItemEnchantEvent event) {
            if (event.isCancelled()) {
                return;
            }
            Hero hero = plugin.getCharacterManager().getHero(event.getEnchanter());
            if (!hero.hasEffect(getName())) {
                // Don't offer enchants to players that don't meet the requirements
                hero.getPlayer().sendMessage("You aren't an enchanter!");
                event.setCancelled(true);
                return;
            }
            HeroClass hc = hero.getEnchantingClass();
            if (hc != null) {
                hero.syncExperience(hc);
            } else {
                // if for some reason we don't have an enchanting class also cancel the event
                hero.getPlayer().sendMessage("You aren't an enchanter!");
                event.setCancelled(true);
            }
        }

        @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
        public void onEnchantItem(EnchantItemEvent event) {
            Player player = event.getEnchanter();
            Hero hero = plugin.getCharacterManager().getHero(player);

            HeroClass enchanter = hero.getEnchantingClass();
            hero.setSyncPrimary(enchanter.equals(hero.getHeroClass()));
            int level = hero.getHeroLevel(enchanter);

            double perLevel = SkillConfigManager.getUseSettingDouble(hero, skill, "experience-cost-per-level", true);

            Map<Enchantment, Integer> enchants = event.getEnchantsToAdd();
            Iterator<Entry<Enchantment, Integer>> iter = (new HashMap<>(enchants)).entrySet().iterator(); //copy to avoid concurrent modification
            double levelCost = 0;
            while (iter.hasNext()) {
                Entry<Enchantment, Integer> entry = iter.next();
                int enchLevel = entry.getValue();
                Enchantment ench = entry.getKey();
                double reqLevel = SkillConfigManager.getUseSetting(hero, skill, entry.getKey().getKey().getKey(), -1.0, true);
                if(reqLevel == -1) {
                    reqLevel = SkillConfigManager.getUseSetting(hero, skill, entry.getKey().getName(), 1.0, true);
                }

                //IF level of enchanter is less than the required level.
                if (level < reqLevel || !ench.canEnchantItem(event.getItem())) {
                    iter.remove();
                    enchants.remove(ench);
                } else if(perLevel > -1) {
                    levelCost += SkillConfigManager.getUseSettingDouble(hero, skill, "experience-cost-per-level", true) * entry.getValue();
                }
            }
            if (event.getEnchantsToAdd().isEmpty()) {
                player.sendMessage("You don't have enough experience to enchant that item!");
                event.setCancelled(true);
                return;
            }
            ItemStack reagent = getReagentCost(hero);
            if (!hasReagentCost(player, reagent)) {
                player.sendMessage("You need " + reagent.getAmount() + " " + reagent.getType().name().toLowerCase().replace("_", " ") + " to enchant an item!");
                event.setCancelled(true);
            }

            if(perLevel == -1) {
                levelCost = event.getExpLevelCost();
            }

            if (event.getExpLevelCost() == 0) {
                player.sendMessage("Enchanting failed!");
                event.setCancelled(true);
            } else {
                event.setExpLevelCost((int) levelCost);
                levelCost *= Heroes.properties.enchantXPMultiplier;

                if (hero.getHeroLevel(enchanter) < levelCost) {
                    player.sendMessage("You don't have enough experience to enchant that item!");
                    event.setCancelled(true);
                    return;
                }
                double exp = Properties.getExp((int) levelCost) * -1;
                hero.gainExp(exp, ExperienceType.ENCHANTING, player.getLocation());

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        hero.syncExperience();
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
