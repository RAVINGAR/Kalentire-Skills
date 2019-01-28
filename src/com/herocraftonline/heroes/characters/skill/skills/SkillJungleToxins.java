package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.WeaponDamageEvent;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.skill.*;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SkillJungleToxins extends ActiveSkill
{
	public SkillJungleToxins(Heroes plugin)
	{
		super(plugin, "JungleToxins");
		setDescription("You poison your weapons for $1 seconds. Any target hit is poisoned for the remaining duration of the effect, and will take $2 damage every 2 seconds.");
		setArgumentRange(0, 0);
		setUsage("/skill jungletoxins");
		setIdentifiers("skill jungletoxins");
		setTypes(SkillType.BUFFING, SkillType.DAMAGING);
		Bukkit.getServer().getPluginManager().registerEvents(new ToxinHitListener(this), plugin);
	}

	public String getDescription(Hero hero)
	{
		long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 14000, false);
		String formattedDuration = String.valueOf(duration / 1000);
		double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 5, false);
		damage += SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE_PER_DEXTERITY, 0.1, false) * hero.getAttributeValue(AttributeType.DEXTERITY);

		return getDescription().replace("$1", formattedDuration).replace("$2", damage + "");
	}

	public ConfigurationSection getDefaultConfig()
	{		
		ConfigurationSection node = super.getDefaultConfig();

		node.set(SkillSetting.DURATION.node(), 14000);
		node.set(SkillSetting.DAMAGE.node(), 5);
		node.set(SkillSetting.DAMAGE_INCREASE_PER_DEXTERITY.node(), 0.05);
		node.set(SkillSetting.APPLY_TEXT.node(), " %hero% poisons his weapons!");
		node.set(SkillSetting.EXPIRE_TEXT.node(), " %hero%'s weapons are no longer poisoned.");
		node.set("toxin-apply-text", " You have been poisoned by %hero%'s JungleToxins!");
		node.set("toxin-expire-text", " You are no longer poisoned.");

		return node;		
	}

	public SkillResult use(Hero hero, String[] args)
	{
		final Player player = hero.getPlayer();
		long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 14000, false);
		double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 5, false);
		damage += SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE, 0.1, false) * hero.getHeroLevel(this);

		String aText1 = SkillConfigManager.getUseSetting(hero, this, SkillSetting.APPLY_TEXT, " §f%hero%§7 poisons his weapons!").replace("%hero%", hero.getName());
		String eText1 = SkillConfigManager.getUseSetting(hero, this, SkillSetting.EXPIRE_TEXT, " §f%hero%§7's weapons are no longer poisoned.").replace("%hero%", hero.getName());
		String aText2 = SkillConfigManager.getUseSetting(hero, this, "toxin-apply-text", " You have been poisoned by §f%hero%§7's §fJungleToxins§7!").replace("%hero%", hero.getName());
		String eText2 = SkillConfigManager.getUseSetting(hero, this, "toxin-expire-text", " You are no longer poisoned.");

		JTApplierEffect jta = new JTApplierEffect(this, player, duration, aText1, eText1, aText2, eText2);
		hero.addEffect(jta);

		return SkillResult.NORMAL;
	}

	public class JTApplierEffect extends ExpirableEffect
	{
		private String at1;
		private String et1;
		public String at2;
		public String et2;

		public JTApplierEffect(Skill skill, Player applier, long duration, String at1, String et1, String at2, String et2)
		{
			super(skill, "JTApplierEffect", applier, duration);
			this.at1 = at1;
			this.et1 = et1;
			this.at2 = at2;
			this.et2 = et2;
		}

		public void applyToHero(Hero hero)
		{
			super.applyToHero(hero);
			broadcast(hero.getPlayer().getLocation(), at1);
		}

		public void removeFromHero(Hero hero)
		{
			super.removeFromHero(hero);
			broadcast(hero.getPlayer().getLocation(), et1);
		}
	}

	public class JungleToxinsEffect extends PeriodicExpirableEffect
	{
		private String aText;
		private String eText;
		private Hero applier;
		private double dmg;

		public JungleToxinsEffect(Skill skill, Hero applier, long duration, double dmgPerTick, String applyText, String expireText)
		{
			super(skill, "JungleToxins", applier.getPlayer(), 2000, duration);
			aText = applyText;
			eText = expireText;
			dmg = dmgPerTick;
			this.applier = applier;

			types.add(EffectType.DISPELLABLE);
			types.add(EffectType.POISON);
			types.add(EffectType.HARMFUL);
		}

		public void applyToHero(Hero hero)
		{
			super.applyToHero(hero);
			hero.getPlayer().sendMessage(aText);
		}

		public void removeFromHero(Hero hero)
		{
			super.removeFromHero(hero);
			hero.getPlayer().sendMessage(eText);
		}

		public void tickHero(Hero hero)
		{
			addSpellTarget(applier.getPlayer(), hero);
			damageEntity(hero.getPlayer(), applier.getPlayer(), dmg, DamageCause.MAGIC, false);
		}

		public void tickMonster(Monster monster) 
		{
			damageEntity(monster.getEntity(), applier.getPlayer(), dmg, DamageCause.MAGIC, false);
		}
	}

	public class ToxinHitListener implements Listener
	{
		private Skill skill;
		public ToxinHitListener(Skill skill)
		{
			this.skill = skill;
		}

		@EventHandler
		public void poisonDamageCancel(EntityDamageEvent event)
		{
			if (!(event.getEntity() instanceof LivingEntity)) return;
			LivingEntity ent = (LivingEntity) event.getEntity();
			if (!(event.getCause() == DamageCause.POISON)) return;
			if (plugin.getCharacterManager().getCharacter(ent).hasEffect("JungleToxins")) 
			{
				event.setCancelled(true);
			}
		}

		@EventHandler
		public void onWeaponHit(WeaponDamageEvent event)
		{
			if (!(event.getDamager() instanceof Hero) || !(event.getEntity() instanceof LivingEntity)) return;
			LivingEntity target = (LivingEntity) event.getEntity();
			Hero hero = (Hero) event.getDamager();
			if(event.getAttackerEntity() instanceof Wolf)
				return;
			if (!hero.hasEffect("JTApplierEffect")) return;
			JTApplierEffect aEffect = (JTApplierEffect) hero.getEffect("JTApplierEffect");

			long duration = aEffect.getRemainingTime();
			double damage = SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE, 5, false);
			damage += SkillConfigManager.getUseSetting(hero, skill, SkillSetting.DAMAGE_INCREASE_PER_DEXTERITY, 0.1, false)
					* hero.getAttributeValue(AttributeType.DEXTERITY);

			JungleToxinsEffect jt = new JungleToxinsEffect(skill, hero, duration, damage, aEffect.at2, aEffect.et2);
			CharacterTemplate targCT = plugin.getCharacterManager().getCharacter(target);
			if (targCT.hasEffect("JungleToxins")) return;
			int durationTicks = (int) ((duration / 1000) * 20);
			target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, durationTicks, 0, true, false));
			targCT.addEffect(jt);
			return;
		}
	}
}
