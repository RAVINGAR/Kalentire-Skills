package com.herocraftonline.heroes.characters.skill.codari;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.RecastData;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.chat.ChatComponents;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class SkillPair extends ActiveSkill implements Listener {

    public SkillPair(Heroes plugin) {
        super(plugin, "Pair");
        setDescription("Stuff");

        setUsage("/skill " + getName());
        setArgumentRange(0, 0);
        setIdentifiers("skill " + getName());
    }

    @Override
    public void init() {
        super.init();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        return null;
    }

    @Override
    public SkillResult use(Hero hero, String[] strings) {

        RecastData recastData = new RecastData("Riposte");
        recastData.setNeverReady();
        startRecast(hero, 500, recastData);

        broadcastExecuteText(hero);

        return SkillResult.NORMAL;
    }

    @Override
    public void recast(Hero hero, RecastData recastData) {

        Bukkit.broadcastMessage("RIPOSTE!!!!");
        endRecast(hero);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private void onDamage(EntityDamageByEntityEvent e) {

        if (e.getEntity() instanceof Player) {

            Player player = (Player) e.getEntity();

            if (isRecasting(player)) {
                e.setCancelled(true);
                getRecastData(player).setReady();
                player.sendMessage("    " + ChatComponents.GENERIC_SKILL + "You blocked something with parry!");
            }
        }
    }
}
