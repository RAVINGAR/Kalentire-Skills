package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.characters.skill.*;
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
import com.herocraftonline.heroes.listeners.HBlockListener;
import com.herocraftonline.heroes.util.Util;

public class SkillWoodcutting extends PassiveSkill {

    public SkillWoodcutting(Heroes plugin) {
        super(plugin, "Woodcutting");
        this.setDescription("You have a $1% chance to get extra materials when logging.");
        this.setEffectTypes(EffectType.BENEFICIAL);
        this.setTypes(SkillType.KNOWLEDGE, SkillType.ABILITY_PROPERTY_EARTH, SkillType.BUFFING);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillBlockListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        double chance = SkillConfigManager.getUseSetting(hero, this, SkillSetting.CHANCE_PER_LEVEL, .001, false);
        int level = hero.getHeroLevel(this);
        if (level < 1)
            level = 1;
        return getDescription().replace("$1", Util.stringDouble(chance * level * 100));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.CHANCE_PER_LEVEL.node(), .001);
        return node;
    }
    
    /**
	 * Something messes up just using getData(), need to turn the extra leaves into a player-usable version.
	 */
	public byte transmuteLogs(Material mat, byte data) {
	    //FIXME Data usage
//		if (mat == Material.LOG)
//		{
//			switch (data)
//			{
//			case 4:
//			case 8:
//			case 12:
//				return 0;
//			case 5:
//			case 9:
//			case 13:
//				return 1;
//			case 6:
//			case 10:
//			case 14:
//				return 2;
//			case 7:
//			case 11:
//			case 15:
//				return 3;
//			default:
//				return 0;
//			}
//		}
//		else if (mat == Material.LOG_2)
//		{
//			switch (data)
//			{
//			case 4:
//			case 8:
//			case 12:
//				return 0;
//			case 5:
//			case 9:
//			case 13:
//				return 1;
//			default:
//				return 0;
//			}
//		}
//		else
		{
			return 0;
		}
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
                case LOG_2:
                //case OAK_LOG:
                //case ACACIA_LOG:
                //case BIRCH_LOG:
                //case DARK_OAK_LOG:
                //case JUNGLE_LOG:
                //case SPRUCE_LOG:
                    break;
                default:
                    return;
            }

            final Hero hero = plugin.getCharacterManager().getHero(event.getPlayer());
            if (!hero.hasEffect("Woodcutting") || (Util.nextRand() > (SkillConfigManager.getUseSetting(hero, skill, SkillSetting.CHANCE_PER_LEVEL, .001, false) * hero.getHeroLevel(skill)))) {
                return;
            }

            extraDrops = 1;

            block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(block.getType(), extraDrops, (short) 0, transmuteLogs(block.getType(), block.getData())));
        }
    }
}
