package com.herocraftonline.heroes.characters.skill.unfinishedskills;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainManaEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

public class SkillReplenish extends ActiveSkill {

    public SkillReplenish(Heroes plugin) {
        super(plugin, "Replenish");
        setDescription("You regain $1% (" + ChatColor.BLUE + "$2" + ChatColor.GOLD + ") of your mana.");
        setUsage("/skill replenish");
        setArgumentRange(0, 0);
        setIdentifiers("skill replenish");
        setTypes(SkillType.MANA);
    }

    @Override
    public String getDescription(Hero hero) {
        double percent = Util.formatDouble(SkillConfigManager.getUseSetting(hero, this, "mana-bonus", 1.0, false));
        percent += Util.formatDouble(SkillConfigManager.getUseSetting(hero, this, "mana-bonus-per-level", 0.0, false) * hero.getSkillLevel(this));
        int amount = (int) (hero.getMaxMana() * percent);

        return getDescription().replace("$1", Util.stringDouble(percent * 100)).replace("$2", amount + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("mana-bonus", 1.0);
        node.set("mana-bonus-per-level", 0.0);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        double percent = SkillConfigManager.getUseSetting(hero, this, "mana-bonus", 1.0, false);
        percent += SkillConfigManager.getUseSetting(hero, this, "mana-bonus-per-level", 0.0, false) * hero.getSkillLevel(this);
        int manaBonus = (int) (hero.getMaxMana() * percent);
        HeroRegainManaEvent hrmEvent = new HeroRegainManaEvent(hero, manaBonus, this);
        plugin.getServer().getPluginManager().callEvent(hrmEvent);
        if (hrmEvent.isCancelled()) {
            return SkillResult.CANCELLED;
        }

        hero.setMana(hrmEvent.getAmount() + hero.getMana());
        if (hero.isVerbose()) {
            Messaging.send(hero.getPlayer(), Messaging.createManaBar(hero.getMana(), hero.getMaxMana()));
        }
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.ORB_PICKUP , 0.8F, 1.0F); 
        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }
}
