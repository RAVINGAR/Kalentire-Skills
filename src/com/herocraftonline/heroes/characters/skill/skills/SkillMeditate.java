package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainManaEvent;
import com.herocraftonline.heroes.api.events.HeroRegainStaminaEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

public class SkillMeditate extends ActiveSkill {
	
	public SkillMeditate(Heroes plugin) {
        super(plugin, "Meditate");
        setDescription("You regain $1% (" + ChatColor.BLUE + "$2" + ChatColor.GOLD + ") of your mana, and $3% (" + ChatColor.YELLOW + "$4" + ChatColor.GOLD + ") of your stamina");
        setUsage("/skill meditate");
        setArgumentRange(0, 0);
        setIdentifiers("skill meditate");
        setTypes(SkillType.MANA_INCREASING, SkillType.STAMINA_INCREASING);
    }

    @Override
    public String getDescription(Hero hero) {
        double manaPercent = SkillConfigManager.getUseSetting(hero, this, "mana-bonus", 0.5, false);
        int manaAmount = (int) (hero.getMaxMana() * manaPercent);
        String formattedManaPercent = Util.decFormat.format(manaPercent * 100);

        double staminaPercent = SkillConfigManager.getUseSetting(hero, this, "stamina-bonus", 0.7, false);
        int staminaAmount = (int) (hero.getMaxStamina() * staminaPercent);
        String formattedStaminaPercent = Util.decFormat.format(staminaPercent * 100);

        return getDescription().replace("$1", formattedManaPercent).replace("$2", manaAmount + "").replace("$3", formattedStaminaPercent).replace("$4", staminaAmount + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set("mana-bonus", 0.5);
        node.set("stamina-bonus", 0.7);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero);

        double manaGainPercent = SkillConfigManager.getUseSetting(hero, this, "mana-bonus", 0.5, false);
        int manaBonus = (int) (hero.getMaxMana() * manaGainPercent);

        HeroRegainManaEvent hrmEvent = new HeroRegainManaEvent(hero, manaBonus, this);
        plugin.getServer().getPluginManager().callEvent(hrmEvent);
        if (!hrmEvent.isCancelled()) {
            hero.setMana(hrmEvent.getAmount() + hero.getMana());

            if (hero.isVerbose())
                Messaging.send(player, Messaging.createManaBar(hero.getMana(), hero.getMaxMana()));
        }

        double staminaGainPercent = SkillConfigManager.getUseSetting(hero, this, "stamina-bonus", 0.7, false);
        int staminaBonus = (int) (hero.getMaxStamina() * staminaGainPercent);

        HeroRegainStaminaEvent hrsEvent = new HeroRegainStaminaEvent(hero, staminaBonus, this);
        plugin.getServer().getPluginManager().callEvent(hrsEvent);
        if (!hrsEvent.isCancelled()) {
            hero.setStamina(hrsEvent.getAmount() + hero.getStamina());

            if (hero.isVerbose())
                Messaging.send(player, Messaging.createStaminaBar(hero.getStamina(), hero.getMaxStamina()));
        }
        
        player.getWorld().playSound(player.getLocation(), Sound.WITHER_SPAWN, 0.5F, 1.0F);

        return SkillResult.NORMAL;
    }
}
