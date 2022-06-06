package com.herocraftonline.heroes.characters.skill.general;

//http://pastie.org/private/t1jpidpc04doapzhli2ogg

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.RootEffect;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;

public class SkillRoot extends TargettedSkill {

    public SkillRoot(Heroes plugin) {
        super(plugin, "Root");
        this.setDescription("You root your target in place for $1 second(s).");
        this.setUsage("/skill root");
        this.setArgumentRange(0, 0);
        this.setIdentifiers("skill root");
        this.setTypes(SkillType.MOVEMENT_SLOWING, SkillType.DEBUFFING, SkillType.SILENCEABLE, SkillType.ABILITY_PROPERTY_EARTH, SkillType.AGGRESSIVE);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        final ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DURATION.node(), 5000);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        //Player player = hero.getPlayer(); //Adding for Fireworks
        //player.setWalkSpeed(0);// lets see if this works.
        final long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
        int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 100, false);
        this.plugin.getCharacterManager().getCharacter(target).addEffect(new RootEffect(this, hero.getPlayer(), period, duration));
        this.broadcastExecuteText(hero, target);
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(Hero hero) {
        final int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
        return this.getDescription().replace("$1", (duration / 1000) + "");
    }
}
