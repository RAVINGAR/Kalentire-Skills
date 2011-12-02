package com.herocraftonline.dev.heroes.skill.skills;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityListener;

import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.effects.EffectType;
import com.herocraftonline.dev.heroes.hero.Hero;
import com.herocraftonline.dev.heroes.skill.PassiveSkill;
import com.herocraftonline.dev.heroes.skill.SkillType;

public class SkillQuake extends PassiveSkill {

    public SkillQuake(Heroes plugin) {
        super(plugin, "Quake");
        setDescription("You hit the ground with a thunderous roar!");
        setArgumentRange(0, 0);
        setEffectTypes(EffectType.PHYSICAL, EffectType.BENEFICIAL);
        setTypes(SkillType.PHYSICAL);

        registerEvent(Event.Type.ENTITY_DAMAGE, new SkillDamageListener(), Event.Priority.Highest);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("damage", 0.10);
        node.set("radius", 10);
        return node;
    }

    public class SkillDamageListener extends EntityListener {

        public void onEntityDamage(EntityDamageEvent event) {
            Heroes.debug.startTask("HeroesSkillListener");
            if (event.getCause() != DamageCause.FALL || !(event.getEntity() instanceof Player)) {
                Heroes.debug.stopTask("HeroesSkillListener");
                return;
            }

            Player player = (Player) event.getEntity();
            Hero hero = plugin.getHeroManager().getHero(player);

            if (!hero.hasEffect(getName())) {
                Heroes.debug.stopTask("HeroesSkillListener");
                return;
            }

            double damage = event.getDamage() * getSetting(hero, "damage", 0.10, false);
            int radius = getSetting(hero, "radius", 10, false);

            for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
                if (!(entity instanceof LivingEntity))
                    continue;

                LivingEntity target = (LivingEntity) entity;
                if (!damageCheck(player, target)) {
                    continue;
                }
                addSpellTarget(target, hero);
                target.damage((int) damage, player);
            }

            Heroes.debug.stopTask("HeroesSkillListener");
        }
    }
}
