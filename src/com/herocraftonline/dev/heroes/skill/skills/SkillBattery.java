package com.herocraftonline.dev.heroes.skill.skills;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.api.SkillResult;
import com.herocraftonline.dev.heroes.api.SkillResult.ResultType;
import com.herocraftonline.dev.heroes.hero.Hero;
import com.herocraftonline.dev.heroes.skill.SkillType;
import com.herocraftonline.dev.heroes.skill.TargettedSkill;
import com.herocraftonline.dev.heroes.util.Messaging;

public class SkillBattery extends TargettedSkill {

    public SkillBattery(Heroes plugin) {
        super(plugin, "Battery");
        setDescription("Gives your target mana");
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

        int transferAmount = getSetting(hero, "transfer-amount", 20, false);
        if (hero.getMana() > transferAmount) {
            if (tHero.getMana() + transferAmount > 100) {
                transferAmount = 100 - tHero.getMana();
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

}
