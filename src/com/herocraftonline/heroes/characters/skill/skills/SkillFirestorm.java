package com.herocraftonline.heroes.characters.skill.skills;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.VisualEffect;

public class SkillFirestorm extends ActiveSkill {

    public VisualEffect fplayer = new VisualEffect();
    public SkillFirestorm(Heroes plugin) {
        super(plugin, "Firestorm");
        setDescription("Firestorm deals $1 damage to all nearby enemies.");
        setUsage("/skill firestorm");
        setArgumentRange(0, 0);
        setIdentifiers("skill firestorm");
        setTypes(SkillType.DAMAGING, SkillType.FORCE, SkillType.SILENCABLE, SkillType.HARMFUL);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DAMAGE.node(), 1);
        node.set(SkillSetting.RADIUS.node(), 5);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();
        int radius = SkillConfigManager.getUseSetting(hero, this, SkillSetting.RADIUS, 5, false);
        List<Entity> entities = hero.getPlayer().getNearbyEntities(radius, radius, radius);
        for (Entity entity : entities) {
            if (!(entity instanceof LivingEntity)) {
                continue;
            }
            LivingEntity target = (LivingEntity) entity;

            if (!damageCheck(player, target))
                continue;

            int damage = SkillConfigManager.getUseSetting(hero, this, "damage", 1, false);
            //  fireworks 
            try {
                fplayer.playFirework(player.getWorld(), 
                		target.getLocation().add(0,1.5,0), 
                		FireworkEffect.builder()
                		.flicker(false)
                		.trail(false)
                		.with(FireworkEffect.Type.BALL)
                		.withColor(Color.ORANGE)
                		.withFade(Color.RED)
                		.build());
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
            // fireworks-end
            addSpellTarget(target, hero);
            damageEntity(target, player, damage, DamageCause.MAGIC);
        }
        // sound
        hero.getPlayer().getWorld().playSound(hero.getPlayer().getLocation(), Sound.EXPLODE , 0.5F, 1.0F);
        // sound-end
        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }
    
	protected List<Location> circle(Player player, Location loc, Integer r, Integer h, boolean hollow, boolean sphere, int plus_y) {
		List<Location> circleblocks = new ArrayList<Location>();
        int cx = loc.getBlockX();
        int cy = loc.getBlockY();
        int cz = loc.getBlockZ();
        for (int x = cx - r; x <= cx +r; x++)
            for (int z = cz - r; z <= cz +r; z++)
                for (int y = (sphere ? cy - r : cy); y < (sphere ? cy + r : cy + h); y++) {
                    double dist = (cx - x) * (cx - x) + (cz - z) * (cz - z) + (sphere ? (cy - y) * (cy - y) : 0);
                    if (dist < r*r && !(hollow && dist < (r-1)*(r-1))) {
                        Location l = new Location(loc.getWorld(), x, y + plus_y, z);
                        circleblocks.add(l);
                        }
                    }
     
        return circleblocks;
    }
	
    @Override
    public String getDescription(Hero hero) {
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 1, false);
        return getDescription().replace("$1", damage + "");
    }
}
