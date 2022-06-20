package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.skill.PassiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;

public class SkillImprovedArchery extends PassiveSkill
{
	public SkillImprovedArchery(Heroes plugin)
	{
		super(plugin, "ImprovedArchery");
        setDescription("Improved muscles and elevated dexterity cause your arrows to fly $1% faster.");
        setArgumentRange(0, 0);
        setTypes(SkillType.BUFFING);
        setEffectTypes(EffectType.BENEFICIAL, EffectType.PHYSICAL);
        Bukkit.getServer().getPluginManager().registerEvents(new ArcheryListener(this), plugin);
	}
	
    @Override
    public String getDescription(Hero hero) 
    {
    	double speedIncrease = SkillConfigManager.getUseSetting(hero, this, "speed-increase", 1.2, false);
    	String formattedSpd = String.valueOf(Math.ceil((speedIncrease - 1.0D) * 100));
        return getDescription().replace("$1", formattedSpd);
    }

    @Override
    public ConfigurationSection getDefaultConfig() 
    {
        ConfigurationSection node = super.getDefaultConfig();
        
        node.set("speed-increase", 1.2);

        return node;
    }
    
    public class ArcheryListener implements Listener
    {
    	private Skill skill;
    	public ArcheryListener(Skill imprArch)
    	{
    		skill = imprArch;
    	}
    	
    	@EventHandler
    	public void bowFired(EntityShootBowEvent event)
    	{
    		if (!(event.getEntity() instanceof Player) || !(event.getProjectile() instanceof Arrow) || event.isCancelled()) return;
    		Player player = (Player) event.getEntity();
    		Hero hero = plugin.getCharacterManager().getHero(player);
    		if (hero.canUseSkill(skill))
    		{
    			double speedMult = SkillConfigManager.getUseSetting(hero, skill, "speed-increase", 1.2, false);
    			event.getProjectile().setVelocity(event.getProjectile().getVelocity().multiply(speedMult));
    		}
    	}
    }
}
