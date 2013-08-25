package com.herocraftonline.heroes.characters.skill.unfinishedskills;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
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

public class SkillMegabolt extends TargettedSkill {

    public SkillMegabolt(Heroes plugin) {
        super(plugin, "Megabolt");
        setDescription("Calls down multiple bolts of lightning centered on the target that deal $1 damage.");
        setUsage("/skill megabolt");
        setArgumentRange(0, 0);
        setIdentifiers("skill megabolt", "skill mbolt");
        setTypes(SkillType.LIGHTNING, SkillType.DAMAGING, SkillType.SILENCABLE, SkillType.HARMFUL);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DAMAGE.node(), 4);
        node.set(SkillSetting.RADIUS.node(), 5);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();

        int range = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5, false);
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 4, false);

        // Damage the first target
        addSpellTarget(target, hero);
        target.getWorld().strikeLightningEffect(target.getLocation());
        damageEntity(target, player, damage, DamageCause.MAGIC, false);

        for (Entity entity : target.getNearbyEntities(range, range, range)) {
            if (entity instanceof LivingEntity && !entity.equals(player)) {
                // Check if the target is damagable
                if (!damageCheck(player, (LivingEntity) entity)) {
                    continue;
                }

                
                entity.getWorld().strikeLightningEffect(entity.getLocation());
                addSpellTarget(entity, hero);
                damageEntity((LivingEntity) entity, player, damage, DamageCause.MAGIC);
            }
        }

        broadcastExecuteText(hero, target);
        return SkillResult.NORMAL;
    }
    
    @Override
    public String getDescription(Hero hero) {
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 4, false);
        return getDescription().replace("$1", damage + "");
    }
}
