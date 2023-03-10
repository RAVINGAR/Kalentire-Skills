package com.herocraftonline.heroes.characters.skill.general;

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

public class SkillBlessing extends TargettedSkill {
    private String applyText;
    private String expireText;
    
    public SkillBlessing(Heroes plugin) {
        super(plugin, "Blessing");
        setDescription("Bless the target, increasing their attributes by $1 for $2 minutes");
        setArgumentRange(0,1);
        setUsage("/skill Blessing <target>");
        setIdentifiers("skill blessing");
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.BUFFING, SkillType.SILENCEABLE);
    }
    
    @Override
    public String getDescription(Hero h) {
        long duration = SkillConfigManager.getUseSetting(h, this, SkillSetting.DURATION, 300000, false);
        duration += (long) SkillConfigManager.getUseSetting(h, this, SkillSetting.DURATION_INCREASE_PER_LEVEL, 1000, false) * h.getHeroLevel(this);
        int conIncrease = SkillConfigManager.getUseSetting(h, this, "attribute-increase", 5, false);
        
        return getDescription()
                .replace("$1", conIncrease + "")
                .replace("$2", duration / 60000 + "");
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "$1 is Blessed!");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "$1 is no longer Blessed.");
    }
    
    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 10);
        node.set(SkillSetting.DURATION.node(), 300000);
        node.set(SkillSetting.DURATION_INCREASE_PER_LEVEL.node(), 10000);
        node.set("attribute-increase", 5);
        node.set(SkillSetting.APPLY_TEXT.node(), "$1 is Blessed!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "$1 is no longer Blessed.");

        return node;
    }
    
    @Override
    public SkillResult use(Hero hero, final LivingEntity target, String[] args) {
        Hero targetHero = plugin.getCharacterManager().getHero((Player) target);
        Player player = hero.getPlayer();
        
        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 300000, false);
        duration += (long) SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE_PER_LEVEL, 1000, false) * hero.getHeroLevel(this);
        int conIncrease = SkillConfigManager.getUseSetting(hero, this, "attribute-increase", 5, false);
        
        // Only using the one effect for checks. Checking all of them would get crazy fast
        AttributeIncreaseEffect aEffect = new AttributeIncreaseEffect(this, "BlessingConIncreaseEffect", player, duration, AttributeType.CONSTITUTION, conIncrease, applyText, expireText);
        if(hero.hasEffect("BlessingConIncreaseEffect")) {
            if(((AttributeIncreaseEffect) hero.getEffect("BlessingConIncreaseEffect")).getDelta() > aEffect.getDelta()) {
                player.sendMessage("Target has a more powerful effect already!");
                return SkillResult.CANCELLED;
            }
        }
        
        AttributeIncreaseEffect aEffectS = new AttributeIncreaseEffect(this, "BlessingStrIncreaseEffect", player, duration, AttributeType.STRENGTH, conIncrease, null, null);
        AttributeIncreaseEffect aEffectW = new AttributeIncreaseEffect(this, "BlessingWisIncreaseEffect", player, duration, AttributeType.WISDOM, conIncrease, null, null);
        AttributeIncreaseEffect aEffectA = new AttributeIncreaseEffect(this, "BlessingAgiIncreaseEffect", player, duration, AttributeType.DEXTERITY, conIncrease, null, null);
        AttributeIncreaseEffect aEffectI = new AttributeIncreaseEffect(this, "BlessingIntIncreaseEffect", player, duration, AttributeType.INTELLECT, conIncrease, null, null);
        AttributeIncreaseEffect aEffectE = new AttributeIncreaseEffect(this, "BlessingEndIncreaseEffect", player, duration, AttributeType.ENDURANCE, conIncrease, null, null);
        AttributeIncreaseEffect aEffectC = new AttributeIncreaseEffect(this, "BlessingChaIncreaseEffect", player, duration, AttributeType.CHARISMA, conIncrease, null, null);

        
        new BukkitRunnable() {
            
            private final Location location = target.getLocation();

            private double time = 0;

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
        
        // Lot of effects to add.
        targetHero.addEffect(aEffect);
        targetHero.addEffect(aEffectS);
        targetHero.addEffect(aEffectW);
        targetHero.addEffect(aEffectA);
        targetHero.addEffect(aEffectI);
        targetHero.addEffect(aEffectE);
        targetHero.addEffect(aEffectC);
        
        return SkillResult.NORMAL;
    }
    
}
