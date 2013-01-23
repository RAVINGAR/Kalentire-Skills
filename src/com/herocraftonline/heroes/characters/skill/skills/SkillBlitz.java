package com.herocraftonline.heroes.characters.skill.skills;

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
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.characters.skill.VisualEffect;
import com.herocraftonline.heroes.util.Setting;

public class SkillBlitz extends TargettedSkill {
    // This is for Firework Effects
    public VisualEffect fplayer = new VisualEffect();
    
    public SkillBlitz(Heroes plugin) {
        super(plugin, "Blitz");
        setDescription("Your skills in ninjutsu cause $1 lightning damage to your target.");
        setUsage("/skill blitz <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill blitz");
        setTypes(SkillType.LIGHTNING, SkillType.SILENCABLE, SkillType.DAMAGING, SkillType.HARMFUL);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(Setting.DAMAGE.node(), 4);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        target.getWorld().strikeLightningEffect(target.getLocation());
        int damage = SkillConfigManager.getUseSetting(hero, this, Setting.DAMAGE, 20, false);
        
        plugin.getDamageManager().addSpellTarget(target, hero, this);
        damageEntity(target, player, damage, DamageCause.MAGIC);

        broadcastExecuteText(hero, target);
        // this is our fireworks shit
        try {
            fplayer.playFirework(player.getWorld(), target.getLocation().add(0,1.5,0), FireworkEffect.builder().flicker(false).trail(true).with(FireworkEffect.Type.BALL).withColor(Color.YELLOW).withFade(Color.SILVER).build());
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(Hero hero) {
        int damage = SkillConfigManager.getUseSetting(hero, this, Setting.DAMAGE, 20, false);
        return getDescription().replace("$1", damage + "");
    }
}
