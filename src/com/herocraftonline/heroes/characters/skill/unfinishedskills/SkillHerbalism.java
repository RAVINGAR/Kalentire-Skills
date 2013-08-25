package com.herocraftonline.heroes.characters.skill.unfinishedskills;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.listeners.HBlockListener;
import com.herocraftonline.heroes.util.Util;

public class SkillHerbalism extends PassiveSkill {

    public SkillHerbalism(Heroes plugin) {
        super(plugin, "Herbalism");
        setDescription("You have a $1% chance to harvest extra herbs, fruits, and vegetables.");
        setEffectTypes(EffectType.BENEFICIAL);
        setTypes(SkillType.KNOWLEDGE, SkillType.EARTH, SkillType.BUFF);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillBlockListener(this), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.CHANCE_LEVEL.node(), .001);
        return node;
    }

    public class SkillBlockListener implements Listener {

        private Skill skill;
        
        SkillBlockListener(Skill skill) {
            this.skill = skill;
        }
        
        @EventHandler(priority = EventPriority.MONITOR)
        public void onBlockBreak(BlockBreakEvent event) {
            if (event.isCancelled()) {
                return;
            }

            Block block = event.getBlock();
            if (HBlockListener.placedBlocks.containsKey(block.getLocation())) {
                return;
            }

            int extraDrops = 0;
            Material mat = null;
            switch (block.getType()) {
                case CROPS:
                    extraDrops = 3;
                    mat = Material.WHEAT;
                    break;
                case SUGAR_CANE_BLOCK:
                    mat = Material.SUGAR_CANE;
                    extraDrops = 2;
                    break;
                case MELON_BLOCK:
                    mat = Material.MELON;
                    extraDrops = 7;
                    break;
                case SAPLING:
                case LEAVES:
                case YELLOW_FLOWER:
                case RED_ROSE:
                case BROWN_MUSHROOM:
                case RED_MUSHROOM:
                case CACTUS:
                case LONG_GRASS:
                case PUMPKIN:
                case DEAD_BUSH:
                    break;
                default:
                    return;
            }

            Hero hero = plugin.getCharacterManager().getHero(event.getPlayer());
            if (!hero.hasEffect("Herbalism") || Util.nextRand() >= SkillConfigManager.getUseSetting(hero, skill, SkillSetting.CHANCE_LEVEL, .001, false) * hero.getSkillLevel(skill)) {
                return;
            }

            if (extraDrops != 0) {
                extraDrops = Util.nextInt(extraDrops) + 1;
            } else {
                extraDrops = 1;
            }
            if (mat != null) {
                block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(mat, extraDrops));
            } else {
                block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(block.getType(), extraDrops, block.getData()));
            }
        }
    }

    @Override
    public String getDescription(Hero hero) {
        double chance = SkillConfigManager.getUseSetting(hero, this, SkillSetting.CHANCE_LEVEL, .001, false);
        int level = hero.getSkillLevel(this);
        if (level < 1)
            level = 1;
        return getDescription().replace("$1", Util.stringDouble(chance * level * 100));
    }
}
