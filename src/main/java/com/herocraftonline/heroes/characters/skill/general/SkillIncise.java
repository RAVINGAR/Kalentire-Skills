package com.herocraftonline.heroes.characters.skill.general;

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

        Player pt = (Player) target;
        double damage = getArmorPoints(pt)*damagePerPoint;
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_ANVIL_LAND, 1, 1.5f);
        damageEntity(target,player,damage, DamageCause.MAGIC);
        broadcastExecuteText(hero, target);
        return SkillResult.NORMAL;
    }

    private int getArmorPoints(Player pt) {
        int armorPoints = 0;
        if(pt.getInventory().getHelmet() != null){
            switch(pt.getInventory().getHelmet().getType()){
                case LEATHER_HELMET: armorPoints+= 1;
                    break;
                case CHAINMAIL_HELMET: armorPoints+= 2;
                    break;
                case IRON_HELMET: armorPoints+= 2;
                    break;
                case DIAMOND_HELMET: armorPoints+= 3;
                    break;
                case GOLDEN_HELMET: armorPoints+= 2;
                    break;
                case TURTLE_HELMET: armorPoints+= 2;
                    break;
                default: armorPoints+=0;	//They aren't wearing a helm.
                    break;
            }
        }
        //next switch statement: Chestplates!
        if(pt.getInventory().getChestplate() != null){
            switch(pt.getInventory().getChestplate().getType()){
                case LEATHER_CHESTPLATE: armorPoints+= 3;
                    break;
                case CHAINMAIL_CHESTPLATE: armorPoints+= 5;
                    break;
                case IRON_CHESTPLATE: armorPoints+= 6;
                    break;
                case DIAMOND_CHESTPLATE: armorPoints+= 8;
                    break;
                case GOLDEN_CHESTPLATE: armorPoints+= 5;
                    break;
                default: armorPoints+=0;  //They aren't wearing a chest.
                    break;
            }
        }
        //next switch statement: Leggings!
        if(pt.getInventory().getLeggings() != null){
            switch(pt.getInventory().getLeggings().getType()){
                case LEATHER_LEGGINGS: armorPoints+= 2;
                    break;
                case CHAINMAIL_LEGGINGS: armorPoints+= 4;
                    break;
                case IRON_LEGGINGS: armorPoints+= 5;
                    break;
                case DIAMOND_LEGGINGS: armorPoints+= 6;
                    break;
                case GOLDEN_LEGGINGS: armorPoints+= 3;
                    break;
                default: armorPoints+=0;  //They aren't wearing leggings.
                    break;
            }
        }
        //ayy were almost done. Boots are last!
        if(pt.getInventory().getBoots() != null){
            switch(pt.getInventory().getBoots().getType()){
                case LEATHER_BOOTS: armorPoints+= 1;
                    break;
                case CHAINMAIL_BOOTS: armorPoints+= 1;
                    break;
                case IRON_BOOTS: armorPoints+= 2;
                    break;
                case DIAMOND_BOOTS: armorPoints+= 3;
                    break;
                case GOLDEN_BOOTS: armorPoints+= 1;
                    break;
                default: armorPoints+=0;  //They aren't wearing boots
                    break;
            }

        }
        return armorPoints;
    }

    public String getDescription(Hero hero)
    {
        //TODO may need to fix description and properly align with used stats (e.g. damage per armor points)
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 10, false);

        double range = SkillConfigManager.getUseSetting(hero, this, SkillSetting.MAX_DISTANCE, 4, false);

        return getDescription().replace("$1", damage + "").replace("$4", range + "");

    }
}
