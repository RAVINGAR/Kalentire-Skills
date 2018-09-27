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
import org.bukkit.scheduler.BukkitRunnable;

public class SkillSplendor extends TargettedSkill {
    private String applyText;
    private String expireText;

    public SkillSplendor(Heroes plugin) {
        super(plugin, "Splendor");
        setDescription("Grant the target Splendor which increases their Charisma by $1 for $2 minutes");
        setArgumentRange(0,1);
        setUsage("/skill Splendor <target>");
        setIdentifiers("skill splendor");
        setTypes(SkillType.ABILITY_PROPERTY_ILLUSION, SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.BUFFING, SkillType.SILENCEABLE);
    }
    
    @Override
    public String getDescription(Hero h) {
        long duration = SkillConfigManager.getUseSetting(h, this, SkillSetting.DURATION, 600000, false);
        duration += SkillConfigManager.getUseSetting(h, this, SkillSetting.DURATION_INCREASE_PER_LEVEL, 1000, false) * h.getHeroLevel(this);
        int chaIncrease = SkillConfigManager.getUseSetting(h, this, "cha-increase", 15, false);
        
        return getDescription()
                .replace("$1", chaIncrease + "")
                .replace("$2", duration / 60000 + "");
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "$1 is willing to use Splendor!");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "$1 is no longer using Splendor.");
    }
    
    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 10);
        node.set(SkillSetting.DURATION.node(), 600000);
        node.set(SkillSetting.DURATION_INCREASE_PER_LEVEL.node(), 30000);
        node.set("cha-increase", 15);
        node.set(SkillSetting.APPLY_TEXT.node(), "$1 is willing to use Splendor!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "$1 is no longer using Splendor.");

        return node;
    }
    
    @Override
    public SkillResult use(Hero hero, final LivingEntity target, String[] args) {
        Hero targetHero = plugin.getCharacterManager().getHero((Player) target);
        Player player = hero.getPlayer();
        
        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 600000, false);
        duration += SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE_PER_LEVEL, 1000, false) * hero.getHeroLevel(this);
        int chaIncrease = SkillConfigManager.getUseSetting(hero, this, "cha-increase", 15, false);
        
        AttributeIncreaseEffect aEffect = new AttributeIncreaseEffect(this, "SplendorChaIncreaseEffect", player, duration, AttributeType.CHARISMA, chaIncrease, applyText, expireText);
        if(hero.hasEffect("SplendorChaIncreaseEffect")) {
            if(((AttributeIncreaseEffect) hero.getEffect("SplendorChaIncreaseEffect")).getDelta() > aEffect.getDelta()) {
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
