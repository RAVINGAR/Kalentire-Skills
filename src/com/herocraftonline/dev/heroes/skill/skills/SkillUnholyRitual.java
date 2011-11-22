package com.herocraftonline.dev.heroes.skill.skills;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.config.ConfigurationNode;

import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.api.HeroRegainManaEvent;
import com.herocraftonline.dev.heroes.api.SkillResult;
import com.herocraftonline.dev.heroes.hero.Hero;
import com.herocraftonline.dev.heroes.skill.SkillType;
import com.herocraftonline.dev.heroes.skill.TargettedSkill;

public class SkillUnholyRitual extends TargettedSkill {

    public SkillUnholyRitual(Heroes plugin) {
        super(plugin, "UnholyRitual");
        setDescription("You sacrifice a summoned monster for mana");
        setUsage("/skill unholyritual");
        setArgumentRange(0, 0);
        setIdentifiers("skill unholyritual", "skill uritual");
        setTypes(SkillType.DARK, SkillType.SILENCABLE, SkillType.DAMAGING);
    }
    
    @Override
    public ConfigurationNode getDefaultConfig() {
        ConfigurationNode node = super.getDefaultConfig();
        node.setProperty("mana-regen", 20);
        return node;
    }
    
    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();
        
        if (!hero.getSummons().contains(target))
        	return SkillResult.INVALID_TARGET;

        addSpellTarget(target, hero);
        target.damage(target.getHealth(), player);
        int mana = getSetting(hero, "mana-regen", 20, false);
        
        //Fire the Mana regen event
        HeroRegainManaEvent hrmEvent = new HeroRegainManaEvent(hero, mana, this);
        plugin.getServer().getPluginManager().callEvent(hrmEvent);
        if (hrmEvent.isCancelled())
        	mana = 0;
        else
        	mana = hrmEvent.getAmount();
        
        hero.setMana(hero.getMana() + mana);
        broadcastExecuteText(hero, target);
        return SkillResult.NORMAL;
    }

}
