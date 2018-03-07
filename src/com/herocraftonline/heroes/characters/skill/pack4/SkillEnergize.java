package com.herocraftonline.heroes.characters.skill.pack4;

import org.bukkit.Effect;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainStaminaEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.CompatSound;

public class SkillEnergize extends ActiveSkill {

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
            player.sendMessage("You cannot regenerate stamina right now!");
            return SkillResult.FAIL;
        }

        broadcastExecuteText(hero);

        hero.setStamina(hrsEvent.getDelta() + hero.getStamina());
        if (hero.isVerboseStamina())
            player.sendMessage(ChatComponents.Bars.stamina(hero.getStamina(), hero.getMaxStamina(), true));

        player.getWorld().spigot().playEffect(player.getLocation(), Effect.VILLAGER_THUNDERCLOUD, 0, 0, 0.5F, 1.0F, 0.5F, 0, 45, 16);
        player.getWorld().playSound(player.getLocation(), CompatSound.ENTITY_PLAYER_LEVELUP.value(), 0.8F, 1.0F);

        return SkillResult.NORMAL;
    }
}
