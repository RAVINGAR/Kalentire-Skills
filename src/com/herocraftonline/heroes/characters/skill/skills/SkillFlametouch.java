package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.effects.common.SilenceEffect;
import com.herocraftonline.heroes.characters.skill.*;
import com.herocraftonline.heroes.util.Util;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.scheduler.BukkitRunnable;

public class SkillFlametouch extends ActiveSkill
{
	private String applyText, expireText;

	private String skillPrefix = ChatColor.GRAY + " [" + ChatColor.DARK_GREEN + "Skill" + ChatColor.GRAY + "] ";

	public SkillFlametouch(Heroes plugin)
	{
		super(plugin, "Flametouch");
		setDescription("Imbues your strikes with fire for $1 seconds, causing you to deal $2 more damage and igniting your target for 2 seconds.");
		setUsage("/skill flametouch");
		setArgumentRange(0, 0);
		setIdentifiers("skill flametouch");
		setTypes(SkillType.BUFFING, SkillType.ABILITY_PROPERTY_FIRE);
		Bukkit.getServer().getPluginManager().registerEvents(new SkillDamageListener(this), plugin);
	}

	public ConfigurationSection getDefaultConfig() 
	{
		ConfigurationSection node = super.getDefaultConfig();

		node.set(SkillSetting.DAMAGE_INCREASE_PER_INTELLECT.node(), 0.1);
		node.set(SkillSetting.DAMAGE.node(), 5);

		node.set(SkillSetting.APPLY_TEXT.node(), " %hero%'s strikes are imbued with flame!");
		node.set(SkillSetting.EXPIRE_TEXT.node(), " %hero%'s strikes have returned to normal.");

		return node;
	}

	public void init()
	{
		super.init();

		applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, " %hero%'s strikes are imbued with flame!").replace("%hero%", "$1");
		expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, " %hero%'s strikes have returned to normal.").replace("%hero%", "$1");
	}

	public String getDescription(Hero hero) 
	{
		double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 5, false);
		damage += SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 0.1, false)
			* hero.getAttributeValue(AttributeType.INTELLECT);
		int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
		String formattedDuration = Util.decFormat.format(duration / 1000);
		return getDescription().replace("$1", formattedDuration).replace("$2", damage + "");
	}

	public SkillResult use(Hero hero, String[] args)
	{
		final Player player = hero.getPlayer();
		int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
		
		// fuck it
		applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, skillPrefix + " %hero%'s strikes are imbued with flame!")
				.replace("%hero%", ChatColor.WHITE + hero.getName() + ChatColor.GRAY);
		expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, skillPrefix + " %hero%'s strikes have returned to normal.")
				.replace("%hero%", ChatColor.WHITE + hero.getName() + ChatColor.GRAY);

		SilenceEffect silence = new SilenceEffect(this, player, duration);
		hero.addEffect(silence);

		hero.addEffect(new FlametouchEffect(plugin, this, hero.getPlayer(), duration));

		player.getWorld().spigot().playEffect(player.getLocation().add(0, 0.3, 0), Effect.FLAME, 0, 0, 1.2F, 1.6F, 1.2F, 0.5F, 100, 16);
		player.getWorld().spigot().playEffect(player.getLocation(), Effect.LAVA_POP, 0, 0, 1.2F, 0.2F, 1.2F, 0.5F, 50, 16);
		player.getWorld().playEffect(player.getLocation(), Effect.BLAZE_SHOOT, 2);

		final Player p = player;

		new BukkitRunnable() 
		{       
			private int ticks = 0;
			private int maxTicks = 40; // 4 times a second for 10 seconds
			private boolean isNoise = false; // toggles back and forth

			public void run() 
			{
				Location location = p.getLocation().add(0, 0.5, 0);
				p.getWorld().spigot().playEffect(location, Effect.FLAME, 0, 0, 0.3F, 0.5F, 0.3F, 0.0F, 25, 16);
				ticks++;

				if (isNoise == true)
				{
					p.getWorld().playSound(location, Sound.BLOCK_FIRE_AMBIENT, 1.2F, 0.8F); // BURRRRN
				}
				else if (isNoise == false) isNoise = true;

				if (ticks == maxTicks)
				{
					cancel();
				}
			}
		}.runTaskTimer(plugin, 1, 5);

		broadcastExecuteText(hero);

		return SkillResult.NORMAL;
	}

	public class SkillDamageListener implements Listener 
	{
		private final Skill skill;

		public SkillDamageListener(Skill skill) 
		{
			this.skill = skill;
		}

		@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
		public void onEntityDamage(EntityDamageEvent event) 
		{
			if ((!(event instanceof EntityDamageByEntityEvent)) || (!(event.getEntity() instanceof LivingEntity))) return;

			EntityDamageByEntityEvent subEvent = (EntityDamageByEntityEvent) event;

			if (event.getCause() != DamageCause.ENTITY_ATTACK) return;

			LivingEntity target = (LivingEntity) event.getEntity();			
			Player player;

			if (!(plugin.getDamageManager().isSpellTarget(target))) 
			{
				if (!(subEvent.getDamager() instanceof Player))	return;

				player = (Player) subEvent.getDamager();
			}
			else return;

			Hero hero = plugin.getCharacterManager().getHero(player);
			if (!hero.hasEffect("Flametouch")) return;
			extraDamage(hero, target);
		}

		private void extraDamage(final Hero hero, final LivingEntity target) 
		{
			Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() 
			{
				public void run() 
				{
					if (!(damageCheck(hero.getPlayer(), target))) return;

					double damage = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE, 5, false);
					damage += SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE_INCREASE_PER_INTELLECT, 0.1, false)
							* hero.getAttributeValue(AttributeType.INTELLECT);
					addSpellTarget(target, hero);
					damageEntity(target, hero.getPlayer(), damage, DamageCause.FIRE, false);
					
					target.setFireTicks(40);
	
					target.getWorld().spigot().playEffect(target.getLocation().add(0, 0.5, 0), Effect.FLAME, 0, 0, 0.3F, 0.5F, 0.3F, 0.5F, 25, 16);
					target.getWorld().playEffect(target.getLocation(), Effect.BLAZE_SHOOT, 2);
				}
			}, 2L);
		}
	}

	public class FlametouchEffect extends ExpirableEffect 
	{
		public FlametouchEffect(Heroes plugin, Skill skill, Player applier, long duration) 
		{
			super(skill, plugin, "Flametouch", applier, duration);

			types.add(EffectType.BENEFICIAL);
		}

		public void applyToHero(Hero hero) 
		{
			super.applyToHero(hero);
			broadcast(hero.getPlayer().getLocation(), applyText);
		}

		public void removeFromHero(Hero hero) 
		{
			super.removeFromHero(hero);
			broadcast(hero.getPlayer().getLocation(), expireText);
			hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1.3F, 0.8F);
			hero.getPlayer().getWorld().spigot().playEffect(hero.getPlayer().getLocation().add(0, 0.5, 0), Effect.LARGE_SMOKE, 0, 0, 0.4F, 0.2F, 0.4F, 0.3F, 45, 16);
		}
	}

}
