package com.herocraftonline.heroes.characters.skill.skills;


import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;

public class SkillPigWizard extends PassiveSkill {

    private HashMap<String, Long> explosions;
    private long explodingPigInterval = 5000;

    public SkillPigWizard(Heroes plugin) {
        super(plugin, "PigWizard");
        super.setDescription("Passive: When mining, pigs will mysteriously appear in place of the blocks." 
                            + "It's quite the useless skill while still being highly amusing..");

        Bukkit.getPluginManager().registerEvents(new PigWizardListener(this), this.plugin);
        this.explosions = new HashMap<String, Long>();
    }

    @Override
    public String getDescription(Hero arg0) {
        return super.getDescription();
    }

    public class PigWizardListener implements Listener {

        private Skill skill;

        public PigWizardListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onBlockBreak(BlockBreakEvent e) {

            // Lets get the player and hero so we can do some checks.
            Player p = e.getPlayer();
            Hero h = skill.plugin.getCharacterManager().getHero(p);

            // If they cannot use the skill, no need to continue here.
            if (!h.canUseSkill(skill)) {
                return;
            }

            // Now we know they is a Pig Wizard, lets magically appear some pigs
            // while they mines.
            World w = p.getWorld();
            Location l = e.getBlock().getLocation();

            // And thus, the pig is spawned.
            Pig pig = w.spawn(l, Pig.class);

            // If don't have a record of them placing, first pig will explode.
            if (!explosions.containsKey(p.getName())) {
                explodePig(p.getName(), pig);
            }
            // If we do, got some more checking to do here.
            else {
                // If the last time + the interval is less or equal then the current
                // time, i.e. has it been more then 5 seconds.
                // Pig will then explode.
                if (explosions.get(p.getName()) + explodingPigInterval <= System.currentTimeMillis()) {
                    explodePig(p.getName(), pig);
                }
            }
        }

        // This method is cruel.... yet amusing.
        private void explodePig(String name, Pig pig) {
            pig.getWorld().createExplosion(pig.getLocation(), 5F);
            explosions.put(name, System.currentTimeMillis());
        }

    }

}
