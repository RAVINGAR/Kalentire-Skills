package com.herocraftonline.heroes.characters.skill.skills;

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

public class SkillAngelsShout extends TargettedSkill {
    public VisualEffect fplayer = new VisualEffect();

    public SkillAngelsShout(Heroes plugin) {
        super(plugin, "AngelsShout");
        setDescription("Bless your target with the shout of an Angel, restoring $1 health to your target and negating their fire effects.");
        setUsage("/skill angelsshout <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill angelsshout");
        setTypes(SkillType.HEAL, SkillType.SILENCABLE, SkillType.LIGHT);
    }

    public String getDescription(Hero hero) {

        int health = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALTH, Integer.valueOf(300), false);

        return getDescription().replace("$1", health + "");
    }

    public ConfigurationSection getDefaultConfig() {

        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.HEALTH.node(), Integer.valueOf(300));

        return node;
    }

    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();
        if (!(target instanceof Player)) {
            return SkillResult.INVALID_TARGET;
        }

        Hero targetHero = plugin.getCharacterManager().getHero((Player) target);

        double targetHealth = target.getHealth();

        // Check to see if they are at full health
        if (targetHealth >= target.getMaxHealth()) {
            Messaging.send(player, "Target is already at full health.", new Object[0]);
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        // Ensure they can be healed.
        double healAmount = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALTH, Integer.valueOf(300), false);
        HeroRegainHealthEvent hrhEvent = new HeroRegainHealthEvent(targetHero, healAmount, this, hero);
        plugin.getServer().getPluginManager().callEvent(hrhEvent);
        if (hrhEvent.isCancelled()) {
            Messaging.send(player, "Unable to heal the target at this time!", new Object[0]);
            return SkillResult.CANCELLED;
        }

        broadcastExecuteText(hero, target);

        // Heal target
        targetHero.heal(hrhEvent.getAmount());

        // Remove fire ticks and fire effects
        ((Player) target).setFireTicks(0);
        for (Effect effect : targetHero.getEffects()) {
            if (effect.isType(EffectType.FIRE)) {
                targetHero.removeEffect(effect);
            }
        }

        // Play effect
        try {
            fplayer.playFirework(player.getWorld(), target.getLocation().add(0.0D, 1.5D, 0.0D), FireworkEffect.builder().flicker(false).trail(false).with(FireworkEffect.Type.BURST).withColor(Color.SILVER).withFade(Color.YELLOW).build());
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