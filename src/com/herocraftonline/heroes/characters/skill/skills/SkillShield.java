package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;

public class SkillShield extends PassiveSkill {

    public SkillShield(Heroes plugin) {
        super(plugin, "Shield");
        setDescription("You are able to use doors as shields to absorbs damage!");
        setArgumentRange(0, 0);
        setEffectTypes(EffectType.BENEFICIAL, EffectType.PHYSICAL);
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL);
        Bukkit.getServer().getPluginManager().registerEvents(new CustomListener(this), plugin);
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
        node.set("iron-door", 0.85);
        node.set("wooden-door", 0.90);
        node.set("trapdoor", 0.95);
        
        return node;
    }

    public class CustomListener implements Listener {

        private final Skill skill;
        
        public CustomListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.HIGHEST)
        public void onWeaponDamage(WeaponDamageEvent event) {
            if (event.getCause() != DamageCause.ENTITY_ATTACK || event.getDamage() == 0 || !(event.getEntity() instanceof Player)) {
                return;
            }

            Player player = (Player) event.getEntity();
            Hero hero = plugin.getCharacterManager().getHero(player);
            if (hero.hasEffect(getName())) {
                double multiplier = 1;
                if (player.getItemInHand().getType() == Material.IRON_DOOR) {
                    multiplier = SkillConfigManager.getUseSetting(hero, skill, "iron-door", 0.75, true);
                } else if (player.getItemInHand().getType() == Material.WOOD_DOOR) {
                    multiplier = SkillConfigManager.getUseSetting(hero, skill, "wooden-door", 0.85, true);
                } else if (player.getItemInHand().getType() == Material.TRAP_DOOR) {
                    multiplier = SkillConfigManager.getUseSetting(hero, skill, "trapdoor", 0.60, true);
                }
                event.setDamage((event.getDamage() * multiplier));
            }
        }
    }
}