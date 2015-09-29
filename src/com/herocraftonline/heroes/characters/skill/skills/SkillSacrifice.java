package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.common.AttributeIncreaseEffect;
import com.herocraftonline.heroes.characters.effects.common.SlowEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class SkillSacrifice extends ActiveSkill {
    private String applyText;
    private String expireText;

    public SkillSacrifice(Heroes plugin) {
        super(plugin, "Honor");
        setDescription("Sacrifice youself in combat to increase allies' Constitution and Strength by $1 and $2 for $3 seconds.");
        setArgumentRange(0, 0);
        setUsage("/skill honor");
        setIdentifiers("skill honor");
        setTypes(SkillType.BUFFING, SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.AREA_OF_EFFECT, SkillType.SILENCEABLE);
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 30000, false);

        int conIncrease = SkillConfigManager.getUseSetting(hero, this, "con-increase", 20, false);
        double conIncreaseScaling = SkillConfigManager.getUseSetting(hero, this, "con-increase-per-strength", 0.0, false);
        conIncrease += (int) (conIncreaseScaling * hero.getAttributeValue(AttributeType.STRENGTH));
        
        int strIncrease = SkillConfigManager.getUseSetting(hero, this, "str-increase", 20, false);
        double strIncreaseScaling = SkillConfigManager.getUseSetting(hero, this, "str-increase-per-strength", 0.0, false);
        strIncrease += (int) (strIncreaseScaling * hero.getAttributeValue(AttributeType.STRENGTH));

        String formattedDuration = Util.decFormat.format(duration / 1000.0);   // Convert to seconds.

        return getDescription().replace("$1", conIncrease + "").replace("$2", strIncrease + "").replace("3", formattedDuration);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.RADIUS.node(), 10);
        node.set("con-increase", 5);
        node.set("con-increase-per-strength", 0.0);
        node.set("str-increase", 5);
        node.set("str-increase-per-strength", 0.0);
        node.set(SkillSetting.DURATION.node(), 180000);
        node.set(SkillSetting.APPLY_TEXT.node(), Messaging.getSkillDenoter() + "A sacrifice was made for your sake!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), Messaging.getSkillDenoter() + "The sacrifice's effect wore off...");

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, Messaging.getSkillDenoter() + "A sacrifice was made for your sake!");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, Messaging.getSkillDenoter() + "The sacrifice's effect wore off...");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 30000, false);

        int conIncrease = SkillConfigManager.getUseSetting(hero, this, "con-increase", 20, false);
        double conIncreaseScaling = SkillConfigManager.getUseSetting(hero, this, "con-increase-per-strength", 0.0, false);
        conIncrease += (int) (conIncreaseScaling * hero.getAttributeValue(AttributeType.STRENGTH));
        
        int strIncrease = SkillConfigManager.getUseSetting(hero, this, "str-increase", 20, false);
        double strIncreaseScaling = SkillConfigManager.getUseSetting(hero, this, "str-increase-per-strength", 0.0, false);
        strIncrease += (int) (strIncreaseScaling * hero.getAttributeValue(AttributeType.STRENGTH));

        SacrificeEffect mEffect = new SacrificeEffect(this, player, duration, conIncrease, strIncrease);

        if (!hero.hasParty()) {
            Messaging.send(player, "You must have a party to sacrifice for!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }
        else {
            broadcastExecuteText(hero);
            player.getWorld().playSound(player.getLocation(), Sound.WITHER_SPAWN, 0.5F, 1.0F);
            double maxHealth = player.getMaxHealth();
            if(maxHealth * 0.01 < player.getHealth()) {
                player.setHealth(maxHealth * 0.01);
            }
            hero.addEffect(new SlowEffect(this, "SacrificeSlow", player, 60000, 3, applyText, applyText));
            int rangeSquared = (int) Math.pow(SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 10, false), 2);
            for (Hero pHero : hero.getParty().getMembers()) {
                Player pPlayer = pHero.getPlayer();
                
                if (pHero == hero) {
                    // Doesn't apply to the user
                    continue;
                }
                
                if (!pPlayer.getWorld().equals(player.getWorld())) {
                    continue;
                }

                if (pPlayer.getLocation().distanceSquared(player.getLocation()) > rangeSquared) {
                    continue;
                }

                if (pHero.hasEffect("Sacrifice")) {
                    if (((SacrificeEffect) pHero.getEffect("Sacrifice")).getIncreaseValue() > mEffect.getIncreaseValue())
                        continue;
                }

                pHero.addEffect(mEffect);
            }
        }
        
        return SkillResult.NORMAL;
    }

    public class SacrificeEffect extends AttributeIncreaseEffect {

        AttributeIncreaseEffect strengthEffect;
        
        public SacrificeEffect(Skill skill, Player applier, long duration, int conIncrease, int strIncrease) {
            super(skill, "Sacrifice", applier, duration, AttributeType.CONSTITUTION, conIncrease, null, null);
            strengthEffect = new AttributeIncreaseEffect(skill, "SacrificeStrength", applier, duration, AttributeType.STRENGTH, strIncrease, null, null);
            
            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.MAGIC);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            strengthEffect.applyToHero(hero);
            
            Messaging.send(hero.getPlayer(), applyText);
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            strengthEffect.removeFromHero(hero);
            
            Messaging.send(hero.getPlayer(), expireText);
        }
    }
}
