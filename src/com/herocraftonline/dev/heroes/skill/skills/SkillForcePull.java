package com.herocraftonline.dev.heroes.skill.skills;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.Vector;

import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.api.SkillResult;
import com.herocraftonline.dev.heroes.hero.Hero;
import com.herocraftonline.dev.heroes.skill.SkillConfigManager;
import com.herocraftonline.dev.heroes.skill.SkillType;
import com.herocraftonline.dev.heroes.skill.TargettedSkill;
import com.herocraftonline.dev.heroes.util.Setting;

public class SkillForcePull extends TargettedSkill {

    public SkillForcePull(Heroes plugin) {
        super(plugin, "Forcepull");
        setDescription("Forces your target toward you");
        setUsage("/skill forcepull <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill forcepull", "skill fpull");
        setTypes(SkillType.FORCE, SkillType.SILENCABLE, SkillType.DAMAGING, SkillType.HARMFUL);
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
        
        double xDir = playerLoc.getX() - targetLoc.getX();
        double zDir = playerLoc.getZ() - targetLoc.getZ();
        double magnitude = Math.sqrt(xDir * xDir + zDir * zDir);
        double multiplier = playerLoc.distanceSquared(targetLoc) / 8;
        xDir = xDir / magnitude * multiplier;
        zDir = zDir / magnitude * multiplier;
        
        target.setVelocity(new Vector(xDir, 1, zDir));

        broadcastExecuteText(hero, target);
        return SkillResult.NORMAL;
    }

}