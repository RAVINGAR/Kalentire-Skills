package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.SilenceEffect;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Messaging;

public class SkillGarrote extends TargettedSkill {

    public SkillGarrote(Heroes plugin) {
        super(plugin, "Garrote");
        setDescription("Deals $1 damage and silences the target for $2 seconds.");
        setUsage("/skill garrote");
        setArgumentRange(0, 0);
        setIdentifiers("skill garrote");
        setTypes(SkillType.PHYSICAL, SkillType.DEBUFF, SkillType.DAMAGING, SkillType.HARMFUL, SkillType.STEALTHY, SkillType.INTERRUPT);
    }
    
    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DAMAGE.node(), 4);
        node.set(SkillSetting.DURATION.node(), 4000);
        node.set(SkillSetting.MAX_DISTANCE.node(), 3);
        return node;
    }
    
    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();
        
        if (player.getItemInHand().getType() != Material.STRING) {
            Messaging.send(player, "You must have a piece of string to use garrote!");
            return SkillResult.FAIL;
        }
        
        if (!hero.hasEffect("Sneak") && !hero.hasEffect("Invisible")) {
            Messaging.send(player, "You must be sneaking or invisible to garrote!");
            return SkillResult.FAIL;
        }
        
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 4, false);
        damageEntity(target, player, damage, DamageCause.ENTITY_ATTACK);
        if (target instanceof Player) {
            long duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 4000, false);
            plugin.getCharacterManager().getHero((Player) target).addEffect(new SilenceEffect(this, duration));
        }
        broadcastExecuteText(hero, target);
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(Hero hero) {
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 4, false);
        return getDescription().replace("$1", damage + "").replace("$2", duration / 1000 + "");
    }

}
