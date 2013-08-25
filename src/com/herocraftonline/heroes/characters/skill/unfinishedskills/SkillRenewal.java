package com.herocraftonline.heroes.characters.skill.unfinishedskills;

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
import com.herocraftonline.heroes.util.Messaging;

public class SkillRenewal extends TargettedSkill {
    // This is for Firework Effects
    public VisualEffect fplayer = new VisualEffect();

    public SkillRenewal(Heroes plugin) {
        super(plugin, "Renewal");
        setDescription("You restore $1 health to your target, curing disease.");
        setUsage("/skill renewal <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill renewal");
        setTypes(SkillType.HEAL, SkillType.SILENCABLE, SkillType.LIGHT);
    }

    @Override
    public String getDescription(Hero hero) {
        int health = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALTH.node(), 10, false);

        return getDescription().replace("$1", health + "");
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

        if (targetHero.equals(hero)) {
            boolean allowSelfTarget = SkillConfigManager.getUseSetting(hero, this, "allow-self-target", true);
            if (!allowSelfTarget) {
                Messaging.send(player, "You cannot use this ability on yourself!", new Object[0]);
                return SkillResult.INVALID_TARGET_NO_MSG;
            }
        }

        double hpPlus = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALTH, 10, false);
        double targetHealth = target.getHealth();

        if (targetHealth >= target.getMaxHealth()) {
            if (player.equals(targetHero.getPlayer())) {
                Messaging.send(player, "You are already at full health.");
            }
            else {
                Messaging.send(player, "Target is already fully healed.");
            }
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        HeroRegainHealthEvent hrhEvent = new HeroRegainHealthEvent(targetHero, hpPlus, this, hero);
        plugin.getServer().getPluginManager().callEvent(hrhEvent);
        if (hrhEvent.isCancelled()) {
            Messaging.send(player, "Unable to heal the target at this time!");
            return SkillResult.CANCELLED;
        }
        targetHero.heal(hrhEvent.getAmount());
        for (Effect effect : targetHero.getEffects()) {
            if (effect.isType(EffectType.DISPELLABLE) && effect.isType(EffectType.HARMFUL)) {
                if (effect.isType(EffectType.DISEASE)) {
                    targetHero.removeEffect(effect);
                }
            }
        }
        broadcastExecuteText(hero, target);
        // this is our fireworks shit
        try {
            fplayer.playFirework(player.getWorld(), target.getLocation().add(0, 1.5, 0), FireworkEffect.builder().flicker(false).trail(false).with(FireworkEffect.Type.BURST).withColor(Color.SILVER).withFade(Color.WHITE).build());
        }
        catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return SkillResult.NORMAL;
    }
}
