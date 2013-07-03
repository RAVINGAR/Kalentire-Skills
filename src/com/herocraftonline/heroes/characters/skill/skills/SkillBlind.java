package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.common.BlindEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Messaging;

public class SkillBlind extends TargettedSkill {
	private String applyText;
	private String expireText;

	public SkillBlind(Heroes plugin) {
		super(plugin, "Blind");
		setDescription("You blind the target for $1 seconds.");
		setUsage("/skill blind");
		setArgumentRange(0, 0);
		setIdentifiers(new String[] { "skill blind" });
		setTypes(new SkillType[] { SkillType.DEBUFF, SkillType.ILLUSION, SkillType.HARMFUL, SkillType.DAMAGING });
	}

	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection node = super.getDefaultConfig();

		node.set(SkillSetting.DURATION.node(), Integer.valueOf(3000));
		node.set(SkillSetting.APPLY_TEXT.node(), "%target% has been blinded!");
		node.set(SkillSetting.EXPIRE_TEXT.node(), "%target% can see again!");
		return node;
	}

	public void init() {
		super.init();
		applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT.node(), "%target% has been blinded!").replace("%target%", "$1");
		expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT.node(), "%target% can see again!").replace("%target%", "$1");
	}

	public SkillResult use(Hero hero, LivingEntity target, String[] args) {
		Player player = hero.getPlayer();
		if (!(target instanceof Player)) {
			Messaging.send(player, "You must target a player!", new Object[0]);
			return SkillResult.INVALID_TARGET_NO_MSG;
		}
		int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 3000, false);
		BlindEffect effect = new CustomBlindEffect(this, duration);
		plugin.getCharacterManager().getHero((Player) target).addEffect(effect);

		hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.ENDERMAN_IDLE, 0.8F, 1.0F);
		return SkillResult.NORMAL;
	}

	public String getDescription(Hero hero) {
		int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 3000, false);
		return getDescription().replace("$1", duration / 1000 + "");
	}

	public class CustomBlindEffect extends BlindEffect {

		public CustomBlindEffect(Skill skill, long duration) {
			super(skill, duration, applyText, expireText);
			types.add(EffectType.BLIND);

			addMobEffect(9, (int) ((duration + 4000) / 1000) * 20, 3, false);
		}
	}
}