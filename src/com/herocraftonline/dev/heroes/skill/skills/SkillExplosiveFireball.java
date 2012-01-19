package com.herocraftonline.dev.heroes.skill.skills;


import net.minecraft.server.EntityFireball;
import net.minecraft.server.EntityLiving;
import net.minecraft.server.Vec3D;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftFireball;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.api.SkillResult;
import com.herocraftonline.dev.heroes.effects.common.CombustEffect;
import com.herocraftonline.dev.heroes.hero.Hero;
import com.herocraftonline.dev.heroes.skill.ActiveSkill;
import com.herocraftonline.dev.heroes.skill.Skill;
import com.herocraftonline.dev.heroes.skill.SkillConfigManager;
import com.herocraftonline.dev.heroes.skill.SkillType;
import com.herocraftonline.dev.heroes.util.Setting;
import com.herocraftonline.dev.heroes.util.Util;

public class SkillExplosiveFireball extends ActiveSkill {

    public SkillExplosiveFireball(Heroes plugin) {
        super(plugin, "ExplosiveFireball");
        setDescription("You shoot an explosive ball of fire which deals $1 damage.");
        setUsage("/skill fireball");
        setArgumentRange(0, 0);
        setIdentifiers("skill explosivefireball");
        setTypes(SkillType.FIRE, SkillType.SILENCABLE, SkillType.DAMAGING, SkillType.HARMFUL);
        Bukkit.getServer().getPluginManager().registerEvents(new SkillEntityListener(this), plugin);
    }

    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(Setting.DAMAGE.node(), 4);
        node.set("fire-ticks", 100);
        return node;
    }

    @Override
    public SkillResult use(Hero hero, String[] args) {
        Player player = hero.getPlayer();

        Block target = player.getTargetBlock(Util.transparentIds, 100);
        Location playerLoc = player.getLocation();

        double dx = target.getX() - playerLoc.getX();
        double height = 1;
        double dy = target.getY() + (height / 2.0F) - (playerLoc.getY() + (height / 2.0F));
        double dz = target.getZ() - playerLoc.getZ();

        CraftPlayer craftPlayer = (CraftPlayer) player;
        EntityLiving playerEntity = craftPlayer.getHandle();
        EntityFireball fireball = new EntityFireball(((CraftWorld) player.getWorld()).getHandle(), playerEntity, dx, dy, dz);
        fireball.isIncendiary = false;
        double d8 = 4D;
        Vec3D vec3d = Util.getLocation(player);
        fireball.locX = playerLoc.getX() + vec3d.a * d8;
        fireball.locY = playerLoc.getY() + (height / 2.0F) + 0.5D;
        fireball.locZ = playerLoc.getZ() + vec3d.c * d8;

        ((CraftWorld) player.getWorld()).getHandle().addEntity(fireball);

        broadcastExecuteText(hero);
        return SkillResult.NORMAL;
    }

    public class SkillEntityListener implements Listener {

        private final Skill skill;

        public SkillEntityListener(Skill skill) {
            this.skill = skill;
        }

        @EventHandler(priority = EventPriority.LOW)
        public void onEntityDamage(EntityDamageEvent event) {
            if (event.isCancelled()) {
                return;
            }

            if (event instanceof EntityDamageByEntityEvent) {
                EntityDamageByEntityEvent subEvent = (EntityDamageByEntityEvent) event;
                Entity attacker = subEvent.getDamager();
                if (attacker instanceof CraftFireball) {
                    CraftFireball fireball = (CraftFireball) attacker;
                    if (fireball.getShooter() instanceof Player) {
                        Entity entity = event.getEntity();
                        Player shooter = (Player) fireball.getShooter();
                        Hero hero = plugin.getHeroManager().getHero(shooter);
                        int damage = SkillConfigManager.getUseSetting(hero, skill, Setting.DAMAGE, 4, false);
                        entity.setFireTicks(SkillConfigManager.getUseSetting(hero, skill, "fire-ticks", 100, false));
                        if (entity instanceof Player) {
                            addSpellTarget(entity, hero);
                            plugin.getHeroManager().getHero((Player) entity).addEffect(new CombustEffect(skill, shooter));
                        } else if (entity instanceof LivingEntity) {
                            addSpellTarget(entity, hero);
                            plugin.getEffectManager().addEntityEffect((LivingEntity) entity, new CombustEffect(skill, shooter));
                        }
                        event.setDamage(damage);
                    }
                }
            }
        }
    }

    @Override
    public String getDescription(Hero hero) {
        int damage = SkillConfigManager.getUseSetting(hero, this, Setting.DAMAGE, 4, false);
        return getDescription().replace("$1", damage + "");
    }
}
