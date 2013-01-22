package com.herocraftonline.heroes.characters.skill.skills;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
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
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Setting;
import com.herocraftonline.heroes.util.Util;

public class SkillSpear extends TargettedSkill {
	
	public SkillSpear(Heroes plugin) {
        super(plugin, "Spear");
        setDescription("Spear your target, pulling him back towards you and dealing $1 damage");
        setUsage("/skill spear <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill spear");
        setTypes(SkillType.PHYSICAL, SkillType.DAMAGING, SkillType.HARMFUL, SkillType.INTERRUPT);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(Setting.DAMAGE.node(), 8);
        node.set("weapons", Util.shovels);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
        Player player = hero.getPlayer();
        
        Material item = player.getItemInHand().getType();
        if (!SkillConfigManager.getUseSetting(hero, this, "weapons", Util.shovels).contains(item.name())) {
            Messaging.send(player, "You can't use spear with that weapon!");
            return SkillResult.FAIL;
        }

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
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.HURT , 10.0F, 1.0F); 
        broadcastExecuteText(hero, target);
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(Hero hero) {
    	int damage = SkillConfigManager.getUseSetting(hero, this, Setting.DAMAGE, 8, false);
        return getDescription().replace("$1", damage + "");
    }

}
