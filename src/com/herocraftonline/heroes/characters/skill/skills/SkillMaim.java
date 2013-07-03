package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.SlowEffect;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

public class SkillMaim extends TargettedSkill {
	private String applyText;
	private String expireText;

	public SkillMaim(Heroes plugin) {
		super(plugin, "Maim");
		setDescription("You Maim your target with your axe, dealing $1 damage and slowing them for $2 seconds.");
		setUsage("/skill maim");
		setArgumentRange(0, 0);
		setIdentifiers("skill maim");
		setTypes(SkillType.PHYSICAL, SkillType.DAMAGING, SkillType.HARMFUL, SkillType.MOVEMENT, SkillType.INTERRUPT);
	}

	public String getDescription(Hero hero) {
		int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 75, false);
		int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 2000, false) / 1000;

		return getDescription().replace("$1", damage + "").replace("$2", duration + "");
	}

	public ConfigurationSection getDefaultConfig() {
		ConfigurationSection node = super.getDefaultConfig();

		node.set("weapons", Util.axes);
		node.set("amplitude", Integer.valueOf(3));
		node.set(SkillSetting.DAMAGE.node(), Integer.valueOf(75));
		node.set(SkillSetting.DURATION.node(), Integer.valueOf(2000));
		node.set(SkillSetting.APPLY_TEXT.node(), "§7[§2Skill§7] %target% has been maimed by %hero%!");
		node.set(SkillSetting.EXPIRE_TEXT.node(), "§7[§2Skill§7] %target% is no longer slowed.");

		return node;
	}

	public void init() {
		super.init();

		applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, "§7[§2Skill§7] %target% has been maimed by %hero%!").replace("%target%", "$1").replace("%hero%", "$2");
		expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, "§7[§2Skill§7] %target% is no longer slowed!").replace("%target%", "$1");
	}

	public SkillResult use(Hero hero, LivingEntity target, String[] args) {
		Player player = hero.getPlayer();

		Material item = player.getItemInHand().getType();
		if (!SkillConfigManager.getUseSetting(hero, this, "weapons", Util.axes).contains(item.name())) {
			Messaging.send(player, "You can't use Maim with that weapon!", new Object[0]);
			return SkillResult.FAIL;
		}

		long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
		int amplitude = SkillConfigManager.getUseSetting(hero, this, "amplitude", 4, false);
		int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 40, false);

		// Create the effect and slow the target
		SlowEffect sEffect = new SlowEffect(this, duration, amplitude, false, applyText, expireText, hero);

		// Prep variables
		CharacterTemplate targCT = plugin.getCharacterManager().getCharacter((LivingEntity) target);

		// Damage and silence the target
		plugin.getDamageManager().addSpellTarget(target, hero, this);
		damageEntity(target, hero.getPlayer(), damage, EntityDamageEvent.DamageCause.MAGIC);
		targCT.addEffect(sEffect);

		hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.HURT, 0.8F, 1.0F);
		broadcastExecuteText(hero, target);
		return SkillResult.NORMAL;
	}
}