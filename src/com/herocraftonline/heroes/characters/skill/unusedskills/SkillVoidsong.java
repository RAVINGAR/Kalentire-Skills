package com.herocraftonline.heroes.characters.skill.unusedskills;

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
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.VisualEffect;
import com.herocraftonline.heroes.util.Util;

public class SkillVoidsong extends ActiveSkill {
    // This is for Firework Effects
    public VisualEffect fplayer = new VisualEffect();
    public SkillVoidsong(Heroes plugin) {
        super(plugin, "Voidsong");
        setDescription("You create a void dealing $3 magic damage and silencing everyone within $1 blocks for $2 seconds.");
        setUsage("/skill voidsong");
        setArgumentRange(0, 0);
        setIdentifiers("skill voidsong");
        setTypes(SkillType.MOVEMENT_PREVENTING, SkillType.ABILITY_PROPERTY_DARK, SkillType.INTERRUPTING);
    }
    
    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DURATION.node(), 10000);
        node.set(SkillSetting.RADIUS.node(), 10);
        node.set(SkillSetting.RADIUS_INCREASE.node(), 0);
        node.set(SkillSetting.DAMAGE.node(), 0);
        node.set(SkillSetting.DAMAGE_INCREASE.node(), 0);
        return node;
    }

    @Override
    public String getDescription(Hero hero) {
        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 30, false)
                + (SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS_INCREASE, 0, false) * hero.getSkillLevel(this));
        double duration = Util.formatDouble(SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false) / 1000.0);
        int damage = (SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 0, false) + 
                (SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE, 0, false) * hero.getSkillLevel(this)));
        String description = getDescription().replace("$1", radius + "").replace("$2", duration + "").replace("$3", damage + "");
        int cooldown = (SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN.node(), 0, false)
                - SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN_REDUCE.node(), 0, false) * hero.getSkillLevel(this)) / 1000;
        if (cooldown > 0) {
            description += " CD:" + cooldown + "s";
        }
        int mana = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MANA.node(), 10, false)
                - (SkillConfigManager.getUseSetting(hero, this, SkillSetting.MANA_REDUCE.node(), 0, false) * hero.getSkillLevel(this));
        if (mana > 0) {
            description += " M:" + mana;
        }
        int healthCost = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALTH_COST, 0, false) - 
                (SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALTH_COST_REDUCE, mana, true) * hero.getSkillLevel(this));
        if (healthCost > 0) {
            description += " HP:" + healthCost;
        }
        int staminaCost = SkillConfigManager.getUseSetting(hero, this, SkillSetting.STAMINA.node(), 0, false)
                - (SkillConfigManager.getUseSetting(hero, this, SkillSetting.STAMINA_REDUCE.node(), 0, false) * hero.getSkillLevel(this));
        if (staminaCost > 0) {
            description += " FP:" + staminaCost;
        }
        int delay = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DELAY.node(), 0, false) / 1000;
        if (delay > 0) {
            description += " W:" + delay + "s";
        }
        int exp = SkillConfigManager.getUseSetting(hero, this, SkillSetting.EXP.node(), 0, false);
        if (exp > 0) {
            description += " XP:" + exp;
        }
        return description;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS.node(), 30, false);
        radius += SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS_INCREASE, 0, false) * hero.getSkillLevel(this);
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE.node(), 0, false);
        damage += SkillConfigManager.getUseSetting(hero, this, "damage-increase", 0, false) * hero.getSkillLevel(this);
        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 10000, false);
        duration += SkillConfigManager.getUseSetting(hero, this, "duration-increase", 0, false) * hero.getSkillLevel(this);
        Player player = hero.getPlayer();
        //boolean hit = false;
        // this is our fireworks shit
        try {
            fplayer.playFirework(player.getWorld(), player.getLocation().add(0,2,0), FireworkEffect.builder()
            		.flicker(false).trail(false)
            		.with(FireworkEffect.Type.CREEPER)
            		.withColor(Color.BLACK)
            		.withFade(Color.MAROON)
            		.build());
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
            character.addEffect(new SilenceEffect(this, hero.getPlayer(), duration));
            //hit = true;
            
        }
        player.getWorld().playEffect(player.getLocation(), Effect.SMOKE, 3);
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.WITHER_DEATH , 0.5F, 1.0F); 
        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }
}
