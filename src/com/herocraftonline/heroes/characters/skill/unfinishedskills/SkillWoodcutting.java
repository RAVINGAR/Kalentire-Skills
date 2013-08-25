package com.herocraftonline.heroes.characters.skill.unfinishedskills;

import org.bukkit.Bukkit;
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

public class SkillWoodcutting extends PassiveSkill {

    public SkillWoodcutting(Heroes plugin) {
        super(plugin, "Woodcutting");
        setDescription("You have a $1% chance to get extra materials when logging.");
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
        
        @SuppressWarnings("deprecation")
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
            switch (block.getType()) {
                case LOG:
                    break;
                default:
                    return;
            }

            Hero hero = plugin.getCharacterManager().getHero(event.getPlayer());
            if (!hero.hasEffect("Woodcutting") || Util.nextRand() > SkillConfigManager.getUseSetting(hero, skill, SkillSetting.CHANCE_LEVEL, .001, false) * hero.getSkillLevel(skill)) {
                return;
            }

            if (extraDrops != 0) {
                extraDrops = Util.nextInt(extraDrops) + 1;
            } else {
                extraDrops = 1;
            }

            block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(block.getType(), extraDrops, (short) 0, block.getData()));
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
