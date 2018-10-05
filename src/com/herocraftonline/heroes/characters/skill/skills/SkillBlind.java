package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.BlindEffect;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Util;

public class SkillBlind extends TargettedSkill {
    private String applyText;
    private String expireText;

    public SkillBlind(Heroes plugin) {
        super(plugin, "Blind");
        setDescription("You blind the target for $1 seconds.");
        setUsage("/skill blind");
        setArgumentRange(0, 0);
        setIdentifiers("skill blind");
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.DEBUFFING, SkillType.AGGRESSIVE, SkillType.DAMAGING);
    }

    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 4000, false);
        int durationIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE_PER_CHARISMA, 50, false);
        duration += hero.getAttributeValue(AttributeType.CHARISMA) * durationIncrease;

        String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription().replace("$1", formattedDuration);
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 10);
        node.set(SkillSetting.DURATION.node(), 2000);
        node.set(SkillSetting.DURATION_INCREASE_PER_CHARISMA.node(), 62);
        node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% has been blinded!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% can see again!");

        return node;
    }

    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% has been blinded!").replace("%target%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% can see again!").replace("%target%", "$1");
    }

    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        if (!(target instanceof Player)) {
            player.sendMessage("You must target a player!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 4000, false);
        int durationIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE_PER_CHARISMA, 50, false);
        duration += hero.getAttributeValue(AttributeType.CHARISMA) * durationIncrease;

        BlindEffect effect = new BlindEffect(this, player, duration, applyText, expireText);

        plugin.getCharacterManager().getHero((Player) target).addEffect(effect);

        //player.getWorld().spigot().playEffect(target.getLocation(), org.bukkit.Effect.SPELL, 0, 0, 0, 0, 0, 0, 16, 16);
        player.getWorld().spawnParticle(Particle.SPELL, target.getLocation(), 10, 0, 0, 0, 0);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_AMBIENT, 0.8F, 1.0F);

        return SkillResult.NORMAL;
    }
}