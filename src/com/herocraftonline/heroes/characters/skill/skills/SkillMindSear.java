package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.effects.common.RootEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.util.List;

public class SkillMindSear extends ActiveSkill 
{
	private final String skillText = ChatColor.GRAY + "["+ChatColor.DARK_GREEN+"Skill"+ ChatColor.GRAY+ "] ";
	private final String defApplyText = skillText + "�f%target%�7's mind has shattered!";
	private final String defExpireText = skillText + "�f%target%�7 has recovered from the Mind Sear!";

	public SkillMindSear(Heroes plugin) 
	{
		super(plugin, "MindSear");
		setDescription("Shatter the minds of all enemies within $2 blocks, dealing $1 damage and rooting them in place for $3 second(s).");
		setUsage("/skill mindsear");
		setArgumentRange(0, 0);
		setIdentifiers("skill mindsear");
		setTypes(SkillType.DAMAGING, SkillType.SILENCEABLE, SkillType.INTERRUPTING);
	}

	@Override
	public String getDescription(Hero hero) 
	{
		int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 7, false);

		double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 20, false);
		double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 0.6, false);
		damage += (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

		String formattedDamage = Util.decFormat.format(damage);

		long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 1500, false);
		String formattedDuration = String.valueOf(duration / 1000);

		return getDescription().replace("$2", radius + "").replace("$1", formattedDamage).replace("$3", formattedDuration);
	}

	@Override
	public ConfigurationSection getDefaultConfig() 
	{
		ConfigurationSection node = super.getDefaultConfig();

		node.set(SkillSetting.DAMAGE.node(), 20);
		node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 0.6);
		node.set(SkillSetting.RADIUS.node(), Integer.valueOf(7));
		node.set(SkillSetting.DURATION.node(), 1500);

		return node;
	}

	@Override
	public SkillResult use(Hero hero, String[] args) 
	{
		Player player = hero.getPlayer();

		int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 7, false);

		double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 20, false);
		double damageIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 0.6, false);
		damage += (damageIncrease * hero.getAttributeValue(AttributeType.INTELLECT));

		int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 1500, false);

		List<Entity> entities = hero.getPlayer().getNearbyEntities(radius, radius, radius);
		for (Entity entity : entities)
		{
			if (!(entity instanceof LivingEntity)) 
			{
				continue;
			}

			if (!damageCheck(player, (LivingEntity) entity)) {
				continue;
			}

			LivingEntity target = (LivingEntity) entity;
			CharacterTemplate targCT = plugin.getCharacterManager().getCharacter(target);
			if (target instanceof Player)
			{
				String applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, defApplyText).replace("%target%", targCT.getName());
				String expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, defExpireText).replace("%target%", targCT.getName());
				MindSearEffect mindsear = new MindSearEffect(this, 100, duration, player, applyText, expireText);
				targCT.addEffect(mindsear);
			}
			else
			{
				RootEffect root = new RootEffect(this, player, 1, duration);
				targCT.addEffect(root);
			}

			addSpellTarget(target, hero);
			damageEntity(target, player, damage, DamageCause.MAGIC, false);

			//target.getWorld().spigot().playEffect(target.getLocation().add(0, 0.5, 0), Effect.TILE_BREAK, Material.ICE.getId(), 0, 0.3F, 0.3F, 0.3F, 0.0F, 25, 16);
			target.getWorld().spawnParticle(Particle.BLOCK_CRACK, target.getLocation().add(0, 0.5, 0), 25, 0.3, 0.3, 0.3, 0, Bukkit.createBlockData(Material.ICE));
			target.getWorld().playSound(target.getLocation(), Sound.BLOCK_GLASS_BREAK, 0.5F, 0.85F);
		}

		//player.getWorld().spigot().playEffect(player.getLocation().add(0, 0.3, 0), Effect.EXPLOSION_LARGE, 0, 0, 6.5F, 0.8F, 6.5F, 0.0F, 100, 16);
		player.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, player.getLocation().add(0, 0.3, 0), 100, 6.5, 0.8, 6.5, 0);

		player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.5F, 1.5F);
		player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GHAST_SCREAM, 0.7F, 0.85F);
		player.getWorld().playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.3F, 1.25F);

		broadcastExecuteText(hero);

		return SkillResult.NORMAL;
	}
	
	private class MindSearEffect extends PeriodicExpirableEffect
	{
		private final String applyText;
		private final String expireText;
		private final Player applier;

		private Location loc;

		public MindSearEffect(Skill skill, int period, int duration, Player applier, String applyText, String expireText) {
			super(skill, "MindSear", applier, period, duration);
			this.applier = applier;
			this.applyText = applyText;
			this.expireText = expireText;
			this.types.add(EffectType.ROOT);
			this.types.add(EffectType.HARMFUL);
			this.types.add(EffectType.DISPELLABLE);
		}

		@Override
		public void applyToMonster(Monster monster) {
			super.applyToMonster(monster);
			broadcast(monster.getEntity().getLocation(), applyText, monster.getName(), applier.getDisplayName());
		}

		@Override
		public void applyToHero(Hero hero) {
			super.applyToHero(hero);
			final Player player = hero.getPlayer();
			loc = hero.getPlayer().getLocation();
			broadcast(player.getLocation(), applyText, player.getDisplayName());
		}

		@Override
		public void removeFromHero(Hero hero) {
			super.removeFromHero(hero);
			final Player player = hero.getPlayer();
			broadcast(player.getLocation(), expireText, player.getDisplayName());
		}

		public void tickHero(Hero hero)
		{
			final Location location = hero.getPlayer().getLocation();
			if ((location.getX() != loc.getX()) || (location.getZ() != loc.getZ())) 
			{
				loc.setYaw(location.getYaw());
				loc.setPitch(location.getPitch());
				loc.setY(location.getY());
				hero.getPlayer().teleport(loc);
			}
		}

		public void tickMonster(Monster monster) 
		{
		}
	}
}
