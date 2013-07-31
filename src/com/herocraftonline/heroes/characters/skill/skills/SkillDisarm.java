package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.common.DisarmEffect;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

public class SkillDisarm extends TargettedSkill {

    private String applyText;
    private String expireText;

    public SkillDisarm(Heroes plugin) {
        super(plugin, "Disarm");
        setDescription("You disarm your target for $1 seconds.");
        setUsage("/skill disarm");
        setArgumentRange(0, 0);
        setTypes(SkillType.PHYSICAL, SkillType.DEBUFF, SkillType.HARMFUL);
        setIdentifiers("skill disarm");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DURATION.node(), 3000);
        node.set(SkillSetting.APPLY_TEXT.node(), "%target% was disarmed!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), "%target% has found his weapon again!");
        return node;
    }

    @Override
    public void init() {
        super.init();
        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, "%target% has stopped regenerating mana!").replace("%target%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "%target% is once again regenerating mana!").replace("%target%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        if (!(target instanceof Player))
        	return SkillResult.INVALID_TARGET;

        Hero tHero = plugin.getCharacterManager().getHero((Player) target);

        Material heldItem = tHero.getPlayer().getItemInHand().getType();

        if (!Util.isWeapon(heldItem) && !Util.isAwkwardWeapon(heldItem)) {
            Messaging.send(player, "You cannot disarm bare hands!");
            return SkillResult.FAIL;
        }

        if (tHero.hasEffectType(EffectType.DISARM)) {
            Messaging.send(player, "%target% is already disarmed.");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 500, false);
        tHero.addEffect(new DisarmEffect(this, duration, applyText, expireText));
        player.getWorld().playEffect(player.getLocation(), Effect.EXTINGUISH, 3);
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.ITEM_BREAK , 0.8F, 1.0F);
        broadcastExecuteText(hero, target);
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 3000, false);
        return getDescription().replace("$1", duration / 1000 + "");
    }
}
