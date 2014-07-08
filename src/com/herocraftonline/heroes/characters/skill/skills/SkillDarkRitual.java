package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainManaEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.VisualEffect;
import com.herocraftonline.heroes.util.Messaging;

public class SkillDarkRitual extends ActiveSkill {
    // This is for Firework Effects
    public VisualEffect fplayer = new VisualEffect();

    public SkillDarkRitual(Heroes plugin) {
        super(plugin, "DarkRitual");
        setDescription("Converts $1 health to $2 mana.");
        setUsage("/skill darkritual");
        setArgumentRange(0, 0);
        setIdentifiers("skill darkritual");
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
        Player player = hero.getPlayer();

        int manaGain = SkillConfigManager.getUseSetting(hero, this, "mana-gain", 150, false);

        if (hero.getMana() >= hero.getMaxMana()) {
            Messaging.send(player, "You are already at full mana.");
            return SkillResult.FAIL;
        }

        HeroRegainManaEvent hrmEvent = new HeroRegainManaEvent(hero, manaGain, this);
        plugin.getServer().getPluginManager().callEvent(hrmEvent);
        if (hrmEvent.isCancelled()) {
            Messaging.send(player, "You cannot regenerate mana right now!");
            return SkillResult.CANCELLED;
        }

        broadcastExecuteText(hero);

        hero.setMana(hrmEvent.getAmount() + hero.getMana());

        player.getWorld().playSound(player.getLocation(), Sound.WITHER_SPAWN, 0.4F, 1.0F);

        // this is our fireworks shit
        try {
            fplayer.playFirework(player.getWorld(),
                                 player.getLocation().add(0, 3, 0),
                                 FireworkEffect.builder()
                                               .flicker(false).trail(true)
                                               .with(FireworkEffect.Type.BALL_LARGE)
                                               .withColor(Color.BLACK)
                                               .withFade(Color.GRAY)
                                               .build());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return SkillResult.NORMAL;
    }
}
