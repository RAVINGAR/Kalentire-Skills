package com.herocraftonline.dev.heroes.skill.skills;

import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.api.HeroRegainManaEvent;
import com.herocraftonline.dev.heroes.api.SkillResult;
import com.herocraftonline.dev.heroes.hero.Hero;
import com.herocraftonline.dev.heroes.skill.ActiveSkill;
import com.herocraftonline.dev.heroes.skill.SkillConfigManager;
import com.herocraftonline.dev.heroes.skill.SkillType;
import com.herocraftonline.dev.heroes.util.Messaging;
import com.herocraftonline.dev.heroes.util.Setting;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

public class SkillTransfuse extends ActiveSkill {

    public SkillTransfuse(Heroes plugin) {
        super(plugin, "Transfuse");
        setDescription("Converts $1 health in $2 mana.");
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
        int healthCost = SkillConfigManager.getUseSetting(hero, this, "health-cost", 10, false);
        int manaGain = SkillConfigManager.getUseSetting(hero, this, "mana-gain", 10, false);
        
        if (hero.getHealth() <= healthCost) {
            Messaging.send(hero.getPlayer(), "You don't have enough health.");
            return SkillResult.LOW_HEALTH;
        }
        
        if (hero.getMana() > 99) {
            Messaging.send(hero.getPlayer(), "You are already at full mana.");
            return SkillResult.FAIL;
        }
        
        HeroRegainManaEvent hrmEvent = new HeroRegainManaEvent(hero, manaGain, this);
        plugin.getServer().getPluginManager().callEvent(hrmEvent);
        if (hrmEvent.isCancelled()) {
            return SkillResult.CANCELLED;
        }

        hero.setMana(hrmEvent.getAmount() + hero.getMana());

        Player player = hero.getPlayer();
        addSpellTarget(player, hero);
        damageEntity(player, player, healthCost, EntityDamageEvent.DamageCause.MAGIC);

        broadcastExecuteText(hero);

        if (hero.isVerbose()) {
            Messaging.send(hero.getPlayer(), Messaging.createManaBar(100));
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
