package com.herocraftonline.dev.heroes.skill.skills;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

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

import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.classes.HeroClass;
import com.herocraftonline.dev.heroes.classes.HeroClass.ExperienceType;
import com.herocraftonline.dev.heroes.effects.EffectType;
import com.herocraftonline.dev.heroes.hero.Hero;
import com.herocraftonline.dev.heroes.skill.PassiveSkill;
import com.herocraftonline.dev.heroes.skill.Skill;
import com.herocraftonline.dev.heroes.skill.SkillConfigManager;
import com.herocraftonline.dev.heroes.skill.SkillType;
import com.herocraftonline.dev.heroes.util.Messaging;
import com.herocraftonline.dev.heroes.util.Setting;
import com.herocraftonline.dev.heroes.util.Util;

public class SkillEnchant extends PassiveSkill {

    public SkillEnchant(Heroes plugin) {
        super(plugin, "Enchant");
        setDescription("You are able to enchant items.");
        setArgumentRange(0, 0);
        setTypes(SkillType.KNOWLEDGE, SkillType.ITEM);
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
        section.set(Setting.APPLY_TEXT.node(), "");
        section.set(Setting.UNAPPLY_TEXT.node(), "");
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
            Hero hero = plugin.getHeroManager().getHero(event.getEnchanter());
            if (!hero.hasEffect(getName()) || !hero.hasExperienceType(ExperienceType.ENCHANTING)) {
                // Don't offer enchants to players that don't meet the requirements
                event.setCancelled(true);
                return;
            }
            HeroClass hc = hero.getEnchantingClass();
            if (hc != null) {
                hero.syncExperience(hc);
            } else {
                // if for some reason we don't have an enchanting class also cancel the event
                event.setCancelled(true);
                return;
            }
            int level = hero.getLevel(hc);
            int minLevel = level / 3;
            // this causes us to ignore the surrounding bookcases and just tell the client to generate numbers
            for (int i = 0; i < event.getExpLevelCostsOffered().length; i++) {
                event.getExpLevelCostsOffered()[i] = Util.rand.nextInt(level - minLevel) + minLevel + 1;
            }
        }

        @EventHandler(priority = EventPriority.LOWEST)
        public void onEnchantItem(EnchantItemEvent event) {
            Player player = event.getEnchanter();
            Hero hero = plugin.getHeroManager().getHero(player);
            if (event.isCancelled()) {
                return;
            }

            HeroClass enchanter = hero.getEnchantingClass();
            hero.setSyncPrimary(enchanter.equals(hero.getHeroClass()));
            int level = hero.getLevel(enchanter);

            Map<Enchantment, Integer> enchants = event.getEnchantsToAdd();
            Iterator<Entry<Enchantment, Integer>> iter = enchants.entrySet().iterator();
            int xpCost = 0;
            while (iter.hasNext()) {
                Entry<Enchantment, Integer> entry = iter.next();
                int enchLevel = entry.getValue();
                int reqLevel = SkillConfigManager.getUseSetting(hero, skill, entry.getKey().getName(), 1, true);
                if (entry.getKey().getStartLevel() > enchLevel || entry.getKey().getMaxLevel() < enchLevel || level < reqLevel || !entry.getKey().canEnchantItem(event.getItem())) {
                    iter.remove();
                } else {
                    int val = entry.getValue();
                    int maxVal = entry.getKey().getMaxLevel();
                    xpCost +=  Math.max(event.getExpLevelCost() * ((double) val / maxVal), 1);
                }
            }
            event.setExpLevelCost(0);
            ItemStack reagent = getReagentCost(hero);
            if (!hasReagentCost(player, reagent)) {
                Messaging.send(player, "You need $1 $2 to enchant an item!", reagent.getAmount(), reagent.getType().name().toLowerCase().replace("_", " "));
                event.setCancelled(true);
            }

            if (xpCost == 0) {
                Messaging.send(player, "Enchanting failed!");
                event.setCancelled(true);
            } else {
                xpCost *= Heroes.properties.enchantXPMultiplier;
                if (hero.getExperience(enchanter) < xpCost) {
                    Messaging.send(player, "You don't have enough experience to enchant that item!");
                    event.setCancelled(true);
                }
                hero.gainExp(-xpCost, ExperienceType.ENCHANTING);
            }
        }
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }
}
