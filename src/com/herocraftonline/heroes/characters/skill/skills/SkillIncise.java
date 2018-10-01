package com.herocraftonline.heroes.characters.skill.skills;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;

import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

// what the hell is this
public class SkillIncise
        extends TargettedSkill
{
    public SkillIncise(Heroes plugin)
    {
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
        node.set(SkillSetting.DAMAGE_INCREASE.node(), Integer.valueOf(0));
        node.set(SkillSetting.MAX_DISTANCE.node(), Integer.valueOf(4));
        node.set("damage-per-armor-point", Integer.valueOf(4));
        return node;
    }

    public SkillResult use(Hero hero, LivingEntity target, String[] args)
    {
        Player player = hero.getPlayer();
        int armorPoints = 0;
        double damagePerPoint = SkillConfigManager.getUseSetting(hero, this, "damage-per-armor-point", 10, false);
        damagePerPoint+= SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE_INCREASE, 4, false);
        //We will add up the armor points. Armor type order goes Leather, Chain, Iron, Diamond, Gold
        //first switch statement: Helmets!
        if(!(target instanceof Player))
            return SkillResult.INVALID_TARGET_NO_MSG;
        Player pt = (Player) target;
        if(pt.getInventory().getHelmet() != null){
            //FIXME raw data use
//            switch(pt.getInventory().getHelmet().getTypeId()){
//                case 298: armorPoints+= 1;
//                    break;
//                case 302: armorPoints+= 2;
//                    break;
//                case 306:armorPoints+= 2;
//                    break;
//                case 310:armorPoints+= 3;
//                    break;
//                case 314:armorPoints+= 2;
//                    break;
//                default: armorPoints+=0;	//They aren't wearing a helm.
//                    break;
//            }
        }
        //next switch statement: Chestplates!
        if(pt.getInventory().getChestplate() != null){
            //FIXME raw data use
//            switch(pt.getInventory().getChestplate().getTypeId()){
//                case 299: armorPoints+= 3;
//                    break;
//                case 303: armorPoints+= 5;
//                    break;
//                case 307:armorPoints+= 6;
//                    break;
//                case 311:armorPoints+= 8;
//                    break;
//                case 315:armorPoints+= 5;
//                    break;
//                default: armorPoints+=0;  //They aren't wearing a chest.
//                    break;
//            }
        }
//next switch statement: Leggings!
        if(pt.getInventory().getLeggings() != null){
            //FIXME raw data use
//            switch(pt.getInventory().getLeggings().getTypeId()){
//                case 300: armorPoints+= 2;
//                    break;
//                case 304: armorPoints+= 4;
//                    break;
//                case 308:armorPoints+= 5;
//                    break;
//                case 312:armorPoints+= 6;
//                    break;
//                case 316:armorPoints+= 3;
//                    break;
//                default: armorPoints+=0;  //They aren't wearing leggings.
//                    break;
//            }
        }
        //ayy were almost done. Boots are last!
        if(pt.getInventory().getBoots() != null){
            //FIXME raw data use
//            switch(pt.getInventory().getBoots().getTypeId()){
//                case 301: armorPoints+= 1;
//                    break;
//                case 305: armorPoints+= 1;
//                    break;
//                case 309:armorPoints+= 2;
//                    break;
//                case 313:armorPoints+= 3;
//                    break;
//                case 317:armorPoints+= 1;
//                    break;
//                default: armorPoints+=0;  //They aren't wearing boots
//                    break;
//            }

        }
        double damage = armorPoints*damagePerPoint;
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_ANVIL_LAND, 1, 1.5f);
        damageEntity(target,player,damage, DamageCause.MAGIC);
        broadcastExecuteText(hero, target);
        return SkillResult.NORMAL;
    }

    public String getDescription(Hero hero)
    {
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 10, false);



        double range = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 4, false);

        return getDescription().replace("$1", damage + "").replace("$4", range + "");

    }
}
