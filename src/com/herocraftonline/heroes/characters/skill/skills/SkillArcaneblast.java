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

public class SkillArcaneblast extends TargettedSkill {
    // This is for Firework Effects
    public VisualEffect fplayer = new VisualEffect();

    public SkillArcaneblast(Heroes plugin) {
        super(plugin, "Arcaneblast");
        setDescription("You arcaneblast the target for $1 light damage.");
        setUsage("/skill arcaneblast");
        setArgumentRange(0, 0);
        setIdentifiers("skill arcaneblast");
        setTypes(SkillType.DAMAGING, SkillType.LIGHT, SkillType.SILENCABLE, SkillType.HARMFUL);
    }

    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(Setting.DAMAGE.node(), Integer.valueOf(10));
        return node;
    }

    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        int damage = SkillConfigManager.getUseSetting(hero, this, Setting.DAMAGE, 10, false);
        addSpellTarget(target, hero);
        damageEntity(target, player, damage, DamageCause.MAGIC);
        broadcastExecuteText(hero, target);

        // this is our fireworks shit
        try {
            fplayer.playFirework(player.getWorld(), target.getLocation().add(0,5,0), FireworkEffect.builder().flicker(false).trail(true).with(FireworkEffect.Type.CREEPER).withColor(Color.SILVER).withFade(Color.WHITE).build());
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return SkillResult.NORMAL;
    }

    public String getDescription(Hero hero) {
        int damage = SkillConfigManager.getUseSetting(hero, this, Setting.DAMAGE, 10, false);
        return getDescription().replace("$1", damage + "");
    }
}
