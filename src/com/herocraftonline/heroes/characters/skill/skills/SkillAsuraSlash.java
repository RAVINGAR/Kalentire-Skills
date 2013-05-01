package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Sound;
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

public class SkillAsuraSlash extends TargettedSkill {

    public SkillAsuraSlash(Heroes plugin) {
        super(plugin, "AsuraSlash");
        setDescription("You deal $1 light damage to your target and interrupt any spell they are currently using.");
        setUsage("/skill asuraslash <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill asuraslash");
        setTypes(SkillType.PHYSICAL, SkillType.HARMFUL);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DAMAGE.node(), 10);
        node.set(SkillSetting.DAMAGE_INCREASE.node(), 0.0);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 110, false);
        damage += SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE, 0.0, false) * hero.getSkillLevel(this);
        this.addSpellTarget(target, hero);
        if (!damageEntity(target, hero.getPlayer(), damage, DamageCause.ENTITY_ATTACK)) {
            return SkillResult.INVALID_TARGET;
        }
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.FIRE_IGNITE , 0.4F, 1.0F); 
        broadcastExecuteText(hero, target);
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(Hero hero) {
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 100, false);
        damage += SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE, 0.0, false) * hero.getSkillLevel(this);
        return getDescription().replace("$1", damage + "");
    }
}