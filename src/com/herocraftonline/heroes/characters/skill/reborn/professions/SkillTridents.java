package com.herocraftonline.heroes.characters.skill.reborn.professions;

import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import com.herocraftonline.heroes.Heroes;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRiptideEvent;

public class SkillTridents extends PassiveSkill {

    public SkillTridents(Heroes plugin) {
        super(plugin, "Tridents");
        setDescription("You are able wield Tridents!");
        setArgumentRange(0, 0);
        setEffectTypes(EffectType.BENEFICIAL);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillListener(this), plugin);
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.APPLY_TEXT.node(), "");
        config.set(SkillSetting.UNAPPLY_TEXT.node(), "");
        config.set(SkillSetting.LEVEL.node(), 1);
        return config;
    }

    public class SkillListener implements Listener {

        private final Skill skill;
        public SkillListener(Skill skill) {
            this.skill = skill;
        }

        // called when player right clicks trident, doesn't get called when they actually release the trident
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onPlayerInteract(PlayerInteractEvent event) {
//            Player player = event.getPlayer();
//            player.sendMessage("InteractEvent!");
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onPlayerLaunchProjectileEvent(ProjectileLaunchEvent event) {
            if (!(event.getEntity() instanceof Trident))
                return;

            Trident projectile = (Trident) event.getEntity();
            Player player = (Player) event.getEntity().getShooter();
            player.sendMessage("Launched a Trident!");

        }

        // called when player launches trident with riptide enchantment
        // not sure if it gets called when they are not in rain
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onPlayerRiptide(PlayerRiptideEvent event) {
            Player player = event.getPlayer();
            player.sendMessage("PlayerRiptide");
        }

    }
}
