package com.herocraftonline.heroes.characters.skill.public1;

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
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

public class SkillWoodcutting extends PassiveSkill {

    public SkillWoodcutting(Heroes plugin) {
        super(plugin, "Woodcutting");
        this.setDescription("You have a $1% chance to get extra materials when logging.");
        this.setEffectTypes(EffectType.BENEFICIAL);
        this.setTypes(SkillType.KNOWLEDGE, SkillType.ABILITY_PROPERTY_EARTH, SkillType.BUFFING);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillBlockListener(this), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.CHANCE_LEVEL.node(), .001);
        return node;
    }

    public class SkillBlockListener implements Listener {

        private final Skill skill;

        SkillBlockListener(Skill skill) {
            this.skill = skill;
        }

        @SuppressWarnings("deprecation")
        @EventHandler(priority = EventPriority.MONITOR)
        public void onBlockBreak(BlockBreakEvent event) {
            if (event.isCancelled()) {
                return;
            }

            final Block block = event.getBlock();
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

            final Hero hero = SkillWoodcutting.this.plugin.getCharacterManager().getHero(event.getPlayer());
            if (!hero.hasEffect("Woodcutting") || (Util.nextRand() > (SkillConfigManager.getUseSetting(hero, this.skill, SkillSetting.CHANCE_LEVEL, .001, false) * hero.getLevel(this.skill)))) {
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
        final double chance = SkillConfigManager.getUseSetting(hero, this, SkillSetting.CHANCE_LEVEL, .001, false);
        int level = hero.getLevel(this);
        if (level < 1) {
            level = 1;
        }
        return this.getDescription().replace("$1", Util.stringDouble(chance * level * 100));
    }
}
