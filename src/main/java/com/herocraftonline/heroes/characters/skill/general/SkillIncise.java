package com.herocraftonline.heroes.characters.skill.general;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

public class SkillIncise extends TargettedSkill {

    public SkillIncise(Heroes plugin) {
        super(plugin, "Incise");
        setDescription("You deal the percentage of your targets armor with your weapon as piercing damage.");
        setUsage("/skill incise");
        setArgumentRange(0, 0);
        setIdentifiers("skill incise");
        setTypes(SkillType.DAMAGING, SkillType.AGGRESSIVE, SkillType.ARMOR_PIERCING, SkillType.SILENCEABLE);
    }

    public ConfigurationSection getDefaultConfig()
    {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DAMAGE_INCREASE.node(), 0);
        node.set(SkillSetting.MAX_DISTANCE.node(), 4);
        node.set("damage-per-armor-point", 4);
        return node;
    }

    public SkillResult use(Hero hero, LivingEntity target, String[] args)
    {
        Player player = hero.getPlayer();
        double damagePerPoint = SkillConfigManager.getUseSetting(hero, this, "damage-per-armor-point", 10, false);
        damagePerPoint+= SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE, 0, false);
        //We will add up the armor points. Armor type order goes Leather, Chain, Iron, Diamond, Gold
        //first switch statement: Helmets!
        if(!(target instanceof Player)) {
            player.sendMessage("Incise only effects players.");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }

        // TODO consider adding other living entities (not just players) by using below method to get armor.
        //target.getEquipment().getHelmet();

        AttributeInstance armour = target.getAttribute(Attribute.GENERIC_ARMOR);
        if(armour == null) {
            return SkillResult.INVALID_TARGET;
        }

        double damage = armour.getValue() * damagePerPoint;
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_ANVIL_LAND, 1, 1.5f);
        damageEntity(target,player,damage, DamageCause.MAGIC);
        broadcastExecuteText(hero, target);
        return SkillResult.NORMAL;
    }

    public String getDescription(Hero hero)
    {
        //TODO may need to fix description and properly align with used stats (e.g. damage per armor points)
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 10, false);

        double range = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 4, false);

        return getDescription().replace("$1", damage + "").replace("$4", range + "");

    }
}
