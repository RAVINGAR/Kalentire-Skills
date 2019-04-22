package com.herocraftonline.heroes.characters.skill.pack4;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.common.AttributeIncreaseEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;

import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.Sound;

public class SkillHonor extends ActiveSkill {
    private String applyText;
    private String expireText;

    public SkillHonor(Heroes plugin) {
        super(plugin, "Honor");
        setDescription("Bring honor unto your nearby party members, increasing their mana regeneration by $1 for $2 minutes.");
        setArgumentRange(0, 0);
        setUsage("/skill honor");
        setIdentifiers("skill honor");
        setTypes(SkillType.BUFFING, SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.AREA_OF_EFFECT, SkillType.SILENCEABLE);
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 600000, false);

        int conIncrease = SkillConfigManager.getUseSetting(hero, this, "con-increase", 5, false);
        double conIncreaseScaling = SkillConfigManager.getUseSetting(hero, this, "con-increase-per-strength", 0.0, false);
        conIncrease += (int) (conIncreaseScaling * hero.getAttributeValue(AttributeType.STRENGTH));

        String formattedDuration = Util.decFormat.format((duration / 1000.0) / 60.0);   // Convert to minutes.

        return getDescription().replace("$1", conIncrease + "").replace("$2", formattedDuration);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.RADIUS.node(), 10);
        node.set("con-increase", 5);
        node.set("con-increase-per-strength", 0.0);
        node.set(SkillSetting.DURATION.node(), 180000);
        node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "Honor is bestowed upon you!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "You are no longer honorable!");

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "Honor is bestowed upon you!");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "You are no longer honorable!");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 600000, false);
        int conIncrease = SkillConfigManager.getUseSetting(hero, this, "con-increase", 5, false);
        double conIncreaseScaling = SkillConfigManager.getUseSetting(hero, this, "con-increase-per-strength", 0.0, false);
        conIncrease += (int) (conIncreaseScaling * hero.getAttributeValue(AttributeType.STRENGTH));

        HonorEffect mEffect = new HonorEffect(this, player, duration, conIncrease);

        broadcastExecuteText(hero);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5F, 1.0F);

        if (!hero.hasParty()) {
            if (hero.hasEffect("Honor")) {
                if (((HonorEffect) hero.getEffect("Honor")).getDelta() > mEffect.getDelta()) {
                    player.sendMessage("You have a more powerful effect already!");
                    return SkillResult.CANCELLED;
                }
            }

            hero.addEffect(mEffect);
        }
        else {
            int rangeSquared = (int) Math.pow(SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 10, false), 2);
            for (Hero pHero : hero.getParty().getMembers()) {
                Player pPlayer = pHero.getPlayer();
                if (!pPlayer.getWorld().equals(player.getWorld())) {
                    continue;
                }

                if (pPlayer.getLocation().distanceSquared(player.getLocation()) > rangeSquared) {
                    continue;
                }

                if (pHero.hasEffect("Honor")) {
                    if (((HonorEffect) pHero.getEffect("Honor")).getDelta() > mEffect.getDelta())
                        continue;
                }

                pHero.addEffect(mEffect);
            }
        }

        return SkillResult.NORMAL;
    }

    public class HonorEffect extends AttributeIncreaseEffect {

        public HonorEffect(Skill skill, Player applier, long duration, int conIncrease) {
            super(skill, "Honor", applier, duration, AttributeType.CONSTITUTION, conIncrease, null, null);

            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.MAGIC);
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
