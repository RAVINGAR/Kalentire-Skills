package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.CompatSound;

public class SkillAsuraSlash extends TargettedSkill {

    public SkillAsuraSlash(Heroes plugin) {
        super(plugin, "AsuraSlash");
        setDescription("By the power of Asura you slash your target for $1 dark damage.");
        setUsage("/skill asuraslash");
        setArgumentRange(0, 0);
        setIdentifiers("skill asuraslash");
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.AGGRESSIVE, SkillType.ABILITY_PROPERTY_DARK);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DAMAGE.node(), 10);
        node.set(SkillSetting.DAMAGE_INCREASE.node(), 0.0);
        return node;
    }

    @Override
    public String getDescription(Hero hero) {
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 100, false);
        damage += SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE, 0.0, false) * hero.getHeroLevel(this);
        return getDescription().replace("$1", damage + "");
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 110, false);
        damage += SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE, 0.0, false) * hero.getHeroLevel(this);

        addSpellTarget(target, hero);
        damageEntity(target, hero.getPlayer(), damage, DamageCause.ENTITY_ATTACK);

        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), CompatSound.ITEM_FLINTANDSTEEL_USE.value() , 0.4F, 1.0F); 
        broadcastExecuteText(hero, target);
        return SkillResult.NORMAL;
    }
}
