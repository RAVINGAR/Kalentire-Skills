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
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;

public class SkillBowStrength extends PassiveSkill {

    public SkillBowStrength(Heroes plugin) {
        super(plugin, "BowStrength");
        setDescription("Your arrows fly much faster!");
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
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.APPLY_TEXT.node(), "");
        node.set(SkillSetting.UNAPPLY_TEXT.node(), "");
        node.set("velocity-multiplier", 1.2);

        return node;
    }

    public class SkillBowListener implements Listener {

        private final Skill skill;
        public SkillBowListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler()
        public void onEntityShootBow(EntityShootBowEvent event) {
            if (event.isCancelled() || !(event.getEntity() instanceof Player) || !(event.getProjectile() instanceof Arrow)) {
                return;
            }
            Hero hero = plugin.getCharacterManager().getHero((Player) event.getEntity());
            if (hero.hasEffect(getName())) {
                double mult = SkillConfigManager.getUseSetting(hero, skill, "velocity-multiplier", 1.2, false);
                Projectile proj = (Projectile) event.getProjectile();
                proj.setVelocity(proj.getVelocity().multiply(mult));
            }
        }
    }
}
