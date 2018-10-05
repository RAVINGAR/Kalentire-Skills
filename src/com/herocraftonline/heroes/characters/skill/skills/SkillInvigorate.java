package com.herocraftonline.heroes.characters.skill.skills;

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

public class SkillInvigorate extends TargettedSkill{
    
    public SkillInvigorate(Heroes plugin) {
        super(plugin, "Invigorate");
        setDescription("Invigorate your target, refilling $1 points of their stamina.");
        setUsage("/skill invigorate <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill invigorate");
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.SILENCEABLE, SkillType.ABILITY_PROPERTY_EARTH, SkillType.STAMINA_INCREASING);
    }

    @Override
    public String getDescription(Hero hero) {

        int staminaGain = SkillConfigManager.getUseSetting(hero, this, "stamina-gain", 1000, false);

        return getDescription().replace("$1", staminaGain + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 10);
        node.set("stamina-gain", 1000);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {

        if (!(target instanceof Player))
            return SkillResult.INVALID_TARGET;

        Player player = hero.getPlayer();
        Player targetPlayer = (Player) target;
        Hero targetHero = plugin.getCharacterManager().getHero(targetPlayer);

        if (targetHero.getStamina() >= targetHero.getMaxStamina()) {
            player.sendMessage("Your target already has full stamina!");
            return SkillResult.CANCELLED;
        }

        int staminaGain = SkillConfigManager.getUseSetting(hero, this, "stamina-gain", 1000, false);
        targetHero.setStamina(targetHero.getStamina() + staminaGain);

        broadcastExecuteText(hero, target);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 0.5F, 1.0F);

        return SkillResult.NORMAL;
    }
}
