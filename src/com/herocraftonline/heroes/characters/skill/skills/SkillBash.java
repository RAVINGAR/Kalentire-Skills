package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;

public class SkillBash extends TargettedSkill {

    public SkillBash(Heroes plugin) {
        super(plugin, "Bash");
        setDescription("You deal $1 physical damage to your target and interrupt any warmup ability they are currently using.");
        setUsage("/skill bash");
        setArgumentRange(0, 0);
        setIdentifiers("skill bash");
        setTypes(SkillType.ABILITY_PROPERTY_PHYSICAL, SkillType.AGGRESSIVE, SkillType.INTERRUPTING);
    }

    @Override
    public String getDescription(Hero hero) {
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 10, false);
        double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 0.7, false);
        damage += (int) (damageIncrease * hero.getAttributeValue(AttributeType.STRENGTH));

        return getDescription().replace("$1", damage + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.DAMAGE.node(), 30);

        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 4, false);
        damage += SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_STRENGTH, 0.7, false) * hero.getAttributeValue(AttributeType.STRENGTH);

        addSpellTarget(target, hero);
        damageEntity(target, hero.getPlayer(), damage, DamageCause.ENTITY_ATTACK);

        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.ZOMBIE_METAL , 0.4F, 1.0F); 
        broadcastExecuteText(hero, target);
        return SkillResult.NORMAL;
    }
}