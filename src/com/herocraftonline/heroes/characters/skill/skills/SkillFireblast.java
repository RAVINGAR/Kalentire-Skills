package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Setting;

public class SkillFireblast extends TargettedSkill {

    public SkillFireblast(Heroes plugin) {
        super(plugin, "Fireblast");
        setDescription("You blast the target for $1 fire damage.");
        setUsage("/skill fireblast");
        setArgumentRange(0, 0);
        setIdentifiers("skill fireblast");
        setTypes(SkillType.DAMAGING, SkillType.FIRE, SkillType.SILENCABLE, SkillType.HARMFUL);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(Setting.DAMAGE.node(), 10);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        int damage = SkillConfigManager.getUseSetting(hero, this, Setting.DAMAGE, 10, false);
        addSpellTarget(target, hero);
        damageEntity(target, player, damage, DamageCause.MAGIC);
        broadcastExecuteText(hero, target);
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(Hero hero) {
        int damage = SkillConfigManager.getUseSetting(hero, this, Setting.DAMAGE, 10, false);
        return getDescription().replace("$1", damage + "");
    }

}
