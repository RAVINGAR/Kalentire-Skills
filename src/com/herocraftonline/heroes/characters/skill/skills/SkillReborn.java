package com.herocraftonline.heroes.characters.skill.skills;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.events.HeroRegainHealthEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Setting;

public class SkillReborn extends PassiveSkill {
    private String rebornText;

    public SkillReborn(Heroes plugin) {
        super(plugin, "Reborn");
        setDescription("If you are about to die instead you regain $1% hp, can only trigger once every $2 seconds.");
        setTypes(SkillType.COUNTER, SkillType.DARK);
        Bukkit.getServer().getPluginManager().registerEvents(new RebornListener(this), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("health-percent-on-rebirth", .5);
        node.set("health-increase", 0.0);
        node.set("on-reborn-text", "%hero% is saved from death, but weakened!");
        node.set(Setting.COOLDOWN.node(), 600000);
        node.set(Setting.COOLDOWN_REDUCE.node(), 0);
        return node;
    }

    @Override
    public String getDescription(Hero hero) {
        int health = (int) ((SkillConfigManager.getUseSetting(hero, this, "health-percent-on-rebirth", 0.5, false)
                + (SkillConfigManager.getUseSetting(hero, this, "health-increase", 0.0, false) * hero.getSkillLevel(this))) * 100);
        int cooldown = SkillConfigManager.getUseSetting(hero, this, Setting.COOLDOWN.node(), 600000, false)
                + (SkillConfigManager.getUseSetting(hero, this, Setting.COOLDOWN_REDUCE.node(), 0, false) * hero.getSkillLevel(this));
        String description = getDescription().replace("$1", health + "").replace("$2", cooldown + "");
        return description;
    }
    
    @Override
    public void init() {
        super.init();
        rebornText = SkillConfigManager.getUseSetting(null, this, "on-reborn-text", "%hero% is saved from death, but weakened!").replace("%hero%", "$1");
    }
    
    public class RebornListener implements Listener {
        private Skill skill;
        public RebornListener(Skill skill) {
            this.skill = skill;
        }
        
        @EventHandler(priority = EventPriority.MONITOR)
        public void onEntityDamage(EntityDamageEvent event) {
            if (event.isCancelled() || !(event.getEntity() instanceof Player) || event.getDamage() == 0) {
                return;
            }
            Player player = (Player) event.getEntity();
            Hero hero = plugin.getCharacterManager().getHero(player);
            int currentHealth = hero.getHealth();
            if (currentHealth > event.getDamage()) {
                return;
            }
            if (hero.hasEffect("Reborn")) {
                if (hero.getCooldown("Reborn") == null || hero.getCooldown("Reborn") <= System.currentTimeMillis()) {
                    double regainPercent = SkillConfigManager.getUseSetting(hero, skill, "health-percent-on-rebirth", 0.5, false)
                            + (SkillConfigManager.getUseSetting(hero, skill, "health-increase", 0.0, false) * hero.getSkillLevel(skill));
                    int healthRegain = (int) (hero.getMaxHealth() * regainPercent);
                    HeroRegainHealthEvent hrh = new HeroRegainHealthEvent(hero, healthRegain, skill, hero);
                    if (hrh.isCancelled() || hrh.getAmount() == 0) {
                        return;
                    }
                    event.setDamage(0);
                    event.setCancelled(true);
                    hero.setHealth(currentHealth + hrh.getAmount());
                    hero.syncHealth();
                    long cooldown = (long) (SkillConfigManager.getUseSetting(hero, skill, Setting.COOLDOWN.node(), 600000, false)
                            + (SkillConfigManager.getUseSetting(hero, skill, Setting.COOLDOWN_REDUCE.node(), 0, false) * hero.getLevel()));
                    hero.setCooldown("Reborn", cooldown + System.currentTimeMillis());
                    broadcast(player.getLocation(),rebornText,player.getDisplayName());
                    player.getWorld().playEffect(player.getLocation(), Effect.SMOKE, 3);
                }
            }
        }
    }
}
