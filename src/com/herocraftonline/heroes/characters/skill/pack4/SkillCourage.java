package com.herocraftonline.heroes.characters.skill.pack4;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.AttributeIncreaseEffect;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Messaging;

public class SkillCourage extends TargettedSkill {
    private String applyText;
    private String expireText;
    
    public SkillCourage(Heroes plugin) {
        super(plugin, "Courage");
        setDescription("Encourage the target, granting them Courage that increases their Constitution by $1 for $2 minutes");
        setArgumentRange(0,1);
        setUsage("/skill Courage <target>");
        setIdentifiers("skill courage");
        setTypes(SkillType.MAX_HEALTH_INCREASING, SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.BUFFING, SkillType.SILENCEABLE);
    }
    
    @Override
    public String getDescription(Hero h) {
        long duration = SkillConfigManager.getUseSetting(h, this, SkillSetting.DURATION, 600000, false);
        duration += SkillConfigManager.getUseSetting(h, this, SkillSetting.DURATION_INCREASE_PER_LEVEL, 1000, false) * h.getSkillLevel(this);
        int conIncrease = SkillConfigManager.getUseSetting(h, this, "con-increase", 15, false);
        
        return getDescription()
                .replace("$1", conIncrease + "")
                .replace("$2", duration / 60000 + "");
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "$1 is encouraged and has Courage!");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "$1 goes back to being discouraged.");
    }
    
    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 10);
        node.set(SkillSetting.DURATION.node(), 600000);
        node.set(SkillSetting.DURATION_INCREASE_PER_LEVEL.node(), 30000);
        node.set("con-increase", 15);
        node.set(SkillSetting.APPLY_TEXT.node(), "$1 is encouraged and has Courage!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "$1 goes back to being discouraged.");

        return node;
    }
    
    @Override
    public SkillResult use(Hero hero, final LivingEntity target, String[] args) {
        Hero targetHero = plugin.getCharacterManager().getHero((Player) target);
        Player player = hero.getPlayer();
        
        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 600000, false);
        duration += SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE_PER_LEVEL, 1000, false) * hero.getSkillLevel(this);
        int conIncrease = SkillConfigManager.getUseSetting(hero, this, "con-increase", 15, false);
        
        AttributeIncreaseEffect aEffect = new AttributeIncreaseEffect(this, "CourageConIncreaseEffect", player, duration, AttributeType.CONSTITUTION, conIncrease, applyText, expireText);
        if(hero.hasEffect("CourageConIncreaseEffect")) {
            if(((AttributeIncreaseEffect) hero.getEffect("CourageConIncreaseEffect")).getDelta() > aEffect.getDelta()) {
                Messaging.send(player, "Target has a more powerful effect already!");
                return SkillResult.CANCELLED;
            }
        }
        new BukkitRunnable() {
            
            private Location location = target.getLocation();

            private double time = 0;

            @SuppressWarnings("deprecation")
            @Override
            public void run() {
                if (time < 1.0) {
                    target.getLocation(location).add(0.7 * Math.sin(time * 16), time * 2.2, 0.7 * Math.cos(time * 16));
                    target.getWorld().spigot().playEffect(location, Effect.HAPPY_VILLAGER, 0, 0, 0, 0, 0, 0.1f, 1, 16);
                } else {
                    target.getLocation(location).add(0, 2.3, 0);
                    target.getWorld().spigot().playEffect(location, Effect.TILE_BREAK, Material.WATER.getId(), 0, 0, 0, 0, 1f, 500, 16);
                    cancel();
                }
                time += 0.01;
            }
        }.runTaskTimer(plugin, 1, 1);
        targetHero.addEffect(aEffect);
        return SkillResult.NORMAL;
    }
    
}
