package com.herocraftonline.heroes.characters.skill.skills;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.server.EntityLiving;
import net.minecraft.server.MobEffect;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillSetting;
import com.herocraftonline.heroes.characters.skill.SkillType;

public class SkillFauxBomb extends ActiveSkill {
    private Map<Integer, Player> sheepMap = new HashMap<Integer, Player>();

    private class SkillListener implements Listener {
		@EventHandler
        public void onEntityDamage(EntityDamageEvent event) {
            Entity entity = event.getEntity();

            if (entity instanceof Sheep) {
                if (event.getCause() != EntityDamageEvent.DamageCause.POISON) {
                    explodeSheep((Sheep) entity);
                }
            }
        }
    }

    public SkillFauxBomb(Heroes plugin) {
        super(plugin, "FauxBomb");
        setDescription("You spawn a diseased explosive sheep.");
        setUsage("/skill fauxbomb <target>");
        setArgumentRange(0, 1);
        setIdentifiers("skill fauxbomb");
        setTypes(SkillType.HARMFUL, SkillType.EARTH, SkillType.SUMMON, SkillType.SILENCABLE);
        Bukkit.getPluginManager().registerEvents(new SkillListener(), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(SkillSetting.DAMAGE.node(), 0);
        node.set("fuse-time", 5000);
        node.set("velocity", 1.0);
        return node;
    }

    @Override
    public String getDescription(Hero hero) {
        int damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 10, false);
        return getDescription().replace("$1", damage + "");
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        final Player player = hero.getPlayer();
        Vector pLoc = player.getLocation().toVector();
        Vector direction = player.getLocation().getDirection();
        Vector spawnLoc = pLoc.add(direction);
        final World world = player.getWorld();

        final LivingEntity sheep = (LivingEntity) world.spawnEntity(spawnLoc.toLocation(world), EntityType.SHEEP);
        sheepMap.put(sheep.getEntityId(), player);

        EntityLiving cbSheep = ((CraftLivingEntity) sheep).getHandle();
        cbSheep.addEffect(new MobEffect(19, 10000, 0));
        cbSheep.setHealth(5000);

        double velocity = SkillConfigManager.getUseSetting(hero, this, "velocity", 1.0, false);
        sheep.setVelocity(direction.multiply(velocity).add(new Vector(0, 0.15, 0)));

        int fuse = SkillConfigManager.getUseSetting(hero, this, "fuse-time", 5000, true);
        //final int damage = SkillConfigManager.getUseSetting(hero, this, Setting.DAMAGE, 10, false);

        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            @Override
            public void run() {
                explodeSheep(sheep);
            }
        }, fuse / 1000 * 20);

        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

    private void explodeSheep(LivingEntity sheep) {
        int id = sheep.getEntityId();
        if (sheepMap.containsKey(id)) {
            Player player = sheepMap.get(id);
            Hero hero = plugin.getCharacterManager().getHero(player);
            int damage = 10;
            if (hero != null) {
                damage = SkillConfigManager.getUseSetting(hero, this, SkillSetting.DAMAGE, 10, false);
            }

            if (!sheep.isDead()) {
                sheep.getWorld().createExplosion(sheep.getLocation(), 0.0F, false);
                sheep.damage(20000);

                List<Entity> nearby = sheep.getNearbyEntities(5, 5, 5);
                for (Entity entity : nearby) {
                    if (entity instanceof LivingEntity) {
                        LivingEntity livingEntity = (LivingEntity) entity;
                        livingEntity.setNoDamageTicks(0);
                        damageEntity(livingEntity, player, damage, EntityDamageEvent.DamageCause.MAGIC);
                    }
                }
            }

            sheepMap.remove(id);
        }
    }
}
