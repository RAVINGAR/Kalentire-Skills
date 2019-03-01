package com.herocraftonline.heroes.characters.skill.reborn.professions;

import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import net.minecraft.server.v1_13_R2.EntityThrownTrident;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import com.herocraftonline.heroes.Heroes;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRiptideEvent;

public class SkillTridentPassive extends PassiveSkill {

    public SkillTridentPassive(Heroes plugin) {
        super(plugin, "TridentPassive");
        setDescription("You are able wield Tridents!");
        setArgumentRange(0, 0);
        setEffectTypes(EffectType.BENEFICIAL);
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.ABILITY_PROPERTY_PROJECTILE, SkillType.TELEPORTING);

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
            if (event.getClickedBlock() == null || event.getClickedBlock().getType() != Material.TRIDENT || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
                return;
            }
            Hero hero = plugin.getCharacterManager().getHero(event.getPlayer());
            if (!hero.canUseSkill(skill)) {
                event.setCancelled(true);
                event.setUseInteractedBlock(Event.Result.DENY);
            }

            Player player = event.getPlayer();
            player.sendMessage("InteractEvent!");
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onPlayerLaunchProjectileEvent(ProjectileLaunchEvent event) {
            Player player = (Player) event.getEntity().getShooter();
            player.sendMessage("LaunchProjectile!");

        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onEntityThrowTrident(EntityThrownTrident event) {
            Player player = (Player) event.getBukkitEntity();
            player.sendMessage("EntityThrowTrident!");
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
