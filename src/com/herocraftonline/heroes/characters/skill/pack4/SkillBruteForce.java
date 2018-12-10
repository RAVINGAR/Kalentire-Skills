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
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import org.bukkit.scheduler.BukkitRunnable;

public class SkillBruteForce extends TargettedSkill {
    private String applyText;
    private String expireText;
    
    public SkillBruteForce(Heroes plugin) {
        super(plugin, "BruteForce");
        setDescription("Grant the target BruteForce which increases their Strength by $1 for $2 minutes");
        setArgumentRange(0,1);
        setUsage("/skill BruteForce <target>");
        setIdentifiers("skill bruteforce");
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.BUFFING, SkillType.SILENCEABLE);
    }
    
    @Override
    public String getDescription(Hero h) {
        long duration = SkillConfigManager.getUseSetting(h, this, SkillSetting.DURATION, 600000, false);
        duration += SkillConfigManager.getUseSetting(h, this, SkillSetting.DURATION_INCREASE_PER_LEVEL, 1000, false) * h.getHeroLevel(this);
        int strIncrease = SkillConfigManager.getUseSetting(h, this, "str-increase", 15, false);
        
        return getDescription()
                .replace("$1", strIncrease + "")
                .replace("$2", duration / 60000 + "");
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "$1 is willing to use Brute Force!");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "$1 is no longer using Brute Force.");
    }
    
    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 10);
        node.set(SkillSetting.DURATION.node(), 600000);
        node.set(SkillSetting.DURATION_INCREASE_PER_LEVEL.node(), 30000);
        node.set("str-increase", 15);
        node.set(SkillSetting.APPLY_TEXT.node(), "$1 is willing to use Brute Force!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "$1 is no longer using Brute Force.");

        return node;
    }
    
    @Override
    public SkillResult use(Hero hero, final LivingEntity target, String[] args) {
        Hero targetHero = plugin.getCharacterManager().getHero((Player) target);
        Player player = hero.getPlayer();
        
        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 600000, false);
        duration += SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE_PER_LEVEL, 1000, false) * hero.getHeroLevel(this);
        int strIncrease = SkillConfigManager.getUseSetting(hero, this, "str-increase", 15, false);
        
        AttributeIncreaseEffect aEffect = new AttributeIncreaseEffect(this, "BruteForceStrIncreaseEffect", player, duration, AttributeType.STRENGTH, strIncrease, applyText, expireText);
        if(hero.hasEffect("BruteForceStrIncreaseEffect")) {
            if(((AttributeIncreaseEffect) hero.getEffect("BruteForceStrIncreaseEffect")).getDelta() > aEffect.getDelta()) {
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
                    //target.getWorld().spigot().playEffect(location, Effect.HAPPY_VILLAGER, 0, 0, 0, 0, 0, 0.1f, 1, 16);
                    target.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, location, 1, 0, 0, 0, 0.1);
                } else {
                    target.getLocation(location).add(0, 2.3, 0);
                    //target.getWorld().spigot().playEffect(location, Effect.TILE_BREAK, Material.WATER.getId(), 0, 0, 0, 0, 1f, 500, 16);
                    target.getWorld().spawnParticle(Particle.BLOCK_CRACK, location, 500, 0, 0, 0, 1, Bukkit.createBlockData(Material.WATER));
                    cancel();
                }
                time += 0.01;
            }
        }.runTaskTimer(plugin, 1, 1);
        targetHero.addEffect(aEffect);
        return SkillResult.NORMAL;
    }
    
}
