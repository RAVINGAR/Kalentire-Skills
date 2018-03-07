package com.herocraftonline.heroes.characters.skill.pack4;

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
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class SkillAlacrity extends TargettedSkill {
    private String applyText;
    private String expireText;
    
    public SkillAlacrity(Heroes plugin) {
        super(plugin, "Alacrity");
        setDescription("Prepare the target, granting them Alacrity that increases their Dexterity by $1 for $2 minutes");
        setArgumentRange(0,1);
        setUsage("/skill Alacrity <target>");
        setIdentifiers("skill alacrity");
        setTypes(SkillType.MOVEMENT_INCREASING, SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.BUFFING, SkillType.SILENCEABLE);
    }
    
    @Override
    public String getDescription(Hero h) {
        long duration = SkillConfigManager.getUseSetting(h, this, SkillSetting.DURATION, 600000, false);
        duration += SkillConfigManager.getUseSetting(h, this, SkillSetting.DURATION_INCREASE_PER_LEVEL, 1000, false) * h.getHeroLevel(this);
        int agiIncrease = SkillConfigManager.getUseSetting(h, this, "agi-increase", 15, false);
        
        return getDescription()
                .replace("$1", agiIncrease + "")
                .replace("$2", duration / 60000 + "");
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "$1 is prepared and has Alacrity!");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "$1 goes back to being unprepared.");
    }
    
    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 10);
        node.set(SkillSetting.DURATION.node(), 600000);
        node.set(SkillSetting.DURATION_INCREASE_PER_LEVEL.node(), 30000);
        node.set("agi-increase", 15);
        node.set(SkillSetting.APPLY_TEXT.node(), "$1 is prepared and has Alacrity!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "$1 goes back to being unprepared.");

        return node;
    }
    
    @Override
    public SkillResult use(Hero hero, final LivingEntity target, String[] args) {
        Hero targetHero = plugin.getCharacterManager().getHero((Player) target);
        Player player = hero.getPlayer();
        
        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 600000, false);
        duration += SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE_PER_LEVEL, 1000, false) * hero.getHeroLevel(this);
        int agiIncrease = SkillConfigManager.getUseSetting(hero, this, "agi-increase", 15, false);
        
        AttributeIncreaseEffect aEffect = new AttributeIncreaseEffect(this, "AlacrityAgiIncreaseEffect", player, duration, AttributeType.DEXTERITY, agiIncrease, applyText, expireText);
        if(hero.hasEffect("AlacrityAgiIncreaseEffect")) {
            if(((AttributeIncreaseEffect) hero.getEffect("AlacrityAgiIncreaseEffect")).getDelta() > aEffect.getDelta()) {
                player.sendMessage("Target has a more powerful effect already!");
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
