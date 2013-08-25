package com.herocraftonline.heroes.characters.skill.unfinishedskills;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.characters.skill.VisualEffect;

public class SkillBolt extends TargettedSkill {
    // This is for Firework Effects
    public VisualEffect fplayer = new VisualEffect();
    
    public SkillBolt(Heroes plugin) {
        super(plugin, "Bolt");
        setDescription("Calls a bolt of lightning down on the target dealing $1 damage.");
        setUsage("/skill bolt");
        setArgumentRange(0, 0);
        setIdentifiers("skill bolt");
        setTypes(SkillType.LIGHTNING, SkillType.SILENCABLE, SkillType.DAMAGING, SkillType.HARMFUL);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DAMAGE.node(), 4);
        node.set(SkillSetting.DAMAGE_INCREASE.node(), 0);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        target.getWorld().strikeLightningEffect(target.getLocation());
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 4, false);
        damage += (SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE, 0, false) * hero.getSkillLevel(this));
        plugin.getDamageManager().addSpellTarget(target, hero, this);
        damageEntity(target, player, damage, DamageCause.MAGIC, false);

        broadcastExecuteText(hero, target);
        // this is our fireworks shit
        try {
            fplayer.playFirework(player.getWorld(), 
            		target.getLocation().add(0,1.5,0), 
            		FireworkEffect.builder()
            		.flicker(false)
            		.trail(false)
            		.with(FireworkEffect.Type.BALL)
            		.withColor(Color.YELLOW)
            		.withFade(Color.SILVER)
            		.build());
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(Hero hero) {
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 4, false);
        damage += (int) (SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE, 0, false) * hero.getSkillLevel(this));
        return getDescription().replace("$1", damage + "");
    }
}
