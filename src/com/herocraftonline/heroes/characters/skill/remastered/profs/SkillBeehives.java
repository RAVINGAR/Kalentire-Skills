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
import org.bukkit.plugin.Plugin;
import org.bukkit.event.block.BlockBreakEvent;

public class SkillBeehives extends PassiveSkill{
    public SkillBeehives(Heroes plugin) {
        super(plugin, "Beehives");
        setDescription("You are able to harvest beehives!");
        setArgumentRange(0, 0);
        setTypes(new SkillType[] { SkillType.BLOCK_REMOVING });
        setEffectTypes(new EffectType[] { EffectType.BENEFICIAL });
        Bukkit.getServer().getPluginManager().registerEvents(new SkillListener((Skill)this), (Plugin)plugin);
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.APPLY_TEXT.node(), "");
        node.set(SkillSetting.UNAPPLY_TEXT.node(), "");
        node.set(SkillSetting.LEVEL.node(), 1);
        return node;
    }

    public class SkillListener implements Listener {
        private final Skill skill;

        public SkillListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.LOW)
        public void onBlockBreak(BlockBreakEvent event) {
            if (event.getBlock().getType() != Material.BEE_NEST && event.getBlock().getType() != Material.BEEHIVE)
                return;

            Hero hero = SkillBeehives.this.plugin.getCharacterManager().getHero(event.getPlayer());

            if (hero.canUseSkill(this.skill)) {
                if (event.getBlock().getType() == Material.BEE_NEST)
                    event.getBlock().getLocation().getWorld().dropItem(event.getBlock().getLocation(), new ItemStack(Material.BEE_NEST, 1));
                else return;
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