package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.AttributeIncreaseEffect;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Messaging;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class SkillEnlightenment extends TargettedSkill {
    private String applyText;
    private String expireText;
    
    public SkillEnlightenment(Heroes plugin) {
        super(plugin, "Enlightenment");
        setArgumentRange(0,1);
        setUsage("/skill Enlightenment <target>");
        setIdentifiers("skill enlightenment");
        setDescription("Prepare the target, granting them Enlightenment that increases their Intellect and Wisdom by $1 for $2 minutes");
        setTypes(SkillType.MAX_MANA_INCREASING, SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.BUFFING, SkillType.SILENCABLE);
    }
    
    @Override
    public String getDescription(Hero h) {
        long duration = SkillConfigManager.getUseSetting(h, this, SkillSetting.DURATION, 600000, false);
        duration += SkillConfigManager.getUseSetting(h, this, SkillSetting.DURATION_INCREASE_PER_LEVEL, 1000, false) * h.getSkillLevel(this);
        int attributeIncrease = SkillConfigManager.getUseSetting(h, this, "attribute-increase", 15, false);
        
        return getDescription()
                .replace("$1", attributeIncrease + "")
                .replace("$2", duration / 60000 + "");
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, Messaging.getSkillDenoter() + "$1 is Enlightened!");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, Messaging.getSkillDenoter() + "$1 is no longer Enlightened.");
    }
    
    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 10);
        node.set(SkillSetting.DURATION.node(), 600000);
        node.set(SkillSetting.DURATION_INCREASE_PER_LEVEL.node(), 30000);
        node.set("attribute-increase", 15);
        node.set(SkillSetting.APPLY_TEXT.node(), "$1 is Enlightened!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "$1 is no longer Enlightened.");

        return node;
    }
    
    @Override
    public SkillResult use(Hero hero, final LivingEntity target, String[] args) {
        Hero targetHero = plugin.getCharacterManager().getHero((Player) target);
        Player player = hero.getPlayer();
        
        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 600000, false);
        duration += SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE_PER_LEVEL, 1000, false) * hero.getSkillLevel(this);
        int attributeIncrease = SkillConfigManager.getUseSetting(hero, this, "attribute-increase", 15, false);
        
        // Only using the one effect for checks. Checking both of them would get crazy
        AttributeIncreaseEffect aEffect = new AttributeIncreaseEffect(this, "EnlightenmentIntIncreaseEffect", player, duration, AttributeType.INTELLECT, attributeIncrease, applyText, expireText);
        if(hero.hasEffect("EnlightenmentIntIncreaseEffect")) {
            if(((AttributeIncreaseEffect) hero.getEffect("EnlightenmentIntIncreaseEffect")).getIncreaseValue() > aEffect.getIncreaseValue()) {
                Messaging.send(player, "Target has a more powerful effect already!");
                return SkillResult.CANCELLED;
            }
        }
        
        AttributeIncreaseEffect aEffectW = new AttributeIncreaseEffect(this, "EnlightenmentWisIncreaseEffect", player, duration, AttributeType.WISDOM, attributeIncrease, null, null);

        
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
        targetHero.addEffect(aEffectW);
        return SkillResult.NORMAL;
    }
    
}
