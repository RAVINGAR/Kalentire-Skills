package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainManaEvent;
import com.herocraftonline.heroes.attributes.AttributeType;
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
        setTypes(SkillType.BUFFING, SkillType.MANA_INCREASING);
    }

    @Override
    public String getDescription(Hero hero) {
        double manaGainPercent = SkillConfigManager.getUseSetting(hero, this, "mana-gain-percent", 0.15, false);
        double manaGainPercentIncrease = SkillConfigManager.getUseSetting(hero, this, "mana-gain-percent-increase-per-wisdom", 0.015, false);
        manaGainPercent += manaGainPercentIncrease * hero.getAttributeValue(AttributeType.WISDOM);

        int manaIncreaseAmount = (int) (hero.getMaxMana() * manaGainPercent);
        String formattedManaPercent = Util.decFormat.format(manaGainPercent * 100);

        return getDescription().replace("$1", formattedManaPercent).replace("$2", manaIncreaseAmount + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set("mana-gain-percent", 0.20);
        node.set("mana-gain-percent-increase-per-wisdom", 0.015);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero);

        double manaGainPercent = SkillConfigManager.getUseSetting(hero, this, "mana-gain-percent", 0.15, false);
        double manaGainPercentIncrease = SkillConfigManager.getUseSetting(hero, this, "mana-gain-percent-increase-per-wisdom", 0.015, false);
        manaGainPercent += manaGainPercentIncrease * hero.getAttributeValue(AttributeType.WISDOM);

        int manaIncreaseAmount = (int) (hero.getMaxMana() * manaGainPercent);

        HeroRegainManaEvent hrmEvent = new HeroRegainManaEvent(hero, manaIncreaseAmount, this);
        plugin.getServer().getPluginManager().callEvent(hrmEvent);
        if (!hrmEvent.isCancelled()) {
            hero.setMana(hrmEvent.getAmount() + hero.getMana());

            if (hero.isVerboseMana())
                Messaging.send(player, Messaging.createFullManaBar(hero.getMana(), hero.getMaxMana()));
        }

        player.getWorld().playSound(player.getLocation(), Sound.ORB_PICKUP, 0.8F, 1.0F);
        
        player.getWorld().spigot().playEffect(player.getLocation(), Effect.SPLASH, 0, 0, 0, 0.1F, 0, 0.1F, 35, 5);

        return SkillResult.NORMAL;
    }
}