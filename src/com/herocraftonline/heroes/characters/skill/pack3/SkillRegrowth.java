package com.herocraftonline.heroes.characters.skill.pack3;

import java.util.ArrayList;

import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroRegainHealthEvent;
import com.herocraftonline.heroes.attributes.AttributeType;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicHealEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.chat.ChatComponents;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Util;

public class SkillRegrowth extends TargettedSkill {
	
    private String expireText;
    private String applyText;

    public SkillRegrowth(Heroes plugin) {
        super(plugin, "Regrowth");
        setDescription("You instantly restore $1(" + ChatColor.GRAY + "$4" + ChatColor.GOLD + ") health to your target, and then restore an additional $2(" + ChatColor.GRAY + "$5" + ChatColor.GOLD + ") of their health over the course of $3 seconds. Self-heal numbers are highlighted in gray.");
        setUsage("/skill regrowth <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill regrowth");
        setTypes(SkillType.BUFFING, SkillType.HEALING, SkillType.SILENCEABLE);
    }

    public String getDescription(Hero hero) {
        double healing = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING, 25, false);
        double healingIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING_INCREASE_PER_WISDOM, 0.875, false);
        healing += hero.getAttributeValue(AttributeType.WISDOM) * healingIncrease;

        int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 3000, false);
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 15000, false);

        double hot = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING_TICK, 29, false);
        double hotIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING_TICK_INCREASE_PER_WISDOM, 0.7, false);
        hot += hero.getAttributeValue(AttributeType.WISDOM) * hotIncrease;

        String formattedHealing = Util.decFormat.format(healing);
        String formattedHoT = Util.decFormat.format(hot * ((double) duration / (double) period));
        String formattedSelfHealing = Util.decFormat.format(healing * Heroes.properties.selfHeal);
        String formattedSelfHoT = Util.decFormat.format((hot * ((double) duration / (double) period)) * Heroes.properties.selfHeal);
        String formattedDuration = Util.decFormat.format(duration / 1000.0);

        return getDescription().replace("$1", formattedHealing).replace("$2", formattedHoT).replace("$3", formattedDuration).replace("$4", formattedSelfHealing).replace("$5", formattedSelfHoT);
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 12);
        node.set(SkillSetting.DURATION.node(), 15000);
        node.set(SkillSetting.PERIOD.node(), 3000);
        node.set(SkillSetting.HEALING.node(), 25);
        node.set(SkillSetting.HEALING_INCREASE_PER_WISDOM.node(), 0.875);
        node.set(SkillSetting.HEALING_TICK.node(), 29);
        node.set(SkillSetting.HEALING_TICK_INCREASE_PER_WISDOM.node(), 0.7);
        node.set(SkillSetting.APPLY_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% has been given the gift of regrowth!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), ChatComponents.GENERIC_SKILL + "%target% has lost the gift of regrowth!");

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT, ChatComponents.GENERIC_SKILL + "%target% has been given the gift of regrowth!").replace("%target%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT, ChatComponents.GENERIC_SKILL + "%target% has lost the gift of regrowth!").replace("%target%", "$1");
    }
    
    public ArrayList<Location> helix(Location center, double height, double radius, double particleInterval)
	{
		ArrayList<Location> locations = new ArrayList<Location>();
		
		for (double y = 0; y <= height; y += particleInterval) 
		{
			double x = center.getX() + (radius * Math.cos(y));
			double z = center.getZ() + (radius * Math.sin(y));
			locations.add(new Location(center.getWorld(), x, center.getY() + y, z));
		}
		return locations;
	}

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();
        if (!(target instanceof Player))
            return SkillResult.INVALID_TARGET;

        Hero targetHero = plugin.getCharacterManager().getHero((Player) target);

        if (target.getHealth() >= target.getMaxHealth()) {
            Messaging.send(player, "Target is already at full health.");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        double healing = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING, 25, false);
        double healingIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING_INCREASE_PER_WISDOM, 0.875, false);
        healing += hero.getAttributeValue(AttributeType.WISDOM) * healingIncrease;

        int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 3000, false);
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION.node(), 15000, false);

        double hot = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING_TICK, 29, false);
        double hotIncrease = SkillConfigManager.getUseSetting(hero, this, SkillSetting.HEALING_TICK_INCREASE_PER_WISDOM, 0.7, false);
        hot += hero.getAttributeValue(AttributeType.WISDOM) * hotIncrease;

        HeroRegainHealthEvent hrhEvent = new HeroRegainHealthEvent(targetHero, healing, this, hero);
        plugin.getServer().getPluginManager().callEvent(hrhEvent);
        if (hrhEvent.isCancelled()) {
            Messaging.send(player, "Unable to heal the target at this time!");
            return SkillResult.CANCELLED;
        }
        else
            targetHero.heal(hrhEvent.getDelta());

        RegrowthEffect rEffect = new RegrowthEffect(this, player, period, duration, hot);
        targetHero.addEffect(rEffect);

        ArrayList<Location> particleLocations = helix(targetHero.getPlayer().getLocation(), 3.0D, 2.0D, 0.05D);
        for (Location l : particleLocations)
        {
        	player.getWorld().spigot().playEffect(l, org.bukkit.Effect.HAPPY_VILLAGER, 0, 0, 0, 0, 0, 0, 1, 16);
        }
        
        player.getWorld().spigot().playEffect(player.getLocation(), Effect.HAPPY_VILLAGER, 0, 0, 0.5F, 1.0F, 0.5F, 0.1F, 35, 16);

        return SkillResult.NORMAL;
    }

    public class RegrowthEffect extends PeriodicHealEffect {

        public RegrowthEffect(Skill skill, Player applier, long period, long duration, double tickHealth) {
            super(skill, "Regrowth", applier, period, duration, tickHealth);

            types.add(EffectType.MAGIC);
            types.add(EffectType.DISPELLABLE);
        }

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), "    " + applyText, player.getName());
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), "    " + expireText, player.getName());
        }
        
        public void tickHero(Hero hero)
        {
        	super.tickHero(hero);
        	Player player = hero.getPlayer();
        	player.getWorld().spigot().playEffect(player.getLocation(), Effect.HAPPY_VILLAGER, 0, 0, 0.5F, 1.0F, 0.5F, 0.1F, 25, 16);
        }
    }
}