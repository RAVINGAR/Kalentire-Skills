package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainHealthEvent;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

public class SkillDrainsoul extends TargettedSkill {

    public SkillDrainsoul(Heroes plugin) {
        super(plugin, "Drainsoul");
        setDescription("Drain the soul of your target, dealing $1 damage, and restoring $2 of your own health.");
        setUsage("/skill drainsoul");
        setArgumentRange(0, 0);
        setIdentifiers("skill drainsoul");
        setTypes(SkillType.ABILITY_PROPERTY_MAGICAL, SkillType.ABILITY_PROPERTY_DARK, SkillType.SILENCEABLE, SkillType.DAMAGING, SkillType.AGGRESSIVE);
    }

    @Override
    public String getDescription(Hero hero) {
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 98, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.0, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT);

        double healMult = SkillConfigManager.getUseSetting(hero, this, "heal-mult", 0.77, false);

        String formattedDamage = Util.decFormat.format(damage);
        String formattedHeal = Util.decFormat.format(damage * healMult);

        return getDescription().replace("$1", formattedDamage).replace("$2", formattedHeal);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 6);
        node.set(SkillSetting.MAX_DISTANCE_INCREASE_PER_INTELLECT.node(), 0.15);
        node.set(SkillSetting.DAMAGE.node(), 98);
        node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 1.0);
        node.set("heal-mult", 0.77);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 98, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 1.0, false);
        damage += damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT);

        broadcastExecuteText(hero, target);

        addSpellTarget(target, hero);
        damageEntity(target, player, damage, DamageCause.MAGIC);

        double healMult = SkillConfigManager.getUseSetting(hero, this, "heal-mult", 0.77, false);

        HeroRegainHealthEvent hrEvent = new HeroRegainHealthEvent(hero, (damage * healMult), this);         // Bypass self heal as this can only be used on themself.
        plugin.getServer().getPluginManager().callEvent(hrEvent);
        if (!hrEvent.isCancelled())
            hero.heal(hrEvent.getDelta());

        return SkillResult.NORMAL;
    }
}
