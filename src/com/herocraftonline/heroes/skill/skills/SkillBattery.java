package com.herocraftonline.heroes.skill.skills;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.SkillResult.ResultType;
import com.herocraftonline.heroes.hero.Hero;
import com.herocraftonline.heroes.skill.SkillConfigManager;
import com.herocraftonline.heroes.skill.SkillType;
import com.herocraftonline.heroes.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Messaging;

public class SkillBattery extends TargettedSkill {

    public SkillBattery(Heroes plugin) {
        super(plugin, "Battery");
        setDescription("You grant $1 of your mana to your target.");
        setUsage("/skill battery");
        setArgumentRange(0, 1);
        setTypes(SkillType.SILENCABLE, SkillType.MANA);
        setIdentifiers("skill battery");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("transfer-amount", 20);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
    	Player player = hero.getPlayer();
        if (!(target instanceof Player) || player.equals(target))
        	return SkillResult.INVALID_TARGET;

        Hero tHero = plugin.getHeroManager().getHero((Player) target);	

        int transferAmount = SkillConfigManager.getUseSetting(hero, this, "transfer-amount", 20, false);
        if (hero.getMana() > transferAmount) {
            if (tHero.getMana() + transferAmount > tHero.getMaxMana()) {
                transferAmount = tHero.getMaxMana() - tHero.getMana();
            }
            hero.setMana(hero.getMana() - transferAmount);
            tHero.setMana(tHero.getMana() + transferAmount);
            broadcastExecuteText(hero, target);
            return SkillResult.NORMAL;
        } else {
            Messaging.send(hero.getPlayer(), "You need at least $1 mana to transfer.", transferAmount);
            return new SkillResult(ResultType.LOW_MANA, false);
        }
    }

    @Override
    public String getDescription(Hero hero) {
        int amount = SkillConfigManager.getUseSetting(hero, this, "transfer-amount", 20, false);
        return getDescription().replace("$1", amount + "");
    }

}
