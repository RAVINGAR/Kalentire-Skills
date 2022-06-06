package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.*;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;

public class SkillNoviceArcher extends PassiveSkill {

    public SkillNoviceArcher(Heroes plugin) {
        super(plugin, "NoviceArcher");
        setDescription("You're not very adept at archery, reducing your arrow damage by $1%.");
        setArgumentRange(0, 0);
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.ABILITY_PROPERTY_PROJECTILE);
        setEffectTypes(EffectType.BENEFICIAL, EffectType.PHYSICAL);

        Bukkit.getServer().getPluginManager().registerEvents(new SkillBowListener(this), plugin);
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
        config.set("damage-multiplier", 0.5);
        return config;
    }

    public class SkillBowListener implements Listener {

        private final Skill skill;
        public SkillBowListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true) // Monitor so that we ABSOLUTELY
        public void onEntityShootBow(EntityDamageByEntityEvent event) {
            if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Arrow))
                return;

            Projectile proj = (Projectile) event.getDamager();
            if (!(proj.getShooter() instanceof Player))
                return;

            Hero hero = plugin.getCharacterManager().getHero((Player) proj.getShooter());
            if (!hero.hasEffect(getName()))
                return;

            double damageMulti = SkillConfigManager.getUseSetting(hero, skill, "damage-multiplier", 0.5, false);
            event.setDamage(event.getDamage() * damageMulti);
        }
    }
}
