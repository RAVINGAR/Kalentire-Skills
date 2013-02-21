package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;

public class SkillQuake extends PassiveSkill {

    public SkillQuake(Heroes plugin) {
        super(plugin, "Quake");
        setDescription("You hit the ground with a thunderous roar dealing damage to nearby players!");
        setArgumentRange(0, 0);
        setEffectTypes(EffectType.PHYSICAL, EffectType.BENEFICIAL);
        setTypes(SkillType.PHYSICAL);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillDamageListener(this), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("damage", 0.10);
        node.set("radius", 10);
        return node;
    }

    public class SkillDamageListener implements Listener {

        private final Skill skill;
        
        public SkillDamageListener(Skill skill) {
            this.skill = skill;
        }
        
        @EventHandler(priority = EventPriority.MONITOR)
        public void onEntityDamage(EntityDamageEvent event) {
            if (event.getCause() != DamageCause.FALL || !(event.getEntity() instanceof Player) || event.isCancelled() || event.getDamage() == 0) {
                return;
            }
            if (event.getDamage() > ((LivingEntity) event.getEntity()).getHealth()) {
                return;
            }
            Player player = (Player) event.getEntity();
            Hero hero = plugin.getCharacterManager().getHero(player);

            if (!hero.hasEffect(getName())) {
                return;
            }

            double damage = event.getDamage() * SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE, 0.10, false);
            int radius = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.RADIUS, 10, false);

            for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
                if (!(entity instanceof LivingEntity)) {
                    continue;
                }

                LivingEntity target = (LivingEntity) entity;
                if (!damageCheck(player, target)) {
                    continue;
                }
                addSpellTarget(target, hero);
                Skill.damageEntity(target, player, (int) damage, DamageCause.ENTITY_ATTACK);
            }
        }
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }
}
