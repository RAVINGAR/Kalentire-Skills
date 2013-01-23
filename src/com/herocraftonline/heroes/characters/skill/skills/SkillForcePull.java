package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.Vector;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.characters.skill.VisualEffect;
import com.herocraftonline.heroes.util.Setting;

public class SkillForcePull extends TargettedSkill {
    // This is for Firework Effects
    public VisualEffect fplayer = new VisualEffect();
    public SkillForcePull(Heroes plugin) {
        super(plugin, "Forcepull");
        setDescription("Forces your target toward you.");
        setUsage("/skill forcepull <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill forcepull", "skill fpull");
        setTypes(SkillType.FORCE, SkillType.SILENCABLE, SkillType.DAMAGING, SkillType.HARMFUL, SkillType.INTERRUPT);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(Setting.DAMAGE.node(), 0);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        int damage = SkillConfigManager.getUseSetting(hero, this, Setting.DAMAGE, 0, false);
        if (damage > 0) {
            addSpellTarget(target, hero);
            damageEntity(target, player, damage, DamageCause.ENTITY_ATTACK);
        }        

        Location playerLoc = player.getLocation();
        Location targetLoc = target.getLocation();

        double xDir = (playerLoc.getX() - targetLoc.getX()) / 3;
        double zDir = (playerLoc.getZ() - targetLoc.getZ()) / 3;
        Vector v = new Vector(xDir, 0, zDir).multiply(0.5).setY(0.5);
        target.setVelocity(v);
        broadcastExecuteText(hero, target);
        // this is our fireworks shit
        try {
            fplayer.playFirework(player.getWorld(), target.getLocation().add(0,1.5,0), FireworkEffect.builder().flicker(true).trail(true).with(FireworkEffect.Type.BALL).withColor(Color.YELLOW).withFade(Color.NAVY).build());
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

}