package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;

public class SkillInversion extends TargettedSkill {

    public SkillInversion(Heroes plugin) {
        super(plugin, "Inversion");
        setDescription("You deal magic damage equal to $1% of the mana the target is missing.");
        setUsage("/skill inversion");
        setArgumentRange(0, 0);
        setIdentifiers("skill inversion");
        setTypes(SkillType.MANA_DECREASING, SkillType.DAMAGING, SkillType.SILENCEABLE, SkillType.AGGRESSIVE);
    }

    @Override
    public String getDescription(Hero hero) {
        double damageMod = SkillConfigManager.getUseSetting(hero, this, "damage-modifier", 1.0, false);
        damageMod += (SkillConfigManager.getUseSetting(hero, this, "damage-modifier-increase", 0.0, false) * hero.getHeroLevel(this));
        String description = getDescription().replace("$1", (int) (damageMod * 100) + "");
        
        //COOLDOWN
        int cooldown = (SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN, 0, false)
                - SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN_REDUCE, 0, false) * hero.getHeroLevel(this)) / 1000;
        if (cooldown > 0) {
            description += " CD:" + cooldown + "s";
        }
        
        //MANA
        int mana = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MANA, 10, false)
                - (SkillConfigManager.getUseSetting(hero, this, SkillSetting.MANA_REDUCE, 0, false) * hero.getHeroLevel(this));
        if (mana > 0) {
            description += " M:" + mana;
        }
        
        //HEALTH_COST
        int healthCost = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALTH_COST, 0, false) - 
                (SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALTH_COST_REDUCE, mana, true) * hero.getHeroLevel(this));
        if (healthCost > 0) {
            description += " HP:" + healthCost;
        }
        
        //STAMINA
        int staminaCost = SkillConfigManager.getUseSetting(hero, this, SkillSetting.STAMINA, 0, false)
                - (SkillConfigManager.getUseSetting(hero, this, SkillSetting.STAMINA_REDUCE, 0, false) * hero.getHeroLevel(this));
        if (staminaCost > 0) {
            description += " FP:" + staminaCost;
        }
        
        //DELAY
        int delay = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DELAY.node(), 0, false) / 1000;
        if (delay > 0) {
            description += " W:" + delay + "s";
        }
        
        //EXP
        int exp = SkillConfigManager.getUseSetting(hero, this, SkillSetting.EXP.node(), 0, false);
        if (exp > 0) {
            description += " XP:" + exp;
        }
        return description;
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("max-damage", 0);
        node.set("damage-modifier", 1);
        node.set("damage-modifier-increase", 0);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        if (!(target instanceof Player)) {
            return SkillResult.INVALID_TARGET;
        }
        Player player = hero.getPlayer();
        Hero enemy = plugin.getCharacterManager().getHero((Player) target);

        int maxDamage = SkillConfigManager.getUseSetting(hero, this, "max-damage", 0, false);
        double damageMod = SkillConfigManager.getUseSetting(hero, this, "damage-modifier", 1.0, false) +
                (SkillConfigManager.getUseSetting(hero, this, "damage-modifier-increase", 0.0, false) * hero.getHeroLevel(this));
        double damage = ((enemy.getMaxMana() - enemy.getMana()) * damageMod);
        if (maxDamage != 0 && damage > maxDamage) {
            damage = maxDamage;
        }
        addSpellTarget(target, hero);
        damageEntity(target, player, damage, DamageCause.MAGIC);
        //target.damage(damage, player);
        broadcastExecuteText(hero, target);
        return SkillResult.NORMAL;
    }

}
