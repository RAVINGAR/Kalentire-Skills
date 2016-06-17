package com.herocraftonline.heroes.characters.skill.skills;

import java.util.ArrayList;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Messaging;

public class SkillManaFreeze extends TargettedSkill {

    private String applyText;
    private String expireText;

    public SkillManaFreeze(Heroes plugin) {
        super(plugin, "ManaFreeze");
        setDescription("Prevents your target from regenerating mana for $1 seconds.");
        setUsage("/skill manafreeze");
        setArgumentRange(0, 0);
        setIdentifiers("skill manafreeze", "skill mfreeze");
        setTypes(SkillType.SILENCEABLE, SkillType.DEBUFFING, SkillType.MANA_FREEZING, SkillType.AGGRESSIVE);
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
        return getDescription().replace("$1", duration / 1000 + "");
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();

        node.set(SkillSetting.MAX_DISTANCE.node(), 3);
        node.set(SkillSetting.MAX_DISTANCE_INCREASE_PER_INTELLECT.node(), 0.1);
        node.set(SkillSetting.DURATION.node(), 6000);
        node.set(SkillSetting.APPLY_TEXT.node(), Messaging.getSkillDenoter() + "%target% has stopped regenerating mana!");
        node.set(SkillSetting.EXPIRE_TEXT.node(), Messaging.getSkillDenoter() + "%target% is once again regenerating mana!");

        return node;
    }

    @Override
    public void init() {
        super.init();

        applyText = SkillConfigManager.getRaw(this, SkillSetting.APPLY_TEXT.node(), Messaging.getSkillDenoter() + "%target% has stopped regenerating mana!").replace("%target%", "$1");
        expireText = SkillConfigManager.getRaw(this, SkillSetting.EXPIRE_TEXT.node(), Messaging.getSkillDenoter() + "%target% is once again regenerating mana!").replace("%target%", "$1");
    }
    
    public ArrayList<Location> circle(Location centerPoint, int particleAmount, double circleRadius)
	{
		World world = centerPoint.getWorld();

		double increment = (2 * Math.PI) / particleAmount;

		ArrayList<Location> locations = new ArrayList<Location>();

		for (int i = 0; i < particleAmount; i++)
		{
			double angle = i * increment;
			double x = centerPoint.getX() + (circleRadius * Math.cos(angle));
			double z = centerPoint.getZ() + (circleRadius * Math.sin(angle));
			locations.add(new Location(world, x, centerPoint.getY(), z));
		}
		return locations;
	}

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        if (!(target instanceof Player))
        	return SkillResult.INVALID_TARGET;

        broadcastExecuteText(hero, target);

        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 6000, false);

        Hero targetHero = plugin.getCharacterManager().getHero((Player) target);
        targetHero.addEffect(new ManaFreezeEffect(this, hero.getPlayer(), duration));
        
		ArrayList<Location> particleLocations = circle(targetHero.getPlayer().getLocation().add(0, 0.5, 0), 36, 1.5);
		for (int i = 0; i < particleLocations.size(); i++)
		{
			targetHero.getPlayer().getWorld().spigot().playEffect(particleLocations.get(i), Effect.TILE_BREAK, org.bukkit.Material.ICE.getId(), 0, 0, 0.1F, 0, 0.1F, 1, 16);
		}
		
		targetHero.getPlayer().getWorld().spigot().playEffect(targetHero.getPlayer().getLocation(), Effect.WITCH_MAGIC, 0, 0, 0.5F, 1.0F, 0.5F, 0.1F, 35, 16);
		

        return SkillResult.NORMAL;

    }

    public class ManaFreezeEffect extends ExpirableEffect {

        public ManaFreezeEffect(Skill skill, Player applier, long duration) {
            super(skill, "ManaFreeze", applier, duration);

            types.add(EffectType.HARMFUL);
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
    }
}
