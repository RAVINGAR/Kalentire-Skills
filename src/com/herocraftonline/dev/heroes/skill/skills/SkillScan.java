package com.herocraftonline.dev.heroes.skill.skills;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.api.SkillResult;
import com.herocraftonline.dev.heroes.hero.Hero;
import com.herocraftonline.dev.heroes.skill.SkillType;
import com.herocraftonline.dev.heroes.skill.TargettedSkill;
import com.herocraftonline.dev.heroes.util.Messaging;

public class SkillScan extends TargettedSkill {

    private final Heroes plugin;
    
    public SkillScan(Heroes plugin) {
        super(plugin, "Scan");
        this.plugin = plugin;
        setDescription("Reports the target's health");
        setUsage("/skill scan <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill scan");
        setTypes(SkillType.KNOWLEDGE, SkillType.STEALTHY);
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();
        if (target instanceof Player) {
            Hero tHero = plugin.getHeroManager().getHero((Player) target);
            Messaging.send(player, "$1 is a level $2 $3 and has $4 / $5 HP", tHero.getPlayer().getDisplayName(), tHero.getLevel(tHero.getHeroClass()), tHero.getHeroClass().getName(), (int) tHero.getHealth(), (int) tHero.getMaxHealth());
        } else {
            Messaging.send(player, "$1 has $2 / $3 HP", Messaging.getLivingEntityName(target), plugin.getDamageManager().getEntityHealth(target), plugin.getDamageManager().getEntityMaxHealth(target));
        }
        
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

}
