package com.herocraftonline.dev.heroes.skill.skills;

import org.bukkit.configuration.ConfigurationSection;

import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.api.HeroRegainManaEvent;
import com.herocraftonline.dev.heroes.api.SkillResult;
import com.herocraftonline.dev.heroes.hero.Hero;
import com.herocraftonline.dev.heroes.skill.ActiveSkill;
import com.herocraftonline.dev.heroes.skill.SkillConfigManager;
import com.herocraftonline.dev.heroes.skill.SkillType;
import com.herocraftonline.dev.heroes.util.Messaging;
import com.herocraftonline.dev.heroes.util.Util;

public class SkillReplenish extends ActiveSkill {

    public SkillReplenish(Heroes plugin) {
        super(plugin, "Replenish");
        setDescription("You regain up to $1% of your mana.");
        setUsage("/skill replenish");
        setArgumentRange(0, 0);
        setIdentifiers("skill replenish");
        setTypes(SkillType.MANA);
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
            Messaging.send(hero.getPlayer(), Messaging.createManaBar(100, hero.getMaxMana()));
        }
        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(Hero hero) {
        double percent = SkillConfigManager.getUseSetting(hero, this, "mana-bonus", 1.0, false);
        percent += SkillConfigManager.getUseSetting(hero, this, "mana-bonus-per-level", 0.0, false) * hero.getSkillLevel(this);
        return getDescription().replace("$1", Util.stringDouble(percent * 100));
    }

}
