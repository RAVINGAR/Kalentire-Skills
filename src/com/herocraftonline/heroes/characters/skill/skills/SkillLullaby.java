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
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.herocraftonline.heroes.util.GeometryUtil.circle;

public class SkillLullaby extends ActiveSkill 
{

	public SkillLullaby(Heroes plugin) 
	{
		super(plugin, "Lullaby");
		setDescription("Sing a lullaby that puts all enemies within $1 blocks to sleep for $2 seconds. During this time, they cannot move, and are blinded and nauseated. The effect is removed on damage.");
		setUsage("/skill lullaby");
		setArgumentRange(0, 0);
		setIdentifiers("skill lullaby");
		setTypes(SkillType.DAMAGING, SkillType.SILENCEABLE, SkillType.INTERRUPTING);
	}

	@Override
	public String getDescription(Hero hero) 
	{
		final double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5, true)
				+ (SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS_INCREASE_PER_CHARISMA, 0.06, true)
				* hero.getAttributeValue(AttributeType.CHARISMA));

		long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 3000, true);
		duration += SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE_PER_CHARISMA, 20, true)
			* hero.getAttributeValue(AttributeType.CHARISMA);
		String formattedDuration = String.valueOf(duration / 1000);

		return getDescription().replace("$1", radius + "").replace("$2", formattedDuration);
	}

	@Override
	public ConfigurationSection getDefaultConfig() 
	{
		ConfigurationSection node = super.getDefaultConfig();

		node.set(SkillSetting.DAMAGE.node(), 20);
		node.set(SkillSetting.DAMAGE_INCREASE_PER_CHARISMA.node(), 0.7);
		node.set(SkillSetting.RADIUS.node(), 5);
		node.set(SkillSetting.RADIUS_INCREASE_PER_CHARISMA.node(), 0.06);
		node.set(SkillSetting.DURATION.node(), 3000);
		node.set(SkillSetting.DURATION_INCREASE_PER_CHARISMA.node(), 20);
		node.set(SkillSetting.APPLY_TEXT.node(), " %hero%'s Lullaby makes you feel sleepy...");
		node.set(SkillSetting.EXPIRE_TEXT.node(), " You recover from the Lullaby!");

		return node;
	}

	@Override
	public SkillResult use(Hero hero, String[] args) 
	{
		Player player = hero.getPlayer();
		final Player p = player;

		final double radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5, true)
				+ (SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS_INCREASE_PER_CHARISMA, 0.06, true)
				* hero.getAttributeValue(AttributeType.CHARISMA));

		long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 3000, true);
		duration += SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION_INCREASE_PER_CHARISMA, 20, true)
				* hero.getAttributeValue(AttributeType.CHARISMA);

		String applyText = SkillConfigManager.getUseSetting(hero, this, SkillSetting.APPLY_TEXT, " §f%hero%§7's §fLullaby§7 makes you feel sleepy...").replace("%hero%", hero.getName());
		String expireText = SkillConfigManager.getUseSetting(hero, this, SkillSetting.EXPIRE_TEXT, " You recover from the §fLullaby§7!");

		List<Entity> entities = hero.getPlayer().getNearbyEntities(radius, radius, radius);
		for (Entity entity : entities) 
		{
			if (!(entity instanceof LivingEntity))
				continue;

			if (!damageCheck(player, (LivingEntity) entity))
				continue;

			LivingEntity target = (LivingEntity) entity;
			CharacterTemplate targCT = plugin.getCharacterManager().getCharacter(target);
			if (target instanceof Player)
			{
				LullabyEffect le = new LullabyEffect(this, 100, duration, player, applyText, expireText);
				targCT.addEffect(le);
			}
			else
			{
				RootEffect root = new RootEffect(this, player, 100, duration);
				targCT.addEffect(root);
			}

			//target.getWorld().spigot().playEffect(target.getLocation().add(0, 0.3, 0), Effect.NOTE, 0, 0, 0.4F, 0.6F, 0.4F, 0.0F, 25, 16);
			target.getWorld().spawnParticle(Particle.NOTE, target.getLocation().add(0, 0.3, 0), 25, 0.4, 0.6, 0.4, 0);
		}

		// visual
		new BukkitRunnable()
		{
			private int ticks = 0;
			private int maxTicks = 40;
			private Random rand = new Random();

			public void run()
			{
				if (ticks < maxTicks)
				{
					final ArrayList<Location> noteLocs = new ArrayList<Location>();
					for (int i = 0; i < radius; i++)
					{
						noteLocs.addAll(circle(p.getLocation().add(0, 0.5, 0), 12, (double) i));
					}
					int index = rand.nextInt(noteLocs.size());
					Location loc = noteLocs.get(index).clone().setDirection(new Vector(0, 1, 0));
					//loc.getWorld().spigot().playEffect(loc, Effect.NOTE, 0, 0, 2.2F, 1.2F, 2.2F, 0.5F, 10, 25);
					loc.getWorld().spawnParticle(Particle.NOTE, loc, 10, 2.2, 1.2, 2.2, 0.5, true);
					//loc.getWorld().playSound(loc, Sound.BLOCK_NOTE_PLING, 1.2F, 1.5F);
					loc.getWorld().playSound(loc, Sound.BLOCK_NOTE_BLOCK_PLING, 1.2f, 1.5f);
					ticks++;
				}
				else
				{
					cancel();
				}
			}
		}.runTaskTimer(plugin, 0, 2);

		player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GHAST_SCREAM, 1.0F, 1.0F);

		broadcastExecuteText(hero);

		return SkillResult.NORMAL;
	}

	private class LullabyEffect extends PeriodicExpirableEffect
	{
		private final String applyText;
		private final String expireText;

		private Location loc;

		public LullabyEffect(Skill skill, long period, long duration, Player applier, String applyText, String expireText)
		{
			super(skill, "Lullaby", applier, period, duration);
			this.applyText = applyText;
			this.expireText = expireText;
			this.types.add(EffectType.ROOT);
			this.types.add(EffectType.HARMFUL);
			this.types.add(EffectType.DISPELLABLE);
		}

		@Override
		public void applyToMonster(Monster monster) 
		{
			super.applyToMonster(monster);
		}

		@Override
		public void applyToHero(Hero hero) 
		{
			super.applyToHero(hero);
			final Player player = hero.getPlayer();
			loc = hero.getPlayer().getLocation();
			player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, (int) getDuration() / 50, 3)); // This actually makes the effects last roughly 5 minutes but they're removed after the effect wears off.
			player.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, (int) getDuration() / 50, 3));
			player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, (int) getDuration() / 50, 3));
			player.sendMessage(applyText);
		}

		@Override
		public void removeFromHero(Hero hero) 
		{
			super.removeFromHero(hero);
			final Player player = hero.getPlayer();
			if (player.hasPotionEffect(PotionEffectType.BLINDNESS)) player.removePotionEffect(PotionEffectType.BLINDNESS);
			if (player.hasPotionEffect(PotionEffectType.CONFUSION)) player.removePotionEffect(PotionEffectType.CONFUSION);
			if (player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) player.removePotionEffect(PotionEffectType.NIGHT_VISION);
			player.sendMessage(expireText);
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

		public void tickMonster(Monster monster) {
		}

	}
}
