package com.herocraftonline.heroes.characters.skill.skills;
import org.bukkit.ChatColor;
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
import com.herocraftonline.heroes.util.Messaging;

public class SkillHarrow extends TargettedSkill {
    // This is for Firework Effects
    public VisualEffect fplayer = new VisualEffect();
    private String notBehindPlayerText;
	public SkillHarrow(Heroes plugin) {
		super(plugin, "Harrow");
		setDescription("You harrow your victim from behind, dealing $1 damage and restoring $2 of own health.");
		setUsage("/skill harrow <target>");
		setArgumentRange(0, 1);
		setIdentifiers("skill harrow");
		setTypes(SkillType.DARK, SkillType.SILENCABLE, SkillType.DAMAGING, SkillType.HARMFUL);
	}

	@Override
	public void init() {
		super.init();
		notBehindPlayerText = SkillConfigManager.getRaw(this, "not-behind-player-text", ChatColor.GRAY.toString() + "You are not behind the target!");
	}
	
	@Override
	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection node = super.getDefaultConfig();
		node.set(SkillSetting.DAMAGE.node(), 4);
		node.set(SkillSetting.DAMAGE_INCREASE.node(), 0.0);
		node.set(SkillSetting.MAX_DISTANCE.node(), 1.0);
		node.set(SkillSetting.MAX_DISTANCE_INCREASE.node(), 0.0);
		node.set("heal-mult", .5);
		node.set("not-behind-player-text", ChatColor.GRAY.toString() + "You are not behind the target!");
		return node;
	}

	@Override
	public SkillResult use(Hero hero, LivingEntity target, String[] args) {
		Player player = hero.getPlayer();
		//TIme to check being behind
		if (target.getLocation().getDirection().dot(hero.getPlayer().getLocation().getDirection()) <= 0) {
			Messaging.send(hero.getPlayer(), notBehindPlayerText);
			return SkillResult.FAIL;
		}
		//On with the show
		int absorbAmount = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 4, false);
		absorbAmount += SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE, 0.0, false) * hero.getSkillLevel(this);
		double healMult = SkillConfigManager.getUseSetting(hero, this, "heal-mult", .5, false);
		HeroRegainHealthEvent hrEvent = new HeroRegainHealthEvent(hero, (int) (absorbAmount * healMult), this, hero);
		plugin.getServer().getPluginManager().callEvent(hrEvent);
		if (!hrEvent.isCancelled()) {
			hero.heal(hrEvent.getAmount());
			//fixed for bukkit events for damage/health
		}
		addSpellTarget(target, hero);
		damageEntity(target, player, absorbAmount, DamageCause.MAGIC);

		broadcastExecuteText(hero, target);
        // this is our fireworks shit
        try {
            fplayer.playFirework(player.getWorld(), target.getLocation(), FireworkEffect.builder().flicker(false).trail(true).with(FireworkEffect.Type.BURST).withColor(Color.GREEN).withFade(Color.PURPLE).build());
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