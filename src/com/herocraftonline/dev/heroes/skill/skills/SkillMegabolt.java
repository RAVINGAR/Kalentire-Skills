package com.herocraftonline.dev.heroes.skill.skills;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.api.SkillResult;
import com.herocraftonline.dev.heroes.hero.Hero;
import com.herocraftonline.dev.heroes.skill.SkillConfigManager;
import com.herocraftonline.dev.heroes.skill.SkillType;
import com.herocraftonline.dev.heroes.skill.TargettedSkill;
import com.herocraftonline.dev.heroes.util.Setting;

public class SkillMegabolt extends TargettedSkill {

    public SkillMegabolt(Heroes plugin) {
        super(plugin, "Megabolt");
        setDescription("Calls down multiple bolts of lightning centered on the target.");
        setUsage("/skill mbolt <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill megabolt", "skill mbolt");
        setTypes(SkillType.LIGHTNING, SkillType.DAMAGING, SkillType.SILENCABLE, SkillType.HARMFUL);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(Setting.DAMAGE.node(), 4);
        node.set(Setting.RADIUS.node(), 5);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        int range = SkillConfigManager.getUseSetting(hero, this, Setting.RADIUS, 5, false);
        int damage = SkillConfigManager.getUseSetting(hero, this, Setting.DAMAGE, 4, false);

        // Damage the first target
        addSpellTarget(target, hero);
        target.getWorld().strikeLightningEffect(target.getLocation());
        damageEntity(target, player, damage, DamageCause.ENTITY_ATTACK);

        for (Entity entity : target.getNearbyEntities(range, range, range)) {
            if (entity instanceof LivingEntity && !entity.equals(player)) {
                // Check if the target is damagable
                if (!damageCheck(player, (LivingEntity) entity)) {
                    continue;
                }

                
                entity.getWorld().strikeLightningEffect(entity.getLocation());
                addSpellTarget(entity, hero);
                damageEntity((LivingEntity) entity, player, damage, DamageCause.ENTITY_ATTACK);
            }
        }

        broadcastExecuteText(hero, target);
        return SkillResult.NORMAL;
    }

}
