package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.SilenceEffect;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;


public class SkillSuffocate extends TargettedSkill
{
	public SkillSuffocate(Heroes plugin) {
		super(plugin, "Suffocate");
		setDescription("Suffocates your target (within 4 blocks) with dirt, dealing $1 damage and silencing them for 2.5 second(s).");
		setUsage("/skill suffocate");
		setArgumentRange(0, 0);
		setIdentifiers("skill suffocate");
		setTypes(SkillType.INTERRUPTING, SkillType.SILENCEABLE, SkillType.DAMAGING, SkillType.ABILITY_PROPERTY_EARTH,
				SkillType.AGGRESSIVE, SkillType.NO_SELF_TARGETTING);
	}

	@Override
	public ConfigurationSection getDefaultConfig() 
	{
		ConfigurationSection node = super.getDefaultConfig();
		node.set(SkillSetting.DAMAGE.node(), 15);
		node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 0.5);
		return node;
	}

	public SkillResult use(Hero hero, LivingEntity target, String[] args) 
	{
		Player player = hero.getPlayer();
		if (!damageCheck(player, target)) return SkillResult.INVALID_TARGET_NO_MSG;
		double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 15, false);
		damage += (SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 0.5, true)
				* hero.getAttributeValue(AttributeType.INTELLECT));

		addSpellTarget(target, hero);
		damageEntity(target, hero.getPlayer(), damage, DamageCause.MAGIC, false);
		CharacterTemplate targCT = this.plugin.getCharacterManager().getCharacter(target);
		SilenceEffect silence = new SilenceEffect(this, player, 2500);
		targCT.addEffect(silence);
		
		//target.getWorld().spigot().playEffect(target.getEyeLocation(), Effect.TILE_BREAK, Material.DIRT.getId(), 0, 0.2F, 0.2F, 0.2F, 0.1F, 45, 16);
		target.getWorld().spawnParticle(Particle.BLOCK_CRACK, target.getEyeLocation(), 45, 0.2, 0.2, 0.2, 0.1, Bukkit.createBlockData(Material.DIRT));
		target.getWorld().playSound(target.getEyeLocation(), Sound.BLOCK_GRASS_BREAK, 0.8F, 0.8F);
		target.getWorld().playSound(target.getEyeLocation(), Sound.BLOCK_GRAVEL_BREAK, 0.8F, 1.0F);
		target.getWorld().playSound(target.getEyeLocation(), Sound.ENTITY_BLAZE_AMBIENT, 1.4F, 1.0F);
		
		broadcastExecuteText(hero, target);
		return SkillResult.NORMAL;
	}

	public String getDescription(Hero hero) 
	{
		int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 15, false);
		damage += (double) (SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 0.5, true)
				* hero.getAttributeValue(AttributeType.INTELLECT));
		return getDescription().replace("$1", damage + "");
	}
}
