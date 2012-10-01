package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainHealthEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Setting;

public class SkillDuskblade extends TargettedSkill {

	public SkillDuskblade(Heroes plugin) {
		super(plugin, "Duskblade");
		setDescription("Your blade drains $1 health from target, restoring $2 of your own health.");
		setUsage("/skill duskblade <target>");
		setArgumentRange(0, 1);
		setIdentifiers("skill duskblade");
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
