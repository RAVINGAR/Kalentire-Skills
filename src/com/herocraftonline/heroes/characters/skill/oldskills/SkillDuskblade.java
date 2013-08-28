package com.herocraftonline.heroes.characters.skill.oldskills;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainHealthEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.characters.skill.VisualEffect;

public class SkillDuskblade extends TargettedSkill {
    // This is for Firework Effects
    public VisualEffect fplayer = new VisualEffect();
	public SkillDuskblade(Heroes plugin) {
		super(plugin, "Duskblade");
		setDescription("Your blade drains $1 health from target, restoring $2 of your own health.");
		setUsage("/skill duskblade");
        setArgumentRange(0, 0);
		setIdentifiers("skill duskblade");
		setTypes(SkillType.DARK, SkillType.SILENCABLE, SkillType.DAMAGING, SkillType.HARMFUL);
	}

	@Override
	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection node = super.getDefaultConfig();
		node.set(SkillSetting.DAMAGE.node(), 4);
		node.set(SkillSetting.DAMAGE_INCREASE.node(), 0.0);
		node.set("heal-mult", .5);
		return node;
	}

	@Override
	public SkillResult use(Hero hero, LivingEntity target, String[] args) {
		Player player = hero.getPlayer();
		
		double absorbAmount = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 4, false);
		absorbAmount += SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE, 0.0, false) * hero.getSkillLevel(this);
		double healMult = SkillConfigManager.getUseSetting(hero, this, "heal-mult", .5, false);
		HeroRegainHealthEvent hrEvent = new HeroRegainHealthEvent(hero, (absorbAmount * healMult), this, hero);
		plugin.getServer().getPluginManager().callEvent(hrEvent);
		if (!hrEvent.isCancelled()) {
			hero.heal(hrEvent.getAmount());
		}
		addSpellTarget(target, hero);
		damageEntity(target, player, absorbAmount, DamageCause.MAGIC);

		broadcastExecuteText(hero, target);
        // this is our fireworks shit
        try {
            fplayer.playFirework(player.getWorld(), 
            		target.getLocation(), 
            		FireworkEffect.builder().
            		flicker(false).trail(false)
            		.with(FireworkEffect.Type.BURST)
            		.withColor(Color.GREEN)
            		.withFade(Color.PURPLE)
            		.build());
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(Hero hero) {
        int amount = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 4, false);
        amount += SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE, 0.0, false) * hero.getSkillLevel(this);
        double mult = SkillConfigManager.getUseSetting(hero, this, "heal-mult", .5, false);
        return getDescription().replace("$1", amount + "").replace("$2", (int) (mult * amount) + "");
    }
    
}