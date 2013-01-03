package com.herocraftonline.heroes.characters.skill.skills;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Anvilment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.AnvilItemEvent;
import org.bukkit.event.enchantment.PrepareItemAnvilEvent;
import org.bukkit.inventory.ItemStack;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.classes.HeroClass;
import com.herocraftonline.heroes.characters.classes.HeroClass.ExperienceType;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Setting;

public class SkillAnvil extends PassiveSkill {

    public SkillAnvil(Heroes plugin) {
        super(plugin, "Anvil");
        setDescription("You are able to use the anvil.");
        setArgumentRange(0, 0);
        setTypes(SkillType.KNOWLEDGE, SkillType.ITEM);
        setEffectTypes(EffectType.BENEFICIAL);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillAnvilListener(this), plugin);
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

    public class SkillAnvilListener implements Listener {

        private final Skill skill;

        public SkillAnvilListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.LOWEST)
        public void onPrepareItemAnvil(PrepareItemAnvilEvent event) {
            if (event.isCancelled()) {
                return;
            }
            Hero hero = plugin.getCharacterManager().getHero(event.getAnviler());
            if (!hero.hasEffect(getName())) {
                // Don't offer enchants to players that don't meet the requirements
                Messaging.send(hero.getPlayer(), "You aren't an smithy!");
                event.setCancelled(true);
                return;
            }
            HeroClass hc = hero.getAnvilingClass();
            if (hc != null) {
                hero.syncExperience(hc);
            } else {
                // if for some reason we don't have an enchanting class also cancel the event
                Messaging.send(hero.getPlayer(), "You aren't an enchanter!");
                event.setCancelled(true);
                return;
            }
        }

        @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
        public void onAnvilItem(AnvilItemEvent event) {
            Player player = event.getAnviler();
            Hero hero = plugin.getCharacterManager().getHero(player);

            HeroClass enchanter = hero.getAnvilingClass();
            hero.setSyncPrimary(enchanter.equals(hero.getHeroClass()));
            int level = hero.getLevel(enchanter);

            Map<Anvilment, Integer> enchants = event.getAnvilsToAdd();
            Iterator<Entry<Anvilment, Integer>> iter = enchants.entrySet().iterator();
            int xpCost = 0;
            while (iter.hasNext()) {
                Entry<Anvilment, Integer> entry = iter.next();
                int enchLevel = entry.getValue();
                Anvilment ench = entry.getKey();
                int reqLevel = SkillConfigManager.getUseSetting(hero, skill, entry.getKey().getName(), 1, true);
                if (enchLevel > ench.getMaxLevel()) {
                    entry.setValue(ench.getMaxLevel());
                } else if (enchLevel < ench.getStartLevel()) {
                    entry.setValue(ench.getStartLevel());
                }
                if (level < reqLevel || !ench.canAnvilItem(event.getItem())) {
                    iter.remove();
                } else {
                    int val = entry.getValue();
                    int maxVal = ench.getMaxLevel();
                    xpCost +=  Math.max(event.getExpLevelCost() * ((double) val / maxVal), 1);
                }
            }
            if (event.getAnvilsToAdd().isEmpty()) {
                event.setCancelled(true);
                return;
            }
            event.setExpLevelCost(0);
            ItemStack reagent = getReagentCost(hero);
            if (!hasReagentCost(player, reagent)) {
                Messaging.send(player, "You need $1 $2 to anvil an item!", reagent.getAmount(), reagent.getType().name().toLowerCase().replace("_", " "));
                event.setCancelled(true);
            }

            if (xpCost == 0) {
                Messaging.send(player, "Anviling failed!");
                event.setCancelled(true);
            } else {
                xpCost *= Heroes.properties.enchantXPMultiplier;
                if (hero.getExperience(enchanter) < xpCost) {
                    Messaging.send(player, "You don't have enough experience to enchant that item!");
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
