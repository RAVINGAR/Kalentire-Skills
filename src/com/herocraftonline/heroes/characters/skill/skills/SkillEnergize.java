package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Color;
import org.bukkit.Effect;
import org.bukkit.FireworkEffect;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainStaminaEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.VisualEffect;
import com.herocraftonline.heroes.util.Messaging;

public class SkillEnergize extends ActiveSkill {
    // This is for Firework Effects
    public VisualEffect fplayer = new VisualEffect();

    public SkillEnergize(Heroes plugin) {
        super(plugin, "Energize");
        setDescription("Replenishes $1 points of your stamina.");
        setUsage("/skill energize");
        setArgumentRange(0, 0);
        setIdentifiers("skill energize");
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.STAMINA_INCREASING);
    }

    @Override
    public String getDescription(Hero hero) {

        int staminaGain = SkillConfigManager.getUseSetting(hero, this, "stamina-gain", 500, false);

        return getDescription().replace("$1", staminaGain + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set("stamina-gain", 500);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        if (hero.getStamina() >= hero.getMaxStamina())
            return SkillResult.CANCELLED;

        int staminaGain = SkillConfigManager.getUseSetting(hero, this, "stamina-gain", 500, false);

        HeroRegainStaminaEvent hrsEvent = new HeroRegainStaminaEvent(hero, staminaGain, this);
        plugin.getServer().getPluginManager().callEvent(hrsEvent);

        if (hrsEvent.isCancelled()) {
            Messaging.send(player, "You cannot regenerate stamina right now!");
            return SkillResult.FAIL;
        }

        broadcastExecuteText(hero);

        hero.setStamina(hrsEvent.getAmount() + hero.getStamina());
        if (hero.isVerboseStamina())
            Messaging.send(player, Messaging.createFullStaminaBar(hero.getStamina(), hero.getMaxStamina()));

        player.getWorld().playEffect(player.getLocation(), Effect.ENDER_SIGNAL, 3);
        player.getWorld().playSound(player.getLocation(), Sound.LEVEL_UP, 0.8F, 1.0F);

        // this is our fireworks shit
        try {
            fplayer.playFirework(player.getWorld(),
                                 player.getLocation().add(0, 1.5, 0),
                                 FireworkEffect.builder().flicker(false).trail(false)
                                               .with(FireworkEffect.Type.STAR)
                                               .withColor(Color.YELLOW)
                                               .withFade(Color.TEAL)
                                               .build());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return SkillResult.NORMAL;
    }
}
