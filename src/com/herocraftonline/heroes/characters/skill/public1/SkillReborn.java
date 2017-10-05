package com.herocraftonline.heroes.characters.skill.public1;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.events.HeroRegainHealthEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class SkillReborn extends PassiveSkill {

    private String rebornText;

    public SkillReborn(Heroes plugin) {
        super(plugin, "Reborn");
        this.setDescription("If you are about to die instead you regain $1% hp, can only trigger once every $2 seconds.");
        this.setTypes(SkillType.DISABLE_COUNTERING, SkillType.ABILITY_PROPERTY_DARK);
        Bukkit.getServer().getPluginManager().registerEvents(new RebornListener(this), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();
        node.set("health-percent-on-rebirth", .5);
        node.set("health-increase", 0.0);
        node.set("on-reborn-text", "%hero% is saved from death, but weakened!");
        node.set(SkillSetting.COOLDOWN.node(), 600000);
        node.set(SkillSetting.COOLDOWN_REDUCE.node(), 0);
        return node;
    }

    @Override
    public String getDescription(Hero hero) {
        final double health = ((SkillConfigManager.getUseSetting(hero, this, "health-percent-on-rebirth", 0.5, false) + (SkillConfigManager.getUseSetting(hero, this, "health-increase", 0.0, false) * hero.getHeroLevel(this))) * 100);
        final int cooldown = SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN.node(), 600000, false) + (SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN_REDUCE.node(), 0, false) * hero.getHeroLevel(this));
        final String description = this.getDescription().replace("$1", health + "").replace("$2", cooldown + "");
        return description;
    }

    @Override
    public void init() {
        super.init();
        this.rebornText = SkillConfigManager.getUseSetting(null, this, "on-reborn-text", "%hero% is saved from death, but weakened!").replace("%hero%", "$1");
    }

    public class RebornListener implements Listener {

        private final Skill skill;

        public RebornListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onEntityDamage(EntityDamageEvent event) {
            if (event.isCancelled() || !(event.getEntity() instanceof Player) || (event.getDamage() == 0)) {
                return;
            }
            final Player player = (Player) event.getEntity();
            final Hero hero = SkillReborn.this.plugin.getCharacterManager().getHero(player);
            final double currentHealth = player.getHealth();
            if (currentHealth > event.getDamage()) {
                return;
            }
            if (hero.hasEffect("Reborn")) {
                if ((hero.getCooldown("Reborn") == null) || (hero.getCooldown("Reborn") <= System.currentTimeMillis())) {
                    final double regainPercent = SkillConfigManager.getUseSetting(hero, this.skill, "health-percent-on-rebirth", 0.5, false) + (SkillConfigManager.getUseSetting(hero, this.skill, "health-increase", 0.0, false) * hero.getHeroLevel(this.skill));
                    final double healthRegain = (player.getMaxHealth() * regainPercent);
                    final HeroRegainHealthEvent hrh = new HeroRegainHealthEvent(hero, healthRegain, this.skill, hero);
                    if (hrh.isCancelled() || (hrh.getDelta() == 0)) {
                        return;
                    }
                    event.setDamage(0);
                    event.setCancelled(true);
                    hero.heal(hrh.getDelta());
                    final long cooldown = SkillConfigManager.getUseSetting(hero, this.skill, SkillSetting.COOLDOWN.node(), 600000, false) + (SkillConfigManager.getUseSetting(hero, this.skill, SkillSetting.COOLDOWN_REDUCE.node(), 0, false) * hero.getLevel());
                    hero.setCooldown("Reborn", cooldown + System.currentTimeMillis());
                    SkillReborn.this.broadcast(player.getLocation(), SkillReborn.this.rebornText, player.getDisplayName());
                    player.getWorld().playEffect(player.getLocation(), Effect.SMOKE, 3);
                }
            }
        }
    }
}
