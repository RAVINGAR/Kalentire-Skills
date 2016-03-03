package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainManaEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

public class SkillInnervate extends ActiveSkill {

    public SkillInnervate(Heroes plugin) {
        super(plugin, "Innervate");
        setDescription("You regain $1% (" + ChatColor.BLUE + "$2" + ChatColor.GOLD + ") of your mana.");
        setUsage("/skill innervate");
        setArgumentRange(0, 0);
        setIdentifiers("skill innervate");
        setTypes(SkillType.BUFFING, SkillType.MANA_INCREASING);
    }

    @Override
    public String getDescription(Hero hero) {
        double manaPercent = SkillConfigManager.getUseSetting(hero, this, "mana-bonus", 0.75, false);
        int manaAmount = (int) (hero.getMaxMana() * manaPercent);
        String formattedManaPercent = Util.decFormat.format(manaPercent * 100);

        return getDescription().replace("$1", formattedManaPercent).replace("$2", manaAmount + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set("mana-bonus", Double.valueOf(0.75));

        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        broadcastExecuteText(hero);

        double manaGainPercent = SkillConfigManager.getUseSetting(hero, this, "mana-bonus", Double.valueOf(0.75), false);
        int manaBonus = (int) Math.floor(hero.getMaxMana() * manaGainPercent);

        HeroRegainManaEvent hrmEvent = new HeroRegainManaEvent(hero, manaBonus, this);
        plugin.getServer().getPluginManager().callEvent(hrmEvent);
        if (!hrmEvent.isCancelled()) {
            hero.setMana(hrmEvent.getAmount() + hero.getMana());

            if (hero.isVerboseMana())
                Messaging.send(player, Messaging.createFullManaBar(hero.getMana(), hero.getMaxMana()));
        }

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8F, 1.0F);
        
        player.getWorld().spigot().playEffect(player.getLocation(), Effect.SPLASH, 0, 0, 0, 0.9F, 0, 0.1F, 65, 11);

        return SkillResult.NORMAL;
    }
}