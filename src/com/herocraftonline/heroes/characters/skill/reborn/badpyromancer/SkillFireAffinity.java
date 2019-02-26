package com.herocraftonline.heroes.characters.skill.reborn.badpyromancer;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class SkillFireAffinity extends PassiveSkill {

    public SkillFireAffinity(Heroes plugin) {
        super(plugin, "FireAffinity");
        setDescription("Passive: You have an affinity with flame, giving you immunity to fire ticks and taking $1 reduced damage from lava.");
        setEffectTypes(EffectType.BENEFICIAL, EffectType.FIRE);

        Bukkit.getPluginManager().registerEvents(new SkillHeroListener(this), plugin);
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
        config.set("lava-damage-reduction-percent", 0.4);
        return config;
    }

    private class SkillHeroListener implements Listener {
        private Skill skill;

        SkillHeroListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onEntityDamage(EntityDamageEvent event) {
            if (event.getCause() != EntityDamageEvent.DamageCause.FIRE_TICK
                    && event.getCause() != EntityDamageEvent.DamageCause.FIRE
                    && event.getCause() != EntityDamageEvent.DamageCause.LAVA) {
                return;
            }
            if (!(event.getEntity() instanceof Player))
                return;

            Player player = (Player) event.getEntity();
            Hero hero = plugin.getCharacterManager().getHero(player);
            EntityDamageEvent.DamageCause cause = event.getCause();
            if (hero.canUseSkill(skill)) {
                if (cause == EntityDamageEvent.DamageCause.FIRE_TICK) {
                    event.setCancelled(true);
                } else if (cause == EntityDamageEvent.DamageCause.FIRE && player.getLocation().getBlock().getType() == Material.FIRE) {
                    event.setCancelled(true);
                } else if (cause == EntityDamageEvent.DamageCause.LAVA && event.getDamage() > 1) {
                    double damageReductionPercent = SkillConfigManager.getUseSetting(hero, skill, "lava-damage-reduction-percent", 0.4, false);
                    event.setDamage(damageReductionPercent * (1D - damageReductionPercent));
                }
            }
        }
    }
}