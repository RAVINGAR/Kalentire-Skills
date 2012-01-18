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

public class SkillForcePush extends TargettedSkill {

    public SkillForcePush(Heroes plugin) {
        super(plugin, "Forcepush");
        setDescription("Forces your target way from you.");
        setUsage("/skill forcepush <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill forcepush", "skill fpush");
        setTypes(SkillType.FORCE, SkillType.SILENCABLE, SkillType.DAMAGING, SkillType.HARMFUL);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("horizontal-power", 1);
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

        double xDir = targetLoc.getX() - targetLoc.getX();
        double zDir = targetLoc.getZ() - playerLoc.getZ();
        double hPower = SkillConfigManager.getUseSetting(hero, this, "horizontal-power", 1.0, false);
        Vector v = new Vector(xDir, 0, zDir).normalize().multiply(hPower / 12).setY(.3);
        target.setVelocity(v);

        broadcastExecuteText(hero, target);
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }

}
