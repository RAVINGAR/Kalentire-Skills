package com.herocraftonline.heroes.characters.skill.remastered;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.Listenable;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

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
        node.set("speed-multiplier", 7);
        return node;
    }

    @Override
    public Listener getListener() {
        return listener;
    }

    public class SkillEntityListener implements Listener
    {
        Map<UUID, Float> shooting;

        public SkillEntityListener() {
            shooting = new HashMap<>();
        }
        //A bow draw can only be cancelled by these things; You shoot the bow, you drop the bow, you change slot, or you swap hands

        @EventHandler(priority = EventPriority.MONITOR)
        public void onDrawBow(PlayerInteractEvent event)
        {
            Player player = event.getPlayer();
            Hero hero = SkillNimbleDraw.this.plugin.getCharacterManager().getHero(player);

            if(hasPassive(hero) && (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR)) {
                ItemStack inUse = player.getItemInUse();
                if(inUse != null && (inUse.getType() == Material.BOW || inUse.getType() == Material.CROSSBOW)) {
                    addShooting(hero);
                }
            }
        }

        @EventHandler
        public void onSlotChange(PlayerItemHeldEvent event) {
            if(event.isCancelled()) {
                return;
            }
            removeShooting(event.getPlayer());
        }

        @EventHandler
        public void onShootBow(EntityShootBowEvent event) {
            if(event.isCancelled() || !(event.getEntity() instanceof Player)) {
                return;
            }
            removeShooting((Player)event.getEntity());
        }

        @EventHandler
        public void onSwapHands(PlayerSwapHandItemsEvent event) {
            Player player = event.getPlayer();
            if(event.isCancelled() || !shooting.containsKey(player.getUniqueId())) {
                return;
            }

            if(player.getItemInUse() == null) {
                //If inUse == null means that player is no longer using their bow.
                //Therefore if they swap hands and are STILL using their bow then still apply movement bonus
                removeShooting(player);
            }
        }

        private void addShooting(Hero hero) {
            Player player = hero.getPlayer();
            float f = player.getWalkSpeed();
            shooting.put(player.getUniqueId(), f);
            player.setWalkSpeed(f * SkillConfigManager.getUseSettingInt(hero, SkillNimbleDraw.this, "speed-multiplier", false));
        }

        private void removeShooting(Player player) {
            Float speed = shooting.remove(player.getUniqueId());
            if(speed != null) {
                player.setWalkSpeed(speed);
            }
        }
    }
}

