package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import net.minecraft.server.v1_11_R1.ContainerAnvil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.lang.reflect.Method;
import java.util.logging.Level;

public class SkillAnvil extends PassiveSkill {

    public SkillAnvil(Heroes plugin) throws NoSuchMethodException, SecurityException {
        super(plugin, "Anvil");
        setDescription("You can use Anvils!");
        Bukkit.getPluginManager().registerEvents(new AnvilListener(), this.plugin);
        try {
            Method method = ContainerAnvil.class.getDeclaredMethod("d", new Class[] {});
            method.setAccessible(true);
        } catch(Exception e) {
            Bukkit.getLogger().log(Level.SEVERE, "SkillAnvil: Error overriding vanilla anvil behaviour! Behaviour may be erratic");
            e.printStackTrace();
        }
    }
    @Override
    public String getDescription(Hero arg0) {
        return getDescription();
    }
    /*
     * Checks for right clicks on anvil and cancels if this skill is not possessed
     * Handled as highest priority due to us not wanting people to override us canceling the event
     */
    public class AnvilListener implements Listener {
        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onPlayerInteract(PlayerInteractEvent event) {
            if(event.getAction() != Action.RIGHT_CLICK_BLOCK) {
                return;
            }
            if(!event.getClickedBlock().getType().equals(Material.ANVIL)) {
                return;
            }

            Hero h = SkillAnvil.this.plugin.getCharacterManager().getHero(event.getPlayer());
            if(!h.hasEffect("Anvil")) {
                event.setCancelled(true);
                h.getPlayer().sendMessage(ChatColor.GRAY + "You lack the training to use an Anvil!");
            } else {

            }
        }
    }

}
