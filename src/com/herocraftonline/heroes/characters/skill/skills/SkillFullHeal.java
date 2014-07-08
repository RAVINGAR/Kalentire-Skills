package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainHealthEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.characters.skill.VisualEffect;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

public class SkillFullHeal extends TargettedSkill {

    // This is for Firework Effects
    public VisualEffect fplayer = new VisualEffect();

    public SkillFullHeal(Heroes plugin) {
        super(plugin, "FullHeal");
        setDescription("You restore your target to full health. However, this ability will only heal you for $1% of your max health however. Targetting distance for this ability is increased by your Wisdom level.");
        setUsage("/skill fullheal <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill fullheal");
        setTypes(SkillType.ABILITY_PROPERTY_LIGHT, SkillType.HEALING, SkillType.SILENCEABLE);
    }

    @Override
    public String getDescription(Hero hero) {
        double modifier = SkillConfigManager.getUseSetting(hero, this, "self-heal-modifier", 0.5, false);

        String formattedModifier = Util.decFormat.format(modifier * 100);

        return getDescription().replace("$1", formattedModifier);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 8);
        node.set(SkillSetting.MAX_DISTANCE_INCREASE_PER_INTELLECT.node(), 0.15);
        node.set("self-heal-modifier", 0.5);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        if (!(target instanceof Player)) {
            return SkillResult.INVALID_TARGET;
        }

        Player player = hero.getPlayer();

        Hero targetHero = plugin.getCharacterManager().getHero((Player) target);

        HeroRegainHealthEvent hrhEvent;
        if (player.equals(targetHero.getPlayer())) {
            double modifier = SkillConfigManager.getUseSetting(hero, this, "self-heal-modifier", 0.5, false);
            double healAmount = target.getMaxHealth() * modifier;
            hrhEvent = new HeroRegainHealthEvent(targetHero, healAmount, this);
        }
        else {
            double healAmount = target.getMaxHealth();
            hrhEvent = new HeroRegainHealthEvent(targetHero, healAmount, this, hero);
        }

        plugin.getServer().getPluginManager().callEvent(hrhEvent);
        if (hrhEvent.isCancelled()) {
            Messaging.send(player, "Unable to heal the target at this time!");
            return SkillResult.CANCELLED;
        }
        targetHero.heal(hrhEvent.getAmount());

        player.getWorld().playSound(player.getLocation(), Sound.LEVEL_UP, 0.9F, 1.0F);
        broadcastExecuteText(hero, target);

        // this is our fireworks shit        
        try {
            fplayer.playFirework(player.getWorld(), target.getLocation().add(0,1.5,0), 
            		FireworkEffect.builder().flicker(false).trail(false)
            		.with(FireworkEffect.Type.BALL)
            		.withColor(Color.FUCHSIA)
            		.withFade(Color.WHITE)
            		.build());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return SkillResult.NORMAL;
    }
}
