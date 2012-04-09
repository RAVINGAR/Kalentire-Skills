package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainManaEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;

public class SkillUnholyRitual extends TargettedSkill {

    public SkillUnholyRitual(Heroes plugin) {
        super(plugin, "UnholyRitual");
        setDescription("You sacrifice a summoned monster for $1 mana.");
        setUsage("/skill unholyritual");
        setArgumentRange(0, 0);
        setIdentifiers("skill unholyritual", "skill uritual");
        setTypes(SkillType.DARK, SkillType.SILENCABLE, SkillType.DAMAGING);
    }
    
    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("mana-regen", 20);
        return node;
    }
    
    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        if (target instanceof Player) {
            return SkillResult.INVALID_TARGET;
        }
        Player player = hero.getPlayer();
        
        Monster monster = plugin.getCharacterManager().getMonster(target);
        if (!hero.getSummons().contains(monster)) {
        	return SkillResult.INVALID_TARGET;
        }

        addSpellTarget(target, hero);
        damageEntity(target, player, target.getHealth(), DamageCause.MAGIC);
        int mana = SkillConfigManager.getUseSetting(hero, this, "mana-regen", 20, false);
        
        //Fire the Mana regen event
        HeroRegainManaEvent hrmEvent = new HeroRegainManaEvent(hero, mana, this);
        plugin.getServer().getPluginManager().callEvent(hrmEvent);
        if (hrmEvent.isCancelled()) {
        	mana = 0;
        } else {
        	mana = hrmEvent.getAmount();
        }
        
        hero.setMana(hero.getMana() + mana);
        broadcastExecuteText(hero, target);
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(Hero hero) {
        int mana = SkillConfigManager.getUseSetting(hero, this, "mana-regen", 20, false);
        return getDescription().replace("$1", mana + "");
    }
}
