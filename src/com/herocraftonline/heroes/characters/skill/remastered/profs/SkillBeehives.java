package com.herocraftonline.heroes.characters.skill.remastered.profs;


import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.block.BlockBreakEvent;

public class SkillBeehives extends PassiveSkill {
    public SkillBeehives(Heroes plugin) {
        super(plugin, "Beehives");
        setDescription("You are able to harvest beehives!");
        setArgumentRange(0, 0);
        setTypes(SkillType.BLOCK_REMOVING);
        setEffectTypes(EffectType.BENEFICIAL);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillListener(this), plugin);
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.APPLY_TEXT.node(), "");
        node.set(SkillSetting.UNAPPLY_TEXT.node(), "");
        node.set(SkillSetting.LEVEL.node(), 1); // not necessary we can just assign the passive skill the class at a certain level in config
        return node;
    }

    public class SkillListener implements Listener {
        private final Skill skill;

        public SkillListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.LOW)
        public void onBlockBreak(BlockBreakEvent event) {
            final Material blockType = event.getBlock().getType();
            if (blockType != Material.BEE_NEST && blockType != Material.BEEHIVE)
                return;

            Hero hero = SkillBeehives.this.plugin.getCharacterManager().getHero(event.getPlayer());

            if (hero.canUseSkill(this.skill)) {
                if (blockType == Material.BEE_NEST)
                    event.getBlock().getLocation().getWorld().dropItem(event.getBlock().getLocation(), new ItemStack(Material.BEE_NEST, 1));
                else return;
                //FIXME make this work for BEEHIVE as well (one is craftable, other is not)

                // Maybe also allow storing bees in the item, like if the item was destroyed by silktouch.
                // see https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/block/EntityBlockStorage.html
                // https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/block/Beehive.html
                // https://www.spigotmc.org/threads/getting-number-of-bees-in-an-item-hive.457744/
                // Looks to be easy to get data from block, but storing it in the item seems less documented.
            }
            else {
                event.setCancelled(true);
                event.getPlayer().sendMessage("You must be a farmer to harvest beehives!");
            }

        }
    }




    public String getDescription(Hero hero) {
        return getDescription();
    }
}