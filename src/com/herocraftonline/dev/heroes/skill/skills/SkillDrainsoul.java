package com.herocraftonline.dev.heroes.skill.skills;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.api.HeroRegainHealthEvent;
import com.herocraftonline.dev.heroes.api.SkillResult;
import com.herocraftonline.dev.heroes.hero.Hero;
import com.herocraftonline.dev.heroes.skill.SkillConfigManager;
import com.herocraftonline.dev.heroes.skill.SkillType;
import com.herocraftonline.dev.heroes.skill.TargettedSkill;
import com.herocraftonline.dev.heroes.util.Setting;

public class SkillDrainsoul extends TargettedSkill {

	public SkillDrainsoul(Heroes plugin) {
		super(plugin, "Drainsoul");
		setDescription("You drain $1 health from target, restoring $2 of your own health.");
		setUsage("/skill drainsoul <target>");
		setArgumentRange(0, 1);
		setIdentifiers("skill drainsoul");
		setTypes(SkillType.DARK, SkillType.SILENCABLE, SkillType.DAMAGING, SkillType.HARMFUL);
	}

	@Override
	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection node = super.getDefaultConfig();
		node.set(Setting.DAMAGE.node(), 4);
		node.set(Setting.DAMAGE_INCREASE.node(), 0.0);
		node.set("heal-mult", .5);
		return node;
	}

	@Override
	public SkillResult use(Hero hero, LivingEntity target, String[] args) {
		Player player = hero.getPlayer();
		
		int absorbAmount = SkillConfigManager.getUseSetting(hero, this, Setting.DAMAGE, 4, false);
		absorbAmount += SkillConfigManager.getUseSetting(hero, this, Setting.DAMAGE_INCREASE, 0.0, false) * hero.getSkillLevel(this);
		double healMult = SkillConfigManager.getUseSetting(hero, this, "heal-mult", .5, false);
		HeroRegainHealthEvent hrEvent = new HeroRegainHealthEvent(hero, (int) (absorbAmount * healMult), this);
		plugin.getServer().getPluginManager().callEvent(hrEvent);
		if (!hrEvent.isCancelled()) {
			hero.setHealth(hero.getHealth() + hrEvent.getAmount());
			hero.syncHealth();
		}
		addSpellTarget(target, hero);
		damageEntity(target, player, absorbAmount, DamageCause.MAGIC);

		broadcastExecuteText(hero, target);
		return SkillResult.NORMAL;
	}

    @Override
    public String getDescription(Hero hero) {
        int amount = SkillConfigManager.getUseSetting(hero, this, Setting.DAMAGE, 4, false);
        amount += SkillConfigManager.getUseSetting(hero, this, Setting.DAMAGE_INCREASE, 0.0, false) * hero.getSkillLevel(this);
        double mult = SkillConfigManager.getUseSetting(hero, this, "heal-mult", .5, false);
        return getDescription().replace("$1", amount + "").replace("$2", (int) (mult * amount) + "");
    }

}
