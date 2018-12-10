package com.herocraftonline.heroes.characters.skill.skills;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.CustomNameManager;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Util;

public class SkillFireArmor extends PassiveSkill {

    private String igniteText;
    private List<String> defaultArmors = new ArrayList<String>();

    public SkillFireArmor(Heroes plugin) {
        super(plugin, "FireArmor");
        setDescription("Your armor has a $1% chance to ignite your attackers!");
        setTypes(SkillType.ABILITY_PROPERTY_FIRE, SkillType.SILENCEABLE, SkillType.BUFFING);
        setEffectTypes(EffectType.FIRE);
        defaultArmors.add(Material.GOLDEN_CHESTPLATE.name());
        Bukkit.getServer().getPluginManager().registerEvents(new SkillDamageListener(this), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("armors", defaultArmors);
        node.set("ignite-chance", 0.20);
        node.set("ignite-duration", 5000);
        node.set("ignite-text", "%hero% ignited %target% with firearmor!");
        return node;
    }

    @Override
    public void init() {
        super.init();
        igniteText = SkillConfigManager.getRaw(this, "ignite-text", "%hero% ignited %target% with firearmor!");
    }

    public class SkillDamageListener implements Listener {

        private final Skill skill;
        
        public SkillDamageListener(Skill skill) {
            this.skill = skill;
        }
        
        @EventHandler(priority = EventPriority.MONITOR)
        public void onEntityDamage(EntityDamageEvent event) {
            if (event.isCancelled() || !(event.getEntity() instanceof Player) || !(event instanceof EntityDamageByEntityEvent) || event.getDamage() == 0) {
                return;
            }

            Player player = (Player) event.getEntity();
            Hero hero = plugin.getCharacterManager().getHero(player);
            ItemStack chest = player.getInventory().getChestplate() ;
            if (!hero.hasEffect("FireArmor") || chest == null || !SkillConfigManager.getUseSetting(hero, skill, "armors", defaultArmors).contains(chest.getType().name())) {
                return;
            }
            EntityDamageByEntityEvent subEvent = (EntityDamageByEntityEvent) event;
            // Dont set Projectiles on fire
            if (!(subEvent.getDamager() instanceof LivingEntity)) {
                return;
            }

            // Check our ignite chance
            double chance = SkillConfigManager.getUseSetting(hero, skill, "ignite-chance", .2, false);
            if (Util.nextRand() >= chance) {
                return;
            }

            // Set the damager on fire if it was successful
            int fireTicks = SkillConfigManager.getUseSetting(hero, skill, "ignite-duration", 5000, false) / 50;
            subEvent.getDamager().setFireTicks(fireTicks);

            String name = null;
            if (subEvent.getDamager() instanceof Player) {
                name = ((Player) subEvent.getDamager()).getName();
            } else {
                name = CustomNameManager.getName((LivingEntity) subEvent.getDamager());
            }
            
            broadcast(player.getLocation(), igniteText.replace("%hero%", player.getName()).replace("%target%", name));
        }
    }

    @Override
    public String getDescription(Hero hero) {
        double chance = SkillConfigManager.getUseSetting(hero, this, "ignite-chance", .20, false);
        return getDescription().replace("$1", chance * 100 + "");
    }
}
