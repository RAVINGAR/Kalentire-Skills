package com.herocraftonline.heroes.characters.skill.unusedskills;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Effect;
import org.bukkit.FireworkEffect;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainHealthEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.PeriodicHealEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.VisualEffect;

public class SkillReborn extends ActiveSkill {
    private String rebornText;

    public SkillReborn(Heroes plugin) {
        super(plugin, "Reborn");
        setDescription("If you are about to die instead you regain $1% hp, can only trigger once every $2 seconds.");
        setTypes(SkillType.ABILITY_PROPERTY_LIGHT);
        Bukkit.getServer().getPluginManager().registerEvents(new RebornListener(this), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("health-percent-on-rebirth", .5);
        node.set("health-increase", 0.0);
        node.set("on-reborn-text", "%hero% is saved from death, but weakened!");
        node.set(SkillSetting.COOLDOWN.node(), 600000);
        node.set(SkillSetting.COOLDOWN_REDUCE.node(), 0);
        return node;
    }

    @Override
    public String getDescription(Hero hero) {
        int health = (int) ((SkillConfigManager.getUseSetting(hero, this, "health-percent-on-rebirth", 0.5, false)
                + (SkillConfigManager.getUseSetting(hero, this, "health-increase", 0.0, false) * hero.getSkillLevel(this))) * 100);
        int cooldown = SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN.node(), 600000, false)
                + (SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN_REDUCE.node(), 0, false) * hero.getSkillLevel(this));
        String description = getDescription().replace("$1", health + "").replace("$2", cooldown + "");
        return description;
    }
    
    @Override
    public void init() {
        super.init();
        rebornText = SkillConfigManager.getUseSetting(null, this, "on-reborn-text", "%hero% is saved from death, but weakened!").replace("%hero%", "$1");
    }
    
    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero);

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 20000, false);
        int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 2500, true);
        double tickDamage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING_TICK, (double) 17, false);

        plugin.getCharacterManager().getCharacter(player).addEffect(new PeriodicHealEffect(this, "Reborn", player, duration, period, tickDamage));

        // this is our fireworks shit
        VisualEffect fplayer = new VisualEffect();
        try {
            fplayer.playFirework(player.getWorld(),
                                 player.getLocation().add(0, 1.5, 0),
                                 FireworkEffect.builder()
                                               .flicker(true).trail(false)
                                               .with(FireworkEffect.Type.BALL)
                                               .withColor(Color.BLACK)
                                               .withFade(Color.GRAY)
                                               .build());
        }
        catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return SkillResult.NORMAL;
    }

    public class RebornListener implements Listener {
        private Skill skill;
        public RebornListener(Skill skill) {
            this.skill = skill;
        }
        
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onEntityDamage(EntityDamageEvent event) {
            if (!(event.getEntity() instanceof Player) || event.getDamage() == 0) {
                return;
            }
            Player player = (Player) event.getEntity();
            Hero hero = plugin.getCharacterManager().getHero(player);
            double currentHealth = player.getHealth();
            if (currentHealth > event.getDamage()) {
                return;
            }
            if (hero.hasEffect("Reborn")) {
                if (hero.getCooldown("Reborn") == null || hero.getCooldown("Reborn") <= System.currentTimeMillis()) {
                    double regainPercent = SkillConfigManager.getUseSetting(hero, skill, "health-percent-on-rebirth", 0.5, false)
                            + (SkillConfigManager.getUseSetting(hero, skill, "health-increase", 0.0, false) * hero.getSkillLevel(skill));
                    double healthRegain = (player.getMaxHealth() * regainPercent);
                    HeroRegainHealthEvent hrh = new HeroRegainHealthEvent(hero, healthRegain, skill, hero);
                    if (hrh.isCancelled() || hrh.getDelta() == 0.0) {
                        return;
                    }
                    event.setDamage(0.0);
                    event.setCancelled(true);
                    hero.heal(hrh.getDelta());
                    long cooldown = (long) (SkillConfigManager.getUseSetting(hero, skill, SkillSetting.COOLDOWN.node(), 600000, false)
                            + (SkillConfigManager.getUseSetting(hero, skill, SkillSetting.COOLDOWN_REDUCE.node(), 0, false) * hero.getLevel()));
                    hero.setCooldown("Reborn", cooldown + System.currentTimeMillis());
                    broadcast(player.getLocation(),rebornText,player.getName());
                    player.getWorld().playEffect(player.getLocation(), Effect.SMOKE, 3);
                }
            }
        }
    }
}
