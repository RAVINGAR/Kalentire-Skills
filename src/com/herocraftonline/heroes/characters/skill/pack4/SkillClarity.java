package com.herocraftonline.heroes.characters.skill.pack4;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.common.ManaRegenIncreaseEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.CompatSound;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class SkillClarity extends ActiveSkill {
    private String applyText;
    private String expireText;

    public SkillClarity(Heroes plugin) {
        super(plugin, "Clarity");
        setDescription("Bless nearby party members with the gift of clarity, increasing their mana regeneration by $1 for $2 minutes.");
        setArgumentRange(0, 0);
        setUsage("/skill clarity");
        setIdentifiers("skill clarity");
        setTypes(SkillType.BUFFING, SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.AREA_OF_EFFECT, SkillType.SILENCEABLE);
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 600000, false);

        int manaRegen = SkillConfigManager.getUseSetting(hero, this, "mana-regen", 10, false);
        double manaRegenIncrease = SkillConfigManager.getUseSetting(hero, this, "mana-regen-increase-per-intellect", 0.375, false);
        manaRegen += (int) (manaRegenIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        String formattedDuration = Util.decFormat.format((duration / 1000.0) / 60.0);   // Convert to minutes.

        return getDescription().replace("$1", manaRegen + "").replace("$2", formattedDuration);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.RADIUS.node(), 10);
        node.set("mana-regen", 1.2);
        node.set("mana-regen-increase-per-intellect", 0.375);
        node.set(SkillSetting.DURATION.node(), 180000);
        node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "You feel a bit wiser!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "You no longer feel as wise!");

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "You feel a bit wiser!");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "You no longer feel as wise!");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 600000, false);
        int manaRegen = SkillConfigManager.getUseSetting(hero, this, "mana-regen", 10, false);
        double manaRegenIncrease = SkillConfigManager.getUseSetting(hero, this, "mana-regen-increase-per-intellect", 0.375, false);
        manaRegen += (int) (manaRegenIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

        ClarityEffect mEffect = new ClarityEffect(this, player, duration, manaRegen);

        broadcastExecuteText(hero);
        player.getWorld().playSound(player.getLocation(), CompatSound.ENTITY_WITHER_SPAWN.value(), 0.5F, 1.0F);

        if (!hero.hasParty()) {
            if (hero.hasEffect("Clarity")) {
                if (((ClarityEffect) hero.getEffect("Clarity")).getDelta() > mEffect.getDelta()) {
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

                if (pHero.hasEffect("Clarity")) {
                    if (((ClarityEffect) pHero.getEffect("Clarity")).getDelta() > mEffect.getDelta())
                        continue;
                }

                pHero.addEffect(mEffect);
            }
        }

        return SkillResult.NORMAL;
    }

    public class ClarityEffect extends ManaRegenIncreaseEffect {

        public ClarityEffect(Skill skill, Player applier, long duration, int manaRegenIncrease) {
            super(skill, "Clarity", applier, duration, manaRegenIncrease, null, null);

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
