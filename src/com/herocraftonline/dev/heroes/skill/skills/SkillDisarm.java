package com.herocraftonline.dev.heroes.skill.skills;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.api.SkillResult;
import com.herocraftonline.dev.heroes.effects.EffectType;
import com.herocraftonline.dev.heroes.effects.common.DisarmEffect;
import com.herocraftonline.dev.heroes.hero.Hero;
import com.herocraftonline.dev.heroes.skill.SkillType;
import com.herocraftonline.dev.heroes.skill.TargettedSkill;
import com.herocraftonline.dev.heroes.util.Messaging;
import com.herocraftonline.dev.heroes.util.Setting;
import com.herocraftonline.dev.heroes.util.Util;

public class SkillDisarm extends TargettedSkill {

    private String applyText;
    private String expireText;

    public SkillDisarm(Heroes plugin) {
        super(plugin, "Disarm");
        setDescription("Disarm your target");
        setUsage("/skill disarm <target>");
        setArgumentRange(0, 1);
        setTypes(SkillType.PHYSICAL, SkillType.DEBUFF, SkillType.HARMFUL);
        setIdentifiers("skill disarm");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(Setting.DURATION.node(), 3000);
        node.set(Setting.APPLY_TEXT.node(), "%target% was disarmed!");
        node.set(Setting.EXPIRE_TEXT.node(), "%target% has found his weapon again!");
        return node;
    }

    @Override
    public void init() {
        super.init();
        applyText = getSetting(null, Setting.APPLY_TEXT.node(), "%target% has stopped regenerating mana!").replace("%target%", "$1");
        expireText = getSetting(null, Setting.EXPIRE_TEXT.node(), "%target% is once again regenerating mana!").replace("%target%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        if (!(target instanceof Player))
        	return SkillResult.INVALID_TARGET;

        Hero tHero = plugin.getHeroManager().getHero((Player) target);

        if (!Util.isWeapon(tHero.getPlayer().getItemInHand().getType())) {
            Messaging.send(player, "You cannot disarm bare hands!");
            return SkillResult.FAIL;
        }

        if (tHero.hasEffectType(EffectType.DISARM)) {
            Messaging.send(player, "%target% is already disarmed.");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        int duration = getSetting(hero, Setting.DURATION.node(), 500, false);
        tHero.addEffect(new DisarmEffect(this, duration, applyText, expireText));
        broadcastExecuteText(hero, target);
        return SkillResult.NORMAL;
    }
}
