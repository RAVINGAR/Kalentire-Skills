package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.RootEffect;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.characters.skill.VisualEffect;
import com.herocraftonline.heroes.util.Setting;

public class SkillRoot extends TargettedSkill {
    // This is for Firework Effects
    public VisualEffect fplayer = new VisualEffect();
    public SkillRoot(Heroes plugin) {
        super(plugin, "Root");
        setDescription("You root your target in place for $1 seconds.");
        setUsage("/skill root <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill root");
        setTypes(SkillType.MOVEMENT, SkillType.DEBUFF, SkillType.SILENCABLE, SkillType.EARTH, SkillType.HARMFUL);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(Setting.DURATION.node(), 5000);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        long duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 5000, false);
        
        int damage = SkillConfigManager.getUseSetting(hero, this, Setting.DAMAGE, 1, false); //Adding to combat-tag the player
        Player player = hero.getPlayer(); //Adding for Fireworks
        addSpellTarget(target, hero); // Combat Tagging
        damageEntity(target, player, damage, DamageCause.MAGIC); //Combat tagging
        plugin.getCharacterManager().getCharacter(target).addEffect(new RootEffect(this, duration));
        broadcastExecuteText(hero, target);
        
        //This is the Sound stuff.
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.ZOMBIE_WOODBREAK , 0.8F, 1.0F); 
        // this is our fireworks shit
        try {
            fplayer.playFirework(player.getWorld(), target.getLocation().add(0,1.5,0), FireworkEffect.builder().flicker(true).trail(true).with(FireworkEffect.Type.BURST).withColor(Color.OLIVE).build());
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 5000, false);
        return getDescription().replace("$1", duration / 1000 + "");
    }
}
