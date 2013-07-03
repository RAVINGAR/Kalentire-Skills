package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

//src=http://pastie.org/private/fzesxgiubhrxtfy5pgs0wg
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;

public class SkillQuantumLeap extends TargettedSkill {

	public SkillQuantumLeap(Heroes plugin){
		super(plugin, "QuantumLeap");
		setDescription("Through quantum physics, change places with your target.");
		setUsage("/skill quantumleap");
        setArgumentRange(0, 0);
		setIdentifiers("skill quantumleap");
		setTypes(SkillType.SILENCABLE, SkillType.DAMAGING, SkillType.HARMFUL, SkillType.TELEPORT);
	}

	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection node = super.getDefaultConfig();
		node.set(SkillSetting.DAMAGE.node(), 4);
		return node;
	}

	public SkillResult use(Hero hero, LivingEntity target, String[] args) {
		if(!(target instanceof Player)) {return SkillResult.INVALID_TARGET;}
		Player player = hero.getPlayer();
		Location tlocation = target.getLocation();
		Location plocation = player.getLocation();
		player.teleport(tlocation);
		target.teleport(plocation);
        player.getWorld().playEffect(player.getLocation(), Effect.ENDER_SIGNAL, 3);
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.PORTAL , 0.5F, 1.0F); 
		broadcastExecuteText(hero, target);
		return SkillResult.NORMAL;
	}

	public String getDescription(Hero hero) {
		int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 4, false);
		return getDescription().replace("$1", damage + "");
	}
}
