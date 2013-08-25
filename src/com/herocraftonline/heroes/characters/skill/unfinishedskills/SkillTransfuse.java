package com.herocraftonline.heroes.characters.skill.unfinishedskills;

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

public class SkillTransfuse extends ActiveSkill {

    public SkillTransfuse(Heroes plugin) {
        super(plugin, "Transfuse");
        setDescription("Converts $1 health to $2 mana.");
        setUsage("/skill transfuse");
        setArgumentRange(0, 0);
        setIdentifiers("skill transfuse");
        setTypes(SkillType.MANA, SkillType.DARK);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("health-cost", 10);
        node.set("mana-gain", 10);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        int manaGain = SkillConfigManager.getUseSetting(hero, this, "mana-gain", 10, false);

        if (hero.getMana() >= hero.getMaxMana()) {
            Messaging.send(hero.getPlayer(), "You are already at full mana.");
            return SkillResult.FAIL;
        }
        
        HeroRegainManaEvent hrmEvent = new HeroRegainManaEvent(hero, manaGain, this);
        plugin.getServer().getPluginManager().callEvent(hrmEvent);
        if (hrmEvent.isCancelled()) {
            return SkillResult.CANCELLED;
        }

        hero.setMana(hrmEvent.getAmount() + hero.getMana());

        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.ORB_PICKUP , 0.8F, 1.0F);
        broadcastExecuteText(hero);

        if (hero.isVerbose()) {
            Messaging.send(hero.getPlayer(), Messaging.createManaBar(hero.getMana(), hero.getMaxMana()));
        }
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(Hero hero) {
        int healthCost = SkillConfigManager.getUseSetting(hero, this, "health-cost", 10, false);
        int manaGain = SkillConfigManager.getUseSetting(hero, this, "mana-gain", 10, false);
        return getDescription().replace("$1", healthCost + "").replace("$2", manaGain + "");
    }

}
