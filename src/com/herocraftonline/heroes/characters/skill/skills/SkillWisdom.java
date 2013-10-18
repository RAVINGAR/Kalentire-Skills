package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.common.ManaRegenIncreaseEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

public class SkillWisdom extends ActiveSkill {
    private String applyText;
    private String expireText;

    public SkillWisdom(Heroes plugin) {
        super(plugin, "Wisdom");
        setDescription("Bless nearby party members with the gift of wisdom, increasing their mana regeneration by $1 for $2 minutes.");
        setArgumentRange(0, 0);
        setUsage("/skill wisdom");
        setIdentifiers("skill wisdom");
        setTypes(SkillType.BUFFING, SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.AREA_OF_EFFECT, SkillType.SILENCABLE);
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 600000, false);

        int manaRegen = SkillConfigManager.getUseSetting(hero, this, "mana-regen", Integer.valueOf(10), false);
        double manaRegenIncrease = SkillConfigManager.getUseSetting(hero, this, "mana-regen-increase-per-intellect", Double.valueOf(0.375), false);
        manaRegen += (int) (manaRegenIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        String formattedDuration = Util.decFormat.format((duration / 1000.0) / 60.0);   // Convert to minutes.

        return getDescription().replace("$1", manaRegen + "").replace("$2", formattedDuration);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.RADIUS.node(), Integer.valueOf(10));
        node.set("mana-regen", Double.valueOf(1.2));
        node.set("mana-regen-increase-per-intellect", Double.valueOf(0.375));
        node.set(SkillSetting.DURATION.node(), Integer.valueOf(180000));
        node.set(SkillSetting.APPLY_TEXT.node(), Messaging.getSkillDenoter() + "You feel a bit wiser!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), Messaging.getSkillDenoter() + "You no longer feel as wise!");

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, Messaging.getSkillDenoter() + "You feel a bit wiser!");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, Messaging.getSkillDenoter() + "You no longer feel as wise!");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 600000, false);
        int manaRegen = SkillConfigManager.getUseSetting(hero, this, "mana-regen", Integer.valueOf(10), false);
        double manaRegenIncrease = SkillConfigManager.getUseSetting(hero, this, "mana-regen-increase-per-intellect", Double.valueOf(0.375), false);
        manaRegen += (int) (manaRegenIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        WisdomEffect mEffect = new WisdomEffect(this, player, duration, manaRegen);

        broadcastExecuteText(hero);
        player.getWorld().playSound(player.getLocation(), Sound.WITHER_SPAWN, 0.5F, 1.0F);

        if (!hero.hasParty()) {
            if (hero.hasEffect("Wisdom")) {
                if (((WisdomEffect) hero.getEffect("Wisdom")).getIncreaseValue() > mEffect.getIncreaseValue()) {
                    Messaging.send(player, "You have a more powerful effect already!");
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

                if (pHero.hasEffect("Wisdom")) {
                    if (((WisdomEffect) pHero.getEffect("Wisdom")).getIncreaseValue() > mEffect.getIncreaseValue())
                        continue;
                }

                pHero.addEffect(mEffect);
            }
        }

        return SkillResult.NORMAL;
    }

    public class WisdomEffect extends ManaRegenIncreaseEffect {

        public WisdomEffect(Skill skill, Player applier, long duration, int manaRegenIncrease) {
            super(skill, "Wisdom", applier, duration, manaRegenIncrease, null, null);

            types.add(EffectType.DISPELLABLE);
            types.add(EffectType.MAGIC);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            Messaging.send(hero.getPlayer(), applyText);
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            Messaging.send(hero.getPlayer(), expireText);
        }
    }
}
