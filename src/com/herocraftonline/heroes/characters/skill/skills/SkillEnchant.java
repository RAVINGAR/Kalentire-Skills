package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.classes.HeroClass;
import com.herocraftonline.heroes.characters.classes.HeroClass.ExperienceType;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.*;
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
        section.set("PROTECTION_ENVIRONMENTAL", 200);
        section.set("PROTECTION_FIRE", 1);
        section.set("PROTECTION_FALL", 1);
        section.set("PROTECTION_EXPLOSIONS", 200);
        section.set("PROTECTION_PROJECTILE", 200);
        section.set("OXYGEN", 1);
        section.set("WATER_WORKER", 1);
        section.set("DAMAGE_ALL", 200);
        section.set("DAMAGE_UNDEAD", 200);
        section.set("DAMAGE_ARTHROPODS", 200);
        section.set("KNOCKBACK", 1);
        section.set("FIRE_ASPECT", 1);
        section.set("LOOT_BONUS_MOBS", 1);
        section.set("DIG_SPEED", 1);
        section.set("SILK_TOUCH", 200);
        section.set("DURABILITY", 1);
        section.set("LOOT_BONUS_BLOCKS", 1);
        section.set("ARROW_DAMAGE", 200);
        section.set("ARROW_KNOCKBACK", 1);
        section.set("ARROW_FIRE", 1);
        section.set("ARROW_INFINITE", 1);
        section.set("FROST_WALKER", 200);
        section.set("MENDING", 200);
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

            Map<Enchantment, Integer> enchants = event.getEnchantsToAdd();
            Iterator<Entry<Enchantment, Integer>> iter = enchants.entrySet().iterator();
            int xpCost = 0;
            while (iter.hasNext()) {
                Entry<Enchantment, Integer> entry = iter.next();
                int enchLevel = entry.getValue();
                Enchantment ench = entry.getKey();
                int reqLevel = SkillConfigManager.getUseSetting(hero, skill, entry.getKey().getName(), 1, true);
                if (enchLevel > ench.getMaxLevel()) {
                    entry.setValue(ench.getMaxLevel());
                } else if (enchLevel < ench.getStartLevel()) {
                    entry.setValue(ench.getStartLevel());
                }
                if (level < reqLevel || !ench.canEnchantItem(event.getItem())) {
                    iter.remove();
                } else {
                    int val = entry.getValue();
                    int maxVal = ench.getMaxLevel();
                    xpCost +=  Math.max(event.getExpLevelCost() * ((double) val / maxVal), 1);
                }
            }
            if (event.getEnchantsToAdd().isEmpty()) {
                event.setCancelled(true);
                return;
            }
            event.setExpLevelCost(1);
            ItemStack reagent = getReagentCost(hero);
            if (!hasReagentCost(player, reagent)) {
                player.sendMessage("You need " + reagent.getAmount() + " " + reagent.getType().name().toLowerCase().replace("_", " ") + " to enchant an item!");
                event.setCancelled(true);
            }

            if (xpCost == 0) {
                player.sendMessage("Enchanting failed!");
                event.setCancelled(true);
            } else {
                xpCost *= Heroes.properties.enchantXPMultiplier;
                if (hero.getExperience(enchanter) < xpCost) {
                    player.sendMessage("You don't have enough experience to enchant that item!");
                    event.setCancelled(true);
                    return;
                }
                hero.gainExp(-xpCost, ExperienceType.ENCHANTING, player.getLocation());
            }
        }
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }
}
