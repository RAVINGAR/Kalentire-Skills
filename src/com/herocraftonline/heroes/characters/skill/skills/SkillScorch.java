package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.SlowEffect;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import org.bukkit.Effect;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

public class SkillScorch extends TargettedSkill
{
	public SkillScorch(Heroes plugin)
	{
		super(plugin, "Scorch");
		setDescription("You deal $1 damage to your target and sear them with intense heat, slowing them for $2 seconds.");
		setUsage("/skill scorch");
		setArgumentRange(0, 0);
		setIdentifiers(new String[] { "skill scorch" });
		setTypes(new SkillType[] { SkillType.INTERRUPTING, SkillType.SILENCEABLE, SkillType.DAMAGING, SkillType.AGGRESSIVE, SkillType.NO_SELF_TARGETTING });
	}

	public ConfigurationSection getDefaultConfig()
	{
		ConfigurationSection node = super.getDefaultConfig();
		node.set(SkillSetting.DAMAGE.node(), Integer.valueOf(15));
		node.set(SkillSetting.DAMAGE_INCREASE.node(), Double.valueOf(0.5D));
		node.set(SkillSetting.DURATION.node(), 2000);
		node.set(SkillSetting.DURATION_INCREASE.node(), 40);
		return node;
	}

	public SkillResult use(Hero hero, LivingEntity target, String[] args)
	{
		Player player = hero.getPlayer();
		double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 15, false);
		damage += SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE, 0.5D, false) * hero.getSkillLevel(this);
		long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 2000, false);
		duration += SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE, 40, false) * hero.getSkillLevel(this);

		addSpellTarget(target, hero);
		damageEntity(target, hero.getPlayer(), damage, DamageCause.ENTITY_ATTACK, false);
		CharacterTemplate targCT = this.plugin.getCharacterManager().getCharacter(target);
		SlowEffect slow = new SlowEffect(this, "ScorchSlow", player, duration, 0, "", "");
		targCT.addEffect(slow);

		target.getWorld().spigot().playEffect(target.getEyeLocation(), Effect.FLAME, 0, 0, 0.2F, 0.2F, 0.2F, 0.3F, 45, 16);
		target.getWorld().spigot().playEffect(target.getEyeLocation(), Effect.LARGE_SMOKE, 0, 0, 0.2F, 0.2F, 0.2F, 0.3F, 25, 16);
		target.getWorld().playSound(target.getEyeLocation(), Sound.BLOCK_FIRE_AMBIENT, 1.3F, 1.0F);

		broadcast(player.getLocation(), " �f" + hero.getName() + " §7used §f" + this.getName() + " §7on §f" + target.getName() + "§7!" );
		return SkillResult.NORMAL;
	}

	public String getDescription(Hero hero)
	{
		int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 15, false);
		damage += (int)(SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE, 0.5D, false) * hero.getSkillLevel(this));
		long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 2000, false);
		duration += SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE, 40, false) * hero.getSkillLevel(this);
		String dur = String.valueOf(Math.ceil(duration / 1000));
		return getDescription().replace("$1", damage + "").replace("$2", dur);
	}
}
