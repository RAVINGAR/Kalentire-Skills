package com.herocraftonline.heroes.characters.skill.skills;
//http://pastie.org/private/ujku9azqzhmdgu5lki6ooa
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.RootEffect;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.characters.skill.VisualEffect;

public class SkillRoot extends TargettedSkill {

    public VisualEffect fplayer = new VisualEffect();
    public SkillRoot(Heroes plugin) {
        super(plugin, "Root");
        setDescription("You root your target in place for $1 seconds.");
        setUsage("/skill root");
        setArgumentRange(0, 0);
        setIdentifiers("skill root");
        setTypes(SkillType.MOVEMENT_PREVENTING, SkillType.DEBUFFING, SkillType.SILENCEABLE, SkillType.ABILITY_PROPERTY_EARTH, SkillType.AGGRESSIVE);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DURATION.node(), 5000);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
        int period = SkillConfigManager.getUseSetting(hero, this, SkillSetting.PERIOD, 100, false);
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 1, false); //Adding to combat-tag the player
        Player player = hero.getPlayer(); //Adding for Fireworks
        player.setWalkSpeed(0);
        addSpellTarget(target, hero); // Combat Tagging
        damageEntity(target, player, damage, DamageCause.MAGIC); //Combat tagging
        plugin.getCharacterManager().getCharacter(target).addEffect(new RootEffect(this, hero.getPlayer(), period, duration));
        
        broadcastExecuteText(hero, target);
        
        //This is the Sound stuff.
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.ENTITY_ZOMBIE_BREAK_DOOR_WOOD , 0.8F, 1.0F);
        // this is our fireworks shit
        /*try {
            fplayer.playFirework(player.getWorld(), target.getLocation().add(0,1.5,0), 
            		FireworkEffect.builder().flicker(true).trail(false)
            		.with(FireworkEffect.Type.BURST)
            		.withColor(Color.OLIVE)
            		.build());
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }*/
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 5000, false);
        return getDescription().replace("$1", duration / 1000 + "");
    }
}
