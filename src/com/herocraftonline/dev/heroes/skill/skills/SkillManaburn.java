package com.herocraftonline.dev.heroes.skill.skills;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.api.SkillResult;
import com.herocraftonline.dev.heroes.hero.Hero;
import com.herocraftonline.dev.heroes.skill.SkillConfigManager;
import com.herocraftonline.dev.heroes.skill.SkillType;
import com.herocraftonline.dev.heroes.skill.TargettedSkill;
import com.herocraftonline.dev.heroes.util.Messaging;

public class SkillManaburn extends TargettedSkill {

    public SkillManaburn(Heroes plugin) {
        super(plugin, "Manaburn");
        setDescription("Removes $1 mana from the target and gives it to you.");
        setUsage("/skill manaburn");
        setArgumentRange(0, 1);
        setIdentifiers("skill manaburn", "skill mburn");
        setTypes(SkillType.DAMAGING, SkillType.SILENCABLE, SkillType.MANA, SkillType.HARMFUL);
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
        if (!(target instanceof Player))
            return SkillResult.INVALID_TARGET;

        Hero tHero = plugin.getHeroManager().getHero((Player) target);

        int transferamount = SkillConfigManager.getUseSetting(hero, this, "transfer-amount", 20, false);
        if (tHero.getMana() > transferamount) {
            if (hero.getMana() + transferamount > hero.getMaxMana()) {
                transferamount = hero.getMaxMana() - hero.getMana();
            }
            tHero.setMana(tHero.getMana() - transferamount);
            broadcastExecuteText(hero, target);
            return SkillResult.NORMAL;
        } else {
            Messaging.send(player, "Target does not have enough mana!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }
    }

    @Override
    public String getDescription(Hero hero) {
        int mana = SkillConfigManager.getUseSetting(hero, this, "transfer-amount", 20, false);
        return getDescription().replace("$1", mana + "");
    }
}
