package com.herocraftonline.heroes.characters.skill.remastered;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.Listenable;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SkillNimbleDraw extends PassiveSkill implements Listenable
{
    private final Listener listener;
    public SkillNimbleDraw(Heroes plugin)
    {
        super(plugin, "NimbleDraw");
        setDescription("Move quickly while drawing a bow.");
        setUsage("/skill NimbleDraw");
        setArgumentRange(0, 0);
        setIdentifiers("skill nimbledraw");
        setTypes(SkillType.BUFFING, SkillType.MOVEMENT_INCREASING);
        listener = new SkillEntityListener();
    }

    public String getDescription(Hero hero) {
        return getDescription();
    }

    public ConfigurationSection getDefaultConfig()
    {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("speed-increase-percentage", 0.1);
        return node;
    }

    @Override
    public Listener getListener() {
        return listener;
    }

    public class SkillEntityListener implements Listener
    {
        private final Map<UUID, Float> shooting;

        public SkillEntityListener() {
            shooting = new HashMap<>();
        }
        //A bow draw can only be cancelled by these things; You shoot the bow, you drop the bow, you change slot, or you swap hands

        @EventHandler(priority = EventPriority.MONITOR)
        public void onDrawBow(PlayerInteractEvent event) {
            Player player = event.getPlayer();
            Hero hero = SkillNimbleDraw.this.plugin.getCharacterManager().getHero(player);

            if (hasPassive(hero) && (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR)) {
                if (!shooting.containsKey(player.getUniqueId())) {
                    BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
                    addShooting(hero);
                    NimbleRunnable runnable = new NimbleRunnable(scheduler, hero);
                    runnable.setTaskId(scheduler.scheduleSyncRepeatingTask(SkillNimbleDraw.this.plugin, runnable, 2L, 1L));

                }
            }
        }

        private void addShooting(Hero hero) {
            Player player = hero.getPlayer();
            float f = player.getWalkSpeed();
            shooting.put(player.getUniqueId(), f);
        }

        private void removeShooting(Player player) {
            Float speed = shooting.remove(player.getUniqueId());
            if(speed != null) {
                //SkillNimbleDraw.this.plugin.getCharacterManager().getHero(player).resolveMovementSpeed();
                player.setWalkSpeed(speed);
            }
        }

        private class NimbleRunnable implements Runnable {
            private final Hero hero;
            private final Player player;
            private final BukkitScheduler scheduler;
            private long timeout;
            private int taskId;
            private float newWalkSpeed;

            public NimbleRunnable(BukkitScheduler scheduler, Hero hero) {
                this.scheduler = scheduler;
                this.hero = hero;
                this.player = hero.getPlayer();
                timeout = 600L;
                newWalkSpeed = hero.getPlayer().getWalkSpeed() + 0.2f * (float) SkillConfigManager.getUseSettingDouble(hero, SkillNimbleDraw.this, "speed-increase-percentage", false);
                if(newWalkSpeed > 1.0f) {
                    newWalkSpeed = 1.0f;
                }
            }

            public void setTaskId(int id) {
                this.taskId = id;
            }

            @Override
            public void run() {
                if(--timeout > 0) {
                    ItemStack inUse = player.getItemInUse();
                    if(inUse != null && (inUse.getType() == Material.BOW || inUse.getType() == Material.CROSSBOW)) {
                        if(player.getWalkSpeed() != newWalkSpeed) {
                            player.setWalkSpeed(newWalkSpeed);
                        }
                    }
                    else {
                        removeShooting(player);
                        scheduler.cancelTask(taskId);
                    }
                }
                else {
                    removeShooting(player);
                    scheduler.cancelTask(taskId);
                }
            }
        }
    }
}

