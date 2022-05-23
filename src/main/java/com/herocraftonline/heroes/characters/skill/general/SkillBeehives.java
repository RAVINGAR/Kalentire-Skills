package com.herocraftonline.heroes.characters.skill.general;


import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Properties;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Beehive;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.logging.Level;


public class SkillBeehives extends PassiveSkill {
    final int SUBVERSION = 15;

    public SkillBeehives(Heroes plugin) {
        super(plugin, "Beehives");

        setArgumentRange(0, 0);
        setTypes(SkillType.BLOCK_REMOVING);
        setEffectTypes(EffectType.BENEFICIAL);

        setDescription("You are able to harvest from and move beehives and bee nests!");
        if(Properties.SUBVERSION >= this.SUBVERSION) {
            Bukkit.getServer().getPluginManager().registerEvents(new SkillListener(this), plugin);
        }
        else {
            Heroes.log(Level.SEVERE, "Could not load Skill " + this.getName() + " as it requires minimum Minecraft version of 1." + SUBVERSION);
        }


    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
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
            if (blockType != Material.matchMaterial("BEE_NEST") && blockType != Material.matchMaterial("BEEHIVE"))
                return;

            Hero hero = SkillBeehives.this.plugin.getCharacterManager().getHero(event.getPlayer());

            if (hero.canUseSkill(this.skill)) {
                ItemStack silkTool = new ItemStack(Material.WOODEN_PICKAXE);
                silkTool.addEnchantment(Enchantment.SILK_TOUCH, 1);
                event.getBlock().breakNaturally(silkTool);
            }
            else {
                event.setCancelled(true);
                event.getPlayer().sendMessage("You must be a farmer to move beehives!");
            }

        }
        @EventHandler(priority = EventPriority.LOW)
        public void onBlockInteract(PlayerInteractEvent event){
            Block clickedBlock = event.getClickedBlock();
            if(clickedBlock == null) {
                return;
            }
            if(clickedBlock.getType() != Material.matchMaterial("BEE_NEST") && clickedBlock.getType() != Material.matchMaterial("BEEHIVE")) {
                return;
            }

            Hero hero = SkillBeehives.this.plugin.getCharacterManager().getHero(event.getPlayer());
            final Beehive hive = (Beehive) event.getClickedBlock().getBlockData();


            if (hive.getHoneyLevel() != 0 && !hero.canUseSkill(this.skill) && (event.getPlayer().getInventory().getItemInMainHand().getType() == Material.GLASS_BOTTLE || event.getPlayer().getInventory().getItemInMainHand().getType() == Material.SHEARS || event.getPlayer().getInventory().getItemInOffHand().getType() == Material.GLASS_BOTTLE || event.getPlayer().getInventory().getItemInOffHand().getType() == Material.SHEARS) && event.getAction() == Action.RIGHT_CLICK_BLOCK)
            {
                event.setCancelled(true);
                event.getPlayer().sendMessage("You must be a farmer to harvest honey/honeycomb!");
            }
        }
    }




    public String getDescription(Hero hero) {
        return getDescription();
    }
}