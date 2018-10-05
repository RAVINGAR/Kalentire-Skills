package com.herocraftonline.heroes.characters.skill.pack1;

import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.Sound;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainManaEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.chat.ChatComponents;

public class SkillTransfuse extends ActiveSkill {

    public SkillTransfuse(Heroes plugin) {
        super(plugin, "Transfuse");
        setDescription("Converts $1 health to $2 mana.");
        setUsage("/skill transfuse");
        setArgumentRange(0, 0);
        setIdentifiers("skill transfuse");
        setTypes(SkillType.MANA_INCREASING, SkillType.ABILITY_PROPERTY_DARK);
    }

    @Override
    public String getDescription(Hero hero) {
        int healthCost = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALTH_COST, 100, false);
        int manaGain = SkillConfigManager.getUseSetting(hero, this, "mana-gain", 150, false);

        return getDescription().replace("$1", healthCost + "").replace("$2", manaGain + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.HEALTH_COST.node(), 100);
        node.set("mana-gain", 150);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        int manaGain = SkillConfigManager.getUseSetting(hero, this, "mana-gain", 150, false);

        if (hero.getMana() >= hero.getMaxMana()) {
            hero.getPlayer().sendMessage("You are already at full mana.");
            return SkillResult.FAIL;
        }

        HeroRegainManaEvent hrmEvent = new HeroRegainManaEvent(hero, manaGain, this);
        plugin.getServer().getPluginManager().callEvent(hrmEvent);
        if (hrmEvent.isCancelled()) {
            return SkillResult.CANCELLED;
        }

        Player player = hero.getPlayer();

        broadcastExecuteText(hero);

        hero.setMana(hrmEvent.getDelta() + hero.getMana());
        if (hero.isVerboseMana()) {
            hero.getPlayer().sendMessage(ChatComponents.Bars.mana(hero.getMana(), hero.getMaxMana(), true));
        }

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8F, 1.0F);

        return SkillResult.NORMAL;
    }
}
