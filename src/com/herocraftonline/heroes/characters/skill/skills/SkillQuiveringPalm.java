package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.StunEffect;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;

public class SkillQuiveringPalm extends TargettedSkill {
	
	public SkillQuiveringPalm(Heroes plugin) {
		super(plugin, "QuiveringPalm");
		setDescription("You stun your target for $1 seconds, rendering them useless while dealing $2 damage");
		setUsage("/skill quiveringpalm");
        setArgumentRange(0, 0);
        setIdentifiers("skill quiveringpalm");
        setTypes(SkillType.LIGHT, SkillType.SILENCABLE, SkillType.DEBUFF, SkillType.DAMAGING, SkillType.HARMFUL, SkillType.INTERRUPT);
	}
	
	@Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DURATION.node(), 2500);
        node.set(SkillSetting.DAMAGE.node(), 120);
        return node;
    }

	@Override
	public SkillResult use(Hero hero, LivingEntity target, String[] args) {
		int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 2500, false);
		int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 120, false);
		damageEntity(target, hero.getEntity(), damage);
		plugin.getCharacterManager().getCharacter(target).addEffect(new StunEffect(this, duration));
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.ENDERMAN_TELEPORT , 0.5F, 1.0F);
		return SkillResult.NORMAL;
	}

	@Override
	public String getDescription(Hero hero) {
		int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 2500, false);
		int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 120, false);
		return getDescription().replace("$1", (duration / 1000) + "").replace("$2", damage + "");
	}

}
