package com.herocraftonline.heroes.characters.skill.reborn;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainManaEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.chat.ChatComponents;
import org.bukkit.Effect;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SkillBattery extends TargettedSkill {

    public SkillBattery(Heroes plugin) {
        super(plugin, "Battery");
        setDescription("You grant $1 mana to your target, at the cost of $2 mana.");
        setUsage("/skill battery");
        setIdentifiers("skill battery");
        setArgumentRange(0, 1);
        setTypes(SkillType.SILENCEABLE, SkillType.BUFFING, SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.MANA_DECREASING,
                SkillType.MANA_INCREASING, SkillType.ABILITY_PROPERTY_DARK);
    }

    @Override
    public String getDescription(Hero hero) {
        int gain = SkillConfigManager.getUseSetting(hero, this, "target-mana-gain", 150, false);
        int cost = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MANA, 250, false);
        return getDescription()
                .replace("$1", gain + "")
                .replace("$2", cost + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection config = super.getDefaultConfig();
        config.set(SkillSetting.MAX_DISTANCE.node(), 12);
        config.set(SkillSetting.MANA.node(), 250);
        config.set("target-mana-gain", 150);
        return config;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        if (!(target instanceof Player) || player.equals(target))
            return SkillResult.INVALID_TARGET;

        broadcastExecuteText(hero, target);
        Hero targetHero = plugin.getCharacterManager().getHero((Player) target);

        int gainAmount = SkillConfigManager.getUseSetting(hero, this, "target-mana-gain", 150, false);
        HeroRegainManaEvent manaEvent = new HeroRegainManaEvent(targetHero, gainAmount, this);
        plugin.getServer().getPluginManager().callEvent(manaEvent);
        if (manaEvent.isCancelled())
            return SkillResult.CANCELLED;

        targetHero.setMana(manaEvent.getDelta() + targetHero.getMana());
        if (targetHero.isVerboseMana())
            targetHero.getPlayer().sendMessage(ChatComponents.Bars.mana(targetHero.getMana(), targetHero.getMaxMana(), true));

        player.getWorld().playEffect(player.getLocation(), Effect.ENDEREYE_LAUNCH, 3);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5F, 1.0F);
        target.getWorld().spawnParticle(Particle.SPELL_WITCH, target.getLocation(), 55, 0, 1, 0, 10);
        return SkillResult.NORMAL;
    }
}