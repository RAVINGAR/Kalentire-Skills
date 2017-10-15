package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.AttributeDecreaseEffect;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.CompatSound;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.Effect;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SkillEnsnare extends TargettedSkill {

    private String applyText;
    private String expireText;

    public SkillEnsnare(Heroes plugin) {
        super(plugin, "Ensare");
        setDescription("You ensare your target for $1 seconds.");
        setUsage("/skill ensnare");
        setArgumentRange(0, 0);
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.AGGRESSIVE);
        setIdentifiers("skill ensare");
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

        node.set(SkillSetting.USE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% was ensnared by %hero%!");
        node.set(SkillSetting.MAX_DISTANCE.node(), 4);
        node.set(SkillSetting.DURATION.node(), 3000);
        node.set(SkillSetting.APPLY_TEXT.node(), "");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% has broken free!");

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%target% was ensnared by %hero%!").replace("%target%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%target% has broken free!").replace("%target%", "$1");
    }

    @Override
    public SkillResult use(Hero hero, final LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        if (!(target instanceof Player))
            return SkillResult.INVALID_TARGET;

        Hero targetHero = plugin.getCharacterManager().getHero((Player) target);

        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 3000, false);
        int strDecrease = SkillConfigManager.getUseSetting(hero, this, "agi-decrease", 500, false);
        //targetHero.addEffect(new StrDecreaseEffect(this, player, duration, applyText, expireText));

        AttributeDecreaseEffect aEffect = new AttributeDecreaseEffect(this, "AgiDecreaseEffect", player, duration, AttributeType.DEXTERITY, strDecrease, applyText, expireText);
        if(hero.hasEffect("AgiDecreaseEffect")) {
            if(((AttributeDecreaseEffect) hero.getEffect("AgiDecreaseEffect")).getDelta() > aEffect.getDelta()) {
                Messaging.send(player, "Target has a more powerful effect already!");
                return SkillResult.CANCELLED;
            }
        }
        player.getWorld().playEffect(player.getLocation(), Effect.EXTINGUISH, 3);
        player.getWorld().playSound(player.getLocation(), CompatSound.ENTITY_ITEM_BREAK.value(), 0.8F, 1.0F);

        broadcastExecuteText(hero, target);

        targetHero.addEffect(aEffect);
        return SkillResult.NORMAL;
    }
}