package com.herocraftonline.heroes.characters.skill.remastered;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class SkillNimbleDraw extends ActiveSkill
{

    public SkillNimbleDraw(Heroes plugin)
    {
        super(plugin, "NimbleDraw");
        setDescription("Move quickly while drawing a bow.");
        setUsage("/skill NimbleDraw");
        setArgumentRange(0, 0);
        setIdentifiers("skill nimbledraw");
        setTypes(SkillType.BUFFING, SkillType.MOVEMENT_INCREASING);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(), plugin);
    }

    public String getDescription(Hero hero) {
        return getDescription();
    }

    public ConfigurationSection getDefaultConfig()
    {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("speed-multiplier", 7);
        return node;
    }

    public SkillResult use(Hero hero, String[] args)
    {
        Player player = hero.getPlayer();
        player.sendMessage("Nimbledraw is a passive skill!");
        return SkillResult.FAIL;
    }

    public class SkillEntityListener implements Listener
    {

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
        public void onMove(PlayerMoveEvent event)
        {

            Player player = event.getPlayer();
            Hero hero = SkillNimbleDraw.this.plugin.getCharacterManager().getHero(player);
            HumanEntity human = player;
            while(human.getItemInUse().equals(Material.BOW))
            {
                        player.setVelocity(player.getVelocity().multiply(7));
            }
        }

    }
}

