package com.herocraftonline.heroes.characters.skill.pack6;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.AttributeIncreaseEffect;
import com.herocraftonline.heroes.characters.effects.common.SlowEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class SkillSacrifice extends ActiveSkill {
    private String applyText;
    private String expireText;

    public SkillSacrifice(Heroes plugin) {
        super(plugin, "Sacrifice");
        setDescription("Sacrifice yourself in combat to increase allies' Constitution and Strength by $1 and $2 for $3 seconds.");
        setArgumentRange(0, 0);
        setUsage("/skill sacrifice");
        setIdentifiers("skill sacrifice");
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
        node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "A sacrifice was made for your sake!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "The sacrifice's effect wore off...");

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "A sacrifice was made for your sake!");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "The sacrifice's effect wore off...");
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

        SacrificeConstitutionIncreaseEffect cEffect = new SacrificeConstitutionIncreaseEffect(this, player, duration, conIncrease);
        AttributeIncreaseEffect sEffect = new AttributeIncreaseEffect(this, "SacrificeStrengthIncreaseEffect",  player, duration, AttributeType.STRENGTH, strIncrease, null, null);

        if (!hero.hasParty()) {
            player.sendMessage("You must have a party to sacrifice for!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }
        else {
            broadcastExecuteText(hero);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN.value(), 0.5F, 1.0F);
            double maxHealth = player.getMaxHealth();
            if(maxHealth * 0.01 < player.getHealth()) {
                player.setHealth(maxHealth * 0.01);
            }
            hero.addEffect(new SlowEffect(this, "SacrificeSlowEffect", player, 60000, 3, null, null));
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

                // The effects can have different values, so two checks
                if (pHero.hasEffect("SacrificeConstitutionIncreaseEffect")) {
                    if (((AttributeIncreaseEffect) pHero.getEffect("SacrificeConstitutionIncreaseEffect")).getDelta() > cEffect.getDelta())
                        continue;
                }
                pHero.addEffect(cEffect);

                if (pHero.hasEffect("SacrificeStrengthIncreaseEffect")) {
                    if (((AttributeIncreaseEffect) pHero.getEffect("SacrificeConstitutionIncreaseEffect")).getDelta() > cEffect.getDelta())
                        continue;
                }
                pHero.addEffect(sEffect);
            }
        }
        
        return SkillResult.NORMAL;
    }

    // So we can send apply/expire messages to players directly
    public class SacrificeConstitutionIncreaseEffect extends AttributeIncreaseEffect {
        
        public SacrificeConstitutionIncreaseEffect(Skill skill, Player applier, long duration, int conIncrease) {
            super(skill, "SacrificeConstitutionIncreaseEffect", applier, duration, AttributeType.CONSTITUTION, conIncrease, null, null);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            
            hero.getPlayer().sendMessage(applyText);
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            
            hero.getPlayer().sendMessage(expireText);
        }
    }
}
