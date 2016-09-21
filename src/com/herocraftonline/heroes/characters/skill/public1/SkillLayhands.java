package com.herocraftonline.heroes.characters.skill.public1;

import com.herocraftonline.heroes.characters.skill.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainHealthEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.util.CompatSound;
import com.herocraftonline.heroes.util.Messaging;

public class SkillLayhands extends TargettedSkill {
	
    public SkillLayhands(Heroes plugin) {
        super(plugin, "Layhands");
        setDescription("You restore your target to full health. The maximum Targeting range of this ability is increased by your Wisdom.");
        setUsage("/skill layhands <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill layhands");
        setTypes(SkillType.ABILITY_PROPERTY_LIGHT, SkillType.HEALING, SkillType.SILENCEABLE);
    }

    @Override
    public String getDescription(Hero hero) {
        String description = getDescription();
        if (SkillConfigManager.getUseSetting(hero, this, "drain-all-mana", false)) description += " Drains all mana on use.";
        return description;
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 4);
        node.set(SkillSetting.MAX_DISTANCE_INCREASE_PER_WISDOM.node(), 0.1);
        node.set(SkillSetting.COOLDOWN.node(), 900000);
        node.set("drain-all-mana", false);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        if (!(target instanceof Player)) {
            return SkillResult.INVALID_TARGET;
        }
        Player player = hero.getPlayer();

        Hero targetHero = plugin.getCharacterManager().getHero((Player) target);
        double healAmount = target.getMaxHealth();

        HeroRegainHealthEvent event = new HeroRegainHealthEvent(targetHero, healAmount, this, hero);
        plugin.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            Messaging.send(hero.getPlayer(), "Unable to heal the target at this time!");
            return SkillResult.CANCELLED;
        }

        targetHero.heal(event.getAmount());
        if (SkillConfigManager.getUseSetting(hero, this, "drain-all-mana", false)) {
            hero.setMana(0);
        }

        broadcastExecuteText(hero, target);

        player.getWorld().playSound(player.getLocation(), CompatSound.ENTITY_PLAYER_LEVELUP.value(), 0.9F, 1.0F);

        return SkillResult.NORMAL;
    }
}
