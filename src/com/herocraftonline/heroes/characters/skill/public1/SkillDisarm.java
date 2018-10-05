package com.herocraftonline.heroes.characters.skill.public1;

import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.common.DisarmEffect;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;

public class SkillDisarm extends TargettedSkill {

    private String applyText;
    private String expireText;

    public SkillDisarm(Heroes plugin) {
        super(plugin, "Disarm");
        setDescription("You disarm your target for $1 seconds.");
        setUsage("/skill disarm");
        setArgumentRange(0, 0);
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.AGGRESSIVE);
        setIdentifiers("skill disarm");
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 3000, false);
        String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription().replace("$1", formattedDuration);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.USE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% was disarmed by %hero%!");
        node.set(SkillSetting.MAX_DISTANCE.node(), 4);
        node.set(SkillSetting.DURATION.node(), 3000);
        node.set(SkillSetting.APPLY_TEXT.node(), "");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% has found their weapon again!");

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%target% was disarmed by %hero%!").replace("%target%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%target% has found their weapon again!").replace("%target%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, final LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        if (!(target instanceof Player))
            return SkillResult.INVALID_TARGET;

        Hero targetHero = plugin.getCharacterManager().getHero((Player) target);


        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 3000, false);

        // Weird method used when items don't drop on death
        /*int strDecrease = SkillConfigManager.getUseSetting(hero, this, "str-decrease", 90, false);
        //targetHero.addEffect(new StrDecreaseEffect(this, player, duration, applyText, expireText));

        AttributeDecreaseEffect aEffect = new AttributeDecreaseEffect(this, "StrDecreaseEffect", player, duration, AttributeType.STRENGTH, strDecrease, applyText, expireText);
        if(hero.hasEffect("StrDecreaseEffect")) {
            if(((AttributeDecreaseEffect) hero.getEffect("StrDecreaseEffect")).getDecreaseValue() > aEffect.getDecreaseValue()) {
                Messaging.send(player, "Target has a more powerful effect already!");
                return SkillResult.CANCELLED;
            }
        }
        targetHero.addEffect(aEffect);*/

        Material heldItem = targetHero.getPlayer().getItemInHand().getType();

        if (!Util.isWeapon(heldItem) && !Util.isAwkwardWeapon(heldItem)) {
            player.sendMessage("You cannot disarm that target!");
            return SkillResult.FAIL;
        }

        if (targetHero.hasEffectType(EffectType.DISARM)) {
            player.sendMessage("Target is already disarmed.");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }
        targetHero.addEffect(new DisarmEffect(this, player, duration, applyText, expireText));

        player.getWorld().playEffect(player.getLocation(), Effect.EXTINGUISH, 3);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.8F, 1.0F);

        broadcastExecuteText(hero, target);


        return SkillResult.NORMAL;
    }
}