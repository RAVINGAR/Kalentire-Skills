package com.herocraftonline.heroes.characters.skill.skills;

import java.util.List;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.Vector;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.CharacterTemplate;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.common.SlowEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;

public class SkillReckoning extends ActiveSkill {
	
	public SkillReckoning(Heroes plugin) {
        super(plugin, "Reckoning");
        setDescription("You pull in nearby enemies, dealing $1 damage and slowing them for $2 seconds");
        setUsage("/skill reckoning");
        setArgumentRange(0, 0);
        setIdentifiers("skill reckoning");
        setTypes(SkillType.DAMAGING, SkillType.PHYSICAL, SkillType.MOVEMENT, SkillType.HARMFUL, SkillType.INTERRUPT);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DAMAGE.node(), 50);
        node.set(SkillSetting.RADIUS.node(), 5);
        node.set(SkillSetting.DURATION.node(), 4000);
        node.set("slow-amount", 2);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5, false);
        double damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 10, false);
        int slowAmount = SkillConfigManager.getUseSetting(hero, this, "slow-amount", 2, false);
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
        Location playerLoc = player.getLocation();
        
        List<Entity> entities = hero.getPlayer().getNearbyEntities(radius, radius, radius);
        long currentTime = System.currentTimeMillis();
        //boolean hasHit = false;
        for (Entity entity : entities) {
            if (!(entity instanceof LivingEntity)) {
                continue;
            }
            LivingEntity target = (LivingEntity) entity;
            CharacterTemplate character = plugin.getCharacterManager().getCharacter(target);

            if (!damageCheck(player, target))
                continue;
            //hasHit = true;
            Location targetLoc = target.getLocation();

            addSpellTarget(target, hero);
            damageEntity(target, player, damage, DamageCause.ENTITY_ATTACK);
            if(character instanceof Hero) {
            	Hero enemy = (Hero)character;
            	if(enemy.getDelayedSkill() != null) {
	            	enemy.cancelDelayedSkill();
	                enemy.setCooldown("global", Heroes.properties.globalCooldown + currentTime);
            	}
            }
            
            character.addEffect(new SlowEffect(this, duration, slowAmount	, false, "", "", hero));
            
            double xDir = (playerLoc.getX() - targetLoc.getX()) / 3D;
            double zDir = (playerLoc.getZ() - targetLoc.getZ()) / 3D;
            Vector v = new Vector(xDir, 0, zDir).multiply(0.5).setY(0.5);
            target.setVelocity(v);
        }
        /*if(!hasHit) {
        	Messaging.send(player, "No valid targets nearby");
        	return SkillResult.INVALID_TARGET_NO_MSG;
        }
        */
        player.getWorld().playEffect(player.getLocation(), Effect.MOBSPAWNER_FLAMES, 3);
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.AMBIENCE_THUNDER , 0.4F, 1.0F);
        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

    @Override
    public String getDescription(Hero hero) {
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 10, false);
        int duration = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DURATION, 10000, false);
        return getDescription().replace("$1", damage + "").replace("$2", duration / 1000 + "");
    }

}
