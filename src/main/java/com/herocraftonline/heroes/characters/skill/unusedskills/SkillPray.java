package com.herocraftonline.heroes.characters.skill.unusedskills;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainHealthEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.characters.skill.VisualEffect;

public class SkillPray extends TargettedSkill {
    // This is for Firework Effects
    public final VisualEffect fplayer = new VisualEffect();
    public SkillPray(Heroes plugin) {
        super(plugin, "Pray");
        setDescription("You restore $1 health to your target.");
        setUsage("/skill pray <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill pray");
        setTypes(SkillType.HEALING, SkillType.SILENCEABLE);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.HEALTH.node(), 10);
        node.set(SkillSetting.MAX_DISTANCE.node(), 25);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();
        if (!(target instanceof Player)) {
            return SkillResult.INVALID_TARGET;
        }

        Hero targetHero = plugin.getCharacterManager().getHero((Player) target);
        int hpPlus = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALTH, 10, false);
        double targetHealth = target.getHealth();

        if (targetHealth >= target.getMaxHealth()) {
            if (player.equals(targetHero.getPlayer())) {
                player.sendMessage("You are already at full health.");
            } else {
                player.sendMessage("Target is already fully healed.");
            }
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        HeroRegainHealthEvent hrhEvent = new HeroRegainHealthEvent(targetHero, hpPlus, this, hero);
        plugin.getServer().getPluginManager().callEvent(hrhEvent);
        if (hrhEvent.isCancelled()) {
            player.sendMessage("Unable to heal the target at this time!");
            return SkillResult.CANCELLED;
        }
        targetHero.heal(hrhEvent.getDelta());
        for (Effect effect : targetHero.getEffects()) {
            if (effect.isType(EffectType.POISON)) {
                targetHero.removeEffect(effect);
            }
        }
        broadcastExecuteText(hero, target);
        // this is our fireworks shit
        try {
            fplayer.playFirework(player.getWorld(), target.getLocation().add(0,1.5,0), FireworkEffect.builder().flicker(false).trail(true).with(FireworkEffect.Type.BALL).withColor(Color.MAROON).withFade(Color.WHITE).build());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(Hero hero) {
        int health = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALTH.node(), 10, false);
        return getDescription().replace("$1", health + "");
    }
}
