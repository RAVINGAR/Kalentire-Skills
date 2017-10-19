package com.herocraftonline.heroes.characters.skill.public1;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

public class SkillInversion extends TargettedSkill {

    public SkillInversion(Heroes plugin) {
        super(plugin, "Inversion");
        this.setDescription("You deal magic damage equal to $1% of the mana the target is missing.");
        this.setUsage("/skill inversion");
        this.setArgumentRange(0, 1);
        this.setIdentifiers("skill inversion");
        this.setTypes(SkillType.MANA_DECREASING, SkillType.DAMAGING, SkillType.SILENCEABLE, SkillType.AGGRESSIVE);
    }

    @Override
    public String getDescription(Hero hero) {
        double damageMod = SkillConfigManager.getUseSetting(hero, this, "damage-modifier", 1.0, false);
        damageMod += (SkillConfigManager.getUseSetting(hero, this, "damage-modifier-increase", 0.0, false) * hero.getLevel(this));
        String description = this.getDescription().replace("$1", (damageMod * 100) + "");

        //COOLDOWN
        final int cooldown = (SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN, 0, false) - (SkillConfigManager.getUseSetting(hero, this, SkillSetting.COOLDOWN_REDUCE, 0, false) * hero.getLevel(this))) / 1000;
        if (cooldown > 0) {
            description += " CD:" + cooldown + "s";
        }

        //MANA
        final int mana = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MANA, 10, false) - (SkillConfigManager.getUseSetting(hero, this, SkillSetting.MANA_REDUCE, 0, false) * hero.getLevel(this));
        if (mana > 0) {
            description += " M:" + mana;
        }

        //HEALTH_COST
        final int healthCost = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALTH_COST, 0, false) - (SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALTH_COST_REDUCE, mana, true) * hero.getLevel(this));
        if (healthCost > 0) {
            description += " HP:" + healthCost;
        }

        //STAMINA
        final int staminaCost = SkillConfigManager.getUseSetting(hero, this, SkillSetting.STAMINA, 0, false) - (SkillConfigManager.getUseSetting(hero, this, SkillSetting.STAMINA_REDUCE, 0, false) * hero.getLevel(this));
        if (staminaCost > 0) {
            description += " FP:" + staminaCost;
        }

        //DELAY
        final int delay = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DELAY.node(), 0, false) / 1000;
        if (delay > 0) {
            description += " W:" + delay + "s";
        }

        //EXP
        final int exp = SkillConfigManager.getUseSetting(hero, this, SkillSetting.EXP.node(), 0, false);
        if (exp > 0) {
            description += " XP:" + exp;
        }
        return description;
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();
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
        final Player player = hero.getPlayer();
        final Hero enemy = this.plugin.getCharacterManager().getHero((Player) target);

        final double maxDamage = SkillConfigManager.getUseSetting(hero, this, "max-damage", 0, false);
        final double damageMod = SkillConfigManager.getUseSetting(hero, this, "damage-modifier", 1.0, false) + (SkillConfigManager.getUseSetting(hero, this, "damage-modifier-increase", 0.0, false) * hero.getLevel(this));
        double damage = (enemy.getMaxMana() - enemy.getMana()) * damageMod;
        if ((maxDamage != 0) && (damage > maxDamage)) {
            damage = maxDamage;
        }
        this.addSpellTarget(target, hero);
        damageEntity(target, player, damage, DamageCause.MAGIC);
        //target.damage(damage, player);
        this.broadcastExecuteText(hero, target);
        return SkillResult.NORMAL;
    }

}
