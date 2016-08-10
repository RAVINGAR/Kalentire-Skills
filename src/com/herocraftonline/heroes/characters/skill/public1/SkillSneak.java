package com.herocraftonline.heroes.characters.skill.public1;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.SneakEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

public class SkillSneak extends ActiveSkill {

    private boolean damageCancels;
    private boolean attackCancels;

    public SkillSneak(Heroes plugin) {
        super(plugin, "Sneak");
        this.setDescription("You crouch into the shadows.");
        this.setUsage("/skill stealth");
        this.setArgumentRange(0, 0);
        this.setIdentifiers("skill sneak");
        this.setTypes(SkillType.BUFFING, SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.STEALTHY);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillEventListener(), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DURATION.node(), 600000); // 10 minutes in milliseconds
        node.set("damage-cancels", true);
        node.set("attacking-cancels", true);
        node.set("refresh-interval", 5000); // in milliseconds
        return node;
    }

    @Override
    public void init() {
        super.init();
        this.damageCancels = SkillConfigManager.getRaw(this, "damage-cancels", true);
        this.attackCancels = SkillConfigManager.getRaw(this, "attacking-cancels", true);
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        if (hero.hasEffect("Sneak")) {
            hero.removeEffect(hero.getEffect("Sneak"));
        } else {
            hero.getPlayer().sendMessage(ChatColor.GRAY + "You are now sneaking");

            final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 600000, false);
            final int period = SkillConfigManager.getUseSetting(hero, this, "refresh-interval", 5000, true);
            hero.addEffect(new SneakEffect(this, hero.getPlayer(), period, duration, false));
        }
        return SkillResult.NORMAL;
    }

    public class SkillEventListener implements Listener {

        @EventHandler(priority = EventPriority.MONITOR)
        public void onEntityDamage(EntityDamageEvent event) {
            if (event.isCancelled() || !SkillSneak.this.damageCancels || (event.getDamage() == 0)) {
                return;
            }
            Player player = null;
            if (event.getEntity() instanceof Player) {
                player = (Player) event.getEntity();
                final Hero hero = SkillSneak.this.plugin.getCharacterManager().getHero(player);
                if (hero.hasEffect("Sneak")) {
                    player.setSneaking(false);
                    hero.removeEffect(hero.getEffect("Sneak"));
                }
            }
            player = null;
            if (SkillSneak.this.attackCancels && (event instanceof EntityDamageByEntityEvent)) {
                final EntityDamageByEntityEvent subEvent = (EntityDamageByEntityEvent) event;
                if (subEvent.getDamager() instanceof Player) {
                    player = (Player) subEvent.getDamager();
                } else if (subEvent.getDamager() instanceof Projectile) {
                    if (((Projectile) subEvent.getDamager()).getShooter() instanceof Player) {
                        player = (Player) ((Projectile) subEvent.getDamager()).getShooter();
                    }
                }
                if (player != null) {
                    final Hero hero = SkillSneak.this.plugin.getCharacterManager().getHero(player);
                    if (hero.hasEffect("Sneak")) {
                        player.setSneaking(false);
                        hero.removeEffect(hero.getEffect("Sneak"));
                    }
                }
            }
        }

        @EventHandler(priority = EventPriority.HIGHEST)
        public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
            final Hero hero = SkillSneak.this.plugin.getCharacterManager().getHero(event.getPlayer());
            if (hero.hasEffect("Sneak")) {
                event.getPlayer().setSneaking(true);
                event.setCancelled(true);
            }
        }
    }

    @Override
    public String getDescription(Hero hero) {
        return this.getDescription();
    }
}
