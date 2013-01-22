package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Color;
import org.bukkit.Effect;
import org.bukkit.FireworkEffect;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.SilenceEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.VisualEffect;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Setting;

public class SkillVoidsong extends ActiveSkill {
    // This is for Firework Effects
    public VisualEffect fplayer = new VisualEffect();
    public SkillVoidsong(Heroes plugin) {
        super(plugin, "Voidsong");
        setDescription("You create a void dealing $3 magic damage and silencing everyone within $1 blocks for $2 seconds.");
        setUsage("/skill voidsong");
        setArgumentRange(0, 0);
        setIdentifiers("skill voidsong");
        setTypes(SkillType.MOVEMENT, SkillType.DARK, SkillType.INTERRUPT);
    }
    
    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(Setting.DURATION.node(), 10000);
        node.set(Setting.DURATION_INCREASE.node(), 0);
        node.set(Setting.RADIUS.node(), 10);
        node.set(Setting.RADIUS_INCREASE.node(), 0);
        node.set(Setting.DAMAGE.node(), 0);
        node.set(Setting.DAMAGE_INCREASE.node(), 0);
        return node;
    }

    @Override
    public String getDescription(Hero hero) {
        int radius = SkillConfigManager.getUseSetting(hero, this, Setting.RADIUS, 30, false)
                + (SkillConfigManager.getUseSetting(hero, this, Setting.RADIUS_INCREASE, 0, false) * hero.getSkillLevel(this));
        int duration = (SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 10000, false) +
                (SkillConfigManager.getUseSetting(hero, this, Setting.DURATION_INCREASE, 0, false) * hero.getSkillLevel(this))) / 1000;
        int damage = (SkillConfigManager.getUseSetting(hero, this, Setting.DAMAGE, 0, false) + 
                (SkillConfigManager.getUseSetting(hero, this, Setting.DAMAGE_INCREASE, 0, false) * hero.getSkillLevel(this)));
        String description = getDescription().replace("$1", radius + "").replace("$2", duration + "").replace("$3", damage + "");
        
        //COOLDOWN
        int cooldown = (SkillConfigManager.getUseSetting(hero, this, Setting.COOLDOWN.node(), 0, false)
                - SkillConfigManager.getUseSetting(hero, this, Setting.COOLDOWN_REDUCE.node(), 0, false) * hero.getSkillLevel(this)) / 1000;
        if (cooldown > 0) {
            description += " CD:" + cooldown + "s";
        }
        
        //MANA
        int mana = SkillConfigManager.getUseSetting(hero, this, Setting.MANA.node(), 10, false)
                - (SkillConfigManager.getUseSetting(hero, this, Setting.MANA_REDUCE.node(), 0, false) * hero.getSkillLevel(this));
        if (mana > 0) {
            description += " M:" + mana;
        }
        
        //HEALTH_COST
        int healthCost = SkillConfigManager.getUseSetting(hero, this, Setting.HEALTH_COST, 0, false) - 
                (SkillConfigManager.getUseSetting(hero, this, Setting.HEALTH_COST_REDUCE, mana, true) * hero.getSkillLevel(this));
        if (healthCost > 0) {
            description += " HP:" + healthCost;
        }
        
        //STAMINA
        int staminaCost = SkillConfigManager.getUseSetting(hero, this, Setting.STAMINA.node(), 0, false)
                - (SkillConfigManager.getUseSetting(hero, this, Setting.STAMINA_REDUCE.node(), 0, false) * hero.getSkillLevel(this));
        if (staminaCost > 0) {
            description += " FP:" + staminaCost;
        }
        
        //DELAY
        int delay = SkillConfigManager.getUseSetting(hero, this, Setting.DELAY.node(), 0, false) / 1000;
        if (delay > 0) {
            description += " W:" + delay + "s";
        }
        
        //EXP
        int exp = SkillConfigManager.getUseSetting(hero, this, Setting.EXP.node(), 0, false);
        if (exp > 0) {
            description += " XP:" + exp;
        }
        return description;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        int radius = SkillConfigManager.getUseSetting(hero, this, Setting.RADIUS.node(), 30, false);
        radius += SkillConfigManager.getUseSetting(hero, this, Setting.RADIUS_INCREASE, 0, false) * hero.getSkillLevel(this);
        int damage = SkillConfigManager.getUseSetting(hero, this, Setting.DAMAGE.node(), 0, false);
        damage += SkillConfigManager.getUseSetting(hero, this, "damage-increase", 0, false) * hero.getSkillLevel(this);
        long duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION.node(), 10000, false);
        duration += SkillConfigManager.getUseSetting(hero, this, "duration-increase", 0, false) * hero.getSkillLevel(this);
        Player player = hero.getPlayer();
        boolean hit = false;
        // this is our fireworks shit
        try {
            fplayer.playFirework(player.getWorld(), player.getLocation(), FireworkEffect.builder().flicker(false).trail(true).with(FireworkEffect.Type.CREEPER).withColor(Color.BLACK).withFade(Color.MAROON).build());
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        for (Entity e : player.getNearbyEntities(radius, radius, radius)) {
            if (!(e instanceof LivingEntity) || !damageCheck(player, (LivingEntity) e)) {
                continue;
            }
            CharacterTemplate character = plugin.getCharacterManager().getCharacter((LivingEntity) e);
            if (character instanceof Hero) {
                ((Hero) character).cancelDelayedSkill();
            }
            addSpellTarget(e, hero);
            damageEntity(character.getEntity(), player, damage, DamageCause.MAGIC);
            character.addEffect(new SilenceEffect(this, duration));
            hit = true;
            
        }
        if (!hit) {
            Messaging.send(player, "No nearby targets!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }
        player.getWorld().playEffect(player.getLocation(), Effect.SMOKE, 3);
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.WITHER_DEATH , 10.0F, 1.0F); 
        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }
}