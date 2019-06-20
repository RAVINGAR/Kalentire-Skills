package com.herocraftonline.heroes.characters.skill.reborn.disciple.rework;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainManaEvent;
import com.herocraftonline.heroes.api.events.HeroRegainStaminaEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class SkillMeditate extends ActiveSkill {
	
	public SkillMeditate(Heroes plugin) {
        super(plugin, "Meditate");
        setDescription("You perform a meditation based on your current stance.\n" +
                ChatColor.GRAY + "Unfocused: " + ChatColor.WHITE + "You regain $1% of your mana, and $2% of your stamina\n" +
                ChatColor.GOLD + "Tiger: " + ChatColor.WHITE + "You gain a burst of speed for $3 second(s).\n" +
                ChatColor.YELLOW + "Jin: " + ChatColor.WHITE + "You heal all party members within $4 blocks for $5 and cleanse $6 harmful effect(s) from them."
        );
        setUsage("/skill meditate");
        setArgumentRange(0, 0);
        setIdentifiers("skill meditate");
        setTypes(SkillType.MANA_INCREASING, SkillType.STAMINA_INCREASING);
    }

    @Override
    public String getDescription(Hero hero) {
        double manaPercent = SkillConfigManager.getUseSetting(hero, this, "mana-restore-percent", 0.5, false);
        double staminaPercent = SkillConfigManager.getUseSetting(hero, this, "stamina-restore-percent", 0.7, false);
	    int speedDuration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 3000, false);

        return getDescription()
                .replace("$1", Util.decFormat.format(manaPercent * 100))
                .replace("$2", Util.decFormat.format(staminaPercent * 100))
                .replace("$4", Util.decFormat.format(speedDuration / 1000.0));
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set("mana-restore-percent", 0.5);
        config.set("stamina-restore-percent", 0.7);
        return config;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero);

        double manaGainPercent = SkillConfigManager.getUseSetting(hero, this, "mana-bonus", 0.5, false);
        int manaBonus = (int) Math.floor(hero.getMaxMana() * manaGainPercent);

        HeroRegainManaEvent hrmEvent = new HeroRegainManaEvent(hero, manaBonus, this);
        plugin.getServer().getPluginManager().callEvent(hrmEvent);
        if (!hrmEvent.isCancelled()) {
            hero.setMana(hrmEvent.getDelta() + hero.getMana());
            
            if (hero.isVerboseMana())
                player.sendMessage(ChatComponents.Bars.mana(hero.getMana(), hero.getMaxMana(), true));
        }

        double staminaGainPercent = SkillConfigManager.getUseSetting(hero, this, "stamina-bonus", 0.7, false);
        int staminaBonus = (int) Math.ceil(hero.getMaxStamina() * staminaGainPercent);

        HeroRegainStaminaEvent hrsEvent = new HeroRegainStaminaEvent(hero, staminaBonus, this);
        plugin.getServer().getPluginManager().callEvent(hrsEvent);
        if (!hrsEvent.isCancelled()) {
            hero.setStamina(hrsEvent.getDelta() + hero.getStamina());
            
            if (hero.isVerboseStamina())
                player.sendMessage(ChatComponents.Bars.stamina(hero.getStamina(), hero.getMaxStamina(), true));
        }
        
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5F, 1.0F);
        //player.getWorld().spigot().playEffect(player.getLocation().add(0, 0.5, 0), org.bukkit.Effect.VILLAGER_THUNDERCLOUD, 0, 0, 0.3F, 0.3F, 0.3F, 0.0F, 25, 16);
        player.getWorld().spawnParticle(Particle.VILLAGER_ANGRY, player.getLocation().add(0, 0.5, 0), 25, 0.3, 0.3, 0.3, 0);
        //player.getWorld().spigot().playEffect(player.getLocation().add(0, 0.5, 0), org.bukkit.Effect.SPLASH, 0, 0, 0.3F, 0.3F, 0.3F, 0.1F, 25, 16);
        player.getWorld().spawnParticle(Particle.WATER_SPLASH, player.getLocation().add(0, 0.5, 0), 25, 0.5, 0.5, 0.5, 0.1);

        return SkillResult.NORMAL;
    }
}
