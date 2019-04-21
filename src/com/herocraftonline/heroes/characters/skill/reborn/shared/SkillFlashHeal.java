package com.herocraftonline.heroes.characters.skill.reborn.shared;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainHealthEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.SilenceEffect;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SkillFlashHeal extends TargettedSkill {

    public SkillFlashHeal(Heroes plugin) {
        super(plugin, "FlashHeal");
        setDescription("Bless your target with a Sacred Hymn, restoring $1 health to your target. You are only healed for $2 health from this ability.");
        setUsage("/skill flashheal <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill flashheal");
        setTypes(SkillType.ABILITY_PROPERTY_LIGHT, SkillType.DISPELLING, SkillType.HEALING, SkillType.SILENCEABLE);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 10);
        node.set(SkillSetting.HEALING.node(), 150);
        node.set(SkillSetting.HEALING_INCREASE_PER_WISDOM.node(), 3.75);
        node.set(SkillSetting.DURATION.node(), 5000);
        node.set(SkillSetting.EXPIRE_TEXT.node(), "%hero% is no longer silenced!");

        return node;
    }

    @Override
    public String getDescription(Hero hero) {
        double healing = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING, 150, false);
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
        return getDescription();
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        if (!(target instanceof Player)) {
            return SkillResult.INVALID_TARGET;
        }
        Player player = hero.getPlayer();
        if ((Player)target == player)
        {
            player.sendMessage(" You must target another player!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }
        broadcastExecuteText(hero, target);
        Hero targetHero = plugin.getCharacterManager().getHero((Player) target);

        // instant heal
        double healing = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING, 150, false);
        final HeroRegainHealthEvent hrhEvent = new HeroRegainHealthEvent(targetHero, healing, this, hero);
        this.plugin.getServer().getPluginManager().callEvent(hrhEvent);
        if (hrhEvent.isCancelled()) {
            player.sendMessage(ChatColor.GRAY + "Unable to heal the target at this time!");
        }
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.9F, 1.0F);
        targetHero.heal(hrhEvent.getDelta());

        // silence
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
        SilenceEffect silence = new SilenceEffect(this, hero.getPlayer(), duration);
        player.getWorld().playSound(hero.getPlayer().getLocation(), Sound.ENTITY_ENDERMEN_TELEPORT , 0.8F, 1.0F);
        targetHero.addEffect(silence);

        return SkillResult.NORMAL;
    }
}
