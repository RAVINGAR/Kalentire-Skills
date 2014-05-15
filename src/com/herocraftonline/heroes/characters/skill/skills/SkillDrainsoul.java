package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainHealthEvent;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.characters.skill.VisualEffect;
import com.herocraftonline.heroes.util.Util;

public class SkillDrainsoul extends TargettedSkill {
    // This is for Firework Effects
    public VisualEffect fplayer = new VisualEffect();

    public SkillDrainsoul(Heroes plugin) {
        super(plugin, "Drainsoul");
        setDescription("Drain the soul of your target, dealing $1 damage, and restoring $2 of your own health.");
        setUsage("/skill drainsoul");
        setArgumentRange(0, 0);
        setIdentifiers("skill drainsoul");
        setTypes(SkillType.ABILITY_PROPERTY_DARK, SkillType.SILENCEABLE, SkillType.DAMAGING, SkillType.AGGRESSIVE);
    }

    @Override
    public String getDescription(Hero hero) {
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, Integer.valueOf(98), false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, Double.valueOf(1.0), false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT);

        double healMult = SkillConfigManager.getUseSetting(hero, this, "heal-mult", Double.valueOf(0.77), false);

        String formattedDamage = Util.decFormat.format(damage);
        String formattedHeal = Util.decFormat.format(damage * healMult);

        return getDescription().replace("$1", formattedDamage).replace("$2", formattedHeal);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), Integer.valueOf(6));
        node.set(SkillSetting.MAX_DISTANCE_INCREASE_PER_INTELLECT.node(), Double.valueOf(0.15));
        node.set(SkillSetting.DAMAGE.node(), Integer.valueOf(98));
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), Double.valueOf(1.0));
        node.set("heal-mult", Double.valueOf(0.77));

        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, Integer.valueOf(98), false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, Double.valueOf(1.0), false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT);

        broadcastExecuteText(hero, target);

        addSpellTarget(target, hero);
        damageEntity(target, player, damage, DamageCause.MAGIC);

        double healMult = SkillConfigManager.getUseSetting(hero, this, "heal-mult", Double.valueOf(0.77), false);

        HeroRegainHealthEvent hrEvent = new HeroRegainHealthEvent(hero, (damage * healMult), this);         // Bypass self heal as this can only be used on themself.
        plugin.getServer().getPluginManager().callEvent(hrEvent);
        if (!hrEvent.isCancelled())
            hero.heal(hrEvent.getAmount());

        // this is our fireworks shit
        try {
            fplayer.playFirework(target.getWorld(),
                                 target.getLocation(),
                                 FireworkEffect.builder()
                                               .flicker(false).trail(true)
                                               .with(FireworkEffect.Type.BURST)
                                               .withColor(Color.GREEN)
                                               .withFade(Color.PURPLE)
                                               .build());
        }
        catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return SkillResult.NORMAL;
    }
}
